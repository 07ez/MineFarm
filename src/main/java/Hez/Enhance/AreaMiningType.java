package Hez.Enhance;

public enum AreaMiningType {
    NONE("단일 채굴", 1, 1, 1),
    HORIZONTAL_3x1("3x1 수평 채굴", 3, 1, 1),
    AREA_3x3("3x3 평면 채굴", 3, 3, 1),
    DEPTH_3x3x2("3x3x2 깊이 채굴", 3, 3, 2),
    CUBE_3x3x3("3x3x3 큐브 채굴", 3, 3, 3);

    private final String displayName;
    private final int width;
    private final int height;
    private final int depth;

    AreaMiningType(String displayName, int width, int height, int depth) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isAreaMining() {
        return this != NONE;
    }
}
