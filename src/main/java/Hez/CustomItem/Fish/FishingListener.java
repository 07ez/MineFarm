package Hez.CustomItem.Fish;

import Hez.MineFarm.Main;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FishingListener implements Listener {
    private final FishManager fishManager = Main.getInstance().getFishManager();

    // 용암 낚시 추적 맵
    private final Map<UUID, BukkitTask> lavaFishingTasks = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FishHook hook = event.getHook();

        // 1. 찌 던짐 감지 -> 용암 낚시 감지 태스크 가동
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            startLavaCheckTask(player, hook);
            return;
        }

        // 2. 바닐라 물고기 낚아 올림 감지
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // 바닐라 기본 아이템 드롭 제거
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }
            event.setExpToDrop(0);

            Block hookBlock = hook.getLocation().getBlock();
            if (hookBlock.getType() == Material.LAVA) {
                handleLavaCatch(player);
            } else {
                handleWaterCatch(player, hookBlock.getBiome(), hook.getWorld(), hook.getWorld().getTime());
            }
            return;
        }

        // 3. 찌 회수 시 용암 입질 판정 체크
        if (event.getState() == PlayerFishEvent.State.REEL_IN || event.getState() == PlayerFishEvent.State.IN_GROUND) {
            cancelLavaTask(player.getUniqueId());

            // 용암 찌에 입질이 와 있었던 상태라면 용암 낚시 미니게임 시작
            if (hook.hasMetadata("lava_bite")) {
                handleLavaCatch(player);
            }
        }
    }

    /**
     * 체스트 GUI 미니게임 클릭 이벤트
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        FishingMinigame.GameSession session = FishingMinigame.getSession(player.getUniqueId());
        if (session == null || session.isFinished()) return;

        // 미니게임 GUI 내 모든 클릭 기본 취소 (아이템 루팅/이동 방지)
        event.setCancelled(true);

        // 상단 인벤토리 슬롯을 클릭했는지 확인
        if (event.getClickedInventory() == session.getInventory()) {
            int slot = event.getSlot();

            // 물고기가 위치한 슬롯을 정확히 클릭했는지 판정!
            if (slot == session.getCurrentFishSlot()) {
                FishingMinigame.onFishClicked(player);
            }
        }
    }

    /**
     * 미니게임 진행 중 창을 임의로 닫았을 때 실패 처리
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        FishingMinigame.GameSession session = FishingMinigame.getSession(player.getUniqueId());
        if (session != null && !session.isFinished()) {
            FishingMinigame.onFail(player, "창을 닫아 물고기가 도망쳤습니다!");
        }
    }

    /**
     * 용암 찌 추적 및 입질 유도 태스크
     */
    private void startLavaCheckTask(Player player, FishHook hook) {
        cancelLavaTask(player.getUniqueId());

        BukkitTask task = new BukkitRunnable() {
            int ticksInLava = 0;
            final int biteTime = ThreadLocalRandom.current().nextInt(60, 120); // 3~6초 후 입질

            @Override
            public void run() {
                if (!player.isOnline() || !hook.isValid()) {
                    cancelLavaTask(player.getUniqueId());
                    cancel();
                    return;
                }

                Block block = hook.getLocation().getBlock();
                if (block.getType() == Material.LAVA) {
                    ticksInLava++;

                    // 용암 거품 효과
                    hook.getWorld().spawnParticle(Particle.LAVA, hook.getLocation().add(0, 0.2, 0), 2, 0.1, 0.1, 0.1, 0);

                    // 입질 발생!
                    if (ticksInLava >= biteTime && !hook.hasMetadata("lava_bite")) {
                        hook.setMetadata("lava_bite", new FixedMetadataValue(Main.getInstance(), true));

                        // 입질 사운드 및 큰 파티클
                        hook.getWorld().spawnParticle(Particle.FLAME, hook.getLocation().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
                        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f);
                        player.sendMessage("§c§l[용암 낚시] §e무언가가 찌를 물었습니다! 지금 낚싯대를 당기세요!");
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 10L, 2L);

        lavaFishingTasks.put(player.getUniqueId(), task);
    }

    private void cancelLavaTask(UUID uuid) {
        BukkitTask task = lavaFishingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 용암 낚시 처리 (용암장어 EPIC 미니게임)
     */
    private void handleLavaCatch(Player player) {
        FishData eel = fishManager.fish("용암장어");
        if (eel != null) {
            FishingMinigame.start(player, eel.createItemStack(), FishRarity.EPIC);
        } else {
            giveTrashDirectly(player);
        }
    }

    /**
     * 일반 물 낚시 처리
     * - 조건 불일치 시: 미니게임 없이 즉시 쓰레기 획득
     * - 조건 일치 시: 희귀도 가중치 추첨(레어25, 에픽3, 레전드1, 커먼 나머지71) 후 미니게임 시작
     */
    private void handleWaterCatch(Player player, Biome biome, World world, long worldTime) {
        BiomeChecker.CustomBiomeCategory category = BiomeChecker.getCategory(biome);

        // 1. 썩은물(DEADWATER)이거나 기타(OTHER) 지역은 조건 불일치 -> 즉시 쓰레기 획득
        if (category == BiomeChecker.CustomBiomeCategory.DEADWATER || category == BiomeChecker.CustomBiomeCategory.OTHER) {
            giveTrashDirectly(player);
            return;
        }

        // 2. 현재 환경 조건(시간, 날씨, 바이옴)에 부합하는 물고기 필터링 및 희귀도별 분류
        Map<FishRarity, List<FishData>> poolByRarity = new EnumMap<>(FishRarity.class);
        for (FishRarity r : FishRarity.values()) {
            poolByRarity.put(r, new ArrayList<>());
        }

        for (FishData fish : fishManager.fishByKey.values()) {
            // 용암장어는 물에서 제외
            if (fish.displayName() != null && fish.displayName().equals("용암장어")) continue;

            if (fish.isCatchableTime(worldTime) && fish.isCatchableWeather(world) && fish.isCatchaleBiome(biome)) {
                poolByRarity.get(fish.rarity()).add(fish);
            }
        }

        // 잡을 수 있는 물고기가 하나도 없다면 -> 즉시 쓰레기 획득
        boolean hasAnyFish = poolByRarity.values().stream().anyMatch(list -> !list.isEmpty());
        if (!hasAnyFish) {
            giveTrashDirectly(player);
            return;
        }

        // 3. 희귀도 그룹 추첨 (레어 25, 에픽 3, 레전드 1, 커먼 71)
        // 단, 해당 환경에서 특정 희귀도 풀이 비어있다면 존재하는 희귀도 풀 내에서만 가중치 계산
        double totalWeight = 0.0;
        for (Map.Entry<FishRarity, List<FishData>> entry : poolByRarity.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                totalWeight += entry.getKey().getWeight();
            }
        }

        double rand = ThreadLocalRandom.current().nextDouble() * totalWeight;
        FishRarity chosenRarity = null;
        for (Map.Entry<FishRarity, List<FishData>> entry : poolByRarity.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                rand -= entry.getKey().getWeight();
                if (rand <= 0) {
                    chosenRarity = entry.getKey();
                    break;
                }
            }
        }
        if (chosenRarity == null) {
            chosenRarity = FishRarity.COMMON;
        }

        // 4. 선택된 희귀도 풀 내부에서 개별 chance 가중치 추첨
        List<FishData> chosenPool = poolByRarity.get(chosenRarity);
        if (chosenPool.isEmpty()) {
            // fallback
            for (List<FishData> list : poolByRarity.values()) {
                if (!list.isEmpty()) {
                    chosenPool = list;
                    chosenRarity = list.get(0).rarity();
                    break;
                }
            }
        }

        double innerTotalWeight = 0.0;
        for (FishData fish : chosenPool) {
            innerTotalWeight += fish.chance();
        }

        FishData selectedFish = chosenPool.get(0);
        if (innerTotalWeight > 0) {
            double innerRand = ThreadLocalRandom.current().nextDouble() * innerTotalWeight;
            for (FishData fish : chosenPool) {
                innerRand -= fish.chance();
                if (innerRand <= 0) {
                    selectedFish = fish;
                    break;
                }
            }
        } else {
            selectedFish = chosenPool.get(ThreadLocalRandom.current().nextInt(chosenPool.size()));
        }

        // 5. 미니게임 시작!
        FishingMinigame.start(player, selectedFish.createItemStack(), chosenRarity);
    }

    /**
     * 조건 미일치 구역: 미니게임 없이 즉시 쓰레기 획득
     */
    private void giveTrashDirectly(Player player) {
        ItemStack trash = getTrashItem();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(trash);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        player.sendMessage("§7[낚시] 조건에 맞는 물고기가 없어 쓰레기가 건져졌습니다: " + (trash.getItemMeta() != null ? trash.getItemMeta().getDisplayName() : "쓰레기"));
    }

    /**
     * 쓰레기 아이템 추첨 (종이 또는 뼛가루)
     */
    private ItemStack getTrashItem() {
        boolean isPaper = ThreadLocalRandom.current().nextBoolean();
        if (isPaper) {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§7젖은 종이");
                meta.setLore(List.of("§8물에 젖어 찢어진 낡은 종이 쓰레기입니다."));
                paper.setItemMeta(meta);
            }
            return paper;
        } else {
            ItemStack boneMeal = new ItemStack(Material.BONE_MEAL);
            ItemMeta meta = boneMeal.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§7뼛가루 조각");
                meta.setLore(List.of("§8낚시 중 건져 올린 오래된 뼛가루 쓰레기입니다."));
                boneMeal.setItemMeta(meta);
            }
            return boneMeal;
        }
    }
}
