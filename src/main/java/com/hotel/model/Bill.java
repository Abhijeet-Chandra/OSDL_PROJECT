package com.hotel.model;

import java.time.LocalDateTime;

public class Bill {

    private int id;
    private LocalDateTime createdAt;
    private String studentId;
    private int subtotalCents;
    private int taxCents;
    private int totalCents;

    public Bill() {
    }

    public Bill(int id, LocalDateTime createdAt, String studentId, int subtotalCents, int taxCents, int totalCents) {
        this.id = id;
        this.createdAt = createdAt;
        this.studentId = studentId;
        this.subtotalCents = subtotalCents;
        this.taxCents = taxCents;
        this.totalCents = totalCents;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(int subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public int getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(int taxCents) {
        this.taxCents = taxCents;
    }

    public int getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(int totalCents) {
        this.totalCents = totalCents;
    }
}
