package dev.jackpot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Draws the jackpot scoreboard and the payment notification. Everything is recomputed each frame. */
public final class HudRenderer {
    private HudRenderer() {}

    private record Line(Text text, int color) {}

    private static String flashMsg = "";
    private static long flashUntil;

    public static void flash(String s) {
        flashMsg = s;
        flashUntil = System.currentTimeMillis() + 3000;
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen instanceof HudEditorScreen) return;
        Data.Layout l = Store.D.layout;
        if (l.board.enabled) board(ctx, true);
        if (l.notif.enabled) notif(ctx, true, false);
    }

    private static Line line(String s, int rgb) { return new Line(Text.literal(s), 0xFF000000 | rgb); }

    private static String mmss(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private static String itemsText(Data.Jackpot j) {
        StringBuilder sb = new StringBuilder();
        for (Data.JItem i : j.items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(i.name);
            if (i.qty > 1) sb.append(" x").append(i.qty);
        }
        return sb.toString();
    }

    /** @return {x, y, w, h} of the board on screen. Pass ctx = null with draw = false to only measure. */
    public static int[] board(DrawContext ctx, boolean draw) {
        Data d = Store.D;
        Data.Layout l = d.layout;
        Data.El e = l.board;
        Data.Jackpot j = d.jackpot;
        Calc.Result c = Calc.of(j);
        int pd = d.settings.prizeDecimals;
        boolean compact = e.mode.equals("compact"), wide = e.mode.equals("wide");
        int white = e.fg;
        List<Line> lines = new ArrayList<>();
        if (l.showTitle) {
            lines.add(new Line(Text.literal(j.title + " " + c.filled + "/" + c.slots).formatted(Formatting.BOLD),
                    0xFF000000 | e.accent));
        }
        if (!compact && l.showItem) lines.add(line("Item: " + itemsText(j), white));
        if (!compact && l.showEntry) lines.add(line("Entry: " + Money.fmt(c.entryCost, pd), white));
        if (wide && l.showTax) {
            lines.add(line("Total: " + Money.fmt(c.totalValue, pd) + "  Tax: " + Money.fmt(c.tax, pd), 0xAAAAAA));
        }
        if (l.showPrize) lines.add(line("Prize: " + Money.fmt(c.prize, pd) + (c.manual ? " (manual)" : ""), white));
        if (l.showWinner) {
            lines.add(line("WINNER: " + (j.winner.isEmpty() ? "-" : j.winner), j.winner.isEmpty() ? 0xAAAAAA : 0x55FF55));
        }
        long now = System.currentTimeMillis();
        if (!compact && l.showCountdown) {
            if (j.closed) {
                lines.add(line(j.nextAt > now ? "Next round in " + mmss(j.nextAt - now) : "Round " + j.round + " closed", 0xFF5555));
            } else {
                lines.add(line(j.closesAt > now ? "Closes in " + mmss(j.closesAt - now) : "Round " + j.round + " open", 0xAAAAAA));
            }
        }
        if (!compact && l.showEntries) {
            List<Data.Entry> cur = Rounds.in(j, j.round);
            for (int i = 0; i < j.slots; i++) {
                if (i < cur.size()) {
                    Data.Entry en = cur.get(i);
                    lines.add(line((i + 1) + ". " + en.player + "  " + Money.fmt(c.entryCost, pd) + (en.paid ? "" : " (unpaid)"),
                            en.paid ? white : 0xFF5555));
                } else if (l.showEmpty) {
                    lines.add(line((i + 1) + ". -", 0x777777));
                }
            }
        }
        int q = Rounds.queued(j);
        if (!compact && q > 0) lines.add(line("+" + q + " queued for next round", 0x55FFFF));
        return layout(ctx, e, lines, draw);
    }

    public static int[] notif(DrawContext ctx, boolean draw, boolean preview) {
        Data d = Store.D;
        Data.El e = d.layout.notif;
        boolean compact = e.mode.equals("compact");
        int pd = d.settings.decimals;
        List<Line> lines = new ArrayList<>();
        boolean flashing = System.currentTimeMillis() < flashUntil;
        if (flashing) lines.add(line(flashMsg, 0x55FF55));
        List<Data.Payment> pend = d.pending;
        if (pend.isEmpty() && preview && !flashing) {
            lines.add(new Line(Text.literal("Payments (1)").formatted(Formatting.BOLD), 0xFF000000 | e.accent));
            lines.add(line("ExamplePlayer paid you 5.1M", e.fg));
        } else if (!pend.isEmpty()) {
            if (!compact) {
                lines.add(new Line(Text.literal("Payments (" + pend.size() + ")").formatted(Formatting.BOLD), 0xFF000000 | e.accent));
            }
            int show = compact ? 1 : Math.min(3, pend.size());
            for (int i = 0; i < show; i++) {
                Data.Payment p = pend.get(i);
                lines.add(line(p.player + " paid you " + Money.fmt(p.amount, pd) + (p.test ? " [TEST]" : ""), e.fg));
            }
            if (!compact) {
                String key = JackpotMod.REVIEW == null ? "K" : JackpotMod.REVIEW.getBoundKeyLocalizedText().getString();
                lines.add(line("Press [" + key + "] to review", 0xAAAAAA));
            }
        }
        if (lines.isEmpty()) return new int[] {e.x, e.y, 0, 0};
        return layout(ctx, e, lines, draw);
    }

    private static int[] layout(DrawContext ctx, Data.El e, List<Line> lines, boolean draw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        float s = Math.max(0.4f, e.scale);
        int pad = e.pad;
        float lh = (9 + e.spacing) * s;
        int maxW = 0;
        for (Line ln : lines) maxW = Math.max(maxW, tr.getWidth(ln.text()));
        int w = e.w > 0 ? e.w : (int) Math.ceil(maxW * s) + pad * 2;
        int h = e.h > 0 ? e.h : (int) Math.ceil(lines.size() * lh - e.spacing * s) + pad * 2;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int x = Math.max(0, Math.min(e.x, sw - w));
        int y = Math.max(0, Math.min(e.y, sh - h));
        if (draw && ctx != null) {
            ctx.fill(x, y, x + w, y + h, (e.alpha << 24) | (e.bg & 0xFFFFFF));
            int bc = 0xFF000000 | e.accent;
            ctx.fill(x, y, x + w, y + 1, bc);
            ctx.fill(x, y + h - 1, x + w, y + h, bc);
            ctx.fill(x, y, x + 1, y + h, bc);
            ctx.fill(x + w - 1, y, x + w, y + h, bc);
            for (int i = 0; i < lines.size(); i++) {
                float ty = y + pad + i * lh;
                if (ty + 9 * s > y + h - pad + 1) break;
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(x + pad, ty);
                ctx.getMatrices().scale(s, s);
                ctx.drawTextWithShadow(tr, lines.get(i).text(), 0, 0, lines.get(i).color());
                ctx.getMatrices().popMatrix();
            }
        }
        return new int[] {x, y, w, h};
    }
}
