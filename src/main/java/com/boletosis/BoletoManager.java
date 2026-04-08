package com.boletosis;

import com.boletosis.model.Boleto;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.io.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BoletoManager extends JFrame {
    private List<Boleto> boletos = new ArrayList<>();
    private DefaultTableModel modelPendentes, modelPagos;
    private JTable tablePendentes, tablePagos;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Font FONT_TABELA = new Font("SansSerif", Font.BOLD, 14);
    private final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 16);

    private final Color COLOR_PRIMARY = new Color(41, 128, 185); // Azul
    private final Color COLOR_SUCCESS = new Color(39, 174, 96);  // Verde
    private final Color COLOR_DANGER = new Color(192, 57, 43);   // Vermelho
    private final Color COLOR_BG = new Color(236, 240, 241);

    private JTextField txtFilterMonth = new JTextField(3);
    private JTextField txtFilterDay = new JTextField(3);

    public BoletoManager() {
        setTitle("GESTOR FINANCEIRO - EDUARDO");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(15, 15));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // Criando as Abas
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 16));

        setupTables();

        tabbedPane.addTab(" 🕒 PENDENTES / VENCIDOS ", new JScrollPane(tablePendentes));
        tabbedPane.addTab(" ✅ BOLETOS PAGOS ", new JScrollPane(tablePagos));

        add(tabbedPane, BorderLayout.CENTER);
        setupInputPanel();

        carregarDados();
        verificarAlertasDoDia();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupTables() {
        String[] columns = {"NOME", "EMPRESA", "VALOR (R$)", "VENCIMENTO", "STATUS"};
        modelPendentes = createModel(columns);
        modelPagos = createModel(columns);
        tablePendentes = createStyledTable(modelPendentes);
        tablePagos = createStyledTable(modelPagos);
    }

    private DefaultTableModel createModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(40);
        t.setFont(FONT_TABELA);

        // --- CORREÇÃO DO CABEÇALHO ---
        JTableHeader header = t.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 50));
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(new Color(52, 73, 94));
                setForeground(Color.WHITE);
                setFont(FONT_HEADER);
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(BorderFactory.createLineBorder(new Color(44, 62, 80)));
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);

        // Centralização do corpo
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < t.getColumnCount() - 1; i++) t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

        // Coluna de Status
        t.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(FONT_TABELA);
                if (value != null) {
                    String status = value.toString().toUpperCase();
                    label.setText(status);
                    if ("PAGO".equals(status)) label.setForeground(COLOR_SUCCESS);
                    else if ("VENCIDO".equals(status)) label.setForeground(COLOR_DANGER);
                    else label.setForeground(COLOR_PRIMARY);
                }
                return label;
            }
        });
        return t;
    }

    private void setupInputPanel() {
        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setOpaque(false);

        // Filtros
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("FILTRAR DIA:")); filterPanel.add(txtFilterDay);
        filterPanel.add(new JLabel(" MÊS:")); filterPanel.add(txtFilterMonth);
        JButton btnFilter = new JButton("FILTRAR");
        JButton btnClear = new JButton("LIMPAR");
        filterPanel.add(btnFilter); filterPanel.add(btnClear);

        // Formulário
        JPanel formPanel = new JPanel(new GridLayout(2, 4, 10, 5));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextField txtName = new JTextField();
        JTextField txtCompany = new JTextField();
        JTextField txtValue = new JTextField();
        JFormattedTextField txtDate = createDateField();

        formPanel.add(new JLabel("NOME:")); formPanel.add(new JLabel("EMPRESA:"));
        formPanel.add(new JLabel("VALOR:")); formPanel.add(new JLabel("VENCIMENTO:"));
        formPanel.add(txtName); formPanel.add(txtCompany); formPanel.add(txtValue); formPanel.add(txtDate);

        // Botões
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);

        JButton btnImport = createStyledButton("IMPORTAR .DAT", Color.DARK_GRAY);
        JButton btnExport = createStyledButton("EXPORTAR BACKUP", Color.DARK_GRAY);
        JButton btnDelete = createStyledButton("EXCLUIR", COLOR_DANGER);
        JButton btnPay = createStyledButton("MARCAR PAGO", COLOR_SUCCESS);
        JButton btnAdd = createStyledButton("CADASTRAR", COLOR_PRIMARY);

        buttonPanel.add(btnImport); buttonPanel.add(btnExport);
        buttonPanel.add(btnDelete); buttonPanel.add(btnPay); buttonPanel.add(btnAdd);

        southPanel.add(filterPanel, BorderLayout.NORTH);
        southPanel.add(formPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        // --- EVENTOS ---

        btnAdd.addActionListener(e -> {
            try {
                double val = Double.parseDouble(txtValue.getText().replace(",", "."));
                if (val <= 0) {
                    JOptionPane.showMessageDialog(this, "⚠️ VALOR DEVE SER MAIOR QUE ZERO!", "ERRO", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                LocalDate date = LocalDate.parse(txtDate.getText(), formatter);
                boletos.add(new Boleto(txtName.getText().toUpperCase().trim(), txtCompany.getText().toUpperCase().trim(), date, val));
                updateTable(); salvarDados();
                txtName.setText(""); txtCompany.setText(""); txtValue.setText(""); txtDate.setValue(null);
                txtName.requestFocus();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "ERRO NOS DADOS!"); }
        });

        btnPay.addActionListener(e -> {
            int row = tablePendentes.getSelectedRow();
            if (row >= 0) {
                String nome = (String) modelPendentes.getValueAt(row, 0);
                boletos.stream().filter(b -> b.getName().equalsIgnoreCase(nome) && !b.isPaid()).findFirst().ifPresent(b -> b.setPaid(true));
                updateTable(); salvarDados();
            } else {
                JOptionPane.showMessageDialog(this, "SELECIONE UM BOLETO NA ABA DE PENDENTES!");
            }
        });

        btnDelete.addActionListener(e -> {
            int rowP = tablePendentes.getSelectedRow();
            int rowK = tablePagos.getSelectedRow();
            if (rowP >= 0 || rowK >= 0) {
                if (JOptionPane.showConfirmDialog(this, "DESEJA EXCLUIR?") == 0) {
                    String n = (rowP >= 0) ? (String) modelPendentes.getValueAt(rowP, 0) : (String) modelPagos.getValueAt(rowK, 0);
                    boletos.removeIf(b -> b.getName().equalsIgnoreCase(n));
                    updateTable(); salvarDados();
                }
            }
        });

        btnExport.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            String data = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            fc.setSelectedFile(new File("BACKUP_BOLETOS_" + data + ".dat"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fc.getSelectedFile()))) {
                    oos.writeObject(boletos);
                    JOptionPane.showMessageDialog(this, "BACKUP EXPORTADO!");
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        });

        btnImport.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
                    List<Boleto> novos = (List<Boleto>) ois.readObject();
                    int op = JOptionPane.showConfirmDialog(this, "SUBSTITUIR DADOS ATUAIS?");
                    if (op == JOptionPane.YES_OPTION) boletos = novos;
                    else if (op == JOptionPane.NO_OPTION) boletos.addAll(novos);
                    updateTable(); salvarDados();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "ARQUIVO INVÁLIDO!"); }
            }
        });

        btnFilter.addActionListener(e -> updateTable());
        btnClear.addActionListener(e -> { txtFilterDay.setText(""); txtFilterMonth.setText(""); updateTable(); });
    }

    public void updateTable() {
        modelPendentes.setRowCount(0);
        modelPagos.setRowCount(0);
        Collections.sort(boletos, Comparator.comparing(Boleto::getDueDate));
        for (Boleto b : boletos) {
            boolean mDay = txtFilterDay.getText().isEmpty() || String.valueOf(b.getDueDate().getDayOfMonth()).equals(txtFilterDay.getText());
            boolean mMonth = txtFilterMonth.getText().isEmpty() || String.valueOf(b.getDueDate().getMonthValue()).equals(txtFilterMonth.getText());
            if (mDay && mMonth) {
                Object[] row = { b.getName().toUpperCase(), b.getCompany().toUpperCase(), String.format("%.2f", b.getValue()), b.getFormattedDate(), b.getStatus().toUpperCase() };
                if (b.isPaid()) modelPagos.addRow(row);
                else modelPendentes.addRow(row);
            }
        }
    }

    private void salvarDados() { try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("boletos.dat"))) { oos.writeObject(boletos); } catch (IOException e) { e.printStackTrace(); } }
    private void carregarDados() { File f = new File("boletos.dat"); if (f.exists()) { try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) { boletos = (List<Boleto>) ois.readObject(); updateTable(); } catch (Exception e) { e.printStackTrace(); } } }
    private void verificarAlertasDoDia() { LocalDate hoje = LocalDate.now(); long qtd = boletos.stream().filter(b -> b.getDueDate().equals(hoje) && !b.isPaid()).count(); double total = boletos.stream().filter(b -> b.getDueDate().equals(hoje) && !b.isPaid()).mapToDouble(Boleto::getValue).sum(); if (qtd > 0) { JOptionPane.showMessageDialog(this, String.format("🔔 HOJE: %d BOLETOS VENCENDO.\nTOTAL: R$ %.2f", qtd, total), "AVISO", JOptionPane.WARNING_MESSAGE); } }
    private JFormattedTextField createDateField() { try { MaskFormatter m = new MaskFormatter("##/##/####"); m.setPlaceholderCharacter('_'); return new JFormattedTextField(m); } catch (ParseException e) { return new JFormattedTextField(); } }
    private JButton createStyledButton(String text, Color color) { JButton btn = new JButton(text); btn.setBackground(color); btn.setForeground(Color.WHITE); btn.setOpaque(true); btn.setBorderPainted(false); btn.setFont(new Font("SansSerif", Font.BOLD, 14)); btn.setPreferredSize(new Dimension(180, 45)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); return btn; }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(BoletoManager::new);
    }
}