package dev.jackpot;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.ArrayList;
import java.util.List;

public class HistoryScreen extends BaseScreen {
    static String search = "";
    static int pageNo = 0;
    static boolean unpaidOnly = false;
    private static final int ROWS = 10;

    public HistoryScreen() { super("History", 2); }

    @Override
    protected void init() {
        tabs();
        int x = L();
        TextFieldWidget f = Ui.field(textRenderer, x, 28, 150, 16, search, 17, s -> { search = s; pageNo = 0; });
        addDrawableChild(f);
        addDrawableChild(Ui.btn(Ui.onoff("Unpaid only", unpaidOnly), x + 156, 28, 100, 16, () -> { unpaidOnly = !unpaidOnly; pageNo = 0; clearAndInit(); }));
        addDrawableChild(Ui.btn("<", x, 234, 20, 16, () -> { if (pageNo > 0) pageNo--; }));
        addDrawableChild(Ui.btn(">", x + 24, 234, 20, 16, () -> pageNo++));
    }

    private List<Data.Payment> rows() {
        List<Data.Payment> out = new ArrayList<>();
        List<Data.Payment> h = Store.D.history;
        for (int i = h.size() - 1; i >= 0; i--) {
            Data.Payment p = h.get(i);
            if (!search.isEmpty() && !p.player.toLowerCase().contains(search.toLowerCase())) continue;
            if (unpaidOnly && (p.done || p.owed <= 0)) continue;
            out.add(p);
        }
        return out;
    }

    @Override
    protected void draw(DrawContext c, int mx, int my) {
        int x = L();
        List<Data.Payment> rs = rows();
        int pages = Math.max(1, (rs.size() + ROWS - 1) / ROWS);
        pageNo = Math.max(0, Math.min(pageNo, pages - 1));
        int pd = Store.D.settings.decimals;
        tx(c, "Player", x + 4, 50, 0xFFD700);
        tx(c, "Paid", x + 110, 50, 0xFFD700);
        tx(c, "Game", x + 170, 50, 0xFFD700);
        tx(c, "Result", x + 285, 50, 0xFFD700);
        tx(c, "Owed", x + 335, 50, 0xFFD700);
        tx(c, "Settled", x + 410, 50, 0xFFD700);
        for (int i = 0; i < ROWS; i++) {
            int idx = pageNo * ROWS + i;
            if (idx >= rs.size()) break;
            Data.Payment p = rs.get(idx);
            int ry = 60 + i * 17;
            boolean hover = mx >= x && mx < x + 460 && my >= ry && my < ry + 16;
            c.fill(x, ry, x + 460, ry + 16, hover ? 0x80FFFFFF : 0x50000000);
            int rc = p.result.equals("WIN") ? 0x55FF55 : p.result.equals("LOSS") ? 0xFF5555 : 0xFFFFFF;
            tx(c, textRenderer.trimToWidth(p.player + (p.test ? "*" : ""), 100), x + 4, ry + 4, rc);
            tx(c, Money.fmt(p.amount, pd), x + 110, ry + 4, 0xFFFFFF);
            tx(c, textRenderer.trimToWidth(p.gameName.isEmpty() ? "-" : p.gameName, 110), x + 170, ry + 4, 0xFFFFFF);
            tx(c, p.result + (p.result.equals("ENTRY") ? " R" + p.round : ""), x + 285, ry + 4, rc);
            tx(c, p.owed > 0 ? Money.fmt(p.owed, pd) : "-", x + 335, ry + 4, p.owed > 0 && !p.done ? 0xFFAA00 : 0xFFFFFF);
            tx(c, p.owed > 0 ? (p.done ? "PAID" : "UNPAID") : "", x + 410, ry + 4, p.done ? 0x55FF55 : 0xFF5555);
        }
        tx(c, "Page " + (pageNo + 1) + "/" + pages, x + 50, 238, 0xFFFFFF);
        int w = 0, l = 0;
        long owed = 0, bet = 0;
        for (Data.Payment p : rs) {
            if (p.result.equals("WIN")) w++;
            if (p.result.equals("LOSS")) l++;
            if (!p.done) owed += p.owed;
            bet += p.amount;
        }
        tx(c, rs.size() + " records | W " + w + " / L " + l + " | total paid in " + Money.fmt(bet, pd) + " | still owed " + Money.fmt(owed, pd), x + 110, 238, 0xAAAAAA);
        tx(c, "Click a row to toggle PAID / UNPAID.  * = test record", x + 110, 250, 0x888888);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        int x = L();
        double mx = click.x(), my = click.y();
        List<Data.Payment> rs = rows();
        for (int i = 0; i < ROWS; i++) {
            int idx = pageNo * ROWS + i;
            if (idx >= rs.size()) break;
            int ry = 60 + i * 17;
            if (mx >= x && mx < x + 460 && my >= ry && my < ry + 16) {
                Data.Payment p = rs.get(idx);
                if (p.owed > 0) { p.done = !p.done; Store.markDirty(); }
                return true;
            }
        }
        return false;
    }
}
