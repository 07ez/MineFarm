package Hez.Quest;

public class QuestData {
    private final int id;
    private final int page;
    private final int row;
    private final String category;
    private final String description;

    public QuestData(int id, int page, int row, String category, String description) {
        this.id = id;
        this.page = page;
        this.row = row;
        this.category = category;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public int getRow() {
        return row;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}
