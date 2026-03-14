public class Task {
    private final String name;
    private String description;
    private double progress;
    private String status;

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
        this.progress = 0.0;
        this.status = "Not Started";
    }

    public Task(String name) {
        this(name, ""); // default empty description
    }

    public Task(String name, double totalParts, double completedParts) {
        this.name = name;
        this.description = "";
        setProgressFromParts(totalParts, completedParts);
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public void setDescription(String desc) { this.description = desc; }

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
        String descStr = (description != null && !description.isEmpty()) ? " [" + description + "]" : "";
        return name + descStr + " - " + String.format("%.2f", progress) + "% - " + status;
    }
}
