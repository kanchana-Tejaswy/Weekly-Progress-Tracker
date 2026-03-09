import java.awt.*;
import java.awt.event.*;
import java.io.*;

// ------------------------ Task class ------------------------
class Task {
    private final String name;
    private double progress;
    private String status;

    public Task(String name) {
        this.name = name;
        this.progress = 0.0;
        this.status = "Not Started";
    }

    public Task(String name, double totalParts, double completedParts) {
        this.name = name;
        setProgressFromParts(totalParts, completedParts);
    }

    public String getName() { return name; }

    public double getProgress() { return progress; }

    public String getStatus() { return status; }

    public void setProgress(double p) {
        if (p < 0) p = 0;
        if (p > 100) p = 100;
        this.progress = p;
        updateStatus();
    }

    public void setProgressFromParts(double totalParts, double completedParts) {
        if (totalParts <= 0) {
            setProgress(0);
            return;
        }
        double percent = (completedParts / totalParts) * 100.0;
        setProgress(percent);
    }

    private void updateStatus() {
        if (progress == 0.0) status = "Not Started";
        else if (progress < 100.0) status = "In Progress";
        else status = "Completed";
    }

    public String toString() {
        return name + " - " + String.format("%.2f", progress) + "% - " + status;
    }
}

// ---------------------- Category class ----------------------
class Category {
    private static final int MAX_TASKS = 10;
    private final String name;
    private final Task[] tasks;
    private int taskCount;

    public Category(String name) {
        this.name = name;
        this.tasks = new Task[MAX_TASKS];
        this.taskCount = 0;
    }

    public String getName() { return name; }

    public int getTaskCount() { return taskCount; }

    public Task[] getTasks() { return tasks; }

    public boolean addTask(Task t) {
        if (taskCount >= tasks.length) return false;
        tasks[taskCount++] = t;
        return true;
    }

    public Task findTask(String taskName) {
        for (int i = 0; i < taskCount; i++) {
            if (tasks[i].getName().equalsIgnoreCase(taskName.trim()))
                return tasks[i];
        }
        return null;
    }

    public double getAverageProgress() {
        if (taskCount == 0) return 0.0;
        double total = 0.0;
        for (int i = 0; i < taskCount; i++)
            total += tasks[i].getProgress();
        return total / taskCount;
    }

    public String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(name).append("\n");
        if (taskCount == 0) {
            sb.append("  No tasks yet\n");
        } else {
            for (int i = 0; i < taskCount; i++)
                sb.append("  ").append(tasks[i]).append("\n");
        }
        sb.append("  Average: ").append(String.format("%.2f", getAverageProgress())).append("%\n");
        return sb.toString();
    }
}

// ---------------- Efficient Multi-Window Tracker ----------------
public class EfficientMultiWindowTracker extends Frame implements ActionListener {
    private static final int MAX_CATEGORIES = 10;
    private static final String DATA_FILE = "weekly_progress_data.txt";

    final Category[] categories = new Category[MAX_CATEGORIES];
    int categoryCount = 0;

    private TextArea outputArea;
    private Button addCategoryBtn, addTaskBtn, updateBtn, showBtn;

    public EfficientMultiWindowTracker() {
        super("Efficient Weekly Progress Tracker");

        setLayout(new BorderLayout(5, 5));

        Label title = new Label("Weekly Progress Tracker (Improved UI)", Label.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        addCategoryBtn = new Button("Add Category");
        addTaskBtn = new Button("Add Task");
        updateBtn = new Button("Update Progress");
        showBtn = new Button("Show Report");

        addCategoryBtn.addActionListener(this);
        addTaskBtn.addActionListener(this);
        updateBtn.addActionListener(this);
        showBtn.addActionListener(this);

        Panel btnPanel = new Panel(new FlowLayout());
        btnPanel.add(addCategoryBtn);
        btnPanel.add(addTaskBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(showBtn);

        add(btnPanel, BorderLayout.SOUTH);

        outputArea = new TextArea(15, 60);
        outputArea.setEditable(false);
        add(outputArea, BorderLayout.CENTER);

        // Load previous data (if any)
        loadData();

        setSize(700, 420);
        setLocationRelativeTo(null);
        setVisible(true);

        // 🌟 NEW: ask user what they want to do first
        new StartDialog(this).setVisible(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveData();   // save before exit
                dispose();
            }
        });
    }

    // ---------------- Data helpers ----------------
    private Category findCategory(String name) {
        for (int i = 0; i < categoryCount; i++)
            if (categories[i].getName().equalsIgnoreCase(name.trim()))
                return categories[i];
        return null;
    }

    private boolean addCategoryInternal(String name) {
        if (categoryCount >= MAX_CATEGORIES) return false;
        if (findCategory(name) != null) return false;
        categories[categoryCount++] = new Category(name);
        return true;
    }

    private String buildReport() {
        if (categoryCount == 0) return "No categories yet.\n";
        StringBuilder sb = new StringBuilder();
        double totalAvg = 0.0;
        for (int i = 0; i < categoryCount; i++) {
            sb.append(categories[i].buildReport()).append("\n");
            totalAvg += categories[i].getAverageProgress();
        }
        sb.append("Overall Weekly Progress: ")
                .append(String.format("%.2f", totalAvg / categoryCount))
                .append("%\n");
        return sb.toString();
    }

    // ---------------- File save/load ----------------
    private void saveData() {
        try (PrintWriter out = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (int i = 0; i < categoryCount; i++) {
                Category c = categories[i];
                out.println("C|" + c.getName());
                for (int j = 0; j < c.getTaskCount(); j++) {
                    Task t = c.getTasks()[j];
                    out.println("T|" + c.getName() + "|" + t.getName() + "|" + t.getProgress());
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private void loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 0) continue;

                if ("C".equals(parts[0]) && parts.length >= 2) {
                    String cname = parts[1].trim();
                    addCategoryInternal(cname);

                } else if ("T".equals(parts[0]) && parts.length >= 4) {
                    String cname = parts[1].trim();
                    String tname = parts[2].trim();
                    double prog;
                    try {
                        prog = Double.parseDouble(parts[3].trim());
                    } catch (NumberFormatException ex) {
                        prog = 0.0;
                    }
                    Category c = findCategory(cname);
                    if (c != null) {
                        Task t = new Task(tname);
                        t.setProgress(prog);
                        c.addTask(t);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    // ---------------- Button actions ----------------
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addCategoryBtn) {
            new AddCategoryDialog(this).setVisible(true);
        } else if (e.getSource() == addTaskBtn) {
            new AddTaskDialog(this).setVisible(true);
        } else if (e.getSource() == updateBtn) {
            new UpdateDialog(this).setVisible(true);
        } else if (e.getSource() == showBtn) {
            outputArea.setText(buildReport());
        }
    }

    // ---------------- Start Dialog (NEW) ----------------
    class StartDialog extends Dialog implements ActionListener {
        private Button viewBtn, updateBtn, closeBtn;

        StartDialog(Frame owner) {
            super(owner, "What do you want to do?", true);
            setLayout(new GridLayout(3, 1, 5, 5));

            add(new Label("Choose an option:", Label.CENTER));

            Panel p1 = new Panel(new FlowLayout());
            viewBtn = new Button("View Previous Progress");
            updateBtn = new Button("Update Progress");
            viewBtn.addActionListener(this);
            updateBtn.addActionListener(this);
            p1.add(viewBtn);
            p1.add(updateBtn);
            add(p1);

            Panel p2 = new Panel(new FlowLayout());
            closeBtn = new Button("Close");
            closeBtn.addActionListener(this);
            p2.add(closeBtn);
            add(p2);

            setSize(320, 150);
            setLocationRelativeTo(owner);
        }

        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == viewBtn) {
                outputArea.setText(buildReport());
                dispose();
            } else if (src == updateBtn) {
                new UpdateDialog(EfficientMultiWindowTracker.this).setVisible(true);
                dispose();
            } else if (src == closeBtn) {
                dispose();
            }
        }
    }

    // ---------------- Dialog: Add Category ----------------
    class AddCategoryDialog extends Dialog implements ActionListener {
        private TextField txt;
        private Button ok, cancel;

        AddCategoryDialog(Frame parent) {
            super(parent, "Add Category", true);
            setLayout(new GridLayout(3, 1));
            add(new Label("Enter Category Name:"));
            txt = new TextField(20);
            add(txt);

            Panel bottom = new Panel(new FlowLayout());
            ok = new Button("OK");
            cancel = new Button("Cancel");
            ok.addActionListener(this);
            cancel.addActionListener(this);
            bottom.add(ok);
            bottom.add(cancel);
            add(bottom);

            setSize(280, 150);
            setLocationRelativeTo(parent);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }

            String name = txt.getText().trim();
            if (name.isEmpty()) { showError("Enter a valid name"); return; }
            if (!addCategoryInternal(name)) { showError("Duplicate or limit reached"); return; }

            showMsg("Category Added!");
            dispose();
        }
    }

    // ---------------- Dialog: Add Task ----------------
    class AddTaskDialog extends Dialog implements ActionListener {
        private Choice categoryChoice;
        private TextField taskName;
        private Button ok, cancel;

        AddTaskDialog(Frame parent) {
            super(parent, "Add Task", true);
            setLayout(new GridLayout(4, 1));

            add(new Label("Select Category:"));
            categoryChoice = new Choice();
            if (categoryCount == 0) categoryChoice.add("No Categories");
            else for (int i = 0; i < categoryCount; i++) categoryChoice.add(categories[i].getName());
            add(categoryChoice);

            add(new Label("Enter Task Name:"));
            taskName = new TextField(20);
            add(taskName);

            Panel bottom = new Panel(new FlowLayout());
            ok = new Button("OK");
            cancel = new Button("Cancel");
            ok.addActionListener(this);
            cancel.addActionListener(this);
            bottom.add(ok);
            bottom.add(cancel);
            add(bottom);

            setSize(320, 180);
            setLocationRelativeTo(parent);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }

            if (categoryCount == 0) { showError("No categories available"); return; }

            String cat = categoryChoice.getSelectedItem();
            String task = taskName.getText().trim();
            if (task.isEmpty()) { showError("Enter valid task name"); return; }

            Category c = findCategory(cat);
            if (c == null) { showError("Category not found"); return; }

            if (c.addTask(new Task(task))) {
                showMsg("Task Added!");
                dispose();
            } else {
                showError("Task limit reached");
            }
        }
    }

    // ---------------- Dialog: Update Progress ----------------
    class UpdateDialog extends Dialog implements ActionListener {
        private Choice categoryChoice, taskChoice;
        private TextField totalField, doneField;
        private Button ok, cancel;

        UpdateDialog(Frame parent) {
            super(parent, "Update Progress", true);
            setLayout(new GridLayout(6, 1));

            add(new Label("Select Category:"));
            categoryChoice = new Choice();
            if (categoryCount == 0) categoryChoice.add("No Categories");
            else for (int i = 0; i < categoryCount; i++) categoryChoice.add(categories[i].getName());
            add(categoryChoice);

            add(new Label("Select Task:"));
            taskChoice = new Choice();
            add(taskChoice);

            categoryChoice.addItemListener(ev -> refreshTasks());
            refreshTasks();

            add(new Label("Total Steps (decimal):"));
            totalField = new TextField();
            add(totalField);

            add(new Label("Completed Steps (decimal):"));
            doneField = new TextField();
            add(doneField);

            Panel bottom = new Panel(new FlowLayout());
            ok = new Button("OK");
            cancel = new Button("Cancel");
            ok.addActionListener(this);
            cancel.addActionListener(this);
            bottom.add(ok);
            bottom.add(cancel);
            add(bottom);

            setSize(360, 260);
            setLocationRelativeTo(parent);
        }

        private void refreshTasks() {
            taskChoice.removeAll();
            String catName = categoryChoice.getSelectedItem();
            Category c = findCategory(catName);
            if (c == null) { taskChoice.add("No Tasks"); return; }
            if (c.getTaskCount() == 0) { taskChoice.add("No Tasks"); return; }
            for (Task t : c.getTasks()) {
                if (t != null) taskChoice.add(t.getName());
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }

            try {
                Category c = findCategory(categoryChoice.getSelectedItem());
                if (c == null) { showError("Category not found"); return; }
                Task t = c.findTask(taskChoice.getSelectedItem());
                if (t == null) { showError("Task not found"); return; }

                double total = Double.parseDouble(totalField.getText().trim());
                double done = Double.parseDouble(doneField.getText().trim());
                if (total <= 0 || done < 0) { showError("Invalid numbers"); return; }

                t.setProgressFromParts(total, done);
                showMsg("Progress Updated to " + String.format("%.2f", t.getProgress()) + "%");
                dispose();
            } catch (Exception ex) {
                showError("Invalid input");
            }
        }
    }

    // ------------- Popup Dialogs -------------
    private void showError(String msg) {
        new MessageDialog(this, msg, true).setVisible(true);
    }

    private void showMsg(String msg) {
        new MessageDialog(this, msg, false).setVisible(true);
    }

    class MessageDialog extends Dialog implements ActionListener {
        MessageDialog(Frame owner, String msg, boolean error) {
            super(owner, error ? "Error" : "Message", true);
            setLayout(new GridLayout(2, 1));
            Label lbl = new Label(msg, Label.CENTER);
            lbl.setForeground(error ? Color.red : Color.blue);
            lbl.setFont(new Font("Arial", Font.BOLD, 14));
            add(lbl);
            Button ok = new Button("OK");
            ok.addActionListener(this);
            Panel p = new Panel();
            p.add(ok);
            add(p);
            setSize(260, 120);
            setLocationRelativeTo(owner);
        }
        public void actionPerformed(ActionEvent e) { dispose(); }
    }

    // ---------------- main ----------------
    public static void main(String[] args) {
        new EfficientMultiWindowTracker();
    }
}