package Hez.House;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HouseProtector implements Listener {
    private final HouseManager houseManager;

    public HouseProtector(HouseManager houseManager) {
        this.houseManager = houseManager;
    }

    /**
     * 1. 블록 파괴 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!houseManager.hasBuildPermission(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c이곳은 보호된 구역입니다. 블록을 파괴할 수 없습니다.");
        }
    }

    /**
     * 2. 블록 설치 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!houseManager.hasBuildPermission(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c이곳은 보호된 구역입니다. 블록을 설치할 수 없습니다.");
        }
    }

    /**
     * 3. 상호작용 및 경작지 짓밟기 방지
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 3-1. 경작지(Farmland) 짓밟기(점프) 방지: 모든 플레이어 대상
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
            return;
        }

        // 3-2. 블록 우클릭 상호작용
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            // 마을 월드가 아니면 통과
            if (!houseManager.isTownWorld(clickedBlock.getLocation())) return;

            // 양동이, 라이터 등 특수 설치/사용 아이템 체크
            ItemStack item = event.getItem();
            if (item != null && isSpecialPlacementItem(item.getType())) {
                if (!houseManager.hasBuildPermission(player, clickedBlock.getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage("§c이곳에서 해당 도구를 사용할 권한이 없습니다.");
                    return;
                }
            }

            // 블록 자체 상호작용 (문, 상자, 레버 등)
            if (!houseManager.hasInteractPermission(player, clickedBlock)) {
                event.setCancelled(true);
                player.sendMessage("§c이 블록과 상호작용할 권한이 없습니다.");
            }
        }
    }

    private boolean isSpecialPlacementItem(Material material) {
        return material == Material.FLINT_AND_STEEL
                || material == Material.FIRE_CHARGE
                || material == Material.WATER_BUCKET
                || material == Material.LAVA_BUCKET
                || material == Material.BUCKET
                || material == Material.ARMOR_STAND
                || material == Material.END_CRYSTAL;
    }

    /**
     * 4. 엔티티 공격 및 데미지 방지 (PvP는 허용, 가축/액자/갑옷대 등 재산 엔티티 보호)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            if (!houseManager.hasEntityDamagePermission(attacker, event.getEntity())) {
                event.setCancelled(true);
                attacker.sendMessage("§c보호된 엔티티나 가축을 공격할 수 없습니다.");
            }
        }
    }

    /**
     * 5. 엔티티 상호작용 방지 (가축 탑승, 번식, 젖짜기 등)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!houseManager.hasEntityInteractPermission(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c이 엔티티와 상호작용할 권한이 없습니다.");
        }
    }

    /**
     * 6. 갑옷대 등 세부 상호작용 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (!houseManager.hasEntityInteractPermission(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    /**
     * 7. 아이템 액자 파괴 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (!houseManager.hasEntityDamagePermission(player, event.getEntity())) {
                event.setCancelled(true);
                player.sendMessage("§c보호된 전시물을 파괴할 수 없습니다.");
            }
        }
    }

    /**
     * 8. 유체(물/용암) 흐름 침범 방지 (서로 다른 소유권의 청크로 유입 차단)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();

        if (!houseManager.isTownWorld(from.getLocation())) return;

        Chunk fromChunk = from.getChunk();
        Chunk toChunk = to.getChunk();

        if (fromChunk.getX() != toChunk.getX() || fromChunk.getZ() != toChunk.getZ()) {
            HouseData fromHouse = houseManager.GetHouseData(from.getLocation());
            HouseData toHouse = houseManager.GetHouseData(to.getLocation());

            // 유입 대상 청크가 등록된 땅인데 출발지와 소유권이 다르면 차단
            if (toHouse != null && (fromHouse == null || !fromHouse.getOwner().equals(toHouse.getOwner()))) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 9. 피스톤 침범 방지 (다른 소유권 청크로 블록을 밀거나 당기는 것 차단)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        checkPistonCrossChunk(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        checkPistonCrossChunk(event, event.getBlocks());
    }

    private void checkPistonCrossChunk(BlockPistonEvent event, java.util.List<Block> pushedBlocks) {
        Block piston = event.getBlock();
        if (!houseManager.isTownWorld(piston.getLocation())) return;

        HouseData pistonHouse = houseManager.GetHouseData(piston.getLocation());

        for (Block b : pushedBlocks) {
            Block target = b.getRelative(event.getDirection());
            HouseData targetHouse = houseManager.GetHouseData(target.getLocation());

            if (pistonHouse != null && targetHouse != null) {
                if (!pistonHouse.getOwner().equals(targetHouse.getOwner())) {
                    event.setCancelled(true);
                    return;
                }
            } else if (pistonHouse != targetHouse) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 10. 폭발 블록 파괴 방지 (마을 월드 내 폭발 블록 피해 원천 차단)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (houseManager.isTownWorld(event.getBlock().getLocation())) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (houseManager.isTownWorld(event.getLocation())) {
            event.blockList().clear();
        }
    }

    /**
     * 11. 불 번짐 및 불로 인한 블록 소실 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (houseManager.isTownWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE) {
            if (houseManager.isTownWorld(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}
