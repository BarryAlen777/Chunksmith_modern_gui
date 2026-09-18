package cn.blockforge.generated.chunksmithchunksmithgu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模组自己的文件目录：游戏目录/chunksmith_panel/（备份、历史都写在这）。
 */
public final class ChunkSmithGuiPaths {
    private ChunkSmithGuiPaths() {
    }

    /** 游戏运行目录（run directory）。客户端与服务端同一取法。 */
    public static Path gameDir() {
        return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
    }

    public static Path configDir() {
        return gameDir().resolve("config");
    }

    public static Path panelDir() {
        return gameDir().resolve("chunksmith_panel");
    }

    /** 确保目录存在，返回目录本身。 */
    public static Path ensurePanelDir() {
        Path d = panelDir();
        try {
            Files.createDirectories(d);
        } catch (IOException ignored) {
        }
        return d;
    }
}
