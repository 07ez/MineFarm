package Hez.Display;

import Hez.CustomItem.Fish.FishData;
import Hez.CustomItem.Fish.FishManager;
import Hez.Encyclopedia.DefaultEncyclopediaRegistry;
import Hez.Encyclopedia.EncyclopediaItem;
import Hez.MineFarm.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopItemResolver {

    private static final Map<String, Material> TAG_MATERIAL_MAP = new HashMap<>();

    static {
        // [1] 작물 & 씨앗 (BuyCrop & SellCrop)
        TAG_MATERIAL_MAP.put("beetrootSeeds", Material.BEETROOT_SEEDS);
        TAG_MATERIAL_MAP.put("wheatSeeds", Material.WHEAT_SEEDS);
        TAG_MATERIAL_MAP.put("pitcherPod", Material.PITCHER_POD);
        TAG_MATERIAL_MAP.put("torchflowerSeeds", Material.TORCHFLOWER_SEEDS);
        TAG_MATERIAL_MAP.put("melonSeeds", Material.MELON_SEEDS);
        TAG_MATERIAL_MAP.put("pumpkinSeeds", Material.PUMPKIN_SEEDS);
        TAG_MATERIAL_MAP.put("carrot", Material.CARROT);
        TAG_MATERIAL_MAP.put("potato", Material.POTATO);
        TAG_MATERIAL_MAP.put("glowBerries", Material.GLOW_BERRIES);
        TAG_MATERIAL_MAP.put("sweetBerries", Material.SWEET_BERRIES);
        TAG_MATERIAL_MAP.put("netherWart", Material.NETHER_WART);
        TAG_MATERIAL_MAP.put("cocoaBeans", Material.COCOA_BEANS);
        TAG_MATERIAL_MAP.put("sugarCane", Material.SUGAR_CANE);
        TAG_MATERIAL_MAP.put("cactus", Material.CACTUS);
        TAG_MATERIAL_MAP.put("kelp", Material.KELP);
        TAG_MATERIAL_MAP.put("honeycomb", Material.HONEYCOMB);
        TAG_MATERIAL_MAP.put("honeyBottle", Material.HONEY_BOTTLE);
        TAG_MATERIAL_MAP.put("honeyBlock", Material.HONEY_BLOCK);
        TAG_MATERIAL_MAP.put("honeycombBlock", Material.HONEYCOMB_BLOCK);
        TAG_MATERIAL_MAP.put("goldenCarrot", Material.GOLDEN_CARROT);
        TAG_MATERIAL_MAP.put("poisonousPotato", Material.POISONOUS_POTATO);
        TAG_MATERIAL_MAP.put("beetroot", Material.BEETROOT);
        TAG_MATERIAL_MAP.put("wheat", Material.WHEAT);
        TAG_MATERIAL_MAP.put("pitcherPlant", Material.PITCHER_PLANT);
        TAG_MATERIAL_MAP.put("torchflower", Material.TORCHFLOWER);
        TAG_MATERIAL_MAP.put("pumpkin", Material.PUMPKIN);
        TAG_MATERIAL_MAP.put("melonSlice", Material.MELON_SLICE);

        // [2] 광물 (Ore)
        TAG_MATERIAL_MAP.put("netheriteIngot", Material.NETHERITE_INGOT);
        TAG_MATERIAL_MAP.put("goldIngot", Material.GOLD_INGOT);
        TAG_MATERIAL_MAP.put("copperIngot", Material.COPPER_INGOT);
        TAG_MATERIAL_MAP.put("ironIngot", Material.IRON_INGOT);
        TAG_MATERIAL_MAP.put("netheriteScrap", Material.NETHERITE_SCRAP);
        TAG_MATERIAL_MAP.put("rawGold", Material.RAW_GOLD);
        TAG_MATERIAL_MAP.put("rawCopper", Material.RAW_COPPER);
        TAG_MATERIAL_MAP.put("rawIron", Material.RAW_IRON);
        TAG_MATERIAL_MAP.put("emerald", Material.EMERALD);
        TAG_MATERIAL_MAP.put("diamond", Material.DIAMOND);
        TAG_MATERIAL_MAP.put("coal", Material.COAL);
        TAG_MATERIAL_MAP.put("lapis", Material.LAPIS_LAZULI);
        TAG_MATERIAL_MAP.put("redstone", Material.REDSTONE);
        TAG_MATERIAL_MAP.put("echoShard", Material.ECHO_SHARD);
        TAG_MATERIAL_MAP.put("quartz", Material.QUARTZ);
        TAG_MATERIAL_MAP.put("amethystShard", Material.AMETHYST_SHARD);

        // [3] 나무 (Tree)
        TAG_MATERIAL_MAP.put("oak", Material.OAK_LOG);
        TAG_MATERIAL_MAP.put("spruce", Material.SPRUCE_LOG);
        TAG_MATERIAL_MAP.put("birch", Material.BIRCH_LOG);
        TAG_MATERIAL_MAP.put("jungle", Material.JUNGLE_LOG);
        TAG_MATERIAL_MAP.put("acacia", Material.ACACIA_LOG);
        TAG_MATERIAL_MAP.put("darkOak", Material.DARK_OAK_LOG);
        TAG_MATERIAL_MAP.put("cherry", Material.CHERRY_LOG);
        TAG_MATERIAL_MAP.put("paleOak", Material.PALE_OAK_LOG);
        TAG_MATERIAL_MAP.put("mangrove", Material.MANGROVE_LOG);
        TAG_MATERIAL_MAP.put("bamboo", Material.BAMBOO_BLOCK);
        TAG_MATERIAL_MAP.put("azalea", Material.AZALEA);
        TAG_MATERIAL_MAP.put("floweringAzalea", Material.FLOWERING_AZALEA);
        try {
            Material poplar = Material.matchMaterial("POPLAR_LOG");
            TAG_MATERIAL_MAP.put("poplar", poplar != null ? poplar : Material.OAK_LOG);
        } catch (Exception ignored) {
            TAG_MATERIAL_MAP.put("poplar", Material.OAK_LOG);
        }
    }

    /**
     * Display 엔티티와 태그로부터 거래 대상 단일 ItemStack(수량 1)을 결정합니다.
     */
    public static ItemStack resolveShopItem(Display display, Set<String> tags) {
        FishManager fishManager = Main.getInstance().getFishManager();

        // 1. 물고기 태그 검사 (바닐라 및 커스텀 50여종)
        if (fishManager != null && tags != null) {
            for (String tag : tags) {
                FishData fishData = fishManager.fish(tag);
                if (fishData != null) {
                    return fishData.createItemStack();
                }
            }
        }

        // 2. ItemDisplay에 실제 아이템이 들어있는 경우
        if (display instanceof ItemDisplay itemDisplay) {
            ItemStack stack = itemDisplay.getItemStack();
            if (stack != null && !stack.getType().isAir()) {
                ItemStack item = stack.clone();
                item.setAmount(1);
                return item;
            }
        }

        // 3. 태그 매핑 테이블 검사
        if (tags != null) {
            for (String tag : tags) {
                Material mat = TAG_MATERIAL_MAP.get(tag);
                if (mat != null) {
                    return new ItemStack(mat, 1);
                }
            }
        }

        // 4. BlockDisplay에 실제 블록이 설정되어 있는 경우
        if (display instanceof BlockDisplay blockDisplay) {
            Material blockMat = blockDisplay.getBlock().getMaterial();
            if (blockMat.isItem()) {
                return new ItemStack(blockMat, 1);
            }
        }

        return null;
    }

    /**
     * 플레이어 인벤토리의 슬롯 아이템과 상점 상품 아이템이 동일 품목인지 판별합니다.
     */
    public static boolean isMatchingItem(ItemStack inventoryItem, ItemStack shopItem) {
        if (inventoryItem == null || inventoryItem.getType().isAir()) return false;
        if (shopItem == null || shopItem.getType().isAir()) return false;
        if (inventoryItem.getType() != shopItem.getType()) return false;

        boolean shopHasMeta = shopItem.hasItemMeta();
        boolean invHasMeta = inventoryItem.hasItemMeta();

        // 상점 아이템이 커스텀 메타(이름, 커스텀모델데이터 등)를 가진 특수 아이템인 경우
        if (shopHasMeta) {
            ItemMeta shopMeta = shopItem.getItemMeta();
            boolean hasCmd = shopMeta.hasCustomModelData();
            boolean hasName = shopMeta.hasDisplayName();

            if (hasCmd || hasName) {
                return inventoryItem.isSimilar(shopItem);
            }
        }

        // 일반 바닐라 아이템인 경우: 인벤토리 아이템이 커스텀 모델 데이터를 가진 다른 특수 아이템이 아닌지 확인
        if (invHasMeta) {
            ItemMeta invMeta = inventoryItem.getItemMeta();
            if (invMeta != null && invMeta.hasCustomModelData()) {
                // 커스텀 모델 데이터가 있으면 특수 아이템이므로 바닐라 일반 판매 대상에서 제외 (보호 목적)
                return false;
            }
        }

        return true;
    }

    /**
     * 플레이어의 36칸 보관함에서 해당 품목의 총 개수를 집계합니다.
     */
    public static int countItem(Player player, ItemStack targetItem) {
        if (targetItem == null) return 0;
        int count = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (isMatchingItem(slot, targetItem)) {
                count += slot.getAmount();
            }
        }
        return count;
    }

    /**
     * 플레이어의 보관함에서 해당 품목을 지정된 수량만큼 안전하게 차감합니다.
     */
    public static boolean removeItem(Player player, ItemStack targetItem, int amountToRemove) {
        if (amountToRemove <= 0) return true;
        int total = countItem(player, targetItem);
        if (total < amountToRemove) return false;

        int left = amountToRemove;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            ItemStack slot = storage[i];
            if (isMatchingItem(slot, targetItem)) {
                int slotAmount = slot.getAmount();
                if (slotAmount <= left) {
                    storage[i] = null;
                    left -= slotAmount;
                } else {
                    slot.setAmount(slotAmount - left);
                    left = 0;
                }
                if (left <= 0) break;
            }
        }
        player.getInventory().setStorageContents(storage);
        player.updateInventory();
        return true;
    }

    /**
     * 플레이어 인벤토리 공간에 targetItem이 최대 몇 개까지 들어갈 수 있는지 계산합니다.
     */
    public static int calculateCanFit(Player player, ItemStack targetItem, int desiredAmount) {
        if (targetItem == null || desiredAmount <= 0) return 0;
        int maxStack = targetItem.getMaxStackSize();
        int canFit = 0;

        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                canFit += maxStack;
            } else if (isMatchingItem(slot, targetItem)) {
                canFit += Math.max(0, maxStack - slot.getAmount());
            }
            if (canFit >= desiredAmount) {
                return desiredAmount;
            }
        }
        return Math.min(canFit, desiredAmount);
    }

    /**
     * 사용자 친화적인 아이템 표시 이름을 Adventure Component 형식으로 반환합니다.
     */
    public static Component getItemDisplayNameComponent(ItemStack item) {
        if (item == null) return Component.text("알 수 없는 아이템");

        // 1. 커스텀 DisplayName이 있는 경우
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().displayName();
        }

        // 2. 도감 등록 명칭이 있는 경우
        EncyclopediaItem encItem = DefaultEncyclopediaRegistry.findMatchingItem(item);
        if (encItem != null && encItem.name() != null && !encItem.name().isEmpty()) {
            return Component.text(encItem.name());
        }

        // 3. 마인크래프트 클라이언트 번역 키
        return Component.translatable(item.translationKey());
    }

    /**
     * 플레인 텍스트 형태의 아이템 이름 반환
     */
    public static String getItemDisplayNameString(ItemStack item) {
        Component comp = getItemDisplayNameComponent(item);
        return PlainTextComponentSerializer.plainText().serialize(comp).replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }
}
