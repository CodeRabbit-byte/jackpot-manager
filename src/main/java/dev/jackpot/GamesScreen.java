package dev.jackpot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class GamesScreen extends BaseScreen {
    static int sel = 0;

    public GamesScreen() { super("Games", 1); }

    @Override
    protected void init() {
        tabs();
        int x = L();
        List<Data.GameCfg> gs = Store.D.games;
        sel = Math.max(0, Math.min(sel, gs.size() - 1));
        for (int i = 0; i < gs.size(); i++) {
            final int idx = i;
            Data.GameCfg g = gs.get(i);
            Text t = Text.literal(g.name).formatted(!g.enabled ? Formatting.DARK_GRAY : i == sel ? Formatting.YELLOW : Formatting.WHITE);
            ButtonWidget b = Ui.btn(t, x, 34 + i * 22, 130, 18, () -> { sel = idx; clearAndInit(); });
            addDrawableChild(b);
        }
        if (gs.size() < 9) {
            addDrawableChild(Ui.btn("+ Custom game", x, 34 + gs.size() * 22, 130, 18, () -> {
                Data.GameCfg g = new Data.GameCfg("custom" + System.currentTimeMillis(), "New Game");
                gs.add(g);
                sel = gs.size() - 1;
                Store.markDirty();
                clearAndInit();
            }));
        }
        Data.GameCfg g = gs.get(sel);
        int dx = x + 140;
        addDrawableChild(Ui.field(textRenderer, dx + 70, 34, 160, 16, g.name, 28, s -> { g.name = s; Store.markDirty(); }));
        addDrawableChild(Ui.btn(Ui.onoff("Enabled", g.enabled), dx + 70, 58, 100, 16, () -> { g.enabled = !g.enabled; Store.markDirty(); clearAndInit(); }));
        addDrawableChild(Ui.field(textRenderer, dx + 70, 82, 90, 16, Money.exact(g.min), 16, s -> {
            long v = Money.parse(s);
            if (v >= 0) { g.min = v; Store.markDirty(); }
        }));
        addDrawableChild(Ui.field(textRenderer, dx + 70, 106, 90, 16, Money.exact(g.max), 16, s -> {
            long v = Money.parse(s);
            if (v >= 0) { g.max = v; Store.markDirty(); }
        }));
        addDrawableChild(Ui.field(textRenderer, dx + 70, 130, 60, 16, String.valueOf(g.mult), 8, s -> {
            double v = Ui.dbl(s, -1);
            if (v > 0 && v < 1000) { g.mult = v; Store.markDirty(); }
        }));
        addDrawableChild(Ui.field(textRenderer, dx + 70, 154, 250, 16, g.rules, 120, s -> { g.rules = s; Store.markDirty(); }));
        if (g.id.startsWith("custom")) {
            addDrawableChild(Ui.btn("Delete this game", dx + 70, 178, 110, 16, () -> {
                gs.remove(g);
                sel = 0;
                Store.markDirty();
                clearAndInit();
            }));
        }
    }

    @Override
    protected void draw(DrawContext c, int mx, int my) {
        int dx = L() + 140;
        Data.GameCfg g = Store.D.games.get(sel);
        tx(c, "Name", dx, 38, 0xFFFFFF);
        tx(c, "Min bet", dx, 86, 0xFFFFFF);
        tx(c, "Max bet", dx, 110, 0xFFFFFF);
        tx(c, "Payout x", dx, 134, 0xFFFFFF);
        tx(c, "Rules note", dx, 158, 0xFFFFFF);
        tx(c, "(0 = no limit)", dx + 166, 86, 0x888888);
        tx(c, "(0 = no limit)", dx + 166, 110, 0x888888);
        int pd = Store.D.settings.decimals;
        if (g.min > 0) tx(c, "Min bet " + Money.fmt(g.min, pd) + " pays " + Money.fmt(Calc.payout(g.min, g.mult), pd), dx, 204, 0xAAAAAA);
        if (g.max > 0) tx(c, "Max bet " + Money.fmt(g.max, pd) + " pays " + Money.fmt(Calc.payout(g.max, g.mult), pd), dx, 216, 0xAAAAAA);
        tx(c, "The rules note and limit warnings show on the payment screen.", dx, 232, 0x888888);
    }
}
