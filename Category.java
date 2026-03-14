public class Category {
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

    public boolean deleteTask(String taskName) {
        for (int i = 0; i < taskCount; i++) {
            if (tasks[i].getName().equalsIgnoreCase(taskName.trim())) {
                for (int j = i; j < taskCount - 1; j++) {
                    tasks[j] = tasks[j+1];
                }
                tasks[taskCount - 1] = null;
                taskCount--;
                return true;
            }
        }
        return false;
    }

    public Task findTask(String taskName) {
        for (int i = 0; i < taskCount; i++) {
            if (tasks[i] != null && tasks[i].getName().equalsIgnoreCase(taskName.trim()))
                return tasks[i];
        }
        return null;
    }

    public double getAverageProgress() {
        if (taskCount == 0) return 0.0;
        double total = 0.0;
        for (int i = 0; i < taskCount; i++)
            if (tasks[i] != null) total += tasks[i].getProgress();
        return total / taskCount;
    }

    public String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(name).append("\n");
        if (taskCount == 0) {
            sb.append("  No tasks yet\n");
        } else {
            for (int i = 0; i < taskCount; i++)
                if (tasks[i] != null) sb.append("  ").append(tasks[i]).append("\n");
        }
        sb.append("  Average: ").append(String.format("%.2f", getAverageProgress())).append("%\n");
        return sb.toString();
    }
}
