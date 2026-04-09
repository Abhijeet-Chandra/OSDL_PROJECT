package com.hotel.model;

public class BillLine {

    private Integer id;
    private int menuItemId;
    private String name;
    private int unitPriceCents;
    private int qty;
    private int lineTotalCents;

    public BillLine() {
    }

    public BillLine(Integer id, int menuItemId, String name, int unitPriceCents, int qty, int lineTotalCents) {
        this.id = id;
        this.menuItemId = menuItemId;
        this.name = name;
        this.unitPriceCents = unitPriceCents;
        this.qty = qty;
        this.lineTotalCents = lineTotalCents;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(int unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getLineTotalCents() {
        return lineTotalCents;
    }

    public void setLineTotalCents(int lineTotalCents) {
        this.lineTotalCents = lineTotalCents;
    }

    public void recomputeLineTotal() {
        this.lineTotalCents = unitPriceCents * qty;
    }
}
