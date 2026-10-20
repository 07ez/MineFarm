package Hez.Enhance;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AreaMiningListener implements Listener {
    private static final Set<Block> breakingBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block startBlock = event.getBlock();
        if (breakingBlocks.contains(startBlock)) return;

        Player player = event.getPlayer();
        ItemStack pickaxe = player.getInventory().getItemInMainHand();

        // 1. 곡괭이 및 강화 레벨 확인
        if (EnhanceEquipmentType.fromItem(pickaxe) != EnhanceEquipmentType.PICKAXE) return;

        int level = EnhanceManager.getEnhanceLevel(pickaxe);
        if (level <= 0) return;

        PickaxeMiningConfig config = EnhanceManager.getPickaxeConfig(level);
        if (config == null) return;

        // 범위 채굴이 없는 레벨(1~5강)은 속성 가속으로 단일 채굴되므로 통과
        if (config.getAreaType() == AreaMiningType.NONE) return;

        // 쉬프트(웅크리기) 중이면 정밀 단일 채굴 허용
        if (player.isSneaking()) return;

        // 2. 캔 블록이 곡괭이 전용 블록인지 확인 (흙, 나무 등은 범위 채굴 방지)
        if (!startBlock.isPreferredTool(pickaxe) || startBlock.getType().getHardness() < 0) return;

        // 3. 범위 채굴 블록 목록 계산
        List<Block> targetBlocks = calculateAreaBlocks(player, startBlock, config.getAreaType());

        int count = 0;
        for (Block target : targetBlocks) {
            if (target.equals(startBlock)) {
                count++;
                continue; // 시작 블록은 이벤트가 파괴
            }

            // 곡괭이 유효 블록 및 파괴 불가 블록 체크
            if (!target.isPreferredTool(pickaxe)) continue;
            if (target.getType() == Material.BEDROCK || target.getType() == Material.BARRIER || target.getType().getHardness() < 0) continue;

            breakingBlocks.add(target);
            try {
                // 10강 자동 수거(autoPickup)일 경우
                if (config.isAutoPickup()) {
                    Collection<ItemStack> drops = target.getDrops(pickaxe, player);
                    target.setType(Material.AIR);
                    for (ItemStack drop : drops) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                        for (ItemStack left : leftover.values()) {
                            target.getWorld().dropItemNaturally(player.getLocation(), left);
                        }
                    }
                } else {
                    target.breakNaturally(pickaxe, true);
                }

                count++;
                applyDurabilityDamage(player, pickaxe);
            } finally {
                breakingBlocks.remove(target);
            }
        }

        if (count > 1) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.9f);
        }
    }

    /**
     * 시선 각도(Pitch, Yaw) 및 AreaMiningType에 맞춘 범위 블록 목록 계산
     */
    private List<Block> calculateAreaBlocks(Player player, Block center, AreaMiningType areaType) {
        List<Block> blocks = new ArrayList<>();
        Location eyeLoc = player.getEyeLocation();
        float pitch = eyeLoc.getPitch();
        float yaw = eyeLoc.getYaw();

        // 6강: 3x1 수평 채굴
        if (areaType == AreaMiningType.HORIZONTAL_3x1) {
            // 좌우 방향 계산
            if (pitch > 55 || pitch < -55) {
                // 수직을 보고 있을 때 좌우
                blocks.add(center);
                blocks.add(center.getRelative(1, 0, 0));
                blocks.add(center.getRelative(-1, 0, 0));
            } else {
                int facing = Math.round(yaw / 90.0f) & 3;
                if (facing == 0 || facing == 2) {
                    // 남/북을 볼 때 좌우는 X축
                    blocks.add(center);
                    blocks.add(center.getRelative(1, 0, 0));
                    blocks.add(center.getRelative(-1, 0, 0));
                } else {
                    // 동/서를 볼 때 좌우는 Z축
                    blocks.add(center);
                    blocks.add(center.getRelative(0, 0, 1));
                    blocks.add(center.getRelative(0, 0, -1));
                }
            }
            return blocks;
        }

        // 7강 이상: 3x3 평면 / 깊이 / 큐브 채굴
        int depthLimit = areaType.getDepth(); // 1 = 3x3 평면, 2 = 깊이 2, 3 = 3x3x3 큐브

        if (pitch > 55) {
            // 아래 바닥을 캘 때: X-Z 평면, Y 아래로 확장
            for (int d = 0; d < depthLimit; d++) {
                int dy = -d;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        blocks.add(center.getRelative(dx, dy, dz));
                    }
                }
            }
        } else if (pitch < -55) {
            // 위 천장을 캘 때: X-Z 평면, Y 위로 확장
            for (int d = 0; d < depthLimit; d++) {
                int dy = d;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        blocks.add(center.getRelative(dx, dy, dz));
                    }
                }
            }
        } else {
            // 벽면을 캘 때: Y축(-1..1) + 좌우(-1..1) + 시선 전방(depth)
            int facing = Math.round(yaw / 90.0f) & 3; // 0=남, 1=서, 2=북, 3=동
            int forwardX = (facing == 1) ? -1 : (facing == 3 ? 1 : 0);
            int forwardZ = (facing == 0) ? 1 : (facing == 2 ? -1 : 0);

            for (int d = 0; d < depthLimit; d++) {
                int stepX = forwardX * d;
                int stepZ = forwardZ * d;

                for (int dy = -1; dy <= 1; dy++) {
                    if (facing == 0 || facing == 2) {
                        // 남/북을 볼 때 좌우는 X축
                        for (int dx = -1; dx <= 1; dx++) {
                            blocks.add(center.getRelative(dx + stepX, dy, stepZ));
                        }
                    } else {
                        // 동/서를 볼 때 좌우는 Z축
                        for (int dz = -1; dz <= 1; dz++) {
                            blocks.add(center.getRelative(stepX, dy, dz + stepZ));
                        }
                    }
                }
            }
        }

        return blocks;
    }

    private void applyDurabilityDamage(Player player, ItemStack pickaxe) {
        if (pickaxe == null || pickaxe.getType() == Material.AIR) return;
        if (pickaxe.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            pickaxe.setItemMeta(damageable);

            if (damageable.getDamage() >= pickaxe.getType().getMaxDurability()) {
                pickaxe.setAmount(0);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
        }
    }
}
