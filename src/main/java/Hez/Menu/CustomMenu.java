package Hez.Menu;

import Hez.MineFarm.Main;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CustomMenu implements InventoryHolder {

    public static final NamespacedKey ACTION_KEY = new NamespacedKey(Main.getInstance(), "menu_action");
    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");

    private final Inventory inventory;

    public CustomMenu() {
        // [\\uEBBB][\\uEAAB] 타이틀 (화이트 색상, 그림자 제거)
        Component title = Component.text()
                .append(Component.text("\uEBBB\uEAAB")
                        .font(FONT_DEFAULT)
                        .color(NamedTextColor.WHITE)
                        .shadowColor(ShadowColor.none()))
                .build();

        // 9 x 6 (54칸) 상자 생성
        this.inventory = Bukkit.createInventory(this, 54, title);
        setupItems();
    }

    /**
     * 16개 메뉴 아이템 배치 (1-based 번호를 0-based 인덱스로 변환)
     * CustomModelData 제외, DisplayName 및 PDC 태그 설정
     */
    private void setupItems() {
        // 1. (1, 2, 3) 우편함
        ItemStack mailItem = createMenuItem(Material.PAPER, "우편함", "mailbox");
        setSlots(mailItem, 1, 2, 3);

        // 2. (4, 5, 6) 퀘스트
        ItemStack questItem = createMenuItem(Material.PAPER, "퀘스트", "quest");
        setSlots(questItem, 4, 5, 6);

        // 3. (7, 8, 9) 도감
        ItemStack encyclopediaItem = createMenuItem(Material.PAPER, "도감", "encyclopedia");
        setSlots(encyclopediaItem, 7, 8, 9);

        // 4. (10, 11, 12) 펫
        ItemStack petItem = createMenuItem(Material.PAPER, "펫", "pet");
        setSlots(petItem, 10, 11, 12);

        // 5. 21 집
        inventory.setItem(20, createMenuItem(Material.PAPER, "집", "house"));

        // 6. 22 마을
        inventory.setItem(21, createMenuItem(Material.PAPER, "마을", "town"));

        // 7. 23 야생
        inventory.setItem(22, createMenuItem(Material.PAPER, "야생", "wild"));

        // 8. 24 미술관
        inventory.setItem(23, createMenuItem(Material.PAPER, "미술관", "museum"));

        // 9. 25 은행
        inventory.setItem(24, createMenuItem(Material.PAPER, "은행", "bank"));

        // 10. 26 작물
        inventory.setItem(25, createMenuItem(Material.PAPER, "작물", "crops"));

        // 11. 27 물고기
        inventory.setItem(26, createMenuItem(Material.PAPER, "물고기", "fish"));

        // 12. 35 광석
        inventory.setItem(34, createMenuItem(Material.PAPER, "광석", "ore"));

        // 13. 36 나무
        inventory.setItem(35, createMenuItem(Material.PAPER, "나무", "tree"));

        // 14. 40 위키
        inventory.setItem(39, createMenuItem(Material.PAPER, "위키", "wiki"));

        // 15. 41 디스코드
        inventory.setItem(40, createMenuItem(Material.PAPER, "디스코드", "discord"));

        // 16. 42 버그제보
        inventory.setItem(41, createMenuItem(Material.PAPER, "버그제보", "bug_report"));
    }

    /**
     * 1-based 슬롯 번호들에 동일한 아이템 배치 (와이드 버튼 지원)
     */
    private void setSlots(ItemStack item, int... oneBasedSlots) {
        for (int slot : oneBasedSlots) {
            inventory.setItem(slot - 1, item.clone());
        }
    }

    /**
     * 메뉴 아이템 생성 헬퍼
     * - CustomModelData 제외
     * - DisplayName 제목 설정
     * - PersistentDataContainer(PDC) 태그 부여 (안전한 식별용)
     */
    private ItemStack createMenuItem(Material material, String title, String actionId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title).decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(null); // 모델데이터 제거
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, actionId);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void open(Player player) {
        CustomMenu menu = new CustomMenu();
        player.openInventory(menu.getInventory());
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
