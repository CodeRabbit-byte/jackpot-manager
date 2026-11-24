package dev.jackpot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class JackpotMod implements ClientModInitializer {
    public static KeyBinding OPEN;
    public static KeyBinding REVIEW;

    @Override
    public void onInitializeClient() {
        Store.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("jackpotmanager", "main"));
        OPEN = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jackpotmanager.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, cat));
        REVIEW = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jackpotmanager.review", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, cat));

        // Only server messages the game already shows you are read. No packets are sent.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) Pay.onMessage(message);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (Rounds.tick(Store.D.jackpot, System.currentTimeMillis())) Store.markDirty();
            Store.tick();
            while (OPEN.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new JackpotScreen());
            }
            while (REVIEW.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new PaymentScreen());
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Store.save());

        HudElementRegistry.addLast(Identifier.of("jackpotmanager", "hud"), (ctx, tick) -> HudRenderer.render(ctx));
    }
}
