package dev.jackpot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Everything that is saved to config/jackpotmanager.json. */
public class Data {
    public Settings settings = new Settings();
    public Jackpot jackpot = new Jackpot();
    public List<GameCfg> games = new ArrayList<>();
    public List<ItemPreset> presets = new ArrayList<>();
    public List<Payment> history = new ArrayList<>();
    public List<Payment> pending = new ArrayList<>();
    public Layout layout = new Layout();
    public Map<String, Layout> layouts = new LinkedHashMap<>();
    public String lastGame = "blackjack";

    public static class Settings {
        public boolean detect = true;
        public boolean sound = true;
        public int decimals = 3;
        public int prizeDecimals = 2;
        public String copyFormat = "{player} {amount}";
        public String payPattern = DEFAULT_PATTERN;
    }

    public static final String DEFAULT_PATTERN =
            "^(?<player>[A-Za-z0-9_.]{3,17}) (?:has )?paid you \\$?(?<amount>[0-9][0-9,]*\\.?[0-9]*\\s?[kKmMbBtT]?)(?![A-Za-z])";

    public static class GameCfg {
        public String id = "";
        public String name = "";
        public boolean enabled = true;
        public long min = 1_000_000L;
        public long max = 100_000_000L;
        public double mult = 1.96;
        public String rules = "";
        public GameCfg() {}
        public GameCfg(String id, String name) { this.id = id; this.name = name; }
    }

    public static class ItemPreset {
        public String name = "";
        public long price;
        public ItemPreset() {}
        public ItemPreset(String n, long p) { name = n; price = p; }
    }

    public static class JItem {
        public String name = "Totem of Undying";
        public long price = 30_200_000L;
        public int qty = 1; // items each player enters per entry
        public JItem() {}
        public JItem(String n, long p, int q) { name = n; price = p; qty = q; }
    }

    public static class Entry {
        public String player = "";
        public int round;
        public boolean paid;
        public long paidAmount;
        public List<String> paymentIds = new ArrayList<>();
        public boolean test;
        public Entry() {}
        public Entry(String p, int r) { player = p; round = r; }
    }

    public static class Jackpot {
        public String title = "JACKPOT";
        public int round = 1;
        public int slots = 9;            // 1..9
        public boolean allowMulti = false;
        public boolean autoClose = true;
        public boolean closed = false;
        public double taxPct = 13;       // 13% reproduces 9 x 30.2M -> 236.46M
        public double prizePct = 100;    // custom prize percentage applied after tax
        public long manualPrize = -1;    // >= 0 overrides the automatic prize
        public List<JItem> items = new ArrayList<>(List.of(new JItem()));
        public List<Entry> entries = new ArrayList<>();
        public String winner = "";
        public int roundSeconds = 300;
        public long closesAt = 0;
        public int nextDelay = 0;        // seconds until next round starts after closing; 0 = manual
        public long nextAt = 0;
        public boolean autoTimer = false;
    }

    public static class Payment {
        public String id = UUID.randomUUID().toString();
        public long time = System.currentTimeMillis();
        public String player = "";
        public long amount;
        public String gameId = "";
        public String gameName = "";
        public String result = "NEW"; // NEW, WIN, LOSS, ENTRY, IGNORED
        public long owed;
        public boolean done;
        public int round;
        public boolean test;
    }

    public static class El {
        public boolean enabled = true;
        public int x = 10, y = 10;
        public int w = 0, h = 0;      // 0 = automatic
        public float scale = 1.0f;
        public int pad = 4, spacing = 2;
        public int bg = 0x000000, fg = 0xFFFFFF, accent = 0xFFD700;
        public int alpha = 170;
        public String mode = "normal"; // compact | normal | wide
    }

    public static class Layout {
        public El board = new El();
        public El notif = new El();
        public boolean showTitle = true, showItem = true, showEntry = true, showPrize = true,
                showWinner = true, showCountdown = true, showEntries = true, showEmpty = true, showTax = true;
        public Layout() { notif.y = 130; notif.accent = 0x55FF55; }
    }
}
