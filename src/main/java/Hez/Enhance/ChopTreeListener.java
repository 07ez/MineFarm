package Hez.Enhance;

import Hez.MineFarm.Main;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

public class ChopTreeListener implements Listener {
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // 재귀 또는 연쇄 파괴 시 이벤트 루프 방지용 플래그 셋
    private static final Set<Block> breakingBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block startBlock = event.getBlock();
        if (breakingBlocks.contains(startBlock)) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // 1. 도끼인지 및 강화 레벨 확인
        if (EnhanceEquipmentType.fromItem(tool) != EnhanceEquipmentType.AXE) return;

        int level = EnhanceManager.getEnhanceLevel(tool);
        if (level <= 0) return;

        // 2. 캔 블록이 원목(로그)인지 확인
        if (!Tag.LOGS.isTagged(startBlock.getType())) return;

        AxeChopConfig config = EnhanceManager.getAxeConfig(level);
        if (config == null) return;

        // 3. 쿨타임 확인
        long now = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cdMillis = (long) (config.getCooldownSeconds() * 1000);

        if (now - lastUsed < cdMillis) {
            double remaining = (cdMillis - (now - lastUsed)) / 1000.0;
            player.sendMessage(String.format("§c[찹트리] 쿨타임 중입니다! (%.1f초 남음)", remaining));
            return;
        }

        // 4. 연쇄 벌목 (BFS)
        int brokenCount = chopTree(player, tool, startBlock, config);

        if (brokenCount > 1) {
            cooldowns.put(player.getUniqueId(), now);
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§a[찹트리 발동] §f총 §e" + brokenCount + "§f개 벌목 완료! " +
                            (config.getCooldownSeconds() > 0 ? String.format("§7(쿨타임: %.1f초)", config.getCooldownSeconds()) : "§b(노쿨타임)")
            ));
        }
    }

    private int chopTree(Player player, ItemStack axe, Block startBlock, AxeChopConfig config) {
        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();

        queue.add(startBlock);
        visited.add(startBlock);

        int count = 0;
        int max = config.getMaxBlocks();

        List<Block> logsToBreak = new ArrayList<>();
        List<Block> leavesToBreak = new ArrayList<>();

        while (!queue.isEmpty() && logsToBreak.size() < max) {
            Block current = queue.poll();
            logsToBreak.add(current);

            // 주변 26방향 탐색 (원목은 위/대각선 위 위주 탐색)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);

                        if (visited.contains(neighbor)) continue;

                        if (Tag.LOGS.isTagged(neighbor.getType())) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        } else if (config.isBreakLeaves() && Tag.LEAVES.isTagged(neighbor.getType())) {
                            visited.add(neighbor);
                            leavesToBreak.add(neighbor);
                        }
                    }
                }
            }
        }

        // 원목 파괴 실행
        for (Block log : logsToBreak) {
            if (log.equals(startBlock)) {
                count++;
                continue; // 시작 블록은 이벤트가 자연스럽게 파괴
            }

            breakingBlocks.add(log);
            try {
                log.breakNaturally(axe, true);
                count++;

                // 내구도 소모 (내구도 절약 확률 반영)
                applyDurabilityDamage(player, axe, config.getSaveDurabilityRate());
            } finally {
                breakingBlocks.remove(log);
            }
        }

        // 나뭇잎 파괴 (설정된 경우)
        if (config.isBreakLeaves()) {
            for (Block leaf : leavesToBreak) {
                leaf.breakNaturally(axe, true);
            }
        }

        return count;
    }

    private void applyDurabilityDamage(Player player, ItemStack axe, double saveRate) {
        if (axe == null || axe.getType() == Material.AIR) return;
        if (ThreadLocalRandom.current().nextDouble() * 100.0 < saveRate) {
            return; // 내구도 소모 면제
        }

        if (axe.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            axe.setItemMeta(damageable);

            // 내구도가 다 닳았을 때 파괴
            if (damageable.getDamage() >= axe.getType().getMaxDurability()) {
                axe.setAmount(0);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
        }
    }
}
