package cn.blockforge.generated.chunksmithchunksmithgu.gui;

/**
 * 生成形状包含判断（与 ChunkSmith pattern 语义近似的客户端可视化版）。
 * 坐标单位：区块。
 */
public final class PatternMath {

    private PatternMath() {
    }

    public static boolean inShape(String shape, int cx, int cz, double scx, double scz, int radius, int radius2) {
        double dx = cx - scx, dz = cz - scz;
        double r = radius;
        return switch (shape) {
            case "circle" -> dx * dx + dz * dz <= r * r;
            case "diamond" -> Math.abs(dx) + Math.abs(dz) <= r;
            case "triangle" -> Math.abs(dx) * 2 + dz <= r && dz >= -r; // 近似：等腰三角形
            case "oval" -> {
                double ry = radius2 > 0 ? radius2 : r;
                yield (dx * dx) / (r * r) + (dz * dz) / (ry * ry) <= 1;
            }
            case "star" -> {
                // 五角星近似：极坐标 |cos(2.5θ)| 调制
                double dist = Math.hypot(dx, dz);
                if (dist > r) yield false;
                double ang = Math.atan2(dz, dx);
                double mod = 0.55 + 0.45 * Math.abs(Math.cos(2.5 * ang));
                yield dist <= r * mod;
            }
            default -> Math.abs(dx) <= r && Math.abs(dz) <= r; // square
        };
    }
}
