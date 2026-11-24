package dev.jackpot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All automatic calculations. Pure functions - recomputed every frame, so nothing can go stale. */
public final class Calc {
    private Calc() {}

    public static final class PlayerRow {
        public String name; public int entries; public long contributed;
    }

    public static final class Result {
        public int filled, slots, paidCount, perEntryItems;
        public long entryCost, totalItems, totalValue, tax, afterTax, prize;
        public boolean manual;
        public Map<String, Long> itemCounts = new LinkedHashMap<>();
        public List<PlayerRow> players = new ArrayList<>();
    }

    public static long entryCost(Data.Jackpot j) {
        long c = 0;
        for (Data.JItem i : j.items) c += i.price * Math.max(0, i.qty);
        return c;
    }

    public static int itemsPerEntry(Data.Jackpot j) {
        int n = 0;
        for (Data.JItem i : j.items) n += Math.max(0, i.qty);
        return n;
    }

    public static long pct(long v, double pct) {
        return BigDecimal.valueOf(v).multiply(BigDecimal.valueOf(pct))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).longValue();
    }

    public static long payout(long bet, double mult) {
        return BigDecimal.valueOf(bet).multiply(BigDecimal.valueOf(mult)).setScale(0, RoundingMode.DOWN).longValue();
    }

    public static String copyText(String format, String player, long amount, int decimals) {
        return format.replace("{player}", player).replace("{amount}", Money.fmt(amount, decimals));
    }

    public static Result of(Data.Jackpot j) {
        Result r = new Result();
        r.slots = j.slots;
        List<Data.Entry> cur = Rounds.in(j, j.round);
        r.filled = cur.size();
        r.entryCost = entryCost(j);
        r.perEntryItems = itemsPerEntry(j);
        r.totalItems = (long) r.filled * r.perEntryItems;
        r.totalValue = r.entryCost * r.filled;
        r.tax = pct(r.totalValue, j.taxPct);
        r.afterTax = r.totalValue - r.tax;
        r.manual = j.manualPrize >= 0;
        r.prize = r.manual ? j.manualPrize : pct(r.afterTax, j.prizePct);
        for (Data.JItem i : j.items) {
            r.itemCounts.merge(i.name, (long) i.qty * r.filled, Long::sum);
        }
        for (Data.Entry e : cur) {
            if (e.paid) r.paidCount++;
            PlayerRow row = null;
            for (PlayerRow p : r.players) if (p.name.equalsIgnoreCase(e.player)) { row = p; break; }
            if (row == null) { row = new PlayerRow(); row.name = e.player; r.players.add(row); }
            row.entries++;
            row.contributed += r.entryCost;
        }
        return r;
    }
}
