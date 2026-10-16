package Hez.Encyclopedia;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class EncyclopediaHolder implements InventoryHolder {

    private final EncyclopediaType type;
    private int page;
    private Inventory inventory;

    public EncyclopediaHolder(EncyclopediaType type, int page) {
        this.type = type;
        this.page = page;
    }

    public EncyclopediaType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
