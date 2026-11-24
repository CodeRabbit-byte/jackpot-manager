package dev.jackpot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class JackpotScreen extends BaseScreen {
    static int page = 0;
    static int presetIdx = 0;
    private int scroll = 0;
    private TextFieldWidget nameField;

    public JackpotScreen() { super("Jackpot Manager", 0); }

    private Data.Jackpot J() { return Store.D.jackpot; }

    @Override
    protected void init() {
        tabs();
        int x = L();
        String[] pn = {"Setup", "Players", "Round"};
        for (int i = 0; i < 3; i++) {
            final int p = i;
            ButtonWidget b = Ui.btn(pn[i], x + i * 62, 28, 60, 16, () -> { page = p; scroll = 0; clearAndInit(); });
            b.active = p != page;
            addDrawableChild(b);
        }
        switch (page) {
            case 0 -> setup(x);
            case 1 -> players(x);
            default -> round(x);
        }
    }

    // ---------------------------------------------------------------- setup
    private void setup(int x) {
        Data.Jackpot j = J();
        addDrawableChild(Ui.field(textRenderer, x + 50, 50, 150, 16, j.title, 24, s -> { j.title = s; Store.markDirty(); }));

        addDrawableChild(Ui.btn("-", x + 50, 72, 18, 16, () -> slots(-1)));
        addDrawableChild(Ui.btn("+", x + 98, 72, 18, 16, () -> slots(1)));
        addDrawableChild(Ui.btn(Ui.onoff("Multi-entry", j.allowMulti), x + 124, 72, 110, 16,
                () -> { j.allowMulti = !j.allowMulti; Store.markDirty(); clearAndInit(); }));

        addDrawableChild(Ui.btn(Ui.onoff("Auto-close", j.autoClose), x, 94, 100, 16,
                () -> { j.autoClose = !j.autoClose; Rounds.afterChange(j); Store.markDirty(); clearAndInit(); }));
        addDrawableChild(Ui.btn(Text.literal("Prize: " + (j.manualPrize >= 0 ? "MANUAL" : "AUTO")), x + 104, 94, 76, 16, () -> {
            j.manualPrize = j.manualPrize >= 0 ? -1 : Calc.of(j).prize;
            Store.markDirty();
            clearAndInit();
        }));
        if (j.manualPrize >= 0) {
            addDrawableChild(Ui.field(textRenderer, x + 184, 94, 80, 16, Money.exact(j.manualPrize), 16, s -> {
                long v = Money.parse(s);
                if (v >= 0) { j.manualPrize = v; Store.markDirty(); }
            }));
        }

        addDrawableChild(Ui.field(textRenderer, x + 40, 116, 44, 16, trim(j.taxPct), 6, s -> {
            double v = Ui.dbl(s, -1);
            if (v >= 0 && v <= 100) { j.taxPct = v; Store.markDirty(); }
        }));
        addDrawableChild(Ui.field(textRenderer, x + 150, 116, 44, 16, trim(j.prizePct), 6, s -> {
            double v = Ui.dbl(s, -1);
            if (v >= 0 && v <= 100) { j.prizePct = v; Store.markDirty(); }
        }));

        for (int i = 0; i < j.items.size(); i++) {
            final Data.JItem it = j.items.get(i);
            int ry = 152 + i * 18;
            addDrawableChild(Ui.field(textRenderer, x, ry, 106, 16, it.name, 40, s -> { it.name = s; Store.markDirty(); }));
            addDrawableChild(Ui.field(textRenderer, x + 110, ry, 60, 16, Money.exact(it.price), 16, s -> {
                long v = Money.parse(s);
                if (v >= 0) { it.price = v; Store.markDirty(); }
            }));
            addDrawableChild(Ui.field(textRenderer, x + 174, ry, 28, 16, String.valueOf(it.qty), 3, s -> {
                try {
                    int q = Integer.parseInt(s.trim());
                    if (q >= 0 && q <= 999) { it.qty = q; Store.markDirty(); }
                } catch (Exception ignored) {}
            }));
            addDrawableChild(Ui.btn("S", x + 206, ry, 16, 16, () -> savePreset(it)));
            addDrawableChild(Ui.btn("x", x + 224, ry, 16, 16, () -> {
                if (j.items.size() > 1) { j.items.remove(it); Store.markDirty(); clearAndInit(); }
            }));
        }

        int by = 226;
        if (j.items.size() < 4) {
            addDrawableChild(Ui.btn("+ Item", x, by, 48, 16, () -> {
                j.items.add(new Data.JItem("Item", 0, 1));
                Store.markDirty();
                clearAndInit();
            }));
        }
        List<Data.ItemPreset> ps = Store.D.presets;
        if (!ps.isEmpty()) presetIdx = Math.floorMod(presetIdx, ps.size());
        addDrawableChild(Ui.btn("<", x + 52, by, 16, 16, () -> { presetIdx--; clearAndInit(); }));
        String pl = ps.isEmpty() ? "(no presets)" : ps.get(presetIdx).name + " " + Money.fmt(ps.get(presetIdx).price, 3);
        ButtonWidget lbl = Ui.btn(pl, x + 70, by, 106, 16, () -> {});
        lbl.active = false;
        addDrawableChild(lbl);
        addDrawableChild(Ui.btn(">", x + 178, by, 16, 16, () -> { presetIdx++; clearAndInit(); }));
        addDrawableChild(Ui.btn("Use", x + 196, by, 28, 16, () -> usePreset()));
        addDrawableChild(Ui.btn("Del", x + 226, by, 28, 16, () -> {
            if (!ps.isEmpty()) { ps.remove(presetIdx); Store.markDirty(); clearAndInit(); }
        }));
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private void slots(int d) {
        Data.Jackpot j = J();
        int n = j.slots + d;
        if (n < 1 || n > 9) return;
        if (d < 0 && n < Rounds.minSlots(j)) { status = "Remove players first - slots can never be below the players already in."; return; }
        j.slots = n;
        Rounds.afterChange(j);
        Store.markDirty();
        clearAndInit();
    }

    private void savePreset(Data.JItem it) {
        for (Data.ItemPreset p : Store.D.presets) {
            if (p.name.equalsIgnoreCase(it.name)) { p.price = it.price; status = "Updated preset " + p.name; Store.markDirty(); return; }
        }
        Store.D.presets.add(new Data.ItemPreset(it.name, it.price));
        status = "Saved preset " + it.name;
        Store.markDirty();
        clearAndInit();
    }

    private void usePreset() {
        List<Data.ItemPreset> ps = Store.D.presets;
        Data.Jackpot j = J();
        if (ps.isEmpty()) return;
        if (j.items.size() >= 4) { status = "A jackpot can hold up to 4 items."; return; }
        Data.ItemPreset p = ps.get(presetIdx);
        j.items.add(new Data.JItem(p.name, p.price, 1));
        Store.markDirty();
        clearAndInit();
    }

    // -------------------------------------------------------------- players
    private void players(int x) {
        Data.Jackpot j = J();
        nameField = Ui.field(textRenderer, x, 50, 130, 16, "", 17, s -> {});
        addDrawableChild(nameField);
        addDrawableChild(Ui.btn("Add player", x + 134, 50, 70, 16, this::addPlayer));
        List<Data.Entry> all = Rounds.sorted(j);
        int rows = 9;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, all.size() - rows)));
        for (int i = 0; i < rows && i + scroll < all.size(); i++) {
            final Data.Entry e = all.get(i + scroll);
            int ry = 74 + i * 18;
            Text t = e.paid ? Text.literal("Paid").formatted(Formatting.GREEN) : Text.literal("Unpaid").formatted(Formatting.RED);
            addDrawableChild(Ui.btn(t, x + 180, ry, 44, 16, () -> { e.paid = !e.paid; Store.markDirty(); clearAndInit(); }));
            addDrawableChild(Ui.btn("x", x + 226, ry, 16, 16, () -> { Rounds.remove(j, e); Store.markDirty(); clearAndInit(); }));
        }
        if (all.size() > rows) {
            addDrawableChild(Ui.btn("^", x + 246, 74, 16, 16, () -> { scroll--; clearAndInit(); }));
            addDrawableChild(Ui.btn("v", x + 246, 92, 16, 16, () -> { scroll++; clearAndInit(); }));
        }
    }

    private void addPlayer() {
        String n = nameField.getText().trim();
        if (!n.matches("[A-Za-z0-9_.]{1,17}")) { status = "Enter a valid username first."; return; }
        Data.Jackpot j = J();
        int r = Rounds.addManual(j, n);
        status = r == j.round ? "Added " + n + " (unpaid) to round " + r + "."
                : "Round " + j.round + " is closed/full - " + n + " queued for round " + r + ".";
        Store.markDirty();
        clearAndInit();
    }

    // ---------------------------------------------------------------- round
    private void round(int x) {
        Data.Jackpot j = J();
        addDrawableChild(Ui.btn(j.closed ? "Reopen round" : "Close round", x, 50, 100, 16, () -> {
            if (j.closed) { if (!Rounds.reopen(j)) status = "Round is full - raise the slot count first."; }
            else Rounds.closeRound(j);
            Store.markDirty();
            clearAndInit();
        }));
        addDrawableChild(Ui.btn("Start next round >", x + 104, 50, 120, 16, () -> {
            Rounds.newRound(j);
            status = "Round " + j.round + " started. Queued late payments were moved in.";
            Store.markDirty();
            clearAndInit();
        }));

        addDrawableChild(Ui.field(textRenderer, x + 120, 74, 44, 16, String.valueOf(j.roundSeconds), 5, s -> {
            try { int v = Integer.parseInt(s.trim()); if (v >= 0) { j.roundSeconds = v; Store.markDirty(); } } catch (Exception ignored) {}
        }));
        addDrawableChild(Ui.btn("Start", x + 168, 74, 44, 16, () -> {
            if (!j.closed) { j.closesAt = System.currentTimeMillis() + j.roundSeconds * 1000L; Store.markDirty(); }
        }));
        addDrawableChild(Ui.btn("Stop", x + 214, 74, 40, 16, () -> { j.closesAt = 0; Store.markDirty(); }));

        addDrawableChild(Ui.field(textRenderer, x + 170, 96, 40, 16, String.valueOf(j.nextDelay), 5, s -> {
            try { int v = Integer.parseInt(s.trim()); if (v >= 0) { j.nextDelay = v; Store.markDirty(); } } catch (Exception ignored) {}
        }));
        addDrawableChild(Ui.btn(Ui.onoff("Auto-start timer on each new round", j.autoTimer), x, 118, 250, 16,
                () -> { j.autoTimer = !j.autoTimer; Store.markDirty(); clearAndInit(); }));

        addDrawableChild(Ui.btn("Draw winner", x, 146, 90, 16, () -> {
            String w = Rounds.drawWinner(j);
            status = w.isEmpty() ? "No players in this round." : "Winner: " + w;
            Store.markDirty();
            clearAndInit();
        }));
        addDrawableChild(Ui.field(textRenderer, x + 94, 146, 130, 16, j.winner, 17, s -> { j.winner = s; Store.markDirty(); }));
        addDrawableChild(Ui.btn("Log prize owed to winner + copy", x, 168, 200, 16, () -> {
            if (j.winner.isEmpty()) { status = "Pick or draw a winner first."; return; }
            Pay.recordJackpotWin();
            status = "Logged in History and copied. Nothing was sent - you pay manually.";
        }));
    }

    // ----------------------------------------------------------------- draw
    @Override
    protected void draw(DrawContext c, int mx, int my) {
        int x = L();
        Data.Jackpot j = J();
        if (page == 0) {
            tx(c, "Title", x, 54, 0xFFFFFF);
            tx(c, "Slots", x, 76, 0xFFFFFF);
            tx(c, String.valueOf(j.slots), x + 76, 76, 0xFFFF55);
            tx(c, "Tax %", x, 120, 0xFFFFFF);
            tx(c, "Prize %", x + 96, 120, 0xFFFFFF);
            tx(c, "Items:  name  |  price  |  per entry  |  S = save preset", x, 140, 0xAAAAAA);
        } else if (page == 1) {
            List<Data.Entry> all = Rounds.sorted(j);
            long cost = Calc.entryCost(j);
            for (int i = 0; i < 9 && i + scroll < all.size(); i++) {
                Data.Entry e = all.get(i + scroll);
                int idx = Rounds.in(j, e.round).indexOf(e) + 1;
                String t = "R" + e.round + " #" + idx + " " + e.player + " " + Money.fmt(cost, 3);
                tx(c, textRenderer.trimToWidth(t, 172), x, 78 + i * 18, e.paid ? 0xFFFFFF : 0xFF5555);
            }
            if (all.isEmpty()) tx(c, "No players yet. Paid entries are added automatically from the payment screen.", x, 80, 0xAAAAAA);
        } else {
            tx(c, "Round " + j.round + (j.closed ? " - CLOSED" : " - open") + "   queued: " + Rounds.queued(j), x, 36, j.closed ? 0xFF5555 : 0x55FF55);
            tx(c, "Round timer (sec)", x, 78, 0xFFFFFF);
            tx(c, "Auto next-round delay (sec, 0 = manual)", x, 100, 0xAAAAAA);
            tx(c, "Winner", x, 136, 0xFFFFFF);
        }

        // live calculation panel
        int px = x + 282, py = 50;
        panel(c, px, py, 178, 206);
        Calc.Result r = Calc.of(j);
        int pd = Store.D.settings.prizeDecimals;
        int y = py + 5;
        tx(c, "LIVE CALCULATION", px + 5, y, 0xFFD700); y += 12;
        tx(c, "Entries: " + r.filled + "/" + r.slots + "  (paid " + r.paidCount + ")", px + 5, y, 0xFFFFFF); y += 11;
        tx(c, "Items/entry: " + r.perEntryItems + "  Total: " + r.totalItems, px + 5, y, 0xFFFFFF); y += 11;
        int shown = 0;
        for (var en : r.itemCounts.entrySet()) {
            if (shown++ >= 3) break;
            tx(c, textRenderer.trimToWidth("  " + en.getKey() + " x" + en.getValue(), 168), px + 5, y, 0xAAAAAA); y += 11;
        }
        tx(c, "Entry cost: " + Money.fmt(r.entryCost, pd), px + 5, y, 0xFFFFFF); y += 11;
        tx(c, "Total value: " + Money.fmt(r.totalValue, pd), px + 5, y, 0xFFFFFF); y += 11;
        tx(c, "Tax (" + trim(j.taxPct) + "%): -" + Money.fmt(r.tax, pd), px + 5, y, 0xFF8888); y += 11;
        tx(c, "After tax: " + Money.fmt(r.afterTax, pd), px + 5, y, 0xFFFFFF); y += 11;
        tx(c, "PRIZE: " + Money.fmt(r.prize, pd) + (r.manual ? " (manual)" : ""), px + 5, y, 0x55FF55); y += 14;
        tx(c, "Players", px + 5, y, 0xFFD700); y += 11;
        int n = 0;
        for (Calc.PlayerRow p : r.players) {
            if (n++ >= 6) { tx(c, "...", px + 5, y, 0xAAAAAA); break; }
            tx(c, textRenderer.trimToWidth(p.name + " " + p.entries + "x " + Money.fmt(p.contributed, pd), 168), px + 5, y, 0xFFFFFF);
            y += 11;
        }
    }
}
