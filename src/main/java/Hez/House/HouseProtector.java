package Hez.House;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class HouseProtector implements Listener {
    private final HouseManager houseManager;

    public HouseProtector(HouseManager houseManager) {
        this.houseManager = houseManager;
    }

    // 블록 파괴 금지
    @EventHandler
    public void onBreak(BlockBreakEvent event) {

    }

    // 블록 설치 금지
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {

    }
    // 경작지 파괴 금지
    @EventHandler
    public void onFarmlandJump() {

    }
    // 유체흐름 막기
    // 피스톤으로 블록 밀어서 넣기 금지
    // 불 옮기기 금지
    // 상호작용 금지
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

    }
}
