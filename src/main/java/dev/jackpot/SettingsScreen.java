package dev.jackpot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SettingsScreen extends BaseScreen {
    private static String sample = "Player123 paid you $5.1M";
    private static String testPlayer = "TestPlayer";
    private static String testAmount = "5.1M";

    public SettingsScreen() { super("Settings & Test Lab", 3); }

    @Override
    protected void init() {
        tabs();
        int x = L();
        Data.Settings s = Store.D.settings;
        addDrawableChild(Ui.btn(Ui.onoff("Payment detection", s.detect), x, 32, 130, 16, () -> { s.detect = !s.detect; Store.markDirty(); clearAndInit(); }));
        addDrawableChild(Ui.btn(Ui.onoff("Sound", s.sound), x + 134, 32, 80, 16, () -> { s.sound = !s.sound; Store.markDirty(); clearAndInit(); }));

        addDrawableChild(Ui.field(textRenderer, x + 100, 54, 30, 16, String.valueOf(s.decimals), 1, v -> {
            try { int n = Integer.parseInt(v.trim()); if (n >= 0 && n <= 6) { s.decimals = n; Store.markDirty(); } } catch (Exception ignored) {}
        }));
        addDrawableChild(Ui.field(textRenderer, x + 250, 54, 30, 16, String.valueOf(s.prizeDecimals), 1, v -> {
            try { int n = Integer.parseInt(v.trim()); if (n >= 0 && n <= 6) { s.prizeDecimals = n; Store.markDirty(); } } catch (Exception ignored) {}
        }));
        addDrawableChild(Ui.field(textRenderer, x + 100, 76, 200, 16, s.copyFormat, 60, v -> { s.copyFormat = v; Store.markDirty(); }));

        addDrawableChild(Ui.field(textRenderer, x, 110, 380, 16, s.payPattern, 300, v -> { s.payPattern = v; Store.markDirty(); }));
        addDrawableChild(Ui.btn("Reset", x + 384, 110, 60, 16, () -> { s.payPattern = Data.DEFAULT_PATTERN; Store.markDirty(); clearAndInit(); }));

        addDrawableChild(Ui.field(textRenderer, x, 156, 300, 16, sample, 120, v -> sample = v));
        addDrawableChild(Ui.btn("Feed as chat", x + 304, 156, 90, 16, () -> {
            Pay.simulating = true;
            try { Pay.onMessage(Text.literal(sample)); } finally { Pay.simulating = false; }
            status = "Fed through the real detector. Check the HUD alert (marked [TEST]).";
        }));

        addDrawableChild(Ui.field(textRenderer, x, 196, 100, 16, testPlayer, 17, v -> testPlayer = v));
        addDrawableChild(Ui.field(textRenderer, x + 104, 196, 60, 16, testAmount, 12, v -> testAmount = v));
        addDrawableChild(Ui.btn("Simulate payment", x + 168, 196, 110, 16, () -> {
            long a = Money.parse(testAmount);
            if (a <= 0 || !testPlayer.matches("[A-Za-z0-9_.]{1,17}")) { status = "Enter a valid test name and amount."; return; }
            Pay.add(testPlayer, a, true);
            status = "Test payment queued. Press the review key to process it.";
        }));
        addDrawableChild(Ui.btn("Fill jackpot", x + 282, 196, 80, 16, () -> {
            Data.Jackpot j = Store.D.jackpot;
            int n = 0;
            while (Rounds.canJoin(j, j.round, "Test" + (n + 1)) && !j.closed && n < 9) {
                Rounds.addManual(j, "Test" + (n + 1));
                Data.Entry e = j.entries.get(j.entries.size() - 1);
                e.test = true;
                e.paid = true;
                n++;
            }
            Store.markDirty();
            status = "Added " + n + " test players.";
        }));
        addDrawableChild(Ui.btn("Clear test data", x + 366, 196, 94, 16, () -> {
            Store.D.jackpot.entries.removeIf(e -> e.test);
            Store.D.history.removeIf(p -> p.test);
            Store.D.pending.removeIf(p -> p.test);
            Store.markDirty();
            status = "All test data removed.";
        }));
    }

    @Override
    protected void draw(DrawContext c, int mx, int my) {
        int x = L();
        tx(c, "Payout decimals", x, 58, 0xFFFFFF);
        tx(c, "Prize decimals", x + 170, 58, 0xFFFFFF);
        tx(c, "Copy format", x, 80, 0xFFFFFF);
        tx(c, "{player} {amount}  (e.g. /pay {player} {amount} - you still press Enter yourself)", x + 100, 94, 0x888888);
        tx(c, "Payment detection pattern (regex with groups 'player' and 'amount')", x, 100, 0xAAAAAA);
        tx(c, "TEST LAB - nothing here touches a server", x, 136, 0xFFD700);
        tx(c, "Sample chat line", x, 146, 0xAAAAAA);
        Pay.Match m = Pay.parse(sample);
        if (m != null) tx(c, "Detected: " + m.player() + " paid " + Money.fmt(m.amount(), 3), x, 176, 0x55FF55);
        else tx(c, "Not detected by the current pattern", x, 176, 0xFF5555);
        tx(c, "Simulate a payment (flows through the whole system):", x, 186, 0xAAAAAA);
        tx(c, "Everything simulated is marked [TEST] / * and removed by 'Clear test data'.", x, 220, 0x888888);
    }
}
