package com.hotel.model;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private Money() {
    }

    public static String format(int cents) {
        double rupees = cents / 100.0;
        return INR.format(rupees);
    }

    public static int parseRupeesToCents(String text) {
        String t = text == null ? "" : text.trim().replace("₹", "").replace(",", "");
        if (t.isEmpty()) {
            return 0;
        }
        double v = Double.parseDouble(t);
        return (int) Math.round(v * 100);
    }
}
