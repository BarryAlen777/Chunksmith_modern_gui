package cn.blockforge.generated.chunksmithchunksmithgu;

import cn.blockforge.generated.chunksmithchunksmithgu.net.Net;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * ChunkSmith 现代化指令面板 —— Forge 1.20.1 模组。
 * 以 ChunkSmith（/cs 指令族）为前置，把预生成指令封装成可操作面板：
 * 一键执行、参数表单校验、命令预览、任务状态、热力图、日志聚合、前置检测、权限分级、配置备份。
 */
@Mod(ChunkSmithGuiMod.MOD_ID)
public final class ChunkSmithGuiMod {
    public static final String MOD_ID = "chunksmith_modern_gui";
    public static final String MOD_NAME = "Chunksmith modern gui";

    public ChunkSmithGuiMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        Settings.load();
        PrereqStatus.scan();
        Net.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LogCapture.init();
            ClientKeys.init(modBus);
        }
    }
}
