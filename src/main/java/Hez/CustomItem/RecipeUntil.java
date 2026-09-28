package Hez.CustomItem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class RecipeUntil {
    /**
     * 1. 무형 조합법
     */
    @SuppressWarnings("removal")
    public static void registerShapeless(
            JavaPlugin plugin, String keyName, Material resultType,
            String customModelData, String displayName, String groupName, ItemStack... ingredients) {

        NamespacedKey key = new NamespacedKey(plugin, keyName);
        if (Bukkit.getRecipe(key) != null) Bukkit.removeRecipe(key);

        ItemStack result = CreateItem.create(resultType, customModelData, displayName);
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        for (ItemStack ingredient : ingredients) {
            recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));
        }

        if (groupName != null) {
            recipe.setGroup(groupName);
        }

        Bukkit.addRecipe(recipe);
    }

    /**
     * 2. 정형 조합법
     */
    @SuppressWarnings("removal")
    public static void registerShaped(
            JavaPlugin plugin, String keyName, Material resultType, String customModelData,
            String displayName, String[] shape, Map<Character, ItemStack> ingredients, String groupName) {

        NamespacedKey key = new NamespacedKey(plugin, keyName);
        if (Bukkit.getRecipe(key) != null) Bukkit.removeRecipe(key);

        ItemStack result = CreateItem.create(resultType, customModelData, displayName);
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(shape);

        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            recipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(entry.getValue()));
        }

        if (groupName != null) {
            recipe.setGroup(groupName);
        }

        Bukkit.addRecipe(recipe);
    }
}