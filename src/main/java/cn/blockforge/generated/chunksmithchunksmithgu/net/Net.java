package cn.blockforge.generated.chunksmithchunksmithgu.net;

import cn.blockforge.generated.chunksmithchunksmithgu.ChunkSmithGuiMod;
import cn.blockforge.generated.chunksmithchunksmithgu.PanelState;
import cn.blockforge.generated.chunksmithchunksmithgu.PrereqStatus;
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
import net.minecraftforge.network.PacketDistributor;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 面板与服务器之间的两条通道：
 *  Handshake —— 登录时问服务端“有没有前置、什么版本、有哪些世界”；
 *  Heatmap   —— 让服务端扫 region 文件头，回传每个 32x32 区块的 1024bit 位图（体积小、不碰区块对象）。
 *
 * <p>通道按“可选模组”注册：服务端没装本模组时握手会缺席而不是拒绝连接，
 * 客户端单装也能正常进原版/他模服（热力图功能此时不可用）。
 * {@code mods.toml} 里配套设置了 {@code displayTest="IGNORE_ALL_VERSION"}。</p>
 */
public final class Net {

    public static final ResourceLocation CHANNEL_NAME =
            new ResourceLocation(ChunkSmithGuiMod.MOD_ID, "panel_net");

    /**
     * 通道协议号。改过包体结构就必须 +1，否则新旧客户端/服务端会按各自的格式去解析同一串字节
     * （HandshakeS2C 加过 MAX_WORLDS 截断，HeatmapS2C 先加了 requestId、又加了末尾的 reason 字符串，
     * 却一直停在 "1"）。协议号不一致时 Forge 直接不建通道，面板会退化成“服务器未安装服务端部分”
     * 的可读提示，比按错位的字段解析、最后抛异常断线安全得多。
     * <p>对端没装本模组（原版/他模服）仍然放行：通道是可选功能，不该拦握手。</p>
     */
    private static final String PROTOCOL = "2";
    /** 对端缺席（没装本模组）或版本一致都放行。 */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME, () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

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

    /** 世界列表最多回传条数，编解码两侧一致，不会静默截断错位。 */
    private static final int MAX_WORLDS = 64;

    public static final class HandshakeS2C {
        final String prereqId;      // 服务端前置模组 id（null=没有）
        final String prereqVersion; // 版本串
        final List<String> worlds;  // 可选世界/维度列表

        HandshakeS2C(String id, String v, List<String> w) {
            prereqId = id; prereqVersion = v;
            worlds = w.size() > MAX_WORLDS ? new ArrayList<>(w.subList(0, MAX_WORLDS)) : w;
        }

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
            if (n < 0 || n > MAX_WORLDS) throw new IllegalArgumentException("bad world count: " + n);
            List<String> w = new ArrayList<>();
            for (int i = 0; i < n; i++) w.add(b.readUtf(96));
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
                // 服务端明确说没装前置：多人下 prereqOk() 由命令树/本地兜底的
                // isSingleplayer 分支挡住（见 PrereqStatus.computePrereqOk），
                // 这里不写本地字段（Net 是双端类，不碰客户端状态）。
            }
            PrereqStatus.invalidateOkCache();
        });
        c.setPacketHandled(true);
    }

    // ==================== 热力图 ====================

    public static final class HeatmapC2S {
        final int requestId;
        final String dim; final int cx, cz, half;
        HeatmapC2S(int id, String d, int x, int z, int h) { requestId = id; dim = d; cx = x; cz = z; half = h; }
        static void encode(HeatmapC2S m, FriendlyByteBuf b) {
            b.writeVarInt(m.requestId);
            b.writeUtf(m.dim); b.writeVarInt(m.cx); b.writeVarInt(m.cz); b.writeVarInt(m.half);
        }
        static HeatmapC2S decode(FriendlyByteBuf b) {
            int id = b.readVarInt();
            String d = b.readUtf(128);
            if (d.contains("..") || d.contains("/") || d.contains("\\")) {
                throw new IllegalArgumentException("bad dimension id");   // 拒绝路径穿越
            }
            int cx = b.readVarInt();
            int cz = b.readVarInt();
            int half = b.readVarInt();
            if (half < 0 || half > 512) throw new IllegalArgumentException("bad half: " + half);
            return new HeatmapC2S(id, d, cx, cz, half);
        }
    }

    /** 每个 region（32x32 区块）一张 1024bit 位图，按位存在与否。 */
    public static final class HeatmapS2C {
        final int requestId;
        final String dim; final int cx, cz, half;
        final int regions;            // 条目数
        final int[] rx, rz;           // region 坐标
        final byte[][] bits;          // 每 region 128 字节位图
        /** 拒绝原因（报告 P2-2）：空串 = 正常回包；客户端据此区分"被拒"和"真没区块"。 */
        final String reason;
        HeatmapS2C(int id, String d, int x, int z, int h, int[] rx, int[] rz, byte[][] bits) {
            this(id, d, x, z, h, rx, rz, bits, "");
        }
        HeatmapS2C(int id, String d, int x, int z, int h, int[] rx, int[] rz, byte[][] bits, String reason) {
            requestId = id; dim = d; cx = x; cz = z; half = h; regions = rx.length;
            this.rx = rx; this.rz = rz; this.bits = bits;
            this.reason = reason == null ? "" : reason;
        }
        static void encode(HeatmapS2C m, FriendlyByteBuf b) {
            b.writeVarInt(m.requestId);
            b.writeUtf(m.dim); b.writeVarInt(m.cx); b.writeVarInt(m.cz); b.writeVarInt(m.half);
            b.writeVarInt(m.regions);
            for (int i = 0; i < m.regions; i++) {
                b.writeVarInt(m.rx[i]); b.writeVarInt(m.rz[i]);
                b.writeBytes(m.bits[i]);
            }
            b.writeUtf(m.reason);   // 追加在末尾：老客户端解析不到这字段也只是忽略
        }
        static HeatmapS2C decode(FriendlyByteBuf b) {
            int id = b.readVarInt();
            String d = b.readUtf(128);
            int x = b.readVarInt(), z = b.readVarInt(), h = b.readVarInt();
            // 条目数必须是 0..65536 的合法值；负数（恶意/损坏包）直接拒绝，
            // 否则 new int[n] 会抛 NegativeArraySizeException 崩掉客户端网络线程
            int n = b.readVarInt();
            if (n < 0 || n > 65536) throw new IllegalArgumentException("bad region count: " + n);
            int[] rx = new int[n], rz = new int[n];
            byte[][] bits = new byte[n][];
            for (int i = 0; i < n; i++) {
                if (b.readableBytes() < 8) throw new IllegalArgumentException("truncated heatmap payload");
                rx[i] = b.readVarInt(); rz[i] = b.readVarInt();
                bits[i] = new byte[128];
                if (b.readableBytes() < 128) throw new IllegalArgumentException("truncated bitmap");
                b.readBytes(bits[i]);
            }
            String reason = b.isReadable() ? b.readUtf(64) : "";
            return new HeatmapS2C(id, d, x, z, h, rx, rz, bits, reason);
        }
    }

    // 客户端主动发起的那两个方法（requestHandshake / requestHeatmap）在 NetClient 里。
    // 它们要用 net.minecraft.client.Minecraft，属于客户端专属代码，绝不能留在这个
    // 服务端也会加载的公共类里——否则专用服务器验证 Net 时会去加载客户端类并以
    // “invalid dist DEDICATED_SERVER” 报错。

    // ==================== 服务端扫描的安全护栏 ====================

    /** 扫描用有界线程池：最多 2 个并发扫描 + 4 个排队，满了直接拒绝（而不是刷一个请求起一个线程）。 */
    private static final ThreadPoolExecutor HEAT_EXECUTOR = new ThreadPoolExecutor(
            1, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(4),
            r -> {
                Thread t = new Thread(r, "ChunksmithPanel-Heatmap");
                t.setDaemon(true);
                return t;
            });

    /** 每玩家上次请求时间：冷却 3 秒。 */
    private static final ConcurrentHashMap<UUID, Long> HEAT_LAST_REQ = new ConcurrentHashMap<>();
    private static final long HEAT_COOLDOWN_MS = 3000;
    /** （报告 P2-1）冷却表兜底清理：条目过多时把早于 1 小时的先扔掉，玩家退出也不会漏驻留。 */
    private static final long HEAT_REQ_EXPIRY_MS = 60 * 60_000L;

    private static HeatmapS2C deny(HeatmapC2S m, String reason) {
        return new HeatmapS2C(m.requestId, m.dim, m.cx, m.cz, m.half,
                new int[0], new int[0], new byte[0][], reason);
    }

    private static void handleHeatmap(HeatmapC2S m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        if (sp == null) { c.setPacketHandled(true); return; }
        MinecraftServer server = sp.getServer();
        // 拒绝包：空位图 + 带原因（报告 P2-2），客户端能区分"被拒"和"这个范围真没区块"

        // 权限：热力图暴露世界布局，至少要求一级 OP
        if (server == null || !sp.createCommandSourceStack().hasPermission(1)) {
            CHANNEL.reply(deny(m, "需要服务端权限等级 1 以上"), c); c.setPacketHandled(true); return;
        }
        // 维度必须真实存在（同时挡住任何路径拼接的尝试）
        ResourceKey<Level> levelKey = resolveLevel(server, m.dim);
        if (levelKey == null) {
            CHANNEL.reply(deny(m, "维度不存在"), c); c.setPacketHandled(true); return;
        }
        // 每玩家冷却
        long now = System.currentTimeMillis();
        Long last = HEAT_LAST_REQ.get(sp.getUUID());
        if (last != null && now - last < HEAT_COOLDOWN_MS) {
            CHANNEL.reply(deny(m, "请求太快，等几秒再试"), c); c.setPacketHandled(true); return;
        }
        if (HEAT_LAST_REQ.size() > 256) {
            long cutoff = now - HEAT_REQ_EXPIRY_MS;
            HEAT_LAST_REQ.entrySet().removeIf(en -> en.getValue() < cutoff);
        }
        HEAT_LAST_REQ.put(sp.getUUID(), now);

        final String dim = m.dim;
        try {
            HEAT_EXECUTOR.execute(() -> {
                HeatmapS2C reply;
                try {
                    ScanResult r = scanRegions(server, dim, m.cx, m.cz, m.half);
                    reply = new HeatmapS2C(m.requestId, dim, m.cx, m.cz, m.half, r.rx, r.rz, r.bits);
                } catch (Throwable t) {
                    reply = new HeatmapS2C(m.requestId, dim, m.cx, m.cz, m.half,
                            new int[0], new int[0], new byte[0][], "扫描失败: " + t);
                }
                // 回包切回服务端主线程发（SimpleChannel 的 reply 上下文不保证跨线程安全）
                final HeatmapS2C payload = reply;
                server.execute(() -> {
                    if (!sp.hasDisconnected()) {
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), payload);
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            // 队列已满：直接回带原因的拒绝包，避免积压
            CHANNEL.reply(deny(m, "服务端扫描繁忙，稍后再试"), c);
        }
        c.setPacketHandled(true);
    }

    /** 在服务器真实维度里找 dim；找不到返回 null（拒绝任意字符串拼路径）。 */
    private static ResourceKey<Level> resolveLevel(MinecraftServer server, String dim) {
        if (dim == null) return null;
        String want = dim.toLowerCase(Locale.ROOT);
        for (ResourceKey<Level> k : server.levelKeys()) {
            if (k.location().toString().toLowerCase(Locale.ROOT).equals(want)) return k;
        }
        return null;
    }

    private static void handleHeatmapResp(HeatmapS2C m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            if (m.requestId != PanelState.heatRequestId) return;
            // 被拒时给可读原因（报告 P2-2）：之前"权限不足/冷却/维度不存在"和
            // "这块区域真没区块"都画成空图，用户完全分不清。
            if (m.reason != null && !m.reason.isEmpty()) {
                PanelState.log(PanelState.Level.WARN, "热力图请求被服务端拒绝：" + m.reason);
            }
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
            PanelState.heat = new PanelState.HeatData(set, m.dim, m.half, System.currentTimeMillis());
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
            return new PanelState.HeatData(set, dim, half, System.currentTimeMillis());
        }
    }

    /**
     * 找到维度对应的存档目录，兼容 1.20 的 dimensions/ 路径和旧版回退路径。
     *
     * <p>dim 必须是 {@link #resolveLevel} 已经在服务端维度里验证过的字符串，
     * 这里再兜底拒一次路径穿越字符，防止别处直接调用拼出越界路径。</p>
     */
    static Path dimensionRoot(MinecraftServer server, String dim) {
        Path root = server.getWorldPath(LevelResource.ROOT).normalize();
        String d = dim == null ? "minecraft:overworld" : dim.toLowerCase(Locale.ROOT);
        if (d.contains("..") || d.contains("/") || d.contains("\\")) {
            return root;   // 宁可回主世界，也绝不拼接可疑路径
        }
        if (d.equals("minecraft:overworld") || d.endsWith(":overworld")) return root;
        if (d.equals("minecraft:the_nether") || d.endsWith(":the_nether")) return root.resolve("DIM-1");
        if (d.equals("minecraft:the_end") || d.endsWith(":the_end")) return root.resolve("DIM1");
        int colon = d.indexOf(':');
        String namespace = colon >= 0 ? d.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? d.substring(colon + 1) : d;
        Path modern = root.resolve("dimensions").resolve(namespace).resolve(path);
        if (Files.isDirectory(modern.resolve("region")) || !Files.exists(root.resolve("DIM_" + namespace + "_" + path))) {
            return modern;
        }
        return root.resolve("DIM_" + namespace + "_" + path);
    }

    static ScanResult scanRegions(MinecraftServer server, String dim, int cx, int cz, int half) {
        half = Math.max(16, Math.min(half, 256)); // 钳制窗口：±256 区块以内，包体可控
        Path dir = dimensionRoot(server, dim).resolve("region");
        // 扫描上限兜底：即使窗口很大，也只扫有限个 region 文件
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
