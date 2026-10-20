package Hez.Enhance;

import org.bukkit.inventory.ItemStack;

public class EnhanceResult {
    private final EnhanceResultType type;
    private final int previousLevel;
    private final int currentLevel;
    private final ItemStack resultItem;
    private final String message;

    public EnhanceResult(EnhanceResultType type, int previousLevel, int currentLevel, ItemStack resultItem, String message) {
        this.type = type;
        this.previousLevel = previousLevel;
        this.currentLevel = currentLevel;
        this.resultItem = resultItem;
        this.message = message != null ? message : type.getDefaultMessage();
    }

    public EnhanceResultType getType() {
        return type;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 강화 결과 아이템. 파괴(DESTROY)되었거나 실패하여 소멸한 경우 null일 수 있습니다.
     */
    public ItemStack getResultItem() {
        return resultItem;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return type == EnhanceResultType.SUCCESS;
    }

    public boolean isDestroyed() {
        return type == EnhanceResultType.DESTROY;
    }

    public boolean isFailure() {
        return type == EnhanceResultType.FAILURE;
    }
}
