package dev.jackpot;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public final class Ui {
    private Ui() {}

    public static ButtonWidget btn(String label, int x, int y, int w, int h, Runnable r) {
        return btn(Text.literal(label), x, y, w, h, r);
    }

    public static ButtonWidget btn(Text label, int x, int y, int w, int h, Runnable r) {
        return ButtonWidget.builder(label, b -> r.run()).dimensions(x, y, w, h).build();
    }

    public static TextFieldWidget field(TextRenderer tr, int x, int y, int w, int h, String val, int max, Consumer<String> on) {
        TextFieldWidget f = new TextFieldWidget(tr, x, y, w, h, Text.empty());
        f.setMaxLength(max);
        f.setText(val);
        f.setChangedListener(on);
        return f;
    }

    public static Text onoff(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF")).formatted(on ? Formatting.GREEN : Formatting.GRAY);
    }

    public static double dbl(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    public static int hex(String s, int def) {
        try { return s.length() == 6 ? Integer.parseInt(s, 16) : def; } catch (Exception e) { return def; }
    }

    public static String hex(int c) { return String.format("%06X", c & 0xFFFFFF); }
}
