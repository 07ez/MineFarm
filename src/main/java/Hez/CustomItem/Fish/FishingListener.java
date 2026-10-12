package Hez.CustomItem.Fish;

import Hez.MineFarm.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

            // 보상 아이템 결정
            ItemStack rewardItem;
            Block hookBlock = hook.getLocation().getBlock();
            if (hookBlock.getType() == Material.LAVA) {
                rewardItem = getLavaFish();
            } else {
                rewardItem = whatFishIsCatched(hookBlock.getBiome(), hook.getWorld(), hook.getWorld().getTime());
            }

            // 리듬게임 미니게임 시작!
            FishingMinigame.start(player, rewardItem);
            return;
        }

        // 3. 찌 회수 시 용암 입질 판정 체크
        if (event.getState() == PlayerFishEvent.State.REEL_IN || event.getState() == PlayerFishEvent.State.IN_GROUND) {
            cancelLavaTask(player.getUniqueId());

            // 용암 찌에 입질이 와 있었던 상태라면 용암 낚시 미니게임 시작
            if (hook.hasMetadata("lava_bite")) {
                FishingMinigame.start(player, getLavaFish());
            }
        }
    }

    /**
     * 플레이어 클릭 시 리듬게임 판정 (좌클릭 및 우클릭 모두 지원)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerArmSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (FishingMinigame.isPlaying(player.getUniqueId())) {
            FishingMinigame.handleClick(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (FishingMinigame.isPlaying(player.getUniqueId())) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK
                    || event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                FishingMinigame.handleClick(player);
            }
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
     * 용암 낚시 전용 물고기 (용암장어 등)
     */
    private ItemStack getLavaFish() {
        FishData eel = fishManager.fish("용암장어");
        if (eel != null) {
            return eel.createItemStack();
        }
        return getTrashItem();
    }

    /**
     * 조건에 맞는 물고기 또는 쓰레기 추첨
     */
    private ItemStack whatFishIsCatched(Biome biome, World world, long worldTime) {
        BiomeChecker.CustomBiomeCategory category = BiomeChecker.getCategory(biome);

        // 1. 썩은물(DEADWATER)이거나 기타(OTHER) 지역은 쓰레기만 낚임!
        if (category == BiomeChecker.CustomBiomeCategory.DEADWATER || category == BiomeChecker.CustomBiomeCategory.OTHER) {
            return getTrashItem();
        }

        // 2. 15% 기본 확률로 쓰레기 낚시
        if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            return getTrashItem();
        }

        List<FishData> availableFish = new ArrayList<>();
        for (FishData fish : fishManager.fishByKey.values()) {
            if (fish.isCatchableTime(worldTime) && fish.isCatchableWeather(world) && fish.isCatchaleBiome(biome)) {
                availableFish.add(fish);
            }
        }

        // 일치하는 물고기가 없으면 쓰레기 반환
        if (availableFish.isEmpty()) {
            return getTrashItem();
        }

        // 가중치 확률 추첨
        double totalWeight = 0.0;
        for (FishData fish : availableFish) {
            totalWeight += fish.chance();
        }

        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;
        for (FishData fish : availableFish) {
            randomValue -= fish.chance();
            if (randomValue < 0) {
                return fish.createItemStack();
            }
        }

        return availableFish.get(availableFish.size() - 1).createItemStack();
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
