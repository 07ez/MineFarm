package Hez.PvP.Class;

import Hez.PvP.ColosseumManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 콜로세움 '파머(FARMER)' 직업 전용 특수 능력 리스너
 * 1. 자동 경작 및 심기: 감자, 당근, 밀 씨앗 들고 땅 우클릭 -> 경작지(Farmland) 자동 변경 및 심기
 * 2. 뼛가루 수동 성장: 각 작물마다 1초 쿨타임, 뼛가루 10회 적용 시 완숙
 * 3. 완숙 수확:
 *    - 당근 -> 황금당근 1개
 *    - 감자 -> 구운감자 1개
 *    - 밀 -> 밀 1개
 * 4. 자동 조합: 인벤토리에 황금당근, 구운감자, 밀이 모두 모이면 즉시 '토끼스튜' 1개로 자동 변환
 */
public class FarmerListener implements Listener {

    private final ColosseumManager manager;

    // 결투장 내 파머 작물 데이터 (Location -> FarmerCrop)
    private final Map<Location, FarmerCrop> farmerCrops = new ConcurrentHashMap<>();

    public static class FarmerCrop {
        private final Material cropType;
        private int boneMealCount = 0;
        private long lastBoneMealTime = 0L;

        public FarmerCrop(Material cropType) {
            this.cropType = cropType;
        }

        public Material getCropType() { return cropType; }
        public int getBoneMealCount() { return boneMealCount; }
        public void incrementBoneMeal() { this.boneMealCount++; }
        public long getLastBoneMealTime() { return lastBoneMealTime; }
        public void setLastBoneMealTime(long time) { this.lastBoneMealTime = time; }
    }

    public FarmerListener(ColosseumManager manager) {
        this.manager = manager;
    }

    public void clearCrops() {
        farmerCrops.clear();
    }

    /**
     * 파머 직업인지 확인
     */
    private boolean isFarmer(Player player) {
        if (player == null) return false;
        if (!manager.isArenaWorld(player.getWorld())) return false;
        String chosenClass = manager.getSelectedClass(player.getUniqueId());
        return "파머".equalsIgnoreCase(chosenClass);
    }

    /**
     * 1. 자동 경작 + 심기 & 2. 뼛가루 1초 쿨타임 성장
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (!manager.isArenaWorld(clicked.getWorld())) return;

        Player player = event.getPlayer();
        if (!isFarmer(player)) return;

        ItemStack inHand = event.getItem();
        if (inHand == null || inHand.getType() == Material.AIR) return;

        // [기능 1] 감자, 당근, 밀 씨앗 들고 우클릭 -> 모든 블록을 경작지로 자동 변경 및 심기 (위가 공기일 때만)
        Material handType = inHand.getType();
        if (handType == Material.POTATO || handType == Material.CARROT || handType == Material.WHEAT_SEEDS) {
            if (event.getBlockFace() != BlockFace.UP) return;

            Block above = clicked.getRelative(BlockFace.UP);
            // 위에 블록이 있을 때는 안 됨 (반드시 공기여야 함)
            if (above.getType() != Material.AIR) {
                player.sendMessage("§c[파머] 위에 다른 블록이 있어 심을 수 없습니다.");
                return;
            }

            // 클릭한 블록이 공기나 액체가 아니면 어떤 블록이든 가능
            if (clicked.getType() == Material.AIR || clicked.isLiquid()) return;

            // 아레나 외벽/경기장 바깥 보호 검사
            if (manager.getTemplateManager() != null && manager.getTemplateManager().hasBounds()) {
                if (!manager.getTemplateManager().isInArenaBounds(clicked.getLocation())) {
                    player.sendMessage("§c[콜로세움] 경기장 바깥 구역에는 심을 수 없습니다.");
                    return;
                }
            }

            event.setCancelled(true);

            // 경기 종료 후 롤백을 위해 원래 블록 데이터 기록
            if (manager.getTemplateManager() != null) {
                manager.getTemplateManager().recordPlayerBlockBreak(clicked.getLocation(), clicked.getBlockData());
                manager.getTemplateManager().recordPlayerBlockPlace(above.getLocation());
            }

            // 땅을 경작지로 변경
            if (clicked.getType() != Material.FARMLAND) {
                clicked.setType(Material.FARMLAND);
            }

            // 작물 심기
            Material plantType = switch (handType) {
                case POTATO -> Material.POTATOES;
                case CARROT -> Material.CARROTS;
                case WHEAT_SEEDS -> Material.WHEAT;
                default -> null;
            };

            if (plantType != null) {
                above.setType(plantType);
                if (above.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(0);
                    above.setBlockData(ageable);
                }

                // 파머 작물 등록
                farmerCrops.put(above.getLocation(), new FarmerCrop(plantType));

                // 씨앗 1개 소모
                inHand.setAmount(inHand.getAmount() - 1);

                // 연출
                above.getWorld().playSound(above.getLocation(), Sound.ITEM_CROP_PLANT, 1.0f, 1.0f);
                above.getWorld().spawnParticle(Particle.COMPOSTER, above.getLocation().add(0.5, 0.2, 0.5), 8, 0.2, 0.1, 0.2, 0.02);
            }
            return;
        }

        // [기능 2] 뼛가루 들고 작물 우클릭 -> 1초 쿨타임 + 10스택 성장
        if (handType == Material.BONE_MEAL) {
            FarmerCrop crop = farmerCrops.get(clicked.getLocation());
            if (crop == null) {
                // 혹시 직접 심긴 작물이면 자동 등록
                if (clicked.getType() == Material.POTATOES || clicked.getType() == Material.CARROTS || clicked.getType() == Material.WHEAT) {
                    crop = new FarmerCrop(clicked.getType());
                    farmerCrops.put(clicked.getLocation(), crop);
                } else {
                    return;
                }
            }

            event.setCancelled(true);

            if (!(clicked.getBlockData() instanceof Ageable ageable)) {
                return;
            }

            int maxAge = ageable.getMaximumAge();
            if (crop.getBoneMealCount() >= 10 || ageable.getAge() >= maxAge) {
                player.sendMessage("§e[파머] 이미 완전히 다 자란 작물입니다.");
                return;
            }

            long now = System.currentTimeMillis();
            if (now - crop.getLastBoneMealTime() < 1000L) {
                player.sendMessage("§c[파머] 뼛가루는 1초마다 1개씩 사용할 수 있습니다!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                return;
            }

            // 뼛가루 1개 소모
            inHand.setAmount(inHand.getAmount() - 1);
            crop.setLastBoneMealTime(now);
            crop.incrementBoneMeal();

            int count = crop.getBoneMealCount();
            int newAge = Math.min((count * maxAge) / 10, maxAge);
            ageable.setAge(newAge);
            clicked.setBlockData(ageable);

            // 효과 연출
            clicked.getWorld().playSound(clicked.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
            clicked.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, clicked.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.05);

            if (count >= 10 || newAge >= maxAge) {
                player.sendMessage("§a[파머] 작물이 완전히 자랐습니다! (10/10)");
                player.playSound(clicked.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            } else {
                player.sendMessage("§a[파머] 뼛가루 성장 (" + count + "/10)");
            }
        }
    }

    /**
     * 3. 완숙 수확 처리 (황금당근, 구운감자, 밀) 및 4. 토끼스튜 자동 변환
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!manager.isArenaWorld(block.getWorld())) return;

        FarmerCrop crop = farmerCrops.remove(block.getLocation());
        Material type = block.getType();

        if (type == Material.POTATOES || type == Material.CARROTS || type == Material.WHEAT) {
            Player player = event.getPlayer();

            if (block.getBlockData() instanceof Ageable ageable) {
                boolean isFullyGrown = (ageable.getAge() >= ageable.getMaximumAge()) || (crop != null && crop.getBoneMealCount() >= 10);

                // 기본 마인크래프트 드롭 취소
                event.setDropItems(false);

                if (isFullyGrown) {
                    ItemStack reward = switch (type) {
                        case CARROTS -> new ItemStack(Material.GOLDEN_CARROT, 1);
                        case POTATOES -> new ItemStack(Material.BAKED_POTATO, 1);
                        case WHEAT -> new ItemStack(Material.WHEAT, 1);
                        default -> null;
                    };

                    if (reward != null) {
                        // 인벤토리에 바로 지급 (인벤토리가 꽉 찼으면 바닥에 드롭)
                        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
                        for (ItemStack drop : leftover.values()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), drop);
                        }

                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                        String rewardName = switch (reward.getType()) {
                            case GOLDEN_CARROT -> "§6황금당근";
                            case BAKED_POTATO -> "§e구운감자";
                            case WHEAT -> "§a밀";
                            default -> reward.getType().name();
                        };
                        player.sendMessage("§a[파머] 완숙 수확 완료! " + rewardName + "§a 1개를 획득했습니다.");

                        // [기능 4] 황금당근, 구운감자, 밀 3종이 모였는지 확인하여 토끼스튜 자동 변환
                        checkAndCraftRabbitStew(player);
                    }
                } else {
                    player.sendMessage("§7[파머] 아직 다 자라지 않은 작물입니다.");
                }
            }
        }
    }

    public static final String SPECIAL_FOOD_NAME = "§6★§e짱짱음식§6★";

    public static ItemStack createSpecialFood() {
        ItemStack stew = new ItemStack(Material.RABBIT_STEW, 1);
        ItemMeta meta = stew.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SPECIAL_FOOD_NAME);
            stew.setItemMeta(meta);
        }
        return stew;
    }

    /**
     * 황금당근 1개 + 구운감자 1개 + 밀 1개가 인벤토리에 있으면 즉시 '★짱짱음식★'(토끼스튜) 1개로 변환
     */
    public void checkAndCraftRabbitStew(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerInventory inv = player.getInventory();

        int crafted = 0;
        while (inv.contains(Material.GOLDEN_CARROT) &&
                inv.contains(Material.BAKED_POTATO) &&
                inv.contains(Material.WHEAT)) {

            removeOneItem(inv, Material.GOLDEN_CARROT);
            removeOneItem(inv, Material.BAKED_POTATO);
            removeOneItem(inv, Material.WHEAT);

            ItemStack stew = createSpecialFood();
            Map<Integer, ItemStack> leftover = inv.addItem(stew);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            crafted++;
        }

        if (crafted > 0) {
            player.sendMessage("§6[파머] §e황금당근, 구운감자, 밀이 모두 모여 §6★§e짱짱음식§6★ " + crafted + "개§e를 획득하였습니다!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        }
    }

    /**
     * ★짱짱음식★ 섭취 시 엄청 좋은 효과 부여 (lore 설명 없음)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.RABBIT_STEW) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("짱짱음식")) {
            Player player = event.getPlayer();

            // 엄청 좋은 버프 효과 세트:
            // 1. 재생 III (15초)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 15, 2, false, true, true));
            // 2. 흡수 IV (체력 노란 하트 4칸 추가, 2분)
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 3, false, true, true));
            // 3. 저항 II (30초)
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 1, false, true, true));
            // 4. 신속 II (30초)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1, false, true, true));
            // 5. 힘 II (30초)
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 30, 1, false, true, true));
            // 6. 포화 II (배고픔 즉시 가득)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20 * 5, 1, false, true, true));

            // 화려한 효과음 및 토템 파티클 연출
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.0, 0), 35, 0.5, 0.5, 0.5, 0.1);

            player.sendMessage("§6[★짱짱음식★] §a엄청난 힘, 재생, 흡수, 저항, 신속, 포화 효과를 획득하였습니다!");
        }
    }

    private void removeOneItem(PlayerInventory inv, Material material) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && stack.getType() == material) {
                int amount = stack.getAmount();
                if (amount <= 1) {
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(amount - 1);
                }
                return;
            }
        }
    }
}
