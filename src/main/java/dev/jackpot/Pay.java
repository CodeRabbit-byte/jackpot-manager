package dev.jackpot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Payment detection (from chat messages the server already shows you) and payment processing. Never sends anything. */
public final class Pay {
    private Pay() {}

    public record Match(String player, long amount) {}

    private static Pattern pat;
    private static String patSrc = "";
    private static String lastText = "";
    private static long lastTime;
    public static boolean simulating;

    public static Match parse(String txt) {
        Data.Settings s = Store.D.settings;
        try {
            if (pat == null || !patSrc.equals(s.payPattern)) {
                pat = Pattern.compile(s.payPattern);
                patSrc = s.payPattern;
            }
            Matcher m = pat.matcher(txt.strip());
            if (!m.find()) return null;
            String player = m.group("player");
            String amt = m.group("amount");
            if (player == null || amt == null) return null;
            long a = Money.parse(amt.trim());
            return a > 0 ? new Match(player, a) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void onMessage(Text t) {
        if (!Store.D.settings.detect) return;
        String txt = t.getString();
        Match m = parse(txt);
        if (m == null) return;
        long now = System.currentTimeMillis();
        if (txt.equals(lastText) && now - lastTime < 1500) return; // servers sometimes repeat a line
        lastText = txt;
        lastTime = now;
        add(m.player(), m.amount(), simulating);
    }

    public static Data.Payment add(String player, long amount, boolean test) {
        Data.Payment p = new Data.Payment();
        p.player = player;
        p.amount = amount;
        p.test = test;
        Store.D.pending.add(p);
        Store.markDirty();
        if (Store.D.settings.sound) {
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        }
        return p;
    }

    public static Data.GameCfg game(String id) {
        for (Data.GameCfg g : Store.D.games) if (g.id.equals(id)) return g;
        return null;
    }

    public static void copy(String s) {
        MinecraftClient.getInstance().keyboard.setClipboard(s);
        HudRenderer.flash("Copied: " + s);
    }

    public static String copyText(Data.Payment p, long amount) {
        return Calc.copyText(Store.D.settings.copyFormat, p.player, amount, Store.D.settings.decimals);
    }

    private static void finish(Data.Payment p) {
        Store.D.pending.remove(p);
        if (!Store.D.history.contains(p)) Store.D.history.add(p);
        while (Store.D.history.size() > 5000) Store.D.history.remove(0);
        Store.markDirty();
    }

    public static String win(Data.Payment p, Data.GameCfg g) {
        p.gameId = g.id;
        p.gameName = g.name;
        p.result = "WIN";
        p.owed = Calc.payout(p.amount, g.mult);
        p.done = false;
        Store.D.lastGame = g.id;
        finish(p);
        String text = copyText(p, p.owed);
        copy(text);
        return text;
    }

    public static String lose(Data.Payment p, Data.GameCfg g) {
        p.gameId = g.id;
        p.gameName = g.name;
        p.result = "LOSS";
        p.owed = 0;
        p.done = true;
        Store.D.lastGame = g.id;
        finish(p);
        copy(p.player);
        return p.player;
    }

    public static int jackpotEntry(Data.Payment p) {
        Data.Jackpot j = Store.D.jackpot;
        int round = Rounds.addPaid(j, p.player, p.amount, p.id);
        p.gameId = "jackpot";
        p.gameName = j.title;
        p.result = "ENTRY";
        p.round = round;
        p.done = true;
        finish(p);
        return round;
    }

    public static void ignore(Data.Payment p) {
        p.result = "IGNORED";
        p.done = true;
        finish(p);
    }

    /** Puts a processed (non-jackpot) payment back in the queue. */
    public static void undo(Data.Payment p) {
        Store.D.history.remove(p);
        p.result = "NEW";
        p.owed = 0;
        p.done = false;
        Store.D.pending.add(0, p);
        Store.markDirty();
    }

    /** Logs what the jackpot owes its winner so it is not forgotten. Nothing is paid automatically. */
    public static void recordJackpotWin() {
        Data.Jackpot j = Store.D.jackpot;
        if (j.winner.isEmpty()) return;
        Data.Payment p = new Data.Payment();
        p.player = j.winner;
        p.gameId = "jackpot";
        p.gameName = j.title + " prize";
        p.result = "WIN";
        p.owed = Calc.of(j).prize;
        p.round = j.round;
        p.done = false;
        finish(p);
        copy(copyText(p, p.owed));
    }
}
