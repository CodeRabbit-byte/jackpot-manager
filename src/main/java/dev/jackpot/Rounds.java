package dev.jackpot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Round / slot / late-payment logic. Never exceeds the slot limit, never duplicates a payment. */
public final class Rounds {
    private Rounds() {}

    private static final Random RNG = new Random();

    public static List<Data.Entry> in(Data.Jackpot j, int r) {
        List<Data.Entry> l = new ArrayList<>();
        for (Data.Entry e : j.entries) if (e.round == r) l.add(e);
        return l;
    }

    public static int count(Data.Jackpot j, int r) {
        int n = 0;
        for (Data.Entry e : j.entries) if (e.round == r) n++;
        return n;
    }

    public static boolean full(Data.Jackpot j, int r) { return count(j, r) >= j.slots; }

    public static boolean has(Data.Jackpot j, int r, String p) {
        for (Data.Entry e : j.entries) if (e.round == r && e.player.equalsIgnoreCase(p)) return true;
        return false;
    }

    public static boolean canJoin(Data.Jackpot j, int r, String p) {
        return !full(j, r) && (j.allowMulti || !has(j, r, p));
    }

    /** First round this player can legally join: current if open, otherwise the next one with room. */
    public static int target(Data.Jackpot j, String p) {
        int r = j.closed ? j.round + 1 : j.round;
        for (int i = 0; i < 500; i++, r++) if (canJoin(j, r, p)) return r;
        return r;
    }

    public static int roundOfPayment(Data.Jackpot j, String payId) {
        for (Data.Entry e : j.entries) if (e.paymentIds.contains(payId)) return e.round;
        return -1;
    }

    /** Applies a detected payment to the jackpot exactly once. Returns the round it belongs to. */
    public static int addPaid(Data.Jackpot j, String player, long amount, String payId) {
        int existing = roundOfPayment(j, payId);
        if (existing >= 0) return existing; // duplicate protection
        long cost = Calc.entryCost(j);
        Data.Entry u = null;
        for (Data.Entry e : j.entries) {
            if (e.player.equalsIgnoreCase(player) && !e.paid && e.round >= j.round && (u == null || e.round < u.round)) u = e;
        }
        if (u != null) {
            if (j.closed && u.round == j.round) u.round = target(j, player); // paid after close -> next round
            u.paidAmount += amount;
            u.paymentIds.add(payId);
            u.paid = u.paidAmount >= cost;
            afterChange(j);
            return u.round;
        }
        int r = target(j, player);
        Data.Entry e = new Data.Entry(player, r);
        e.paidAmount = amount;
        e.paymentIds.add(payId);
        e.paid = amount >= cost;
        j.entries.add(e);
        afterChange(j);
        return r;
    }

    /** Manually adds an unpaid entry. Returns the round it landed in. */
    public static int addManual(Data.Jackpot j, String player) {
        int r = target(j, player);
        j.entries.add(new Data.Entry(player, r));
        afterChange(j);
        return r;
    }

    public static void remove(Data.Jackpot j, Data.Entry e) { j.entries.remove(e); }

    public static void afterChange(Data.Jackpot j) {
        if (j.autoClose && !j.closed && full(j, j.round)) closeRound(j);
    }

    public static void closeRound(Data.Jackpot j) {
        j.closed = true;
        j.closesAt = 0;
        j.nextAt = j.nextDelay > 0 ? System.currentTimeMillis() + j.nextDelay * 1000L : 0;
    }

    public static boolean reopen(Data.Jackpot j) {
        if (full(j, j.round)) return false;
        j.closed = false;
        j.nextAt = 0;
        return true;
    }

    public static void newRound(Data.Jackpot j) {
        j.round++;
        j.entries.removeIf(e -> e.round < j.round);
        j.winner = "";
        j.closed = false;
        j.closesAt = 0;
        j.nextAt = 0;
        if (j.autoClose && full(j, j.round)) closeRound(j);
        else if (j.autoTimer && j.roundSeconds > 0) j.closesAt = System.currentTimeMillis() + j.roundSeconds * 1000L;
    }

    /** @return true if anything changed. */
    public static boolean tick(Data.Jackpot j, long now) {
        if (!j.closed && j.closesAt > 0 && now >= j.closesAt) { closeRound(j); return true; }
        if (j.closed && j.nextAt > 0 && now >= j.nextAt) { newRound(j); return true; }
        return false;
    }

    public static int minSlots(Data.Jackpot j) {
        int m = 1;
        for (Data.Entry e : j.entries) if (e.round >= j.round) m = Math.max(m, count(j, e.round));
        return m;
    }

    public static int queued(Data.Jackpot j) {
        int n = 0;
        for (Data.Entry e : j.entries) if (e.round > j.round) n++;
        return n;
    }

    public static List<Data.Entry> sorted(Data.Jackpot j) {
        List<Data.Entry> l = new ArrayList<>(j.entries);
        l.sort(Comparator.comparingInt(e -> e.round));
        return l;
    }

    public static String drawWinner(Data.Jackpot j) {
        List<Data.Entry> cur = in(j, j.round);
        List<Data.Entry> paid = new ArrayList<>();
        for (Data.Entry e : cur) if (e.paid) paid.add(e);
        List<Data.Entry> pool = paid.isEmpty() ? cur : paid;
        if (pool.isEmpty()) return "";
        j.winner = pool.get(RNG.nextInt(pool.size())).player;
        return j.winner;
    }
}
