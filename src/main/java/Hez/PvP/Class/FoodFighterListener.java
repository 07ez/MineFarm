package Hez.PvP.Class;

import Hez.PvP.ColosseumManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 푸드파이터(Food Fighter) 직업 스킬 리스너:
 * - 기반암, 깃털, 용암양동이, 유리, 유황, 화염구, 다이아몬드, 커맨드블록을 음식처럼 씹어먹을 수 있음
 * - 섭취 시 각 아이템별 고유 효과 발동
 * - 아레나 내 실수로 블록 설치/용암 쏟기 방지
 */
public class FoodFighterListener implements Listener {

    private final ColosseumManager colosseumManager;

    public FoodFighterListener(ColosseumManager colosseumManager) {
        this.colosseumManager = colosseumManager;
    }

    private boolean isFoodFighter(Player player) {
        if (colosseumManager == null || !colosseumManager.isMatchInProgress()) {
            return false;
        }
        Map<UUID, String> selectedClasses = colosseumManager.getSelectedClasses();
        return selectedClasses != null && "푸드파이터".equals(selectedClasses.get(player.getUniqueId()));
    }

    /**
     * 음식 섭취 시 효과 발동
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!isFoodFighter(player)) return;

        ItemStack item = event.getItem();
        Material mat = item.getType();

        switch (mat) {
            case BEDROCK -> {
                // 섭취시간 20초, 저항3 20초, 힘 15 10초
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 20, 2, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 10, 14, false, true, true));
                player.sendMessage("§6[푸드파이터] §7기반암을 20초 동안 씹어먹었습니다! §f(저항 III 20초, 힘 XV 10초)");
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.8f);
                } catch (Exception ignored) {}
            }
            case FEATHER -> {
                // 섭취시간 0.5초, 공중부양 1 10초
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 10, 0, false, true, true));
                player.sendMessage("§6[푸드파이터] §f깃털을 삼켰습니다! §b(공중부양 I 10초)");
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            }
            case LAVA_BUCKET -> {
                // 섭취시간 2초, 내가 불붙음
                player.setFireTicks(20 * 8); // 8초간 불붙음
                player.sendMessage("§6[푸드파이터] §c용암을 원샷했습니다! 온몸에 불이 활활 타오릅니다!");
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);
                } catch (Exception ignored) {}

                // 빈 양동이가 인벤토리에 남지 않도록 1틱 뒤 제거
                Bukkit.getScheduler().runTask(Hez.MineFarm.Main.getInstance(), () -> {
                    player.getInventory().remove(Material.BUCKET);
                });
            }
            case GLASS -> {
                // 섭취시간 2초, 투명화 30초
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 30, 0, false, true, true));
                player.sendMessage("§6[푸드파이터] §7유리를 와작와작 씹어먹었습니다! §b(투명화 30초)");
                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
            case GUNPOWDER -> {
                // 섭취시간 1.5초, 멀미 10초, 독 1초
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 1, 0, false, true, true));
                player.sendMessage("§6[푸드파이터] §e유황을 먹었습니다! 켁켁... §8(멀미 10초, 독 1초)");
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 0.8f);
                } catch (Exception ignored) {}
            }
            case FIRE_CHARGE -> {
                // 섭취시간 1초, 바라보는 방향으로 화염구 발사
                Fireball fireball = player.launchProjectile(Fireball.class);
                fireball.setYield(1.5f);
                fireball.setIsIncendiary(true);
                player.sendMessage("§6[푸드파이터] §6화염구를 삼켜 불을 뿜었습니다! §c(화염구 발사!)");
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
            case DIAMOND -> {
                // 섭취시간 1.5초, 발광 30초
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 30, 0, false, true, true));
                player.sendMessage("§6[푸드파이터] §b다이아몬드를 삼켰습니다! 온몸이 눈부시게 빛납니다! §e(발광 30초)");
                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            }
            case COMMAND_BLOCK -> {
                // 섭취시간 50초: 엔더 포탈 열리는 소리 + 1초 뒤 0.1초마다 상대방 순서대로 갑작스런 죽음! (타이틀 없음)
                // 1. 엔더 포탈 열리는 소리 재생
                for (Player p : colosseumManager.getInsideOnlinePlayers()) {
                    try {
                        p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                }

                String myTeam = colosseumManager.getSelectedTeams().get(player.getUniqueId());

                // 상대 팀 플레이어 목록 수집 (관전자 제외, 생존 플레이어)
                List<Player> enemies = new ArrayList<>();
                for (Player p : colosseumManager.getInsideOnlinePlayers()) {
                    if (p.equals(player)) continue;
                    String theirTeam = colosseumManager.getSelectedTeams().get(p.getUniqueId());
                    if (theirTeam != null && theirTeam.equalsIgnoreCase("spectator")) continue;
                    if (myTeam != null && myTeam.equalsIgnoreCase(theirTeam)) continue;
                    if (p.getHealth() > 0) {
                        enemies.add(p);
                    }
                }

                // 1초(20틱) 뒤 순서대로 0.1초(2틱)마다 상대 죽이기
                Bukkit.getScheduler().runTaskLater(Hez.MineFarm.Main.getInstance(), () -> {
                    if (enemies.isEmpty()) {
                        String teamFormatted = ColosseumManager.formatTeamName(myTeam);
                        colosseumManager.endGame(teamFormatted + " 승리!");
                        return;
                    }

                    new org.bukkit.scheduler.BukkitRunnable() {
                        int index = 0;

                        @Override
                        public void run() {
                            if (index >= enemies.size()) {
                                cancel();
                                String teamFormatted = ColosseumManager.formatTeamName(myTeam);
                                colosseumManager.endGame(teamFormatted + " 승리!");
                                return;
                            }

                            Player target = enemies.get(index);
                            if (target.isOnline() && target.getHealth() > 0) {
                                target.setHealth(0.0); // 갑작스런 죽음
                            }
                            index++;
                        }
                    }.runTaskTimer(Hez.MineFarm.Main.getInstance(), 0L, 2L); // 2틱 = 0.1초
                }, 20L); // 1초 대기
            }
            default -> {}
        }
    }

    /**
     * 푸드파이터가 아레나에서 특수 음식 아이템을 바닥에 설치하는 행위 차단 (먹기만 가능)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isFoodFighter(player)) return;

        Material mat = event.getBlockPlaced().getType();
        if (mat == Material.BEDROCK || mat == Material.GLASS || mat == Material.COMMAND_BLOCK || mat == Material.FIRE_CHARGE) {
            event.setCancelled(true);
        }
    }

    /**
     * 푸드파이터가 용암 양동이를 바닥에 쏟는 행위 차단 (먹기만 가능)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (!isFoodFighter(player)) return;

        if (event.getBucket() == Material.LAVA_BUCKET) {
            event.setCancelled(true);
        }
    }
}
