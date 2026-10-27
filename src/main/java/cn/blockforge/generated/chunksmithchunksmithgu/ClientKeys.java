package cn.blockforge.generated.chunksmithchunksmithgu;

import cn.blockforge.generated.chunksmithchunksmithgu.gui.PanelScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 默认 P 键开面板；键位可在原版“控制 → 按键绑定”里修改（标准 KeyMapping）。
 *
 * <p>这个类整个都是客户端专属的，注册入口在 {@link ClientSetup}（仅客户端加载）。</p>
 */
public final class ClientKeys {

    public static final KeyMapping TOGGLE = new KeyMapping(
            "key." + ChunkSmithGuiMod.MOD_ID + ".open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories." + ChunkSmithGuiMod.MOD_ID);

    private ClientKeys() {
    }

    /** forge 总线上的客户端 tick 监听。 */
    public static final class TickEvents {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            // P 键只在“没有界面打开”时负责【打开】面板；
            // 面板已开时的关闭交给 PanelScreen.keyPressed ——
            // 否则在输入框里打 “pattern” 的 p 会一边插入字符一边把面板关掉。
            // （报告 P2-5）用 while 把积压的点击全部吃掉再决定动作：
            // 在背包/聊天里连按几次 P 时，若每 tick 只消耗一次，多出来的点击会在
            // 关掉那个界面后“补开”面板。
            boolean acted = false;
            while (TOGGLE.consumeClick()) {
                if (acted) continue;   // 多余点击直接丢弃
                acted = true;
                if (mc.screen == null) {
                    mc.setScreen(new PanelScreen());
                } else if (mc.screen instanceof PanelScreen ps && !ps.isInputFocused()) {
                    // 面板开着且没有输入框聚焦时按 P：关闭面板
                    mc.setScreen(null);
                }
                // 焦点在输入框上：这次 P 是打字，不是开关面板，吃掉不开也不关
            }

            // 面板开着才需要的周期工作（关着的时候别每 tick 去抢锁探命令树）
            if (mc.screen instanceof PanelScreen) {
                PrereqStatus.refreshTree();
            }
            // 两阶段清理的延时驱动（面板可能中途被关掉，这里必须一直转）
            CommandRunner.tick();
        }
    }
}
