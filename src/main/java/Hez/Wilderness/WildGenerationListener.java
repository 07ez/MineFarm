package Hez.Wilderness;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WildGenerationListener implements Listener {

    private final Main plugin;

    // 자연 생성 차단 대상 블록 (수박, 호박, 빛나는 열매/동굴 덩굴, 달콤한 열매 덤불)
    private static final Set<Material> RESTRICTED_BLOCKS = Set.of(
            Material.PUMPKIN,
            Material.CARVED_PUMPKIN,
            Material.MELON,
            Material.SWEET_BERRY_BUSH,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT
    );

    // 루팅 및 거래 차단 대상 아이템 (작물, 열매 및 씨앗)
    private static final Set<Material> RESTRICTED_ITEMS = Set.of(
            Material.PUMPKIN,
            Material.CARVED_PUMPKIN,
            Material.PUMPKIN_SEEDS,
            Material.MELON,
            Material.MELON_SLICE,
            Material.MELON_SEEDS,
            Material.SWEET_BERRIES,
            Material.GLOW_BERRIES
    );

    public WildGenerationListener(Main plugin) {
        this.plugin = plugin;
    }

    private boolean isWildWorld(String worldName) {
        return worldName != null && worldName.equalsIgnoreCase(WildManager.WILD_WORLD_NAME);
    }

    /**
     * 1. 마을(Village) 구조물 생성 차단
     * (마을만 생성되지 않게 막고, 폐광/던전/사원 등 다른 탐험 구조물은 정상 유지)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureSpawn(AsyncStructureSpawnEvent event) {
        if (!isWildWorld(event.getWorld().getName())) return;

        String structureKey = event.getStructure().getKey().getKey().toLowerCase();
        if (structureKey.contains("village")) {
            event.setCancelled(true);
        }
    }

    /**
     * 2. 야생 월드 내 주민(Villager) 및 마을 골렘 자연 스폰 차단
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isWildWorld(event.getEntity().getWorld().getName())) return;

        // 야생에서 주민 및 철골렘 스폰 차단
        if (event.getEntity() instanceof Villager || event.getEntity() instanceof IronGolem) {
            event.setCancelled(true);
            return;
        }

        // 떠돌이 상인이 스폰될 경우 거래 품목에서 제한 작물/열매/씨앗 제거
        if (event.getEntity() instanceof WanderingTrader trader) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!trader.isValid()) return;
                List<MerchantRecipe> filtered = new ArrayList<>();
                for (MerchantRecipe recipe : trader.getRecipes()) {
                    if (isRestrictedItem(recipe.getResult().getType())) continue;
                    boolean hasRestrictedIngredient = false;
                    for (ItemStack ing : recipe.getIngredients()) {
                        if (ing != null && isRestrictedItem(ing.getType())) {
                            hasRestrictedIngredient = true;
                            break;
                        }
                    }
                    if (!hasRestrictedIngredient) {
                        filtered.add(recipe);
                    }
                }
                trader.setRecipes(filtered);
            });
        }
    }

    /**
     * 3. 좀비 주민 치료(일반 주민으로 변환) 차단
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTransform(EntityTransformEvent event) {
        if (!isWildWorld(event.getEntity().getWorld().getName())) return;

        if (event.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
            event.setCancelled(true);
        }
    }

    /**
     * 4. 새로 생성되는 청크에서 수박, 호박, 빛나는열매, 달콤한열매 블록 제거
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return; // 새로 생성된 청크만 검사
        if (!isWildWorld(event.getWorld().getName())) return;

        Chunk chunk = event.getChunk();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        int minHeight = event.getWorld().getMinHeight();
        int maxHeight = event.getWorld().getMaxHeight();

        int minSection = minHeight >> 4;
        int maxSection = (maxHeight - 1) >> 4;

        for (int sec = minSection; sec <= maxSection; sec++) {
            int sectionIndex = sec - minSection;
            if (snapshot.isSectionEmpty(sectionIndex)) {
                continue;
            }

            int startY = sec << 4;
            int endY = startY + 16;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = startY; y < endY; y++) {
                        Material type = snapshot.getBlockType(x, y, z);
                        if (RESTRICTED_BLOCKS.contains(type)) {
                            chunk.getBlock(x, y, z).setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * 5. 폐광, 던전 등 상자(Loot Chest) 루팅에서 수박/호박/열매 및 씨앗 제거
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getWorld() == null || !isWildWorld(event.getWorld().getName())) return;

        event.getLoot().removeIf(item -> item != null && isRestrictedItem(item.getType()));
    }

    private boolean isRestrictedItem(Material material) {
        return material != null && RESTRICTED_ITEMS.contains(material);
    }
}
