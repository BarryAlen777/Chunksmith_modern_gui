package cn.blockforge.generated.chunksmithchunksmithgu;

import cn.blockforge.generated.chunksmithchunksmithgu.gui.PanelScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 默认 P 键开面板；键位可在原版“控制 → 按键绑定”里修改（标准 KeyMapping）。
 */
public final class ClientKeys {

    public static final KeyMapping TOGGLE = new KeyMapping(
            "key." + ChunkSmithGuiMod.MOD_ID + ".open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories." + ChunkSmithGuiMod.MOD_ID);

    /** forge 总线上的客户端 tick 监听。 */
    public static final class TickEvents {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            if (TOGGLE.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new PanelScreen());
                } else if (mc.screen instanceof PanelScreen) {
                    mc.setScreen(null);
                }
            }
            PrereqStatus.refreshTree();
        }
    }

    public static void init(IEventBus modBus) {
        // RegisterKeyMappingsEvent 是 mod 总线事件，直接挂 lambda，服务端不会加载本类
        modBus.addListener((net.minecraftforge.client.event.RegisterKeyMappingsEvent e) -> e.register(TOGGLE));
        modBus.addListener((FMLClientSetupEvent e) ->
                MinecraftForge.EVENT_BUS.register(new TickEvents()));
    }
}
