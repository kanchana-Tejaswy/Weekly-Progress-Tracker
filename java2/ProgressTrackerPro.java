import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

// ------------------------ 1. DATA MODEL ------------------------
class Task {
    int id;
    String name;
    double progress;
    String status;
    String category;

    public Task(int id, String name, double progress, String category) {
        this.id = id;
        this.name = name;
        this.progress = progress;
        this.category = category;
        updateStatus();
    }

    private void updateStatus() {
        if (progress <= 0) status = "Not Started";
        else if (progress < 100) status = "In Progress";
        else status = "Completed";
    }
}

// ------------------------ 2. DATABASE MANAGER ------------------------
class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:tracker.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (name TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, progress REAL, category TEXT, " +
                    "FOREIGN KEY(category) REFERENCES categories(name))");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB Error: " + e.getMessage());
        }
    }

    public static void addCategory(String name) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO categories VALUES (?)")) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public static List<String> getCategories() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM categories")) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void addTask(String name, String cat) {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO tasks (name, progress, category) VALUES (?, 0, ?)")) {
            pstmt.setString(1, name);
            pstmt.setString(2, cat);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void updateProgress(int taskId, double progress) {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE tasks SET progress = ? WHERE id = ?")) {
            pstmt.setDouble(1, progress);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Returns a connection that the UI must close manually or via try-with-resources
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

// ------------------------ 3. MAIN GUI ------------------------
public class ProgressTrackerPro extends JFrame {
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JLabel lblStats;

    public ProgressTrackerPro() {
        DatabaseHandler.initialize();
        setTitle("Weekly Progress Tracker Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Modern Look and Feel
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header Dashboard
        JPanel dashPanel = new JPanel(new GridLayout(1, 1, 10, 10));
        dashPanel.setBackground(new Color(230, 235, 240));
        dashPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        lblStats = new JLabel("📊 Dashboard | Loading...", JLabel.CENTER);
        lblStats.setFont(new Font("SansSerif", Font.BOLD, 15));
        dashPanel.add(lblStats);
        mainPanel.add(dashPanel, BorderLayout.NORTH);

        // Table for Tasks
        String[] cols = {"ID", "Task Name", "Category", "Progress (%)", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(30);
        taskTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Progress Bar Cell Renderer
        taskTable.getColumnModel().getColumn(3).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JProgressBar pb = new JProgressBar(0, 100);
            int val = ((Double) value).intValue();
            pb.setValue(val);
            pb.setStringPainted(true);
            // Change color based on progress
            if (val == 100) pb.setForeground(new Color(40, 167, 69));
            return pb;
        });

        mainPanel.add(new JScrollPane(taskTable), BorderLayout.CENTER);

        // Bottom Button Bar
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAddCat = new JButton("Add Category");
        JButton btnAddTask = new JButton("Add Task");
        JButton btnUpdate = new JButton("Update Progress");
        JButton btnExport = new JButton("Export CSV");

        btnPanel.add(btnAddCat); btnPanel.add(btnAddTask); 
        btnPanel.add(btnUpdate); btnPanel.add(btnExport);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnAddCat.addActionListener(e -> showAddCategory());
        btnAddTask.addActionListener(e -> showAddTask());
        btnUpdate.addActionListener(e -> showUpdateProgress());
        btnExport.addActionListener(e -> exportToCSV());

        add(mainPanel);
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        int completed = 0, total = 0;
        
        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks")) {
            
            while (rs.next()) {
                double prog = rs.getDouble("progress");
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        prog,
                        prog >= 100 ? "Completed" : "In Progress"
                });
                if (prog >= 100) completed++;
                total++;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        int pending = total - completed;
        lblStats.setText("📊 Dashboard | Total Tasks: " + total + " | Completed: " + completed + " | Pending: " + pending);
    }

    private void showAddCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter New Category (e.g., Java, Gym, Project):");
        if (name != null && !name.trim().isEmpty()) {
            try {
                DatabaseHandler.addCategory(name.trim());
                JOptionPane.showMessageDialog(this, "Category Added!");
            } catch (SQLException e) { 
                JOptionPane.showMessageDialog(this, "Category already exists or Database Error."); 
            }
        }
    }

    private void showAddTask() {
        List<String> cats = DatabaseHandler.getCategories();
        if (cats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add a Category first.");
            return;
        }

        String name = JOptionPane.showInputDialog(this, "What is the task name?");
        if (name == null || name.trim().isEmpty()) return;

        String cat = (String) JOptionPane.showInputDialog(this, "Select Category:", "Task Category",
                JOptionPane.QUESTION_MESSAGE, null, cats.toArray(), cats.get(0));
        
        if (cat != null) {
            DatabaseHandler.addTask(name.trim(), cat);
            refreshData();
        }
    }

    private void showUpdateProgress() {
        int row = taskTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please click on a task in the table first.");
            return;
        }
        
        int id = (int) tableModel.getValueAt(row, 0);
        String taskName = (String) tableModel.getValueAt(row, 1);
        
        String val = JOptionPane.showInputDialog(this, "Enter progress % for '" + taskName + "' (0-100):");
        if (val == null) return;

        try {
            double p = Double.parseDouble(val);
            if (p < 0 || p > 100) throw new NumberFormatException();
            DatabaseHandler.updateProgress(id, p);
            refreshData();
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Please enter a valid number between 0 and 100."); 
        }
    }

    private void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new File("weekly_report.csv"))) {
            writer.println("ID,Task Name,Category,Progress,Status");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                writer.printf("%s,%s,%s,%s%%,%s%n",
                        tableModel.getValueAt(i, 0),
                        tableModel.getValueAt(i, 1),
                        tableModel.getValueAt(i, 2),
                        tableModel.getValueAt(i, 3),
                        tableModel.getValueAt(i, 4));
            }
            JOptionPane.showMessageDialog(this, "Report saved to weekly_report.csv successfully!");
        } catch (Exception e) { 
            JOptionPane.showMessageDialog(this, "Error exporting file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Ensure UI runs on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new ProgressTrackerPro().setVisible(true));
    }
}