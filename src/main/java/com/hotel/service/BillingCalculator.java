package com.hotel.service;

import com.hotel.model.BillLine;

import java.util.List;

public final class BillingCalculator {

    /** 5% GST example — adjust if your rubric expects a different rate. */
    public static final double TAX_RATE = 0.05;

    private BillingCalculator() {
    }

    public static int subtotalCents(List<BillLine> lines) {
        int s = 0;
        for (BillLine line : lines) {
            s += line.getLineTotalCents();
        }
        return s;
    }

    public static int taxCents(int subtotalCents) {
        return (int) Math.round(subtotalCents * TAX_RATE);
    }

    public static int totalCents(int subtotalCents, int taxCents) {
        return subtotalCents + taxCents;
    }
}
