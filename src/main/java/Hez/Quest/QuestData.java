package Hez.Quest;

import java.util.List;

public class QuestData {
    private final int id;
    private final int page;
    private final int row;
    private final String title;
    private final List<String> objectives;

    public QuestData(int id, int page, int row, String title, List<String> objectives) {
        this.id = id;
        this.page = page;
        this.row = row;
        this.title = title;
        this.objectives = objectives;
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

    public String getTitle() {
        return title;
    }

    public List<String> getObjectives() {
        return objectives;
    }
}
