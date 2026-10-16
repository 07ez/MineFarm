package Hez.CustomItem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeUntil implements Listener {

    // 등록된 커스텀 레시피 정보 (레시피 키 -> 필요한 재료 식별자 집합)
    public record RegisteredRecipeInfo(
            NamespacedKey key,
            String displayName,
            String groupName,
            Set<String> requiredIngredientIds
    ) {}

    private static final List<RegisteredRecipeInfo> REGISTERED_RECIPES = new ArrayList<>();
    // 그룹별 묶음 해금 지원 (예: 달걀 1종만 얻어도 fried_egg 그룹 전체 해금, 물고기 1종만 얻어도 sashimi/baked_fish 해금)
    private static final Map<String, List<NamespacedKey>> GROUP_KEYS = new ConcurrentHashMap<>();

    // 등록된 레시피들의 모든 필수 재료 식별자 집합 (레시피 재료에 해당하는 아이템만 플레이어 이력에 저장)
    private static final Set<String> ALL_RECIPE_INGREDIENT_IDS = ConcurrentHashMap.newKeySet();

    // 플레이어별 누적 획득한 재료 식별자 집합 (동시에 들고 있지 않아도 과거에 한 번이라도 소유했으면 기록 유지!)
    private static final Map<UUID, Set<String>> playerOwnedItems = new ConcurrentHashMap<>();
    // 플레이어별 이미 해금 완료된 레시피 키 집합
    private static final Map<UUID, Set<NamespacedKey>> playerUnlockedRecipes = new ConcurrentHashMap<>();

    /**
     * 아이템을 고유 식별자 문자열로 변환
     * - 달걀류(EGG, BROWN_EGG, BLUE_EGG)는 "GROUP:EGG"로 통합하여 달걀 하나만 얻어도 모든 달걀 레시피 해금
     * - 버섯류(BROWN_MUSHROOM, RED_MUSHROOM)는 "GROUP:MUSHROOM"으로 통합
     * - 물고기류는 개별 식별자("CUSTOM:COD:1001" 등)와 범용 물고기("GROUP:ANY_FISH")를 함께 처리
     */
    public static String getItemIdentifier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        Material type = item.getType();
        if (type == Material.EGG || type == Material.BROWN_EGG || type == Material.BLUE_EGG) {
            return "GROUP:EGG";
        }
        if (type == Material.BROWN_MUSHROOM || type == Material.RED_MUSHROOM) {
            return "GROUP:MUSHROOM";
        }

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                List<String> strings = cmd.getStrings();
                if (strings != null && !strings.isEmpty()) {
                    return "CUSTOM:" + type.name() + ":" + strings.get(0);
                }
            }
        }

        return "MAT:" + type.name();
    }

    /**
     * 물고기 아이템인지 판별 (생선구이 / 회 그룹 레시피 해금용)
     */
    public static boolean isAnyFishItem(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.COD || type == Material.SALMON || type == Material.TROPICAL_FISH || type == Material.PUFFERFISH;
    }

    /**
     * 1. 무형 조합법 등록
     */
    @SuppressWarnings("removal")
    public static void registerShapeless(
            JavaPlugin plugin, String keyName, Material resultType,
            String customModelData, String displayName, String groupName, ItemStack... ingredients) {

        NamespacedKey key = new NamespacedKey(plugin, keyName);
        if (Bukkit.getRecipe(key) != null) Bukkit.removeRecipe(key);

        ItemStack result = CreateItem.create(resultType, customModelData, displayName);
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);

        Set<String> reqIds = new HashSet<>();
        for (ItemStack ingredient : ingredients) {
            recipe.addIngredient(new RecipeChoice.ExactChoice(ingredient));
            if ("baked_fish".equals(groupName) || "sashimi".equals(groupName)) {
                if (isAnyFishItem(ingredient)) {
                    reqIds.add("GROUP:ANY_FISH");
                    continue;
                }
            }
            String id = getItemIdentifier(ingredient);
            if (id != null) reqIds.add(id);
        }

        if (groupName != null) {
            recipe.setGroup(groupName);
            GROUP_KEYS.computeIfAbsent(groupName, k -> new ArrayList<>()).add(key);
        }

        Bukkit.addRecipe(recipe);
        REGISTERED_RECIPES.add(new RegisteredRecipeInfo(key, displayName, groupName, reqIds));
        ALL_RECIPE_INGREDIENT_IDS.addAll(reqIds);
    }

    /**
     * 2. 정형 조합법 등록
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

        Set<String> reqIds = new HashSet<>();
        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            ItemStack ing = entry.getValue();
            recipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(ing));
            if ("baked_fish".equals(groupName) || "sashimi".equals(groupName)) {
                if (isAnyFishItem(ing)) {
                    reqIds.add("GROUP:ANY_FISH");
                    continue;
                }
            }
            String id = getItemIdentifier(ing);
            if (id != null) reqIds.add(id);
        }

        if (groupName != null) {
            recipe.setGroup(groupName);
            GROUP_KEYS.computeIfAbsent(groupName, k -> new ArrayList<>()).add(key);
        }

        Bukkit.addRecipe(recipe);
        REGISTERED_RECIPES.add(new RegisteredRecipeInfo(key, displayName, groupName, reqIds));
        ALL_RECIPE_INGREDIENT_IDS.addAll(reqIds);
    }

    /**
     * 해당 아이템이 등록된 레시피들의 재료로 쓰이는지 여부 판별
     */
    public static boolean isRecipeIngredient(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (isAnyFishItem(item) && ALL_RECIPE_INGREDIENT_IDS.contains("GROUP:ANY_FISH")) {
            return true;
        }
        String id = getItemIdentifier(item);
        return id != null && ALL_RECIPE_INGREDIENT_IDS.contains(id);
    }

    /**
     * 플레이어가 특정 아이템을 획득했을 때 레시피 재료에 해당하는 경우만 기록하고 조건 충족 레시피를 즉시 해금
     */
    public static void recordItemAcquired(Player player, ItemStack item) {
        if (player == null || item == null || item.getType() == Material.AIR) return;

        UUID uuid = player.getUniqueId();
        Set<String> owned = playerOwnedItems.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        boolean addedNew = false;
        String id = getItemIdentifier(item);
        if (id != null && ALL_RECIPE_INGREDIENT_IDS.contains(id) && owned.add(id)) {
            addedNew = true;
        }
        if (isAnyFishItem(item) && ALL_RECIPE_INGREDIENT_IDS.contains("GROUP:ANY_FISH") && owned.add("GROUP:ANY_FISH")) {
            addedNew = true;
        }

        if (addedNew) {
            checkAndUnlockRecipes(player);
        }
    }

    /**
     * 플레이어의 전체 인벤토리를 스캔하여 레시피 재료 아이템만 보유 기록하고 해금 가능한 모든 레시피 해금
     */
    public static void scanInventoryAndUnlock(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        Set<String> owned = playerOwnedItems.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        boolean added = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String id = getItemIdentifier(item);
            if (id != null && ALL_RECIPE_INGREDIENT_IDS.contains(id) && owned.add(id)) {
                added = true;
            }
            if (isAnyFishItem(item) && ALL_RECIPE_INGREDIENT_IDS.contains("GROUP:ANY_FISH") && owned.add("GROUP:ANY_FISH")) {
                added = true;
            }
        }

        if (added) {
            checkAndUnlockRecipes(player);
        }
    }

    /**
     * 조건을 만족한 모든 레시피를 플레이어 레시피 북에 해금 (player.discoverRecipe)
     */
    private static void checkAndUnlockRecipes(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> owned = playerOwnedItems.get(uuid);
        if (owned == null || owned.isEmpty()) return;

        Set<NamespacedKey> unlocked = playerUnlockedRecipes.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        List<NamespacedKey> newlyUnlocked = new ArrayList<>();

        for (RegisteredRecipeInfo info : REGISTERED_RECIPES) {
            if (unlocked.contains(info.key())) continue;

            // 해당 레시피의 모든 필수 재료들을 과거에 한 번이라도 획득한 적이 있는지 검사!
            if (owned.containsAll(info.requiredIngredientIds())) {
                unlocked.add(info.key());
                newlyUnlocked.add(info.key());

                // 만약 그룹 레시피(예: fried_egg, pancakes, omelet, baked_fish, sashimi, fried_mushroom)라면 같은 그룹의 변형 레시피들도 함께 해금!
                if (info.groupName() != null) {
                    List<NamespacedKey> groupKeys = GROUP_KEYS.get(info.groupName());
                    if (groupKeys != null) {
                        for (NamespacedKey gk : groupKeys) {
                            if (unlocked.add(gk)) {
                                newlyUnlocked.add(gk);
                            }
                        }
                    }
                }
            }
        }

        if (!newlyUnlocked.isEmpty()) {
            player.discoverRecipes(newlyUnlocked);
        }
    }

    // =========================================================================
    // 데이터 저장/로드 연동용 헬퍼 (PlayerManager 연동)
    // =========================================================================

    /**
     * 레시피 등록 완료 후, 기존 로드된 데이터 중 실제 레시피 재료가 아닌 항목들을 일괄 제거
     */
    public static void cleanUpNonRecipeIngredients() {
        for (Set<String> owned : playerOwnedItems.values()) {
            owned.removeIf(id -> !ALL_RECIPE_INGREDIENT_IDS.contains(id));
        }
    }

    public static List<String> getOwnedItemsList(UUID uuid) {
        Set<String> set = playerOwnedItems.get(uuid);
        if (set == null) return List.of();
        List<String> list = new ArrayList<>();
        for (String id : set) {
            if (ALL_RECIPE_INGREDIENT_IDS.contains(id)) {
                list.add(id);
            }
        }
        return list;
    }

    public static void loadOwnedItems(UUID uuid, List<String> items) {
        if (items == null) return;
        Set<String> owned = playerOwnedItems.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        for (String id : items) {
            if (ALL_RECIPE_INGREDIENT_IDS.isEmpty() || ALL_RECIPE_INGREDIENT_IDS.contains(id)) {
                owned.add(id);
            }
        }
    }

    // =========================================================================
    // 이벤트 리스너: 아이템 줍기, 인벤토리 클릭/닫기, 제작, 양동이 채우기, 접속 시 자동 감지
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(Hez.MineFarm.Main.getInstance(), () -> {
            checkAndUnlockRecipes(player);
            scanInventoryAndUnlock(player);
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            recordItemAcquired(player, event.getItem().getItemStack());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            recordItemAcquired(player, event.getCurrentItem());
            Bukkit.getScheduler().runTaskLater(Hez.MineFarm.Main.getInstance(), () -> scanInventoryAndUnlock(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getCurrentItem() != null) {
                recordItemAcquired(player, event.getCurrentItem());
            }
            Bukkit.getScheduler().runTaskLater(Hez.MineFarm.Main.getInstance(), () -> scanInventoryAndUnlock(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scanInventoryAndUnlock(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.getItemStack() != null) {
            recordItemAcquired(event.getPlayer(), event.getItemStack());
        }
    }
}