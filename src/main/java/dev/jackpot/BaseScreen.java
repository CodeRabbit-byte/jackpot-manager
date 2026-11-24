package dev.jackpot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public abstract class BaseScreen extends Screen {
    static final String[] TABS = {"Jackpot", "Games", "History", "Settings"};
    protected final int tab;
    protected String status = "";

    protected BaseScreen(String title, int tab) {
        super(Text.literal(title));
        this.tab = tab;
    }

    protected int L() { return Math.max(4, width / 2 - 230); }

    protected void tabs() {
        int x = L();
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            ButtonWidget b = Ui.btn(TABS[i], x, 6, 72, 18, () -> goTab(idx));
            b.active = idx != tab;
            addDrawableChild(b);
            x += 75;
        }
        addDrawableChild(Ui.btn("HUD Editor", x, 6, 84, 18, () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen())));
    }

    static void goTab(int i) {
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (i) {
            case 0 -> mc.setScreen(new JackpotScreen());
            case 1 -> mc.setScreen(new GamesScreen());
            case 2 -> mc.setScreen(new HistoryScreen());
            default -> mc.setScreen(new SettingsScreen());
        }
    }

    protected void tx(DrawContext c, String s, int x, int y, int rgb) {
        c.drawTextWithShadow(textRenderer, s, x, y, 0xFF000000 | rgb);
    }

    protected void panel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, 0xA0000000);
        c.fill(x, y, x + w, y + 1, 0xFF808080);
        c.fill(x, y + h - 1, x + w, y + h, 0xFF808080);
        c.fill(x, y, x + 1, y + h, 0xFF808080);
        c.fill(x + w - 1, y, x + w, y + h, 0xFF808080);
    }

    protected void draw(DrawContext c, int mx, int my) {}

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        draw(c, mx, my);
        tx(c, status, L(), height - 12, 0xFFFF55);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() { Store.markDirty(); }
}
