package Hez.Crops;

import Hez.MineFarm.Main;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

public class CropListener implements Listener {

    private final CropsManager cropsManager;

    public CropListener(CropsManager cropsManager) {
        this.cropsManager = cropsManager;
    }

    /**
     * 청크 로딩 시:
     * 해당 청크의 작물 데이터를 실시간 시간 기준으로 계산하여 블록 상태를 갱신
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        cropsManager.onChunkLoad(event.getChunk());
    }

    /**
     * 작물 씨앗 설치 시 등록
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        ItemStack itemInHand = event.getItemInHand();

        CropType type = CropType.fromSeed(itemInHand.getType());
        if (type == null) {
            type = CropType.fromBlock(placedBlock.getType());
        }

        if (type != null) {
            cropsManager.registerCrop(placedBlock, itemInHand.getType());
        }
    }

    /**
     * 바닐라 랜덤 틱 자연 성장 방지 (실시간 시간 기반 성장만 허용)
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (cropsManager.isManagedCrop(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 작물 파괴 및 수확 처리
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        CropData data = cropsManager.getCrop(block.getLocation());
        if (data == null) return;

        // 최신 시간 기준으로 동기화 및 수확 가능 여부 판정
        cropsManager.updateBlockVisual(block, data);

        // 작물 데이터 삭제
        cropsManager.removeCrop(block.getLocation());
    }

    /**
     * 작물 우클릭 상호작용:
     * 1. 뼛가루(BONE_MEAL) 들고 우클릭:
     *    - 딱 1회만 자라는 시간 10% 단축 (성장 소요 시간 / 10 차감)
     *    - 이미 사용했거나 다 자란 경우 사용 불가
     * 2. 웅크리기(Shift) + 우클릭: 작물 위에 실시간 TextDisplay 소환 (00:00:00 / 수확가능!)
     *    (크로스헤어가 벗어나면 자동 해제)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        CropData data = cropsManager.syncAndGetCrop(clickedBlock);
        if (data == null) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();

        // 1. 뼛가루 들고 우클릭 처리
        if (itemInHand != null && itemInHand.getType() == Material.BONE_MEAL) {
            // 바닐라 기본 뼛가루 동작 취소 (플러그인 자체 10% 단축 로직으로 대체)
            event.setCancelled(true);

            // 마을 월드인 경우 건축/농사 권한 확인
            if (!Main.getInstance().getHouseManager().hasBuildPermission(player, clickedBlock.getLocation())) {
                player.sendMessage("§c이곳의 작물에 뼛가루를 사용할 권한이 없습니다.");
                return;
            }

            if (data.isHarvestable()) {
                player.sendMessage("§e이미 다 자란 작물입니다.");
                return;
            }

            if (data.isBoneMealUsed()) {
                player.sendMessage("§c이 작물은 이미 뼛가루 단축 혜택을 받았습니다. (1회 제한)");
                return;
            }

            // 뼛가루 10% 단축 적용
            boolean success = data.applyBoneMeal();
            if (success) {
                // 서바이벌 모드일 경우 뼛가루 1개 소모
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                }

                // 나이 즉시 시각적 동기화
                cropsManager.updateBlockVisual(clickedBlock, data);

                // 마인크래프트 바닐라 뼛가루 파티클 및 효과음 재생
                clickedBlock.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, clickedBlock.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
                player.playSound(clickedBlock.getLocation(), org.bukkit.Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);

                long reducedSec = (data.getGrowDurationMs() / 10L) / 1000L;
                player.sendMessage(String.format("§a[농사] §f뼛가루를 사용하여 성장 시간이 §e%d초(10%%) §f단축되었습니다! (남은 시간: §b%s§f)",
                        reducedSec, data.getFormattedRemainingTime()));
            }
            return;
        }

        // 2. 웅크리기 + 우클릭 시에만 TextDisplay 띄우기
        if (player.isSneaking()) {
            event.setCancelled(true);
            cropsManager.showCropDisplay(player, clickedBlock, data);
        }
    }

    /**
     * 플레이어 퇴장 시 떠 있는 작물 TextDisplay 정리
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cropsManager.removeCropDisplay(event.getPlayer());
    }
}
