import java.awt.*;
import java.awt.event.*;

// ---------------- Efficient Multi-Window Tracker ----------------
public class EfficientMultiWindowTracker extends Frame implements ActionListener {
    
    private final ProgressBackend backend;
    private TextArea outputArea;
    private Button addCategoryBtn, addTaskBtn, updateBtn, showBtn;
    private Button delCategoryBtn, delTaskBtn;

    public EfficientMultiWindowTracker() {
        super("Efficient Weekly Progress Tracker");
        
        backend = new ProgressBackend();

        setLayout(new BorderLayout(5, 5));

        Label title = new Label("Weekly Progress Tracker (Improved UI)", Label.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        addCategoryBtn = new Button("Add Category");
        addTaskBtn = new Button("Add Task");
        updateBtn = new Button("Update Progress");
        showBtn = new Button("Show Report");
        delCategoryBtn = new Button("Delete Category");
        delTaskBtn = new Button("Delete Task");

        addCategoryBtn.addActionListener(this);
        addTaskBtn.addActionListener(this);
        updateBtn.addActionListener(this);
        showBtn.addActionListener(this);
        delCategoryBtn.addActionListener(this);
        delTaskBtn.addActionListener(this);

        Panel btnPanel1 = new Panel(new FlowLayout());
        btnPanel1.add(addCategoryBtn);
        btnPanel1.add(addTaskBtn);
        btnPanel1.add(updateBtn);
        btnPanel1.add(showBtn);

        Panel btnPanel2 = new Panel(new FlowLayout());
        btnPanel2.add(delCategoryBtn);
        btnPanel2.add(delTaskBtn);

        Panel botPanel = new Panel(new GridLayout(2, 1));
        botPanel.add(btnPanel1);
        botPanel.add(btnPanel2);
        
        add(botPanel, BorderLayout.SOUTH);

        outputArea = new TextArea(15, 60);
        outputArea.setEditable(false);
        add(outputArea, BorderLayout.CENTER);

        setSize(700, 480);
        setLocationRelativeTo(null);
        setVisible(true);

        // 🌟 ask user what they want to do first
        new StartDialog(this).setVisible(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                backend.saveData();   // save before exit
                dispose();
            }
        });
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
            outputArea.setText(backend.buildReport());
        } else if (e.getSource() == delCategoryBtn) {
            new DeleteCategoryDialog(this).setVisible(true);
        } else if (e.getSource() == delTaskBtn) {
            new DeleteTaskDialog(this).setVisible(true);
        }
    }

    // ---------------- Start Dialog ----------------
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
                outputArea.setText(backend.buildReport());
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
            if (!backend.addCategory(name)) { showError("Duplicate or limit reached"); return; }

            showMsg("Category Added!");
            dispose();
        }
    }

    // ---------------- Dialog: Delete Category ----------------
    class DeleteCategoryDialog extends Dialog implements ActionListener {
        private Choice categoryChoice;
        private Button ok, cancel;

        DeleteCategoryDialog(Frame parent) {
            super(parent, "Delete Category", true);
            setLayout(new GridLayout(3, 1));
            add(new Label("Select Category to Delete:"));
            categoryChoice = new Choice();
            if (backend.getCategoryCount() == 0) categoryChoice.add("No Categories");
            else for (int i = 0; i < backend.getCategoryCount(); i++) categoryChoice.add(backend.getCategories()[i].getName());
            add(categoryChoice);

            Panel bottom = new Panel(new FlowLayout());
            ok = new Button("Delete");
            cancel = new Button("Cancel");
            ok.addActionListener(this);
            cancel.addActionListener(this);
            bottom.add(ok);
            bottom.add(cancel);
            add(bottom);

            setSize(320, 150);
            setLocationRelativeTo(parent);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }
            if (backend.getCategoryCount() == 0) { showError("No categories available"); return; }

            String cat = categoryChoice.getSelectedItem();
            if (backend.deleteCategory(cat)) {
                showMsg("Category Deleted!");
                outputArea.setText(backend.buildReport());
                dispose();
            } else {
                showError("Delete failed");
            }
        }
    }

    // ---------------- Dialog: Add Task ----------------
    class AddTaskDialog extends Dialog implements ActionListener {
        private Choice categoryChoice;
        private TextField taskName;
        private TextField taskDesc;
        private Button ok, cancel;

        AddTaskDialog(Frame parent) {
            super(parent, "Add Task", true);
            setLayout(new GridLayout(6, 1));

            add(new Label("Select Category:"));
            categoryChoice = new Choice();
            if (backend.getCategoryCount() == 0) categoryChoice.add("No Categories");
            else for (int i = 0; i < backend.getCategoryCount(); i++) categoryChoice.add(backend.getCategories()[i].getName());
            add(categoryChoice);

            add(new Label("Enter Task Name:"));
            taskName = new TextField(20);
            add(taskName);
            
            add(new Label("Enter Optional Description:"));
            taskDesc = new TextField(20);
            add(taskDesc);

            Panel bottom = new Panel(new FlowLayout());
            ok = new Button("OK");
            cancel = new Button("Cancel");
            ok.addActionListener(this);
            cancel.addActionListener(this);
            bottom.add(ok);
            bottom.add(cancel);
            add(bottom);

            setSize(320, 260);
            setLocationRelativeTo(parent);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }

            if (backend.getCategoryCount() == 0) { showError("No categories available"); return; }

            String cat = categoryChoice.getSelectedItem();
            String task = taskName.getText().trim();
            String desc = taskDesc.getText().trim();
            
            if (task.isEmpty()) { showError("Enter valid task name"); return; }

            if (backend.addTask(cat, task, desc)) {
                showMsg("Task Added!");
                dispose();
            } else {
                showError("Task limit reached or category missing");
            }
        }
    }

    // ---------------- Dialog: Delete Task ----------------
    class DeleteTaskDialog extends Dialog implements ActionListener {
        private Choice categoryChoice, taskChoice;
        private Button ok, cancel;

        DeleteTaskDialog(Frame parent) {
            super(parent, "Delete Task", true);
            setLayout(new GridLayout(5, 1));

            add(new Label("Select Category:"));
            categoryChoice = new Choice();
            if (backend.getCategoryCount() == 0) categoryChoice.add("No Categories");
            else for (int i = 0; i < backend.getCategoryCount(); i++) categoryChoice.add(backend.getCategories()[i].getName());
            add(categoryChoice);

            add(new Label("Select Task:"));
            taskChoice = new Choice();
            add(taskChoice);

            categoryChoice.addItemListener(ev -> refreshTasks());
            refreshTasks();

            Panel bottom = new Panel(new FlowLayout());
            ok = new Button("Delete");
            cancel = new Button("Cancel");
            ok.addActionListener(this);
            cancel.addActionListener(this);
            bottom.add(ok);
            bottom.add(cancel);
            add(bottom);

            setSize(360, 200);
            setLocationRelativeTo(parent);
        }

        private void refreshTasks() {
            taskChoice.removeAll();
            String catName = categoryChoice.getSelectedItem();
            Category c = backend.findCategory(catName);
            if (c == null) { taskChoice.add("No Tasks"); return; }
            if (c.getTaskCount() == 0) { taskChoice.add("No Tasks"); return; }
            for (Task t : c.getTasks()) {
                if (t != null) taskChoice.add(t.getName());
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }

            String cat = categoryChoice.getSelectedItem();
            String task = taskChoice.getSelectedItem();

            if (backend.deleteTask(cat, task)) {
                showMsg("Task Deleted!");
                outputArea.setText(backend.buildReport());
                dispose();
            } else {
                showError("Delete failed");
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
            if (backend.getCategoryCount() == 0) categoryChoice.add("No Categories");
            else for (int i = 0; i < backend.getCategoryCount(); i++) categoryChoice.add(backend.getCategories()[i].getName());
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
            Category c = backend.findCategory(catName);
            if (c == null) { taskChoice.add("No Tasks"); return; }
            if (c.getTaskCount() == 0) { taskChoice.add("No Tasks"); return; }
            for (Task t : c.getTasks()) {
                if (t != null) taskChoice.add(t.getName());
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancel) { dispose(); return; }

            try {
                String cat = categoryChoice.getSelectedItem();
                String task = taskChoice.getSelectedItem();

                double total = Double.parseDouble(totalField.getText().trim());
                double done = Double.parseDouble(doneField.getText().trim());
                if (total <= 0 || done < 0) { showError("Invalid numbers"); return; }

                if (backend.updateTaskProgress(cat, task, total, done)) {
                    showMsg("Progress Updated!");
                    dispose();
                } else {
                    showError("Update failed");
                }
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