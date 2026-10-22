package Hez.Display;

import Hez.Menu.CropGirlMenu;
import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시선 감지 디스플레이 상점 빠른 거래 리스너
 * - 좌클릭: 1개 구매
 * - Shift + 좌클릭: 64개(1세트) 구매 (잔액 부족 시 보유 금액 내 최대 수량 구매)
 * - 우클릭: 1개 판매
 * - Shift + 우클릭: 인벤토리 내 해당 품목 전부 판매 (일괄 판매)
 */
public class ShopDisplayListener implements Listener {

    private final Main plugin;
    private final DisplayManager displayManager;
    private final Map<UUID, Long> lastClickTimes = new ConcurrentHashMap<>();

    // 중복 클릭 방지 쿨다운 (0.2초)
    private static final long CLICK_COOLDOWN_MS = 200L;

    public ShopDisplayListener(Main plugin, DisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
    }

    private boolean checkAndSetCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastClickTimes.get(uuid);
        if (last != null && (now - last) < CLICK_COOLDOWN_MS) {
            return false;
        }
        lastClickTimes.put(uuid, now);
        return true;
    }

    /**
     * 플레이어가 현재 바라보고 있는 유효한 상점 Display 엔티티를 반환합니다.
     */
    private Display getTargetShopDisplay(Player player) {
        Display display = displayManager.getCurrentDisplay(player.getUniqueId());
        if (display == null || !display.isValid()) {
            display = displayManager.rayTraceShopDisplay(player);
        }
        if (display == null || !display.isValid()) {
            return null;
        }
        if (player.getWorld() != display.getWorld()) {
            return null;
        }
        if (player.getEyeLocation().distance(display.getLocation()) > 5.5) {
            return null;
        }
        return display;
    }

    // ==========================================
    // 1. 블록 / 허공 클릭 인터랙션
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 오프핸드 중복 이벤트 차단 (메인핸드만 처리)
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        boolean isLeft = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        boolean isRight = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);

        if (!isLeft && !isRight) return;

        Display targetDisplay = getTargetShopDisplay(player);
        if (targetDisplay == null) return;

        // cropgirl 디스플레이 우클릭 시 전용 GUI 오픈
        if (targetDisplay.getScoreboardTags().contains("cropgirl")) {
            event.setCancelled(true);
            if (isRight && checkAndSetCooldown(player.getUniqueId())) {
                CropGirlMenu.open(player);
            }
            return;
        }

        // 상점 디스플레이 상호작용인 경우 뒤편 블록 파괴/동작 차단
        event.setCancelled(true);

        if (!checkAndSetCooldown(player.getUniqueId())) return;

        if (isLeft) {
            handlePurchase(player, targetDisplay, player.isSneaking());
        } else {
            handleSale(player, targetDisplay, player.isSneaking());
        }
    }

    // ==========================================
    // 2. 서바이벌 허공 좌클릭 보조 감지
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        Display targetDisplay = getTargetShopDisplay(player);
        if (targetDisplay == null) return;
        if (targetDisplay.getScoreboardTags().contains("cropgirl")) return;

        if (!checkAndSetCooldown(player.getUniqueId())) return;

        handlePurchase(player, targetDisplay, player.isSneaking());
    }

    // ==========================================
    // 3. 엔티티 우클릭 보조 감지
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        // 엔티티 자체 또는 시선 타깃 디스플레이에 cropgirl 태그가 있는 경우
        if (event.getRightClicked().getScoreboardTags().contains("cropgirl")) {
            event.setCancelled(true);
            if (checkAndSetCooldown(player.getUniqueId())) {
                CropGirlMenu.open(player);
            }
            return;
        }

        Display targetDisplay = getTargetShopDisplay(player);
        if (targetDisplay != null && targetDisplay.getScoreboardTags().contains("cropgirl")) {
            event.setCancelled(true);
            if (checkAndSetCooldown(player.getUniqueId())) {
                CropGirlMenu.open(player);
            }
            return;
        }

        if (targetDisplay == null) return;

        event.setCancelled(true);
        if (!checkAndSetCooldown(player.getUniqueId())) return;

        handleSale(player, targetDisplay, player.isSneaking());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getRightClicked().getScoreboardTags().contains("cropgirl")) {
            event.setCancelled(true);
            if (checkAndSetCooldown(event.getPlayer().getUniqueId())) {
                CropGirlMenu.open(event.getPlayer());
            }
        }
    }

    // ==========================================
    // 4. 엔티티 좌클릭(공격) 보조 감지
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Display targetDisplay = getTargetShopDisplay(player);
        if (targetDisplay == null && event.getEntity() instanceof Display entityDisplay) {
            targetDisplay = entityDisplay;
        }

        if (targetDisplay == null) return;
        if (targetDisplay.getScoreboardTags().contains("cropgirl")) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (!checkAndSetCooldown(player.getUniqueId())) return;

        handlePurchase(player, targetDisplay, player.isSneaking());
    }

    // ==========================================
    // 구매 처리 로직 (좌클릭 / Shift+좌클릭)
    // ==========================================
    private void handlePurchase(Player player, Display display, boolean isShift) {
        UUID uuid = player.getUniqueId();
        Set<String> tags = display.getScoreboardTags();
        DisplayTextInfo info = displayManager.getDisplayTextInfo(tags);

        if (info == null) {
            return;
        }

        int unitPrice = info.purchase();
        if (unitPrice <= 0) {
            player.sendMessage("§c[상점] 해당 아이템은 구매할 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ItemStack itemSample = ShopItemResolver.resolveShopItem(display, tags);
        if (itemSample == null || itemSample.getType().isAir()) {
            player.sendMessage("§c[상점] 아이템 정보를 불러올 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String itemName = ShopItemResolver.getItemDisplayNameString(itemSample);
        int currentMoney = MoneyManager.getMoney(uuid);

        if (isShift) {
            // [Shift + 좌클릭]: 64개(1세트) 구매 (잔액 부족 시 보유 금액 내 최대 수량 구매)
            int maxStack = Math.min(64, itemSample.getMaxStackSize());
            int affordableCount = currentMoney / unitPrice;

            if (affordableCount <= 0) {
                player.sendMessage("§c[상점] 잔액이 부족합니다! (1개 필요: " + String.format("%,d", unitPrice) + "원 / 보유: " + String.format("%,d", currentMoney) + "원)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int targetAmount = Math.min(maxStack, affordableCount);
            int canFitAmount = ShopItemResolver.calculateCanFit(player, itemSample, targetAmount);

            if (canFitAmount <= 0) {
                player.sendMessage("§c[상점] 인벤토리에 빈 공간이 없습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int finalBuyAmount = Math.min(targetAmount, canFitAmount);
            int totalCost = finalBuyAmount * unitPrice;

            if (!MoneyManager.removeMoney(uuid, totalCost)) {
                player.sendMessage("§c[상점] 결제 처리 중 잔액이 부족하여 실패하였습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ItemStack giveStack = itemSample.clone();
            giveStack.setAmount(finalBuyAmount);
            player.getInventory().addItem(giveStack);
            player.updateInventory();

            int remainingMoney = MoneyManager.getMoney(uuid);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            player.sendMessage("§a[상점] §f" + itemName + " §e" + finalBuyAmount + "개§f를 구매했습니다. §c(-"
                    + String.format("%,d", totalCost) + "원) §7[잔액: " + String.format("%,d", remainingMoney) + "원]");
        } else {
            // [일반 좌클릭]: 1개 구매
            if (currentMoney < unitPrice) {
                player.sendMessage("§c[상점] 잔액이 부족합니다! (필요: " + String.format("%,d", unitPrice) + "원 / 보유: " + String.format("%,d", currentMoney) + "원)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int canFit = ShopItemResolver.calculateCanFit(player, itemSample, 1);
            if (canFit < 1) {
                player.sendMessage("§c[상점] 인벤토리에 빈 공간이 없습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!MoneyManager.removeMoney(uuid, unitPrice)) {
                player.sendMessage("§c[상점] 결제 처리 중 잔액이 부족하여 실패하였습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ItemStack giveStack = itemSample.clone();
            giveStack.setAmount(1);
            player.getInventory().addItem(giveStack);
            player.updateInventory();

            int remainingMoney = MoneyManager.getMoney(uuid);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            player.sendMessage("§a[상점] §f" + itemName + " §e1개§f를 구매했습니다. §c(-"
                    + String.format("%,d", unitPrice) + "원) §7[잔액: " + String.format("%,d", remainingMoney) + "원]");
        }
    }

    // ==========================================
    // 판매 처리 로직 (우클릭 / Shift+우클릭)
    // ==========================================
    private void handleSale(Player player, Display display, boolean isShift) {
        UUID uuid = player.getUniqueId();
        Set<String> tags = display.getScoreboardTags();
        DisplayTextInfo info = displayManager.getDisplayTextInfo(tags);

        if (info == null) {
            return;
        }

        int salePrice = info.sale();
        if (salePrice <= 0) {
            player.sendMessage("§c[상점] 해당 아이템은 판매할 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        ItemStack itemSample = ShopItemResolver.resolveShopItem(display, tags);
        if (itemSample == null || itemSample.getType().isAir()) {
            player.sendMessage("§c[상점] 아이템 정보를 불러올 수 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String itemName = ShopItemResolver.getItemDisplayNameString(itemSample);

        if (isShift) {
            // [Shift + 우클릭]: 인벤토리 내 해당 품목 전부 판매 (일괄 판매)
            int totalCount = ShopItemResolver.countItem(player, itemSample);
            if (totalCount <= 0) {
                player.sendMessage("§c[상점] 판매할 " + itemName + "을(를) 보유하고 있지 않습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!ShopItemResolver.removeItem(player, itemSample, totalCount)) {
                player.sendMessage("§c[상점] 아이템 회수 중 오류가 발생했습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            int totalEarned = totalCount * salePrice;
            MoneyManager.addMoney(uuid, totalEarned);

            int currentMoney = MoneyManager.getMoney(uuid);
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
            player.sendMessage("§a[상점] §f" + itemName + " §e" + totalCount + "개§f를 모두 판매했습니다. §6(+"
                    + String.format("%,d", totalEarned) + "원) §7[잔액: " + String.format("%,d", currentMoney) + "원]");
        } else {
            // [일반 우클릭]: 1개 판매
            int count = ShopItemResolver.countItem(player, itemSample);
            if (count < 1) {
                player.sendMessage("§c[상점] 판매할 " + itemName + "을(를) 보유하고 있지 않습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!ShopItemResolver.removeItem(player, itemSample, 1)) {
                player.sendMessage("§c[상점] 아이템 회수 중 오류가 발생했습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            MoneyManager.addMoney(uuid, salePrice);

            int currentMoney = MoneyManager.getMoney(uuid);
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
            player.sendMessage("§a[상점] §f" + itemName + " §e1개§f를 판매했습니다. §6(+"
                    + String.format("%,d", salePrice) + "원) §7[잔액: " + String.format("%,d", currentMoney) + "원]");
        }
    }
}
