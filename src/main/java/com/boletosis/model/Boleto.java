package com.boletosis.model;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Boleto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String company;
    private LocalDate dueDate;
    private boolean paid;

    // Formatador brasileiro estático para usar em toda a classe
    private static final DateTimeFormatter BRAZILIAN_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Boleto(String name, String company, LocalDate dueDate) {
        this.name = name;
        this.company = company;
        this.dueDate = dueDate;
        this.paid = false;
    }

    public String getName() { return name; }
    public String getCompany() { return company; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    // Retorna a data como String no formato PT-BR
    public String getFormattedDate() {
        return dueDate.format(BRAZILIAN_FORMAT);
    }

    public String getStatus() {
        if (paid) return "Pago";
        return dueDate.isBefore(LocalDate.now()) ? "Vencido" : "Pendente";
    }
}