package cn.blockforge.generated.chunksmithchunksmithgu.net;

import cn.blockforge.generated.chunksmithchunksmithgu.ChunkSmithGuiMod;
import cn.blockforge.generated.chunksmithchunksmithgu.PanelState;
import cn.blockforge.generated.chunksmithchunksmithgu.PrereqStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 面板与服务器之间的两条通道：
 *  Handshake —— 登录时问服务端“有没有前置、什么版本、有哪些世界”；
 *  Heatmap   —— 让服务端扫 region 文件头，回传每个 32x32 区块的 1024bit 位图（体积小、不碰区块对象）。
 */
public final class Net {

    public static final ResourceLocation CHANNEL_NAME =
            new ResourceLocation(ChunkSmithGuiMod.MOD_ID, "panel_net");

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME, () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Net() {
    }

    public static void register(IEventBus modBus) {
        int i = 0;
        CHANNEL.registerMessage(i++, HandshakeC2S.class, HandshakeC2S::encode, HandshakeC2S::decode, Net::handleHandshake);
        CHANNEL.registerMessage(i++, HandshakeS2C.class, HandshakeS2C::encode, HandshakeS2C::decode, Net::handleHandshakeResp);
        CHANNEL.registerMessage(i++, HeatmapC2S.class, HeatmapC2S::encode, HeatmapC2S::decode, Net::handleHeatmap);
        CHANNEL.registerMessage(i++, HeatmapS2C.class, HeatmapS2C::encode, HeatmapS2C::decode, Net::handleHeatmapResp);
    }

    // ==================== 握手 ====================

    public static final class HandshakeC2S {
        static void encode(HandshakeC2S m, FriendlyByteBuf b) { }
        static HandshakeC2S decode(FriendlyByteBuf b) { return new HandshakeC2S(); }
    }

    public static final class HandshakeS2C {
        final String prereqId;      // 服务端前置模组 id（null=没有）
        final String prereqVersion; // 版本串
        final List<String> worlds;  // 可选世界/维度列表

        HandshakeS2C(String id, String v, List<String> w) { prereqId = id; prereqVersion = v; worlds = w; }

        static void encode(HandshakeS2C m, FriendlyByteBuf b) {
            b.writeBoolean(m.prereqId != null);
            if (m.prereqId != null) { b.writeUtf(m.prereqId); b.writeUtf(m.prereqVersion == null ? "" : m.prereqVersion); }
            b.writeVarInt(m.worlds.size());
            for (String w : m.worlds) b.writeUtf(w);
        }
        static HandshakeS2C decode(FriendlyByteBuf b) {
            String id = null, v = null;
            if (b.readBoolean()) { id = b.readUtf(64); v = b.readUtf(32); }
            int n = b.readVarInt();
            List<String> w = new ArrayList<>();
            for (int i = 0; i < n && i < 64; i++) w.add(b.readUtf(96));
            return new HandshakeS2C(id, v, w);
        }
    }

    private static void handleHandshake(HandshakeC2S m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        String id = null, v = null;
        for (String probe : new String[]{"chunksmith", "chunkysmith", "chunky"}) {
            var container = net.minecraftforge.fml.ModList.get().getModContainerById(probe);
            if (container.isPresent()) {
                id = container.get().getModInfo().getModId();
                v = container.get().getModInfo().getVersion().toString();
                break;
            }
        }
        List<String> worlds = new ArrayList<>();
        ServerPlayer sp = c.getSender();
        if (sp != null && sp.getServer() != null) {
            for (ResourceKey<Level> k : sp.getServer().levelKeys()) {
                worlds.add(k.location().toString());
            }
        }
        CHANNEL.reply(new HandshakeS2C(id, v, worlds), c);
        c.setPacketHandled(true);
    }

    /** 世界（维度）下拉列表，客户端缓存。 */
    public static volatile List<String> serverWorlds = null;

    private static void handleHandshakeResp(HandshakeS2C m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            serverWorlds = m.worlds;
            if (m.prereqId != null) {
                PrereqStatus.serverVersion = m.prereqVersion;
                if (PrereqStatus.localModId == null) {
                    // 服务端有、客户端没有：以服务端信息为准
                    PrereqStatus.localModId = m.prereqId;
                    PrereqStatus.localModName = m.prereqId;
                    PrereqStatus.localVersion = m.prereqVersion;
                }
                PrereqStatus.serverRejected = false;
            } else {
                PrereqStatus.serverVersion = null;
            }
        });
        c.setPacketHandled(true);
    }

    // ==================== 热力图 ====================

    public static final class HeatmapC2S {
        final String dim; final int cx, cz, half;
        HeatmapC2S(String d, int x, int z, int h) { dim = d; cx = x; cz = z; half = h; }
        static void encode(HeatmapC2S m, FriendlyByteBuf b) {
            b.writeUtf(m.dim); b.writeVarInt(m.cx); b.writeVarInt(m.cz); b.writeVarInt(m.half);
        }
        static HeatmapC2S decode(FriendlyByteBuf b) {
            return new HeatmapC2S(b.readUtf(128), b.readVarInt(), b.readVarInt(), b.readVarInt());
        }
    }

    /** 每个 region（32x32 区块）一张 1024bit 位图，按位存在与否。 */
    public static final class HeatmapS2C {
        final String dim; final int cx, cz, half;
        final int regions;            // 条目数
        final int[] rx, rz;           // region 坐标
        final byte[][] bits;          // 每 region 128 字节位图
        HeatmapS2C(String d, int x, int z, int h, int[] rx, int[] rz, byte[][] bits) {
            dim = d; cx = x; cz = z; half = h; regions = rx.length; this.rx = rx; this.rz = rz; this.bits = bits;
        }
        static void encode(HeatmapS2C m, FriendlyByteBuf b) {
            b.writeUtf(m.dim); b.writeVarInt(m.cx); b.writeVarInt(m.cz); b.writeVarInt(m.half);
            b.writeVarInt(m.regions);
            for (int i = 0; i < m.regions; i++) {
                b.writeVarInt(m.rx[i]); b.writeVarInt(m.rz[i]);
                b.writeBytes(m.bits[i]);
            }
        }
        static HeatmapS2C decode(FriendlyByteBuf b) {
            String d = b.readUtf(128);
            int x = b.readVarInt(), z = b.readVarInt(), h = b.readVarInt();
            int n = Math.min(b.readVarInt(), 65536);
            int[] rx = new int[n], rz = new int[n];
            byte[][] bits = new byte[n][];
            for (int i = 0; i < n; i++) {
                rx[i] = b.readVarInt(); rz[i] = b.readVarInt();
                bits[i] = new byte[128];
                b.readBytes(bits[i]);
            }
            return new HeatmapS2C(d, x, z, h, rx, rz, bits);
        }
    }

    /** 面板打开时向服务端问一次前置/世界信息（服务端没装模组则静默失败）。 */
    public static void requestHandshake() {
        try {
            CHANNEL.sendToServer(new HandshakeC2S());
        } catch (Throwable t) {
            serverWorlds = null;
        }
    }

    /** 客户端发起：单人直接本地扫；多人发请求给服务端。 */
    public static void requestHeatmap(String dim, int cx, int cz, int half) {
        PanelState.heatRequestSentMs = System.currentTimeMillis();
        PanelState.heatNoServer = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            // 单人：后台线程直接扫本地存档，不走网络
            Thread t = new Thread(() -> {
                try {
                    ScanResult r = scanRegions(mc.getSingleplayerServer(), dim, cx, cz, half);
                    PanelState.heat = r.toData(dim, cx, cz, half);
                } catch (Throwable e) {
                    PanelState.log(PanelState.Level.WARN, "热力图扫描失败: " + e);
                }
            }, "ChunkSmithPanel-Heatmap-SP");
            t.setDaemon(true);
            t.start();
            return;
        }
        try {
            CHANNEL.sendToServer(new HeatmapC2S(dim, cx, cz, half));
        } catch (Throwable t) {
            PanelState.heatNoServer = true;
            PanelState.log(PanelState.Level.WARN, "服务器未安装面板模组服务端部分，热力图需要服务端支持。");
        }
    }

    private static void handleHeatmap(HeatmapC2S m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        if (sp == null) { c.setPacketHandled(true); return; }
        MinecraftServer server = sp.getServer();
        // 文件 IO 放后台线程，别卡服务端 tick
        Thread worker = new Thread(() -> {
            try {
                ScanResult r = scanRegions(server, m.dim, m.cx, m.cz, m.half);
                CHANNEL.reply(new HeatmapS2C(m.dim, m.cx, m.cz, m.half, r.rx, r.rz, r.bits), c);
            } catch (Throwable ignored) {
            }
        }, "ChunkSmithPanel-Heatmap");
        worker.setDaemon(true);
        worker.start();
        c.setPacketHandled(true);
    }

    private static void handleHeatmapResp(HeatmapS2C m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            Set<Long> set = new HashSet<>();
            for (int i = 0; i < m.regions; i++) {
                byte[] bits = m.bits[i];
                int bx = m.rx[i] << 5, bz = m.rz[i] << 5;
                for (int z = 0; z < 32; z++) for (int x = 0; x < 32; x++) {
                    int idx = x + (z << 5);
                    if ((bits[idx >> 3] & (1 << (idx & 7))) != 0) {
                        int wx = bx + x, wz = bz + z;
                        if (Math.abs(wx - m.cx) <= m.half && Math.abs(wz - m.cz) <= m.half) {
                            set.add(PanelState.pos(wx, wz));
                        }
                    }
                }
            }
            PanelState.heat = new PanelState.HeatData(set, m.dim, 0, 0, m.half, false, System.currentTimeMillis());
        });
        c.setPacketHandled(true);
    }

    // ==================== region 扫描实现（服务端/单人共用） ====================

    static final class ScanResult {
        final int[] rx; final int[] rz; final byte[][] bits;
        ScanResult(int n) { rx = new int[n]; rz = new int[n]; bits = new byte[n][]; }
        PanelState.HeatData toData(String dim, int cx, int cz, int half) {
            Set<Long> set = new HashSet<>();
            for (int i = 0; i < rx.length; i++) {
                if (bits[i] == null) continue;
                int bx = rx[i] << 5, bz = rz[i] << 5;
                for (int z = 0; z < 32; z++) for (int x = 0; x < 32; x++) {
                    int idx = x + (z << 5);
                    if ((bits[i][idx >> 3] & (1 << (idx & 7))) != 0) {
                        int wx = bx + x, wz = bz + z;
                        if (Math.abs(wx - cx) <= half && Math.abs(wz - cz) <= half) set.add(PanelState.pos(wx, wz));
                    }
                }
            }
            return new PanelState.HeatData(set, dim, 0, 0, half, false, System.currentTimeMillis());
        }
    }

    /** 找到维度对应的存档目录（按原版命名惯例回退尝试）。 */
    static Path dimensionRoot(MinecraftServer server, String dim) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        String d = dim.toLowerCase(Locale.ROOT);
        if (d.endsWith(":overworld") || d.equals("minecraft:overworld")) return root;
        if (d.endsWith(":the_nether")) return root.resolve("DIM-1");
        if (d.endsWith(":the_end")) return root.resolve("DIM1");
        int colon = d.indexOf(':');
        if (colon >= 0) return root.resolve("DIM_" + d.substring(0, colon) + "_" + d.substring(colon + 1));
        return root.resolve("DIM_" + d);
    }

    static ScanResult scanRegions(MinecraftServer server, String dim, int cx, int cz, int half) {
        Path dir = dimensionRoot(server, dim).resolve("region");
        half = Math.max(16, Math.min(half, 256)); // 钳制窗口：±256 区块以内，包体可控
        int r0x = Math.floorDiv(cx - half, 32), r1x = Math.floorDiv(cx + half, 32);
        int r0z = Math.floorDiv(cz - half, 32), r1z = Math.floorDiv(cz + half, 32);
        List<Object[]> found = new ArrayList<>();
        for (int rz = r0z; rz <= r1z; rz++) {
            for (int rx = r0x; rx <= r1x; rx++) {
                Path f = dir.resolve("r." + rx + "." + rz + ".mca");
                if (!Files.isRegularFile(f)) continue;
                byte[] header = new byte[4096];
                try (RandomAccessFile raf = new RandomAccessFile(f.toFile(), "r")) {
                    raf.readFully(header);
                } catch (IOException e) {
                    continue;
                }
                byte[] bitmap = new byte[128];
                for (int z = 0; z < 32; z++) for (int x = 0; x < 32; x++) {
                    int idx = x + (z << 5);
                    int off = (header[idx * 4] & 0xFF) << 16 | (header[idx * 4 + 1] & 0xFF) << 8 | (header[idx * 4 + 2] & 0xFF);
                    int cnt = header[idx * 4 + 3] & 0xFF;
                    if (off != 0 && cnt >= 1 && cnt <= 255) {
                        int bi = idx;
                        bitmap[bi >> 3] |= (byte) (1 << (bi & 7));
                    }
                }
                found.add(new Object[]{rx, rz, bitmap});
            }
        }
        ScanResult r = new ScanResult(found.size());
        for (int i = 0; i < found.size(); i++) {
            Object[] o = found.get(i);
            r.rx[i] = (int) o[0]; r.rz[i] = (int) o[1]; r.bits[i] = (byte[]) o[2];
        }
        return r;
    }
}
