package com.hotel.model;

import java.util.Objects;

public class MenuItem {

    private int id;
    private String name;
    private String category;
    private int priceCents;
    private boolean active;

    public MenuItem() {
    }

    public MenuItem(int id, String name, String category, int priceCents, boolean active) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.priceCents = priceCents;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(int priceCents) {
        this.priceCents = priceCents;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name + " — " + Money.format(priceCents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MenuItem menuItem)) {
            return false;
        }
        return id == menuItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
