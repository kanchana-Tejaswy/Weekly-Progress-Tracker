import java.io.*;

public class ProgressBackend {
    private static final int MAX_CATEGORIES = 10;
    private static final String DATA_FILE = "weekly_progress_data.txt";

    private final Category[] categories = new Category[MAX_CATEGORIES];
    private int categoryCount = 0;

    public ProgressBackend() {
        loadData();
    }

    public int getCategoryCount() {
        return categoryCount;
    }

    public Category[] getCategories() {
        return categories;
    }

    public Category findCategory(String name) {
        for (int i = 0; i < categoryCount; i++)
            if (categories[i].getName().equalsIgnoreCase(name.trim()))
                return categories[i];
        return null;
    }

    public boolean addCategory(String name) {
        if (categoryCount >= MAX_CATEGORIES) return false;
        if (findCategory(name) != null) return false;
        categories[categoryCount++] = new Category(name);
        return true;
    }

    public boolean deleteCategory(String name) {
        for (int i = 0; i < categoryCount; i++) {
            if (categories[i].getName().equalsIgnoreCase(name.trim())) {
                // Shift elements to the left
                for (int j = i; j < categoryCount - 1; j++) {
                    categories[j] = categories[j + 1];
                }
                categories[categoryCount - 1] = null;
                categoryCount--;
                return true;
            }
        }
        return false;
    }

    public boolean addTask(String categoryName, String taskName, String description) {
        Category c = findCategory(categoryName);
        if (c != null) {
            return c.addTask(new Task(taskName, description));
        }
        return false;
    }

    public boolean updateTaskProgress(String categoryName, String taskName, double totalParts, double completedParts) {
        Category c = findCategory(categoryName);
        if (c != null) {
            Task t = c.findTask(taskName);
            if (t != null) {
                 t.setProgressFromParts(totalParts, completedParts);
                 return true;
            }
        }
        return false;
    }

    public boolean deleteTask(String categoryName, String taskName) {
        Category c = findCategory(categoryName);
        if (c != null) {
            return c.deleteTask(taskName);
        }
        return false;
    }

    public String buildReport() {
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

    public void saveData() {
        try (PrintWriter out = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (int i = 0; i < categoryCount; i++) {
                Category c = categories[i];
                out.println("C|" + c.getName());
                for (int j = 0; j < c.getTaskCount(); j++) {
                    Task t = c.getTasks()[j];
                    String desc = t.getDescription() == null || t.getDescription().isEmpty() ? " " : t.getDescription();
                    out.println("T|" + c.getName() + "|" + t.getName() + "|" + t.getProgress() + "|" + desc);
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
                    addCategory(cname);

                } else if ("T".equals(parts[0]) && parts.length >= 4) {
                    String cname = parts[1].trim();
                    String tname = parts[2].trim();
                    double prog;
                    try {
                        prog = Double.parseDouble(parts[3].trim());
                    } catch (NumberFormatException ex) {
                        prog = 0.0;
                    }
                    String desc = "";
                    if (parts.length >= 5) {
                        desc = parts[4].trim();
                    }

                    Category c = findCategory(cname);
                    if (c != null) {
                        Task t = new Task(tname, desc);
                        t.setProgress(prog);
                        c.addTask(t);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }
}
