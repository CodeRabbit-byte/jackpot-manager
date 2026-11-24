package dev.jackpot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Review screen for detected payments: choose the game, then Win / Lose. Copies the chat text. Sends nothing. */
public class PaymentScreen extends Screen {
    private Data.Payment cur;
    private boolean resolved;
    private String selGame;
    private String copied = "";
    private String note = "";

    public PaymentScreen() {
        super(Text.literal("Payments"));
        selGame = Store.D.lastGame;
        cur = first();
    }

    private static Data.Payment first() {
        List<Data.Payment> p = Store.D.pending;
        return p.isEmpty() ? null : p.get(0);
    }

    private int cx() { return width / 2; }

    @Override
    protected void init() {
        int lx = Math.max(4, cx() - 230);
        int rx = lx + 160;
        if (cur != null) {
            List<Data.GameCfg> gs = Store.D.games;
            int y = 30;
            for (Data.GameCfg g : gs) {
                boolean on = g.id.equals(selGame);
                Text t = Text.literal(g.name).formatted(!g.enabled ? Formatting.DARK_GRAY : on ? Formatting.YELLOW : Formatting.WHITE);
                ButtonWidget b = Ui.btn(t, lx, y, 150, 18, () -> { selGame = g.id; clearAndInit(); });
                b.active = !resolved && g.enabled;
                addDrawableChild(b);
                y += 20;
            }
            boolean jp = selGame.equals("jackpot");
            ButtonWidget jb = Ui.btn(Text.literal(Store.D.jackpot.title + " entry").formatted(jp ? Formatting.YELLOW : Formatting.AQUA), lx, y, 150, 18, () -> { selGame = "jackpot"; clearAndInit(); });
            jb.active = !resolved;
            addDrawableChild(jb);
            y += 20;
            ButtonWidget ig = Ui.btn("Ignore (not a bet)", lx, y, 150, 18, () -> {
                Pay.ignore(cur);
                next();
            });
            ig.active = !resolved;
            addDrawableChild(ig);

            if (!resolved) {
                if (jp) {
                    addDrawableChild(Ui.btn("Add to jackpot", rx, 170, 280, 22, this::addJackpot));
                } else {
                    ButtonWidget lose = Ui.btn(Text.literal("[ X ]  Lose").formatted(Formatting.RED), rx, 170, 136, 22, this::lose);
                    ButtonWidget win = Ui.btn(Text.literal("[ \u2714 ]  Win").formatted(Formatting.GREEN), rx + 144, 170, 136, 22, this::win);
                    Data.GameCfg g = Pay.game(selGame);
                    lose.active = win.active = g != null && g.enabled;
                    addDrawableChild(lose);
                    addDrawableChild(win);
                }
                addDrawableChild(Ui.btn("Skip (later)", rx, 196, 90, 18, () -> {
                    Store.D.pending.remove(cur);
                    Store.D.pending.add(cur);
                    cur = first();
                    clearAndInit();
                }));
            } else {
                addDrawableChild(Ui.btn("Next player >", rx, 170, 136, 22, this::next));
                if (!cur.result.equals("ENTRY") && !cur.result.equals("IGNORED")) {
                    addDrawableChild(Ui.btn("Undo", rx + 144, 170, 60, 22, () -> {
                        Pay.undo(cur);
                        resolved = false;
                        copied = "";
                        clearAndInit();
                    }));
                    if (cur.owed > 0) {
                        ButtonWidget paid = Ui.btn(cur.done ? "Paid out \u2714" : "Mark paid out", rx + 208, 170, 72, 22, () -> {
                            cur.done = !cur.done;
                            Store.markDirty();
                            clearAndInit();
                        });
                        addDrawableChild(paid);
                    }
                }
            }
        }
        addDrawableChild(Ui.btn("Close", lx + 380, height - 24, 80, 18, this::close));
    }

    private void next() {
        resolved = false;
        copied = "";
        note = "";
        cur = first();
        clearAndInit();
    }

    private void win() {
        Data.GameCfg g = Pay.game(selGame);
        if (g == null || cur == null) return;
        copied = Pay.win(cur, g);
        resolved = true;
        clearAndInit();
    }

    private void lose() {
        Data.GameCfg g = Pay.game(selGame);
        if (g == null || cur == null) return;
        copied = Pay.lose(cur, g);
        resolved = true;
        clearAndInit();
    }

    private void addJackpot() {
        if (cur == null) return;
        Data.Jackpot j = Store.D.jackpot;
        int r = Pay.jackpotEntry(cur);
        note = r == j.round ? "Entered round " + r + "." : "Round closed/full - entered round " + r + ".";
        resolved = true;
        copied = "";
        clearAndInit();
    }

    private void tx(DrawContext c, String s, int x, int y, int rgb) {
        c.drawTextWithShadow(textRenderer, s, x, y, 0xFF000000 | rgb);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        int lx = Math.max(4, cx() - 230);
        int rx = lx + 160;
        int pd = Store.D.settings.decimals;
        if (cur == null) {
            tx(c, "No pending payments.", rx, 60, 0xFFFFFF);
            tx(c, "Detected payments appear here. Use Settings > Test Lab to simulate one.", rx, 76, 0xAAAAAA);
            return;
        }
        tx(c, Store.D.pending.size() + " pending" + (cur.test ? "   [TEST PAYMENT]" : ""), rx, 30, 0xAAAAAA);
        int nameColor = 0xFFFFFF;
        if (resolved && cur.result.equals("WIN")) nameColor = 0x55FF55;
        if (resolved && cur.result.equals("LOSS")) nameColor = 0xFF5555;
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(rx, 44);
        c.getMatrices().scale(1.5f, 1.5f);
        c.drawTextWithShadow(textRenderer, cur.player, 0, 0, 0xFF000000 | nameColor);
        c.getMatrices().popMatrix();
        tx(c, "paid you " + Money.fmt(cur.amount, pd), rx + 6 + (int) (textRenderer.getWidth(cur.player) * 1.5f), 48, 0xFFFFFF);

        if (resolved) {
            drawResult(c, rx, pd);
            return;
        }
        if (selGame.equals("jackpot")) {
            drawJackpotInfo(c, rx, pd);
            return;
        }
        Data.GameCfg g = Pay.game(selGame);
        if (g == null) return;
        long bet = cur.amount;
        tx(c, g.name + "   x" + g.mult, rx, 74, 0xFFD700);
        tx(c, "Bet " + Money.fmt(bet, pd) + "  ->  winning payout " + Money.fmt(Calc.payout(bet, g.mult), pd), rx, 90, 0x55FF55);
        String lim = (g.min > 0 ? "min " + Money.fmt(g.min, pd) : "no min") + " / " + (g.max > 0 ? "max " + Money.fmt(g.max, pd) : "no max");
        tx(c, "Limits: " + lim, rx, 104, 0xAAAAAA);
        if (g.min > 0 && bet < g.min) tx(c, "Below the minimum bet!", rx, 118, 0xFF5555);
        else if (g.max > 0 && bet > g.max) tx(c, "Above the maximum bet!", rx, 118, 0xFF5555);
        if (!g.enabled) tx(c, "This game is disabled.", rx, 118, 0xFF5555);
        if (!g.rules.isEmpty()) tx(c, textRenderer.trimToWidth("Rules: " + g.rules, 290), rx, 134, 0xFFFFFF);
        tx(c, "Win / Lose copies to your clipboard. Nothing is sent - paste and approve yourself.", rx, 152, 0x888888);
    }

    private void drawResult(DrawContext c, int rx, int pd) {
        if (cur.result.equals("WIN")) {
            tx(c, "WIN  -  " + cur.gameName, rx, 78, 0x55FF55);
            tx(c, "You pay " + cur.player + " " + Money.fmt(cur.owed, pd) + (cur.done ? "  (marked paid)" : ""), rx, 94, 0xFFFFFF);
        } else if (cur.result.equals("LOSS")) {
            tx(c, "LOSS  -  " + cur.gameName, rx, 78, 0xFF5555);
            tx(c, "Nothing to pay out.", rx, 94, 0xFFFFFF);
        } else if (cur.result.equals("ENTRY")) {
            tx(c, "JACKPOT ENTRY  -  round " + cur.round, rx, 78, 0x55FFFF);
            tx(c, note, rx, 94, 0xFFFFFF);
        } else {
            tx(c, "Ignored.", rx, 78, 0xAAAAAA);
        }
        if (!copied.isEmpty()) tx(c, "Copied to clipboard:  " + copied, rx, 120, 0xFFD700);
    }

    private void drawJackpotInfo(DrawContext c, int rx, int pd) {
        Data.Jackpot j = Store.D.jackpot;
        long cost = Calc.entryCost(j);
        int r = Rounds.target(j, cur.player);
        tx(c, j.title + " - entry cost " + Money.fmt(cost, pd), rx, 74, 0xFFD700);
        boolean late = r != j.round;
        tx(c, "This payment belongs to round " + r + (late ? "  (round " + j.round + " is " + (j.closed ? "closed" : "full") + ")" : ""),
                rx, 90, late ? 0xFFAA00 : 0x55FF55);
        if (cur.amount < cost) tx(c, "Underpaid by " + Money.fmt(cost - cur.amount, pd) + " - entry stays UNPAID until topped up.", rx, 104, 0xFF5555);
        else if (cur.amount > cost) tx(c, "Overpaid by " + Money.fmt(cur.amount - cost, pd) + " - you may owe a refund.", rx, 104, 0xFFAA00);
        else tx(c, "Exact amount.", rx, 104, 0x55FF55);
        for (Data.Entry e : j.entries) {
            if (e.player.equalsIgnoreCase(cur.player) && !e.paid && e.round >= j.round) {
                tx(c, "Matches an unpaid slot for this player.", rx, 118, 0xAAAAAA);
                break;
            }
        }
        tx(c, "Slots " + Rounds.count(j, j.round) + "/" + j.slots + " in round " + j.round + ".", rx, 134, 0xAAAAAA);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() { Store.markDirty(); }
}
