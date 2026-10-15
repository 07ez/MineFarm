package Hez.House;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;

public class HouseProtector implements Listener {
    private final HouseManager houseManager;

    public HouseProtector(HouseManager houseManager) {
        this.houseManager = houseManager;
    }

    /**
     * 두 위치가 동일한 소유권의 땅인지 판별
     * (둘 다 소유자가 있고 그 소유자가 같을 때만 true. 빈 땅이 섞여있거나 소유자가 다르면 false)
     */
    private boolean isSameOwnerChunk(Location loc1, Location loc2) {
        Chunk c1 = loc1.getChunk();
        Chunk c2 = loc2.getChunk();
        if (c1.getX() == c2.getX() && c1.getZ() == c2.getZ()) {
            return true;
        }
        HouseData h1 = houseManager.GetHouseData(loc1);
        HouseData h2 = houseManager.GetHouseData(loc2);
        return h1 != null && h2 != null && h1.getOwner().equals(h2.getOwner());
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
     * 2. 블록 설치 방지 (침대 등 2블록 경계 침범 포함 완벽 차단)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // 기본 설치 위치 권한 확인
        if (!houseManager.hasBuildPermission(player, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c이곳은 보호된 구역입니다. 블록을 설치할 수 없습니다.");
            return;
        }

        // 침대(Bed)는 2칸을 차지하므로 머리(HEAD) 부분이 넘어가는 인접 블록 위치의 권한도 반드시 확인!
        if (block.getBlockData() instanceof Bed bed) {
            Block headBlock = block.getRelative(bed.getFacing());
            if (!houseManager.hasBuildPermission(player, headBlock.getLocation())) {
                event.setCancelled(true);
                player.sendMessage("§c침대가 다른 구역의 경계를 침범하여 설치할 수 없습니다.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMultiPlace(BlockMultiPlaceEvent event) {
        Player player = event.getPlayer();
        for (BlockState state : event.getReplacedBlockStates()) {
            if (!houseManager.hasBuildPermission(player, state.getLocation())) {
                event.setCancelled(true);
                player.sendMessage("§c이곳은 보호된 구역입니다. 경계를 넘어 블록을 설치할 수 없습니다.");
                return;
            }
        }
    }

    /**
     * 3. 양동이(유체) 측면 경계 설치 및 회수 방지
     * (OO집 | 내집 경계면 측면에 클릭 시 실제로 물/용암이 놓이는 위치 기준으로 검사)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        // event.getBlock()이 실제로 유체가 생성될 대상 블록!
        Block fluidTargetBlock = event.getBlock();
        Block clickedBlock = event.getBlockClicked();

        if (!houseManager.hasBuildPermission(player, fluidTargetBlock.getLocation())
                || !houseManager.hasBuildPermission(player, clickedBlock.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c이곳에 유체를 설치할 권한이 없습니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (!houseManager.hasBuildPermission(player, event.getBlock().getLocation())
                || !houseManager.hasBuildPermission(player, event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c이곳의 유체를 퍼낼 권한이 없습니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        Player player = event.getPlayer();
        if (!houseManager.hasEntityInteractPermission(player, event.getEntity())) {
            event.setCancelled(true);
            player.sendMessage("§c이 생물을 양동이에 담을 권한이 없습니다.");
        }
    }

    /**
     * 4. 상호작용, 웅크리기+버튼(생성알/설치아이템 악용), 경작지 짓밟기 방지
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 4-1. 경작지(Farmland) 짓밟기(점프) 방지: 모든 플레이어 대상
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
            return;
        }

        // 4-2. 블록 좌클릭 시 (방석/특수블록/불끄기 등 비권한자 차단)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && houseManager.isTownWorld(clickedBlock.getLocation())) {
                Block relative = clickedBlock.getRelative(event.getBlockFace());
                if (relative.getType() == Material.FIRE || relative.getType() == Material.SOUL_FIRE) {
                    if (!houseManager.hasBuildPermission(player, relative.getLocation())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // 4-3. 블록 우클릭 상호작용
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            // 마을 월드가 아니면 통과
            if (!houseManager.isTownWorld(clickedBlock.getLocation())) return;

            Block targetPlaceBlock = clickedBlock.getRelative(event.getBlockFace());
            ItemStack item = event.getItem();

            // [버그 10 해결] 웅크리기 상태에서 버튼/레버 등을 우클릭하거나, 손에 생성알/특수 아이템을 들고 우클릭할 때:
            // 버튼 작동이 아니라 아이템 사용(생성알 소환, 방석/보트/갑옷대/양동이 설치 등)이 발생하므로 건축 권한 검사!
            if (item != null && item.getType() != Material.AIR) {
                if (isRestrictedUseItem(item.getType()) || (player.isSneaking() && HouseData.isButtonOrLever(clickedBlock.getType()))) {
                    if (!houseManager.hasBuildPermission(player, clickedBlock.getLocation())
                            || !houseManager.hasBuildPermission(player, targetPlaceBlock.getLocation())) {
                        event.setCancelled(true);
                        player.sendMessage("§c이곳에서 해당 아이템을 사용할 권한이 없습니다.");
                        return;
                    }
                }
            }

            // 블록 자체 상호작용 (문, 상자, 버튼, 레버 등)
            // 단, 작물(밀, 당근, 감자 등)은 우클릭 시 GUI나 특별한 인터랙션이 없고 작물 시스템(CropListener)에서 관리하므로 여기서 차단하지 않음
            if (!(clickedBlock.getBlockData() instanceof org.bukkit.block.data.Ageable) && !houseManager.hasInteractPermission(player, clickedBlock)) {
                event.setCancelled(true);
                player.sendMessage("§c이 블록과 상호작용할 권한이 없습니다.");
            }
        }
    }

    /**
     * 비권한 구역에서 우클릭 사용이 금지되는 아이템 판별
     * (생성알, 양동이, 방석/가구류, 보트, 광산수레, 갑옷대, 폭죽, 뼛가루 등)
     */
    private boolean isRestrictedUseItem(Material material) {
        String name = material.name();
        if (name.endsWith("_SPAWN_EGG")
                || name.endsWith("_BUCKET")
                || name.endsWith("_BOAT")
                || name.endsWith("_RAFT")
                || name.endsWith("_MINECART")
                || name.contains("CUSHION")
                || name.contains("SEAT")) {
            return true;
        }
        return material == Material.FLINT_AND_STEEL
                || material == Material.FIRE_CHARGE
                || material == Material.BUCKET
                || material == Material.POWDER_SNOW_BUCKET
                || material == Material.ARMOR_STAND
                || material == Material.END_CRYSTAL
                || material == Material.BONE_MEAL
                || material == Material.ITEM_FRAME
                || material == Material.GLOW_ITEM_FRAME
                || material == Material.PAINTING;
    }

    /**
     * 5. 엔티티 배치 방지 (방석, 갑옷대, 보트, 수정 등 블록->엔티티 변환 설치 포함)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!houseManager.hasBuildPermission(player, event.getEntity().getLocation())
                || !houseManager.hasBuildPermission(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c이곳에 엔티티나 가구를 배치할 권한이 없습니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!houseManager.hasBuildPermission(player, event.getEntity().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c이곳에 전시물을 설치할 권한이 없습니다.");
        }
    }

    /**
     * 6. 뼛가루 번짐 침범 방지 (내 땅에서 뿌린 뼛가루가 옆 땅으로 꽃/잔디 번지는 것 차단)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        Location originLoc = event.getBlock().getLocation();
        if (!houseManager.isTownWorld(originLoc)) return;

        Player player = event.getPlayer();
        if (player != null && !houseManager.hasBuildPermission(player, originLoc)) {
            event.setCancelled(true);
            player.sendMessage("§c이곳에 뼛가루를 사용할 권한이 없습니다.");
            return;
        }

        // 번져서 생성되는 꽃/잔디 블록들 중 다른 소유권 청크로 넘어가는 블록만 제거!
        Iterator<BlockState> it = event.getBlocks().iterator();
        while (it.hasNext()) {
            BlockState state = it.next();
            if ( player != null ) {
                if (!houseManager.hasBuildPermission(player, state.getLocation()) || !isSameOwnerChunk(originLoc, state.getLocation())) {
                    it.remove();
                }
            } else {
                // 디스펜서 등으로 뿌렸을 경우 출발지 청크와 소유권이 다르면 제거
                if (!isSameOwnerChunk(originLoc, state.getLocation())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * 7. 나무 성장 침범 및 큰 가문비나무 땅(회백토/Podzol) 번짐 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Location saplingLoc = event.getLocation();
        if (!houseManager.isTownWorld(saplingLoc)) return;

        Player player = event.getPlayer();
        if (player != null && !houseManager.hasBuildPermission(player, saplingLoc)) {
            event.setCancelled(true);
            return;
        }

        // 자라나는 나뭇잎, 원목, 그리고 큰 가문비나무가 바꾸는 회백토(PODZOL) 등 모든 블록 중
        // 묘목이 심긴 땅과 소유권이 다른 청크로 넘어가는 블록들을 리스트에서 제거!
        Iterator<BlockState> it = event.getBlocks().iterator();
        while (it.hasNext()) {
            BlockState state = it.next();
            if (!isSameOwnerChunk(saplingLoc, state.getLocation())) {
                it.remove();
            }
        }
    }

    /**
     * 8. 엔티티 공격, 방석/탈것 파괴, 폭발 데미지 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity victim = event.getEntity();
        if (!houseManager.isTownWorld(victim.getLocation())) return;

        // [버그 1 해결] 마을 월드 내 모든 폭발 데미지(ENTITY_EXPLOSION, BLOCK_EXPLOSION) 원천 차단!
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
            return;
        }

        // 플레이어나 몬스터가 아닌 엔티티(방석, 가구, 액자, 갑옷대, 가축 등)가 불/용암/선인장 등에 파괴되는 것 방지
        if (!(victim instanceof Player) && !(victim instanceof Monster)) {
            if (event instanceof EntityDamageByBlockEvent) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!houseManager.isTownWorld(victim.getLocation())) return;

        // 폭발에 의한 엔티티 데미지 무조건 차단
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            if (!houseManager.hasEntityDamagePermission(attacker, victim)) {
                event.setCancelled(true);
                attacker.sendMessage("§c보호된 구역의 엔티티나 가구(방석 등)를 파괴할 수 없습니다.");
            }
        } else {
            // 플레이어가 아닌 엔티티(스켈레톤 화살, 크리퍼 등)가 가축/방석/액자를 부수는 것 차단
            if (!(victim instanceof Player) && !(victim instanceof Monster)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * [버그 9 해결] 방석 등 최신 버전 탑승형 엔티티(Vehicle 판정 엔티티) 파괴/공격 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        Entity vehicle = event.getVehicle();
        if (!houseManager.isTownWorld(vehicle.getLocation())) return;

        if (event.getAttacker() instanceof Player player) {
            if (!houseManager.hasEntityDamagePermission(player, vehicle)) {
                event.setCancelled(true);
                player.sendMessage("§c보호된 가구(방석)나 탈것을 부술 수 없습니다.");
            }
        } else if (event.getAttacker() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Entity vehicle = event.getVehicle();
        if (!houseManager.isTownWorld(vehicle.getLocation())) return;

        if (event.getAttacker() instanceof Player player) {
            if (!houseManager.hasEntityDamagePermission(player, vehicle)) {
                event.setCancelled(true);
                player.sendMessage("§c보호된 가구(방석)나 탈것을 부술 수 없습니다.");
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!houseManager.isTownWorld(event.getBlock().getLocation())) return;
        if (event.getEntity() instanceof Player player) {
            if (!houseManager.hasBuildPermission(player, event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        } else {
            // 엔더맨 블록 줍기, 위더, 보트 등으로 블록 변형 방지
            event.setCancelled(true);
        }
    }

    /**
     * 9. 엔티티 상호작용 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!houseManager.hasEntityInteractPermission(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c이 엔티티와 상호작용할 권한이 없습니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (!houseManager.hasEntityInteractPermission(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c이 엔티티와 상호작용할 권한이 없습니다.");
        }
    }

    /**
     * [26.3 방석(Cushion) 파괴 완벽 차단]
     * 방석(org.bukkit.entity.Cushion)은 LivingEntity도 Vehicle도 아니기 때문에
     * EntityDamageEvent 대신 Paper의 PrePlayerAttackEntityEvent가 먼저 발생하여 파괴됩니다.
     * 여기서 비권한자의 공격을 취소하면 방석 파괴가 100% 원천 차단됩니다!
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlayerAttackEntity(io.papermc.paper.event.player.PrePlayerAttackEntityEvent event) {
        Player attacker = event.getPlayer();
        Entity victim = event.getAttacked();
        if (!houseManager.isTownWorld(victim.getLocation())) return;

        if (!houseManager.hasEntityDamagePermission(attacker, victim)) {
            event.setCancelled(true);
            attacker.sendMessage("§c보호된 구역의 가구(방석)나 엔티티를 파괴할 수 없습니다.");
        }
    }

    /**
     * 10. 아이템 액자 / 그림 파괴 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!houseManager.isTownWorld(event.getEntity().getLocation())) return;

        if (event.getRemover() instanceof Player player) {
            if (!houseManager.hasEntityDamagePermission(player, event.getEntity())) {
                event.setCancelled(true);
                player.sendMessage("§c보호된 전시물을 파괴할 수 없습니다.");
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!houseManager.isTownWorld(event.getEntity().getLocation())) return;
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION) {
            event.setCancelled(true);
        }
    }

    /**
     * 11. 유체(물/용암) 흐름 침범 방지 및 내 땅 방향 흐름 멈춤(Stall) 현상 해결
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();

        if (!houseManager.isTownWorld(from.getLocation())) return;

        Chunk fromChunk = from.getChunk();
        Chunk toChunk = to.getChunk();

        if (fromChunk.getX() != toChunk.getX() || fromChunk.getZ() != toChunk.getZ()) {
            // 청크 경계를 넘어갈 때는 반드시 출발지와 도착지가 동일한 소유자의 땅이어야만 흐름 허용!
            if (!isSameOwnerChunk(from.getLocation(), to.getLocation())) {
                event.setCancelled(true);

                // [유체 멈춤(Stall) 해결]
                // 마인크래프트 유체 엔진은 경계면에서 한쪽 방향의 BlockFromToEvent가 취소되면
                // 인접한 내 땅 방향의 추가 확산 스케줄링이 끊기는 현상이 발생할 수 있습니다.
                // 이를 해결하기 위해 1틱 뒤 내 땅 내부의 인접 블록들에 물리 업데이트를 깨워 정상적으로 흐르게 보장합니다!
                org.bukkit.Bukkit.getScheduler().runTaskLater(Hez.MineFarm.Main.getInstance(), () -> {
                    if (!from.isLiquid()) return;
                    org.bukkit.block.BlockFace[] faces = {
                            org.bukkit.block.BlockFace.NORTH,
                            org.bukkit.block.BlockFace.SOUTH,
                            org.bukkit.block.BlockFace.EAST,
                            org.bukkit.block.BlockFace.WEST,
                            org.bukkit.block.BlockFace.DOWN
                    };
                    for (org.bukkit.block.BlockFace face : faces) {
                        Block neighbor = from.getRelative(face);
                        if (isSameOwnerChunk(from.getLocation(), neighbor.getLocation())) {
                            // 내 땅 쪽으로 비어있거나 유체가 흐를 수 있는 공간이면 상태 업데이트 강제 트리거
                            from.getState().update(true, true);
                            break;
                        }
                    }
                }, 1L);
            }
        }
    }

    /**
     * 11-2. 경계면 물 + 용암 접촉으로 인한 흑요석/조약돌/돌 생성 원천 차단!
     * (내 땅에 용암, 상대 땅에 물이 있을 때 서로 다른 소유권의 유체가 반응하여 블록이 변하는 현상 방지)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        if (!houseManager.isTownWorld(block.getLocation())) return;

        Material newType = event.getNewState().getType();
        if (newType == Material.OBSIDIAN || newType == Material.COBBLESTONE || newType == Material.STONE) {
            org.bukkit.block.BlockFace[] faces = {
                    org.bukkit.block.BlockFace.NORTH,
                    org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.EAST,
                    org.bukkit.block.BlockFace.WEST,
                    org.bukkit.block.BlockFace.UP,
                    org.bukkit.block.BlockFace.DOWN
            };
            // 주변 6방향 중 다른 소유권의 청크에 있는 유체(물/용암)와 접촉해서 발생한 반응이라면 차단!
            for (org.bukkit.block.BlockFace face : faces) {
                Block adj = block.getRelative(face);
                if (adj.isLiquid() || adj.getType() == Material.WATER || adj.getType() == Material.LAVA
                        || adj.getBlockData() instanceof org.bukkit.block.data.Waterlogged wl && wl.isWaterlogged()) {
                    if (!isSameOwnerChunk(block.getLocation(), adj.getLocation())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Block source = event.getSourceBlock();
        if (source == null || block.equals(source)) return;
        if (!houseManager.isTownWorld(block.getLocation())) return;

        // 서로 다른 소유권의 청크 경계에서 물과 용암이 물리 업데이트로 상호작용하는 것을 원천 차단!
        if (!isSameOwnerChunk(block.getLocation(), source.getLocation())) {
            boolean blockIsLiquid = block.isLiquid() || block.getType() == Material.WATER || block.getType() == Material.LAVA;
            boolean sourceIsLiquid = source.isLiquid() || source.getType() == Material.WATER || source.getType() == Material.LAVA;
            if (blockIsLiquid || sourceIsLiquid) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 12. 피스톤 침범 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        checkPistonCrossChunk(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        checkPistonCrossChunk(event, event.getBlocks());
    }

    private void checkPistonCrossChunk(BlockPistonEvent event, List<Block> pushedBlocks) {
        Block piston = event.getBlock();
        if (!houseManager.isTownWorld(piston.getLocation())) return;

        Block pistonHeadTarget = piston.getRelative(event.getDirection());
        if (!isSameOwnerChunk(piston.getLocation(), pistonHeadTarget.getLocation())) {
            event.setCancelled(true);
            return;
        }

        for (Block b : pushedBlocks) {
            Block target = b.getRelative(event.getDirection());
            if (!isSameOwnerChunk(piston.getLocation(), b.getLocation())
                    || !isSameOwnerChunk(piston.getLocation(), target.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * 13. 폭발 완전 무효화 및 주민 '허(NO)' 소리 출력
     * [TNT 및 폭발 이펙트/소리/블록파괴 모두 취소 + ENTITY_VILLAGER_NO 재생]
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        if (houseManager.isTownWorld(loc)) {
            event.blockList().clear();
            event.setCancelled(true);
            loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.5f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        if (houseManager.isTownWorld(loc)) {
            event.blockList().clear();
            event.setCancelled(true); // 폭발 파티클 및 쾅 소리까지 원천 차단!
            loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.5f, 1.0f);
        }
    }

    /**
     * 14. 불 번짐(용암 발화 포함) 및 블록 소실 원천 차단
     * [버그 6 해결] BlockIgniteEvent에서 LAVA, SPREAD, LIGHTNING 등 비권한 발화 모두 차단
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!houseManager.isTownWorld(loc)) return;

        // 플레이어가 직접 라이터로 불을 붙이는 경우: 해당 위치 건축 권한 확인
        if (event.getPlayer() != null) {
            if (!houseManager.hasBuildPermission(event.getPlayer(), loc)) {
                event.setCancelled(true);
            }
            return;
        }

        // 용암 불번짐(LAVA), 불 옮겨붙기(SPREAD), 번개(LIGHTNING), 화염구 등 자연 발화는 마을 월드에서 전면 차단!
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (houseManager.isTownWorld(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        if (!houseManager.isTownWorld(event.getBlock().getLocation())) return;

        if (event.getSource().getType() == Material.FIRE || event.getSource().getType() == Material.SOUL_FIRE) {
            event.setCancelled(true);
            return;
        }

        // 잔디, 균사체, 덩굴 등이 청크 경계를 넘어 다른 땅으로 번지는 것도 차단
        if (!isSameOwnerChunk(event.getSource().getLocation(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 15. 엔더진주(Ender Pearl) 사유지 침범 차단
     * - 마을 월드에서 비권한자의 엔더진주가 타인의 사유지 청크로 진입하면 즉시 진주를 소멸
     * - 진주가 소멸되는 위치에 엔더의 눈(Eye of Ender)으로 변신하여 공중에 둥실 뜬 후,
     *   엔더의 눈이 깨지는 소리와 함께 엔더진주 이미지 파편(Particle.ITEM)이 덮여 산산조각 깨지는 연출 재생!
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        // 관리자(OP)는 자유로운 이동 허용
        if (player.isOp()) return;

        Location spawnLoc = pearl.getLocation();
        if (!houseManager.isTownWorld(spawnLoc)) return;

        // 1) 던진 위치 자체가 이미 비권한 사유지인 경우 즉시 진주를 죽이고 깨짐 연출
        if (!houseManager.hasBuildPermission(player, spawnLoc)) {
            pearl.remove();
            playEnderPearlBreakEffect(player, spawnLoc);
            return;
        }

        // 2) 정상 구역에서 던져진 엔더진주가 비행 도중 타인의 사유지 청크로 진입하는 순간 감지
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (!pearl.isValid() || pearl.isDead() || ticks > 200) {
                    cancel();
                    return;
                }

                Location currentLoc = pearl.getLocation();
                if (!houseManager.isTownWorld(currentLoc)) return;

                // 비행 중 플레이어에게 권한이 없는 청크로 진입했을 때!
                if (!houseManager.hasBuildPermission(player, currentLoc)) {
                    cancel();
                    Location breakLoc = currentLoc.clone();
                    pearl.remove(); // 텔레포트 원천 차단을 위해 즉시 엔더진주 제거
                    playEnderPearlBreakEffect(player, breakLoc);
                }
            }
        }.runTaskTimer(Hez.MineFarm.Main.getInstance(), 1L, 1L);
    }

    /**
     * 엔더진주 텔레포트 2중 방어선
     * (혹시 모를 틱 지연이나 낙하 착지 시점에 비권한 구역으로 순간이동되는 것 완벽 차단)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();
        if (player.isOp()) return;

        Location to = event.getTo();
        if (to == null || !houseManager.isTownWorld(to)) return;

        if (!houseManager.hasBuildPermission(player, to)) {
            event.setCancelled(true);
            playEnderPearlBreakEffect(player, to);
        }
    }

    /**
     * 엔더진주가 청크에 들어왔을 때 엔더의 눈 터지는 소리와 함께 엔더진주 파편이 깨지는 연출
     */
    private void playEnderPearlBreakEffect(Player player, Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        // 엔더의 눈/진주 터질 때 나는 고유 소리만 깔끔하게 재생
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.0f);

        // 엔더진주가 깨지는 아이템 파티클만 깔끔하게 출력
        loc.getWorld().spawnParticle(Particle.ITEM, loc, 35, 0.2, 0.2, 0.2, 0.08, new ItemStack(Material.ENDER_PEARL));

        // 플레이어에게 알림 메시지 및 사운드 전달
        if (player != null && player.isOnline()) {
            player.sendMessage("§c타인의 사유지로는 엔더진주를 던질 수 없어 진주가 깨졌습니다!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.8f, 1.0f);
        }
    }
}
