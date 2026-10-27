package cn.blockforge.generated.chunksmithchunksmithgu.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 面板图标：不依赖贴图，直接用 16x16 的点阵在运行时画出来。
 * 好处是任何界面缩放下都不会糊，颜色也能跟按钮文字一起变（禁用时自动变灰）。
 * '.' = 透明，'#' = 用传入的颜色填充。
 */
public final class Icons {

    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String STOP = "stop";
    public static final String CHART = "chart";
    public static final String GRID = "grid";
    public static final String LOG = "log";
    public static final String TRIM = "trim";
    public static final String PREV = "prev";
    public static final String NEXT = "next";
    public static final String REFRESH = "refresh";
    public static final String COPY = "copy";
    public static final String FOLDER = "folder";
    public static final String ROLLBACK = "rollback";

    private Icons() {
    }

    private static final String[] PLAY_M = {
            "................",
            "................",
            "..##............",
            "..####..........",
            "..######........",
            "..########......",
            "..##########....",
            "..############..",
            "..############..",
            "..##########....",
            "..########......",
            "..######........",
            "..####..........",
            "..##............",
            "................",
            "................"
    };

    private static final String[] PAUSE_M = {
            "................",
            "................",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "...####..####...",
            "................",
            "................"
    };

    private static final String[] STOP_M = {
            "................",
            "................",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "...##########...",
            "................",
            "................",
            "................"
    };

    private static final String[] CHART_M = {
            "................",
            "................",
            "............##..",
            "............##..",
            "............##..",
            ".........##.##..",
            ".........##.##..",
            ".........##.##..",
            "......##.##.##..",
            "......##.##.##..",
            "...##.##.##.##..",
            "...##.##.##.##..",
            "...##.##.##.##..",
            "...##.##.##.##..",
            "................",
            "................"
    };

    private static final String[] GRID_M = {
            "................",
            "................",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "..######..######",
            "................",
            "................"
    };

    private static final String[] LOG_M = {
            "................",
            "................",
            "..############..",
            "..############..",
            "................",
            "..######........",
            "..######........",
            "................",
            "..############..",
            "..############..",
            "................",
            "..########......",
            "..########......",
            "................",
            "................",
            "................"
    };

    private static final String[] TRIM_M = {
            "................",
            "................",
            "..##........##..",
            "..####....####..",
            "...####..####...",
            "....########....",
            ".....######.....",
            "......####......",
            "......####......",
            ".....######.....",
            "....##....##....",
            "...##......##...",
            "..##........##..",
            "................",
            "................",
            "................"
    };

    private static final String[] PREV_M = {
            "................",
            "................",
            ".......##.......",
            "......###.......",
            ".....####.......",
            "....#####.......",
            "...######.......",
            "..#######.......",
            "..#######.......",
            "...######.......",
            "....#####.......",
            ".....####.......",
            "......###.......",
            ".......##.......",
            "................",
            "................"
    };

    private static final String[] NEXT_M = {
            "................",
            "................",
            ".......##.......",
            ".......###......",
            ".......####.....",
            ".......#####....",
            ".......######...",
            ".......#######..",
            ".......#######..",
            ".......######...",
            ".......#####....",
            ".......####.....",
            ".......###......",
            ".......##.......",
            "................",
            "................"
    };

    private static final String[] REFRESH_M = {
            "................",
            ".....######.....",
            "...##########...",
            "..####....####..",
            "..###......###..",
            "..##........##..",
            ".###........###.",
            ".###........###.",
            "..##........##..",
            "..###......###..",
            "..####....####..",
            "...##########...",
            ".....######.....",
            "..##............",
            "..####..........",
            "................"
    };

    private static final String[] COPY_M = {
            "................",
            "................",
            "..##########....",
            "..##########....",
            "..##......##....",
            "..##......##....",
            "..##..######....",
            "..##..######....",
            "..##..##..##....",
            "..######..##....",
            "..######..##....",
            "....##....##....",
            "....##....##....",
            "....########....",
            "....########....",
            "................"
    };

    private static final String[] FOLDER_M = {
            "................",
            "................",
            "..####..........",
            "..######........",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "..##############",
            "................",
            "................"
    };

    private static final String[] ROLLBACK_M = {
            "................",
            ".....######.....",
            "...##########...",
            "..####....####..",
            "..###......###..",
            ".###........###.",
            ".###........###.",
            "..###......###..",
            "..####....####..",
            "...##########...",
            ".....######.....",
            "...##...........",
            "..####..........",
            "..##............",
            "................",
            "................"
    };

    private static String[] matrix(String name) {
        if (name == null) return null;
        return switch (name) {
            case PLAY -> PLAY_M;
            case PAUSE -> PAUSE_M;
            case STOP -> STOP_M;
            case CHART -> CHART_M;
            case GRID -> GRID_M;
            case LOG -> LOG_M;
            case TRIM -> TRIM_M;
            case PREV -> PREV_M;
            case NEXT -> NEXT_M;
            case REFRESH -> REFRESH_M;
            case COPY -> COPY_M;
            case FOLDER -> FOLDER_M;
            case ROLLBACK -> ROLLBACK_M;
            default -> null;
        };
    }

    public static void draw(GuiGraphics g, String name, int x, int y, int size) {
        draw(g, name, x, y, size, 0xFFFFFFFF);
    }

    /** 在 (x,y) 处画一个边长 size 的图标，'#' 像素用 color 填充。
     *  （报告 P1-10）原来是逐像素 g.fill：一个满图标最多 256 次矩形提交、每帧每图标都重来。
     *  现在按"横向 run"合并相邻像素并缓存（图标点阵是固定的），一次绘制只剩几十次 fill，
     *  坐标相对图标左上角，画的时候平移即可；颜色每帧照传，禁用变灰不受影响。
     */
    public static void draw(GuiGraphics g, String name, int x, int y, int size, int color) {
        int[] runs = runsFor(name, size);
        if (runs == null) return;
        for (int i = 0; i < runs.length; i += 4) {
            g.fill(x + runs[i], y + runs[i + 1], x + runs[i + 2], y + runs[i + 3], color);
        }
    }

    /** name+size -> [x0,y0,x1,y1] * n 的缓存；点阵不变、尺寸不变就不用再算几何。 */
    private static final java.util.HashMap<String, int[]> RUN_CACHE = new java.util.HashMap<>();

    private static int[] runsFor(String name, int size) {
        String key = name + ':' + size;
        int[] cached = RUN_CACHE.get(key);
        if (cached != null) return cached.length == 0 ? null : cached;
        int[] built = buildRuns(name, size);
        RUN_CACHE.put(key, built == null ? new int[0] : built);
        return built;
    }

    private static int[] buildRuns(String name, int size) {
        String[] m = matrix(name);
        if (m == null || size <= 0) return null;
        double cell = size / 16.0;
        java.util.ArrayList<Integer> out = new java.util.ArrayList<>();
        for (int r = 0; r < 16; r++) {
            String row = m[r];
            int c = 0;
            while (c < 16) {
                if (c >= row.length() || row.charAt(c) != '#') { c++; continue; }
                int start = c;
                while (c < 16 && c < row.length() && row.charAt(c) == '#') c++;
                int x0 = (int) Math.round(start * cell);
                int x1 = (int) Math.round(c * cell);
                if (x1 <= x0) x1 = x0 + 1;
                int y0 = (int) Math.round(r * cell);
                int y1 = (int) Math.round((r + 1) * cell);
                if (y1 <= y0) y1 = y0 + 1;
                out.add(x0); out.add(y0); out.add(x1); out.add(y1);
            }
        }
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }
}
