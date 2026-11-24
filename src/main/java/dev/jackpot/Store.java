package dev.jackpot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Store {
    private Store() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data D = new Data();
    private static boolean dirty;
    private static long lastSave;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("jackpotmanager.json");
    }

    public static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                Data d = GSON.fromJson(Files.readString(p), Data.class);
                if (d != null) D = d;
            }
        } catch (Exception e) {
            try { Files.copy(p, p.resolveSibling("jackpotmanager.json.bak"), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {}
        }
        seed();
    }

    private static void seed() {
        if (D.settings == null) D.settings = new Data.Settings();
        if (D.jackpot == null) D.jackpot = new Data.Jackpot();
        if (D.jackpot.items == null || D.jackpot.items.isEmpty()) D.jackpot.items = new java.util.ArrayList<>(java.util.List.of(new Data.JItem()));
        if (D.jackpot.entries == null) D.jackpot.entries = new java.util.ArrayList<>();
        if (D.games == null) D.games = new java.util.ArrayList<>();
        if (D.presets == null) D.presets = new java.util.ArrayList<>();
        if (D.history == null) D.history = new java.util.ArrayList<>();
        if (D.pending == null) D.pending = new java.util.ArrayList<>();
        if (D.layout == null) D.layout = new Data.Layout();
        if (D.layouts == null) D.layouts = new java.util.LinkedHashMap<>();
        if (D.games.isEmpty()) {
            D.games.add(new Data.GameCfg("blackjack", "Blackjack"));
            D.games.add(new Data.GameCfg("arrow", "Arrow Game"));
            D.games.add(new Data.GameCfg("paper", "Paper Game"));
            D.games.add(new Data.GameCfg("elytra", "Elytra Game"));
            D.games.add(new Data.GameCfg("dragonarrow", "Dragon Head Arrow Game"));
        }
        if (D.presets.isEmpty()) D.presets.add(new Data.ItemPreset("Totem of Undying", 30_200_000L));
        D.jackpot.slots = Math.max(1, Math.min(9, D.jackpot.slots));
    }

    public static void markDirty() { dirty = true; }

    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSave > 1500) save();
    }

    public static void save() {
        try {
            Path p = path();
            Path tmp = p.resolveSibling("jackpotmanager.json.tmp");
            Files.writeString(tmp, GSON.toJson(D));
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
            lastSave = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    public static Data.Layout copyLayout(Data.Layout l) {
        return GSON.fromJson(GSON.toJson(l), Data.Layout.class);
    }
}
