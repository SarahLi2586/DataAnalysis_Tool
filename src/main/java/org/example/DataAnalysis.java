package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.*;
import java.util.List;

public class DataAnalysis {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(DataAnalysis::new);
    }

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cardPanel; // 用于容纳不同的视图（卡片）
    private JButton executeQueryButton, clearButton, switchThemeButton;
    private JTextArea sqlTextArea;
    private JTable resultsTable;
    private JScrollPane scrollPane, inputScrollPane;
    private JComboBox<String> queryHistoryComboBox;
    private JLabel statusLabel;
    private Set<String> queryHistory = new LinkedHashSet<>();
    private boolean isDarkTheme = false;

    private String url = "jdbc:mysql://localhost:3306/sakila?useSSL=false&serverTimezone=UTC";
    private String user = "root";
    private String password = "LEOsas!199612110";

    public DataAnalysis() {
        frame = new JFrame("Sakila Database Analysis - Custom Query");
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initializeHomePage();
        initializeQueryPage(); // 初始化查询界面

        JScrollPane mainScrollPane = new JScrollPane(cardPanel);
        frame.add(mainScrollPane); // 替换frame.add(cardPanel);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void initializeHomePage() {
        JPanel homePanel = new JPanel();
        homePanel.setLayout(new BoxLayout(homePanel, BoxLayout.Y_AXIS));

        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>Welcome to Sakila Database Analysis</div></html>", SwingConstants.CENTER);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        homePanel.add(welcomeLabel);

        homePanel.add(Box.createRigidArea(new Dimension(0, 20)));

        String dbIntro = "<html><p>The Sakila Database is a sample database provided by MySQL for learning and testing purposes.</p>"
                + "<p>It models a DVD rental store and includes tables for films, actors, customers, and rentals, among others.</p></html>";
        JLabel dbIntroLabel = new JLabel(dbIntro, SwingConstants.CENTER);
        dbIntroLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        homePanel.add(dbIntroLabel);

        Map<String, String> tableDescriptions = prepareTableDescriptions();
        String[] columnNames = {"Table Name", "Description", "Columns"};

        // 将Map转换为List，然后根据表名排序
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(tableDescriptions.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        String[][] data = new String[sortedEntries.size()][3];
        int i = 0;
        for (Map.Entry<String, String> entry : sortedEntries) {
            data[i][0] = entry.getKey();
            String descriptionWithoutDot = entry.getValue().replace(".", "");
            String[] parts = descriptionWithoutDot.split(" Columns: ");
            data[i][1] = parts[0];
            data[i][2] = parts.length > 1 ? parts[1].replace(".", "") : "N/A";
            i++;
        }

        JTable tablesInfoTable = new JTable(data, columnNames);
        tablesInfoTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        tablesInfoTable.setFillsViewportHeight(true);
        tablesInfoTable.setRowHeight(25);

        JScrollPane tablesScrollPane = new JScrollPane(tablesInfoTable);
        homePanel.add(tablesScrollPane);

        JButton startButton = new JButton("Start Analysis");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> cardLayout.show(cardPanel, "QueryPage"));
        homePanel.add(startButton);

        JScrollPane homeScrollPane = new JScrollPane(homePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        homeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        cardPanel.add(homeScrollPane, "HomePage");
    }




    private Map<String, String> prepareTableDescriptions() {
        Map<String, String> tableDescriptions = new HashMap<>();
        tableDescriptions.put("actor", "Stores actors' details. Columns: actor_id, first_name, last_name, last_update.");
        tableDescriptions.put("address", "Contains address data for staff and customers. Columns: address_id, address, address2, district, city_id, postal_code, phone, location, last_update.");
        tableDescriptions.put("category", "Categorizes films. Columns: category_id, name, last_update.");
        tableDescriptions.put("city", "Lists cities. Columns: city_id, city, country_id, last_update.");
        tableDescriptions.put("country", "Lists countries. Columns: country_id, country, last_update.");
        tableDescriptions.put("customer", "Stores customer information. Columns: customer_id, store_id, first_name, last_name, email, address_id, active, create_date, last_update.");
        tableDescriptions.put("film", "Contains films information. Columns: film_id, title, description, release_year, language_id, rental_duration, rental_rate, length, replacement_cost, rating, special_features, last_update.");
        tableDescriptions.put("film_actor", "Associative table mapping actors to films. Columns: actor_id, film_id, last_update.");
        tableDescriptions.put("film_category", "Associative table mapping films to categories. Columns: film_id, category_id, last_update.");
        tableDescriptions.put("film_text", "Contains film titles and descriptions. Columns: film_id, title, description.");
        tableDescriptions.put("inventory", "Stores inventory data. Columns: inventory_id, film_id, store_id, last_update.");
        tableDescriptions.put("language", "Lists languages for films. Columns: language_id, name, last_update.");
        tableDescriptions.put("payment", "Stores payment transactions. Columns: payment_id, customer_id, staff_id, rental_id, amount, payment_date, last_update.");
        tableDescriptions.put("rental", "Records rental transactions. Columns: rental_id, rental_date, inventory_id, customer_id, return_date, staff_id, last_update.");
        tableDescriptions.put("staff", "Stores staff information. Columns: staff_id, first_name, last_name, address_id, email, store_id, active, username, password, last_update.");
        tableDescriptions.put("store", "Contains store information. Columns: store_id, manager_staff_id, address_id, last_update.");
        return tableDescriptions;
    }



    private void initializeQueryPage() {
        JPanel queryPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        statusLabel = new JLabel("Ready.");

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0; // 初始权重设为0

        sqlTextArea = new JTextArea(5, 20);
        inputScrollPane = new JScrollPane(sqlTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        queryPanel.add(inputScrollPane, gbc);

        // 初始化按钮并将它们添加到面板
        executeQueryButton = new JButton("Execute Query");
        clearButton = new JButton("Clear");
        switchThemeButton = new JButton("Switch Theme");
        queryHistoryComboBox = new JComboBox<>();
        setupQueryHistoryComboBoxRenderer(); // 设置下拉框渲染器

        gbc.gridwidth = 1;
        addComponent(queryPanel, executeQueryButton, gbc, 0, 1);
        addComponent(queryPanel, clearButton, gbc, 1, 1);
        addComponent(queryPanel, switchThemeButton, gbc, 2, 1);
        addComponent(queryPanel, queryHistoryComboBox, gbc, 3, 1);

        resultsTable = new JTable();
        scrollPane = new JScrollPane(resultsTable);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = GridBagConstraints.REMAINDER; // 再次跨越所有列
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; // 为结果表格提供额外的垂直空间
        queryPanel.add(scrollPane, gbc);

        JScrollPane outerScrollPane = new JScrollPane(queryPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        cardPanel.add(outerScrollPane, "QueryPage");

        customizeComponents();
        setupListeners();
    }

    private void addComponent(JPanel panel, Component component, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        panel.add(component, gbc);
    }

    private void setupQueryHistoryComboBoxRenderer() {
        queryHistoryComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    String text = value.toString();
                    setToolTipText(text); // 显示完整文本作为工具提示
                    if (text.length() > 50) {
                        text = text.substring(0, 47) + "...";
                    }
                    setText(text);
                }
                return this;
            }
        });
    }

    private void customizeComponents() {
        // 设置按钮外观
        executeQueryButton.setBackground(Color.LIGHT_GRAY);
        clearButton.setBackground(Color.LIGHT_GRAY);
        switchThemeButton.setBackground(Color.LIGHT_GRAY);

        executeQueryButton.setForeground(Color.BLACK);
        clearButton.setForeground(Color.BLACK);
        switchThemeButton.setForeground(Color.BLACK);

        // 设置文本区域外观
        sqlTextArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
        sqlTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // 设置下拉列表和表格的外观
        queryHistoryComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultsTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultsTable.setRowHeight(20);
    }

    private void setupListeners() {
        executeQueryButton.addActionListener(this::executeQueryAction);
        clearButton.addActionListener(this::clearAction);
        switchThemeButton.addActionListener(e -> switchTheme());
        queryHistoryComboBox.addActionListener(this::queryHistoryAction);
    }

// 后续方法（executeQueryAction, clearAction, queryHistory
    private void executeQueryAction(ActionEvent e) {
        if (statusLabel == null) {
            System.out.println("statusLabel is null!");
        }

        String query = sqlTextArea.getText().trim();
        if (!query.isEmpty()) {
            executeQuery(query);
            if (queryHistory.add(query)) {
                queryHistoryComboBox.addItem(query);
                queryHistoryComboBox.setSelectedItem(query);
            }
        } else {
            statusLabel.setText("SQL statement cannot be empty.");
        }
    }

    private void clearAction(ActionEvent e) {
        sqlTextArea.setText("");
        ((DefaultTableModel) resultsTable.getModel()).setRowCount(0);
        statusLabel.setText("Cleared.");
    }

    private void queryHistoryAction(ActionEvent e) {
        if (e.getSource() == queryHistoryComboBox) {
            String selectedQuery = (String) queryHistoryComboBox.getSelectedItem();
            sqlTextArea.setText(selectedQuery != null ? selectedQuery : "");
        }
    }

    private void switchTheme() {
        isDarkTheme = !isDarkTheme;
        updateTheme();
    }

    private void updateTheme() {
        Color background = isDarkTheme ? Color.DARK_GRAY : Color.WHITE;
        Color foreground = isDarkTheme ? Color.WHITE : Color.BLACK;
        Color buttonBackground = Color.GRAY;

        frame.getContentPane().setBackground(background);
        sqlTextArea.setBackground(background);
        sqlTextArea.setForeground(foreground);
        resultsTable.setBackground(background);
        resultsTable.setForeground(foreground);
        statusLabel.setForeground(foreground);

        executeQueryButton.setBackground(buttonBackground);
        clearButton.setBackground(buttonBackground);
        switchThemeButton.setBackground(buttonBackground);

        executeQueryButton.setForeground(foreground);
        clearButton.setForeground(foreground);
        switchThemeButton.setForeground(foreground);

        queryHistoryComboBox.setBackground(background);
        queryHistoryComboBox.setForeground(foreground);
    }

    private void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            Vector<String> columnNames = new Vector<>();
            for (int column = 1; column <= columnCount; column++) {
                columnNames.add(metaData.getColumnLabel(column));
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> vector = new Vector<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    vector.add(rs.getObject(columnIndex));
                }
                data.add(vector);
            }

            DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // make table cells non-editable
                    return false;
                }
            };
            resultsTable.setModel(model);
            statusLabel.setText("Query executed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error executing query: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error executing query.");
        }
    }
}
