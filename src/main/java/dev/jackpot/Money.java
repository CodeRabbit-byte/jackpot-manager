package dev.jackpot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses and formats amounts like 5.1M, 30.2M, 1.5B. All math is exact (BigDecimal, whole dollars). */
public final class Money {
    private Money() {}

    private static final Pattern P =
            Pattern.compile("^\\$?\\s*([0-9][0-9,]*\\.?[0-9]*|\\.[0-9]+)\\s*([kKmMbBtT]?)$");

    /** @return amount in whole dollars, or -1 if it cannot be parsed. */
    public static long parse(String s) {
        if (s == null) return -1;
        Matcher m = P.matcher(s.trim());
        if (!m.matches()) return -1;
        try {
            BigDecimal v = new BigDecimal(m.group(1).replace(",", ""));
            return v.multiply(mult(m.group(2))).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    private static BigDecimal mult(String u) {
        switch (u.toLowerCase()) {
            case "k": return BigDecimal.valueOf(1_000L);
            case "m": return BigDecimal.valueOf(1_000_000L);
            case "b": return BigDecimal.valueOf(1_000_000_000L);
            case "t": return BigDecimal.valueOf(1_000_000_000_000L);
            default: return BigDecimal.ONE;
        }
    }

    /** Short form, truncated (never rounded up) to {@code dec} decimals: 236466000 -> 236.46M at dec=2. */
    public static String fmt(long v, int dec) {
        boolean neg = v < 0;
        long a = Math.abs(v);
        String suf = "";
        long div = 1;
        if (a >= 1_000_000_000_000L) { suf = "T"; div = 1_000_000_000_000L; }
        else if (a >= 1_000_000_000L) { suf = "B"; div = 1_000_000_000L; }
        else if (a >= 1_000_000L) { suf = "M"; div = 1_000_000L; }
        else if (a >= 1_000L) { suf = "K"; div = 1_000L; }
        BigDecimal r = BigDecimal.valueOf(a).divide(BigDecimal.valueOf(div), Math.max(0, dec), RoundingMode.DOWN)
                .stripTrailingZeros();
        return (neg ? "-" : "") + r.toPlainString() + suf;
    }

    /** Exact form for text fields, so editing never silently changes a value. */
    public static String exact(long v) {
        return fmt(v, 12);
    }
}
