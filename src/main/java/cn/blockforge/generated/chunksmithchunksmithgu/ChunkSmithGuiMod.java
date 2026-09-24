package cn.blockforge.generated.chunksmithchunksmithgu;

import cn.blockforge.generated.chunksmithchunksmithgu.net.Net;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * ChunkSmith 现代化指令面板 —— Forge 1.20.1 模组。
 * 以 ChunkSmith（/cs 指令族）为前置，把预生成指令封装成可操作面板：
 * 一键执行、参数表单校验、命令预览、任务状态、热力图、日志聚合、前置检测、权限分级、配置备份。
 *
 * <p>这里只做客户端和服务端都要的公共初始化。键位、聊天捕获、面板界面这些客户端专属的
 * 东西全部放在 {@link ClientSetup} 里——那个类带 {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)}，
 * 专用服务器加载模组时会整类跳过，不会碰任何 {@code net.minecraft.client.*}。
 * 一旦公共类里直接引用了客户端类，专用服务器在验证这个类时就会以
 * “invalid dist DEDICATED_SERVER” 报错并停止启动。</p>
 */
@Mod(ChunkSmithGuiMod.MOD_ID)
public final class ChunkSmithGuiMod {
    public static final String MOD_ID = "chunksmith_modern_gui";
    public static final String MOD_NAME = "Chunksmith modern gui";

    public ChunkSmithGuiMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // —— 公共初始化（客户端 / 服务端都要） ——
        Settings.load();
        PrereqStatus.scan();       // 只看模组列表，不碰客户端类
        Net.register(modBus);      // 注册面板的网络通道（握手 / 热力图）

        // —— 客户端初始化 ——
        // 不在这里写 LogCapture.init() / ClientKeys.init()：那会让服务端也去解析
        // 客户端类。改由 ClientSetup（仅客户端加载）接手。
    }
}
