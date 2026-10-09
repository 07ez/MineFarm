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

        Player player = event.getPlayer();

        if (data.isHarvestable()) {
            // 다 자란 상태: 정상 수확
            // 플레이어 통계 연동 (추후 PlayerManager 연계)
        } else {
            // 아직 덜 자란 상태
            long remainingSec = data.getRemainingTimeMillis() / 1000L;
            long minutes = remainingSec / 60;
            long seconds = remainingSec % 60;
            player.sendMessage(String.format("§c[농사] §f아직 덜 자란 작물입니다. (남은 시간: §e%d분 %d초§f)", minutes, seconds));
        }

        // 작물 데이터 삭제
        cropsManager.removeCrop(block.getLocation());
    }

    /**
     * 작물 우클릭 상호작용:
     * - 웅크리기(Shift) + 우클릭: 작물 위에 실시간 TextDisplay 소환 (00:00:00 / 수확가능!)
     *   (크로스헤어가 벗어나면 자동 해제)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        CropData data = cropsManager.syncAndGetCrop(clickedBlock);
        if (data == null) return;

        Player player = event.getPlayer();

        // 웅크리기 + 우클릭 시 TextDisplay 띄우기
        if (player.isSneaking()) {
            event.setCancelled(true);
            cropsManager.showCropDisplay(player, clickedBlock, data);
        } else {
            // 일반 우클릭 시 채팅 안내
            if (data.isHarvestable()) {
                player.sendMessage("§a[농사] §f작물이 완전히 자라 수확할 수 있습니다! (좌클릭으로 수확)");
            } else {
                long remainingSec = data.getRemainingTimeMillis() / 1000L;
                long hours = remainingSec / 3600L;
                long minutes = (remainingSec % 3600L) / 60L;
                long seconds = remainingSec % 60L;
                player.sendMessage(String.format("§e[농사] §f수확까지 §a%02d:%02d:%02d§f 남았습니다.", hours, minutes, seconds));
            }
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
