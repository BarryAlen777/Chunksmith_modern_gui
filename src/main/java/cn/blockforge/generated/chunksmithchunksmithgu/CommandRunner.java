package cn.blockforge.generated.chunksmithchunksmithgu;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * 指令执行器：统一走玩家聊天指令通道（单人=本城内服，多人=服务器校验权限），
 * 执行前记入面板日志，/cs set 额外记入备份历史。
 */
public final class CommandRunner {

    private CommandRunner() {
    }

    public static boolean send(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return false;
        mc.getConnection().sendCommand(cmd);
        PanelState.log(PanelState.Level.CMD, "/ " + cmd);
        PanelState.lastCommandSentMs = System.currentTimeMillis();
        // 记录 set 变更用于备份/回滚
        String[] parts = cmd.split("\\s+");
        if (parts.length >= 4 && parts[0].equals(PrereqStatus.rootCommand) && parts[1].equals("set")) {
            PanelState.recordSet(parts[2], parts[3]);
        }
        // 粗略状态机：让按钮立刻给出反馈，精确状态以聊天解析为准
        if (parts.length >= 2) {
            switch (parts[1]) {
                case "start", "forcestart" -> {
                    PanelState.task = PanelState.Task.RUNNING;
                    PanelState.startedAtMs = System.currentTimeMillis();
                    PanelState.elapsedMs = -1;
                    PanelState.etaMs = -1;
                    PanelState.completeFlashUntilMs = 0;
                    PanelState.percent = 0;
                    PanelState.processed = -1;
                    PanelState.total = -1;
                    PanelState.rate = -1;
                    PanelState.etaMs = -1;
                    PanelState.lastProgressMs = 0;
                    PanelState.taskRawStatus = "";
                    PanelState.errors = 0; PanelState.warnings = 0;
                }
                case "pause" -> PanelState.task = PanelState.Task.PAUSED;
                case "continue" -> PanelState.task = PanelState.Task.RUNNING;
                // 服务端收到 cancel 后通常还要再发一条 /cs confirm 才算取消，
                // 所以这里先不动状态，等 confirm（或服务端回包）再清。
                case "cancel" -> PanelState.task = PanelState.Task.PAUSED;
                case "confirm" -> PanelState.clearTask();
                default -> { }
            }
        }
        return true;
    }

    /** 按顺序发送多条（跳过 # 注释）。 */
    public static boolean sendSequence(List<String> lines) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return false;
        boolean ok = true;
        for (String l : lines) {
            if (l == null || l.isBlank() || l.trim().startsWith("#")) continue;
            ok &= send(l.trim());
        }
        return ok;
    }

    /** 复制文本到系统剪贴板。 */
    public static void copy(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.keyboardHandler.setClipboard(text);
    }
}
