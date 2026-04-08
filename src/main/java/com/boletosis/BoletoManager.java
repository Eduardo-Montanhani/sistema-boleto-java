package com.boletosis;

import com.boletosis.model.Boleto;
import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BoletoManager extends JFrame {
    private List<Boleto> boletos = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable table;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Cores Modernas
    private final Color COLOR_PRIMARY = new Color(52, 152, 219);
    private final Color COLOR_SUCCESS = new Color(46, 204, 113);
    private final Color COLOR_DANGER = new Color(231, 76, 60);
    private final Color COLOR_BG = new Color(245, 245, 245);

    public BoletoManager() {
        setTitle("Financeiro Master - Gestão de Boletos");
        setSize(1000, 600); // Aumentei um pouco a largura para acomodar os 3 botões
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(15, 15));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        setupTable();
        setupInputPanel();
        carregarDados();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void salvarDados() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("boletos.dat"))) {
            oos.writeObject(boletos);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar dados: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void carregarDados() {
        File arquivo = new File("boletos.dat");
        if (arquivo.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(arquivo))) {
                boletos = (List<Boleto>) ois.readObject();
                updateTable();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Nenhum dado anterior encontrado.");
            }
        }
    }

    private void setupTable() {
        String[] columns = {"NOME", "EMPRESA", "VENCIMENTO", "STATUS"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setGridColor(new Color(230, 230, 230));

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("SansSerif", Font.BOLD, 11));

                String status = (String) value;
                if ("Pago".equals(status)) label.setForeground(COLOR_SUCCESS);
                else if ("Vencido".equals(status)) label.setForeground(COLOR_DANGER);
                else label.setForeground(COLOR_PRIMARY);

                return label;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupInputPanel() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setOpaque(false);

        JPanel formPanel = new JPanel(new GridLayout(2, 3, 15, 5));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JTextField txtName = new JTextField();
        JTextField txtCompany = new JTextField();
        JFormattedTextField txtDate = createDateField();

        formPanel.add(new JLabel("Nome do Boleto:"));
        formPanel.add(new JLabel("Empresa/Fornecedor:"));
        formPanel.add(new JLabel("Data de Vencimento:"));
        formPanel.add(txtName);
        formPanel.add(txtCompany);
        formPanel.add(txtDate);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        // Botões
        JButton btnDelete = createStyledButton("EXCLUIR SELECIONADO", COLOR_DANGER);
        JButton btnPay = createStyledButton("MARCAR COMO PAGO", COLOR_SUCCESS);
        JButton btnAdd = createStyledButton("CADASTRAR BOLETO", COLOR_PRIMARY);

        buttonPanel.add(btnDelete);
        buttonPanel.add(btnPay);
        buttonPanel.add(btnAdd);

        container.add(formPanel, BorderLayout.CENTER);
        container.add(buttonPanel, BorderLayout.SOUTH);

        add(container, BorderLayout.SOUTH);

        // AÇÃO: CADASTRAR
        btnAdd.addActionListener(e -> {
            try {
                if(txtName.getText().trim().isEmpty() || txtCompany.getText().trim().isEmpty()) {
                    throw new Exception("Campos vazios");
                }
                LocalDate date = LocalDate.parse(txtDate.getText(), formatter);
                Boleto b = new Boleto(txtName.getText(), txtCompany.getText(), date);

                boletos.add(b);
                updateTable();
                salvarDados();
                checkNotification(b);

                txtName.setText("");
                txtCompany.setText("");
                txtDate.setValue(null);
                txtName.requestFocus();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: Preencha os dados corretamente (Data: DD/MM/AAAA)!");
            }
        });

        // AÇÃO: MARCAR COMO PAGO
        btnPay.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                boletos.get(row).setPaid(true);
                updateTable();
                salvarDados();
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um boleto na tabela para pagar.");
            }
        });

        // AÇÃO: EXCLUIR
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Deseja realmente excluir este boleto?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    boletos.remove(row);
                    updateTable();
                    salvarDados();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um boleto para excluir.");
            }
        });
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(185, 40));
        return btn;
    }

    private JFormattedTextField createDateField() {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (ParseException e) { return new JFormattedTextField(); }
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        for (Boleto b : boletos) {
            tableModel.addRow(new Object[]{b.getName(), b.getCompany(), b.getFormattedDate(), b.getStatus()});
        }
    }

    private void checkNotification(Boleto b) {
        if (b.getDueDate().equals(LocalDate.now()) && !b.isPaid()) {
            JOptionPane.showMessageDialog(this, "⚠️ ATENÇÃO: '" + b.getName() + "' vence hoje!");
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(BoletoManager::new);
    }
}