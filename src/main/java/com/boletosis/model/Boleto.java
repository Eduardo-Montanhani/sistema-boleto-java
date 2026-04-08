package com.boletosis.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Boleto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String company;
    private LocalDate dueDate;
    private double value;
    private boolean paid;

    private static final DateTimeFormatter BRAZILIAN_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Boleto(String name, String company, LocalDate dueDate, double value) {
        this.name = name;
        this.company = company;
        this.dueDate = dueDate;
        this.value = value;
        this.paid = false;
    }

    public String getName() { return name; }
    public String getCompany() { return company; }
    public LocalDate getDueDate() { return dueDate; }
    public double getValue() { return value; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public String getFormattedDate() { return dueDate.format(BRAZILIAN_FORMAT); }

    public String getStatus() {
        if (paid) return "Pago";
        return dueDate.isBefore(LocalDate.now()) ? "Vencido" : "Pendente";
    }
}