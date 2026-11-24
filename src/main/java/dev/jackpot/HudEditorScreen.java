package dev.jackpot;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Drag the board / alert boxes anywhere; the panel below edits size, text, colours, elements and saved layouts. */
public class HudEditorScreen extends Screen {
    private int sel = 0;            // 0 = jackpot board, 1 = payment alert
    private int dragging = -1;
    private double offX, offY;
    private boolean panelOpen = true;
    private int layoutIdx = 0;
    private String layoutName = "My layout";

    public HudEditorScreen() { super(Text.literal("HUD Editor")); }

    private Data.El el() { return sel == 0 ? Store.D.layout.board : Store.D.layout.notif; }

    private int[] rect(int which) {
        return which == 0 ? HudRenderer.board(null, false) : HudRenderer.notif(null, false, true);
    }

    private int py() { return height - 112; }

    @Override
    protected void init() {
        int px = Math.max(4, width / 2 - 230);
        addDrawableChild(Ui.btn(panelOpen ? "Hide panel" : "Show panel", width - 66, 4, 62, 16, () -> { panelOpen = !panelOpen; clearAndInit(); }));
        addDrawableChild(Ui.btn("Done", width - 66, 22, 62, 16, this::close));
        if (!panelOpen) return;
        Data.Layout l = Store.D.layout;
        Data.El e = el();
        int y = py();
        int x = px;
        x = add(x, y, 84, "Edit: " + (sel == 0 ? "Board" : "Alerts"), () -> { sel = 1 - sel; clearAndInit(); });
        x = addT(x, y, 70, Ui.onoff("Shown", e.enabled), () -> { e.enabled = !e.enabled; Store.markDirty(); clearAndInit(); });
        x = add(x, y, 64, "Mode: " + e.mode, () -> {
            e.mode = e.mode.equals("compact") ? "normal" : e.mode.equals("normal") ? "wide" : "compact";
            Store.markDirty();
            clearAndInit();
        });
        x = add(x, y, 22, "W-", () -> dim(true, -10));
        x = add(x, y, 22, "W+", () -> dim(true, 10));
        x = add(x, y, 22, "H-", () -> dim(false, -6));
        x = add(x, y, 22, "H+", () -> dim(false, 6));
        x = add(x, y, 36, "Auto", () -> { e.w = 0; e.h = 0; Store.markDirty(); });
        add(x, y, 42, "Reset", () -> { e.x = 10; e.y = sel == 0 ? 10 : 130; e.w = 0; e.h = 0; e.scale = 1f; Store.markDirty(); });

        y += 19;
        x = px;
        x = add(x, y, 40, "Txt-", () -> { e.scale = Math.max(0.5f, Math.round((e.scale - 0.1f) * 10) / 10f); Store.markDirty(); });
        x = add(x, y, 40, "Txt+", () -> { e.scale = Math.min(3f, Math.round((e.scale + 0.1f) * 10) / 10f); Store.markDirty(); });
        x = add(x, y, 44, "Alp-", () -> { e.alpha = Math.max(0, e.alpha - 15); Store.markDirty(); });
        x = add(x, y, 44, "Alp+", () -> { e.alpha = Math.min(255, e.alpha + 15); Store.markDirty(); });
        x = add(x, y, 40, "Pad-", () -> { e.pad = Math.max(0, e.pad - 1); Store.markDirty(); });
        x = add(x, y, 40, "Pad+", () -> { e.pad = Math.min(20, e.pad + 1); Store.markDirty(); });
        x = add(x, y, 40, "Gap-", () -> { e.spacing = Math.max(0, e.spacing - 1); Store.markDirty(); });
        add(x, y, 40, "Gap+", () -> { e.spacing = Math.min(12, e.spacing + 1); Store.markDirty(); });

        y += 19;
        addDrawableChild(Ui.field(textRenderer, px + 20, y, 50, 16, Ui.hex(e.bg), 6, s -> { e.bg = Ui.hex(s, e.bg); Store.markDirty(); }));
        addDrawableChild(Ui.field(textRenderer, px + 104, y, 50, 16, Ui.hex(e.fg), 6, s -> { e.fg = Ui.hex(s, e.fg); Store.markDirty(); }));
        addDrawableChild(Ui.field(textRenderer, px + 214, y, 50, 16, Ui.hex(e.accent), 6, s -> { e.accent = Ui.hex(s, e.accent); Store.markDirty(); }));

        y += 19;
        if (sel == 0) {
            x = px;
            x = tog(x, y, "Title", l.showTitle, () -> l.showTitle = !l.showTitle);
            x = tog(x, y, "Item", l.showItem, () -> l.showItem = !l.showItem);
            x = tog(x, y, "Entry", l.showEntry, () -> l.showEntry = !l.showEntry);
            x = tog(x, y, "Prize", l.showPrize, () -> l.showPrize = !l.showPrize);
            x = tog(x, y, "Winner", l.showWinner, () -> l.showWinner = !l.showWinner);
            x = tog(x, y, "Timer", l.showCountdown, () -> l.showCountdown = !l.showCountdown);
            x = tog(x, y, "Slots", l.showEntries, () -> l.showEntries = !l.showEntries);
            x = tog(x, y, "Empty", l.showEmpty, () -> l.showEmpty = !l.showEmpty);
            tog(x, y, "Tax", l.showTax, () -> l.showTax = !l.showTax);
        }

        y += 19;
        x = px;
        addDrawableChild(Ui.field(textRenderer, x, y, 80, 16, layoutName, 20, s -> layoutName = s));
        x += 84;
        x = add(x, y, 38, "Save", () -> {
            if (!layoutName.isBlank()) { Store.D.layouts.put(layoutName.trim(), Store.copyLayout(Store.D.layout)); Store.markDirty(); clearAndInit(); }
        });
        List<String> names = new ArrayList<>(Store.D.layouts.keySet());
        if (!names.isEmpty()) layoutIdx = Math.floorMod(layoutIdx, names.size());
        x = add(x, y, 16, "<", () -> { layoutIdx--; clearAndInit(); });
        ButtonWidget nb = Ui.btn(names.isEmpty() ? "(none saved)" : names.get(layoutIdx), x, y, 90, 16, () -> {});
        nb.active = false;
        addDrawableChild(nb);
        x += 93;
        x = add(x, y, 16, ">", () -> { layoutIdx++; clearAndInit(); });
        x = add(x, y, 38, "Load", () -> {
            if (names.isEmpty()) return;
            Data.Layout src = Store.D.layouts.get(names.get(layoutIdx));
            if (src != null) { Store.D.layout = Store.copyLayout(src); Store.markDirty(); clearAndInit(); }
        });
        add(x, y, 34, "Del", () -> {
            if (names.isEmpty()) return;
            Store.D.layouts.remove(names.get(layoutIdx));
            Store.markDirty();
            clearAndInit();
        });
    }

    private int add(int x, int y, int w, String t, Runnable r) {
        addDrawableChild(Ui.btn(t, x, y, w, 16, r));
        return x + w + 3;
    }

    private int addT(int x, int y, int w, Text t, Runnable r) {
        addDrawableChild(Ui.btn(t, x, y, w, 16, r));
        return x + w + 3;
    }

    private int tog(int x, int y, String label, boolean on, Runnable r) {
        addDrawableChild(Ui.btn(Text.literal(label).formatted(on ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.GRAY),
                x, y, 48, 16, () -> { r.run(); Store.markDirty(); clearAndInit(); }));
        return x + 51;
    }

    private void dim(boolean w, int d) {
        Data.El e = el();
        int[] r = rect(sel);
        if (w) e.w = Math.max(40, (e.w > 0 ? e.w : r[2]) + d);
        else e.h = Math.max(20, (e.h > 0 ? e.h : r[3]) + d);
        Store.markDirty();
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        // transparent: you see the real game behind the editor
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        Data.Layout l = Store.D.layout;
        int[] b = HudRenderer.board(c, true);
        int[] n = HudRenderer.notif(c, true, true);
        int[] cur = sel == 0 ? b : n;
        int hl = 0xFFFFFFFF;
        c.fill(cur[0] - 1, cur[1] - 1, cur[0] + cur[2] + 1, cur[1], hl);
        c.fill(cur[0] - 1, cur[1] + cur[3], cur[0] + cur[2] + 1, cur[1] + cur[3] + 1, hl);
        c.fill(cur[0] - 1, cur[1], cur[0], cur[1] + cur[3], hl);
        c.fill(cur[0] + cur[2], cur[1], cur[0] + cur[2] + 1, cur[1] + cur[3], hl);
        if (panelOpen) c.fill(0, py() - 6, width, height, 0xC0000000);
        super.render(c, mx, my, delta);
        int px = Math.max(4, width / 2 - 230);
        tx(c, "Drag a box to move it. Click a box to select it.", 6, 6, 0xFFFFFF);
        if (panelOpen) {
            Data.El e = el();
            int y = py() + 38;
            tx(c, "Bg", px, y + 4, 0xFFFFFF);
            tx(c, "Text", px + 76, y + 4, 0xFFFFFF);
            tx(c, "Accent", px + 164, y + 4, 0xFFFFFF);
            tx(c, "size " + e.scale + "  alpha " + e.alpha + "  w " + (e.w > 0 ? e.w : "auto") + "  h " + (e.h > 0 ? e.h : "auto"), px + 280, y + 4, 0xAAAAAA);
        }
    }

    private void tx(DrawContext c, String s, int x, int y, int rgb) {
        c.drawTextWithShadow(textRenderer, s, x, y, 0xFF000000 | rgb);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double mx = click.x(), my = click.y();
        for (int which : new int[] {1, 0}) {
            int[] r = rect(which);
            if (r[2] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                if (sel != which) { sel = which; clearAndInit(); }
                dragging = which;
                offX = mx - r[0];
                offY = my - r[1];
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging >= 0) {
            Data.El e = dragging == 0 ? Store.D.layout.board : Store.D.layout.notif;
            e.x = (int) (click.x() - offX);
            e.y = (int) (click.y() - offY);
            Store.markDirty();
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = -1;
        return super.mouseReleased(click);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() { Store.markDirty(); }
}
