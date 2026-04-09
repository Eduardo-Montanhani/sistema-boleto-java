package com.boletosis.service;

import com.boletosis.model.Boleto;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioService {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void gerarRelatorioProtegido(List<Boleto> boletos, int mes) throws Exception {
        String nomeArquivo = "RELATORIO_MES_" + mes + ".html";
        double total = 0;

        try (PrintWriter out = new PrintWriter(new FileWriter(nomeArquivo))) {
            // CSS para deixar o relatório com cara de PDF profissional
            out.println("<html><head><style>");
            out.println("body { font-family: 'Segoe UI', sans-serif; margin: 40px; color: #333; }");
            out.println("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
            out.println("th { background-color: #2c3e50; color: white; padding: 12px; text-align: left; }");
            out.println("td { padding: 10px; border-bottom: 1px solid #ddd; }");
            out.println(".header { text-align: center; border-bottom: 2px solid #2c3e50; padding-bottom: 10px; }");
            out.println(".footer { margin-top: 30px; text-align: right; font-weight: bold; font-size: 1.2em; }");
            out.println("@media print { .no-print { display: none; } }"); // Esconde botão na impressão
            out.println("</style></head><body>");

            out.println("<div class='header'>");
            out.println("<h1>RELATÓRIO FINANCEIRO MENSAL</h1>");
            out.println("<h3>PERÍODO: MÊS " + String.format("%02d", mes) + " / " + LocalDate.now().getYear() + "</h3>");
            out.println("</div>");

            out.println("<table>");
            out.println("<tr><th>NOME</th><th>EMPRESA</th><th>VENCIMENTO</th><th>VALOR</th></tr>");

            for (Boleto b : boletos) {
                if (b.getDueDate().getMonthValue() == mes) {
                    out.println("<tr>");
                    out.println("<td>" + b.getName() + "</td>");
                    out.println("<td>" + b.getCompany() + "</td>");
                    out.println("<td>" + b.getFormattedDate() + "</td>");
                    out.println("<td>R$ " + String.format("%.2f", b.getValue()) + "</td>");
                    out.println("</tr>");
                    total += b.getValue();
                }
            }

            out.println("</table>");
            out.println("<div class='footer'>TOTAL ACUMULADO: R$ " + String.format("%.2f", total) + "</div>");
            out.println("<p style='font-size: 0.8em; color: #7f8c8d;'>Gerado automaticamente em: " + LocalDate.now().format(dtf) + "</p>");

            out.println("<div class='no-print' style='margin-top:20px;'>");
            out.println("<button onclick='window.print()'>IMPRIMIR / SALVAR PDF</button>");
            out.println("</div>");

            out.println("</body></html>");
        }

        // Abre no navegador padrão
        File file = new File(nomeArquivo);
        Desktop.getDesktop().browse(file.toURI());
    }
}