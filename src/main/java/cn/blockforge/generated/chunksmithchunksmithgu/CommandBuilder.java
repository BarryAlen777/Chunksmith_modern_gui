package cn.blockforge.generated.chunksmithchunksmithgu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 表单 → 指令序列的构建与校验。输出用 ChunkSmith 的根命令（cs/chunky，运行期由命令树探测决定）。
 */
public final class CommandBuilder {

    private static final Pattern WORLD_OK = Pattern.compile("[A-Za-z0-9_\\-.]+");

    private CommandBuilder() {
    }

    /** 校验表单，返回问题列表（空 = 全部合法）。 */
    public static List<String> validate() {
        List<String> problems = new ArrayList<>();
        String w = PanelState.world == null ? "" : PanelState.world.trim();
        if (w.isEmpty() || !WORLD_OK.matcher(w).matches())
            problems.add("世界名只允许字母数字与 _ - .");
        if (PanelState.radius < 1 || PanelState.radius > 1_000_000)
            problems.add("半径需在 1 ~ 1000000 之间");
        if (needsRadius2() && (PanelState.radius2 < 1 || PanelState.radius2 > PanelState.radius))
            problems.add("第二半径需在 1 ~ 主半径之间");
        if (PanelState.centerMode.equals("coords")) {
            if (PanelState.centerX < -30_000_000 || PanelState.centerX > 30_000_000)
                problems.add("X 坐标超出世界边界");
            if (PanelState.centerZ < -30_000_000 || PanelState.centerZ > 30_000_000)
                problems.add("Z 坐标超出世界边界");
        }
        if (PanelState.shape.equals("star") && !needsRadius2())
            problems.add("星形需要第二半径");
        return problems;
    }

    public static boolean needsRadius2() {
        return PanelState.shape.equals("star");
    }
    public static boolean shapeHasRadius2(String shape) {
        return shape.equals("star");
    }

    /** 生成完整指令序列（含选择与启动，逐行）。 */
    public static List<String> buildSequence() {
        String cs = PrereqStatus.rootCommand;
        List<String> l = new ArrayList<>();
        l.add(cs + " world " + PanelState.world.trim());
        l.add(cs + " " + shapeCmd() + " " + PanelState.shape);
        l.addAll(centerLines());
        if (shapeHasRadius2(PanelState.shape) && PanelState.radius2 > 0)
            l.add(cs + " radius " + PanelState.radius + " " + PanelState.radius2);
        else
            l.add(cs + " radius " + PanelState.radius);
        l.add(cs + " start");
        return l;
    }

    /** 选形状的子命令名（Chunksmith=pattern / Chunky=shape，按命令树自动认）。 */
    private static String shapeCmd() {
        return PrereqStatus.shapeCommand == null ? "pattern" : PrereqStatus.shapeCommand;
    }

    /** 中心：当前位置 / 世界出生点 / 指定坐标。 */
    private static List<String> centerLines() {
        String cs = PrereqStatus.rootCommand;
        List<String> l = new ArrayList<>();
        switch (PanelState.centerMode) {
            case "spawn" -> l.add(cs + " spawn");
            case "coords" -> l.add(cs + " center " + PanelState.centerX + " " + PanelState.centerZ);
            default -> l.add(cs + " center");
        }
        return l;
    }

    /** 预览文本（多行，行号注释用 # 开头，执行时跳过）。 */
    public static String buildPreview() {
        List<String> seq = buildSequence();
        StringBuilder sb = new StringBuilder();
        sb.append("# ChunkSmith 预生成任务\n");
        sb.append("# 形状 ").append(PanelState.shape)
          .append(" · 半径 ").append(PanelState.radius);
        if (shapeHasRadius2(PanelState.shape) && PanelState.radius2 > 0) sb.append('/').append(PanelState.radius2);
        sb.append(" · 中心 ").append(PanelState.centerMode).append('\n');
        for (String s : seq) sb.append(s).append('\n');
        return sb.toString();
    }

    /**
     * trim 清理指令：Chunksmith（原版 Chunky 也是）的 trim 用的是“当前选择”，
     * 本身不接受形状/半径参数。所以先把世界、形状、中心、保留半径设好，最后再跑 trim，
     * 否则服务端会因为参数不合法直接拒绝，看着就是“清理不动”。
     */
    public static List<String> buildTrim() {
        String cs = PrereqStatus.rootCommand;
        List<String> l = new ArrayList<>();
        l.add(cs + " world " + PanelState.world.trim());
        l.add(cs + " " + shapeCmd() + " " + PanelState.shape);
        l.addAll(centerLines());
        l.add(cs + " radius " + PanelState.radius);
        l.add(cs + " trim");
        return l;
    }

    /** 把编辑过的预览文本拆成可执行行。 */
    public static List<String> parsePreviewLines(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (t.startsWith("/")) t = t.substring(1);
            out.add(t);
        }
        return out;
    }
}
