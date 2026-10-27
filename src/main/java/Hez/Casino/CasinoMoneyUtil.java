package Hez.Casino;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.ArrayList;
import java.util.List;

public class CasinoMoneyUtil {

    public static int getMoneyValue(String cmd) {
        if (cmd == null) return 0;
        return switch (cmd) {
            case "1010" -> 100;
            case "1011" -> 1000;
            case "1012" -> 10000;
            case "1013" -> 50000;
            case "1014" -> 100000;
            default -> 0;
        };
    }

    public static String getCmdFromValue(int value) {
        return switch (value) {
            case 100 -> "1010";
            case 1000 -> "1011";
            case 10000 -> "1012";
            case 50000 -> "1013";
            case 100000 -> "1014";
            default -> "1010";
        };
    }

    public static String getMoneyName(String cmd) {
        return switch (cmd) {
            case "1010" -> "100원";
            case "1011" -> "1000원";
            case "1012" -> "10000원";
            case "1013" -> "50000원";
            case "1014" -> "100000원";
            default -> "돈";
        };
    }

    /**
     * ItemStack에서 지폐 가치(100~100,000)를 반환. 지폐가 아니면 0 반환.
     */
    public static int getMoneyFromItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        try {
            CustomModelDataComponent comp = meta.getCustomModelDataComponent();
            List<String> strings = comp.getStrings();
            if (strings != null && !strings.isEmpty()) {
                int val = getMoneyValue(strings.get(0));
                if (val > 0) return val;
            }
        } catch (Throwable ignored) {
        }

        if (meta.hasDisplayName()) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName()).replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            return switch (name) {
                case "100원" -> 100;
                case "1000원" -> 1000;
                case "10000원" -> 10000;
                case "50000원" -> 50000;
                case "100000원" -> 100000;
                default -> 0;
            };
        }
        return 0;
    }

    public static ItemStack createMoneyItem(String cmd, int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + getMoneyName(cmd));
            try {
                CustomModelDataComponent comp = meta.getCustomModelDataComponent();
                comp.setStrings(List.of(cmd));
                meta.setCustomModelDataComponent(comp);
            } catch (Throwable ignored) {
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 총 금액을 큰 지폐 단위부터 나누어 ItemStack 리스트로 환전
     */
    public static List<ItemStack> convertMoneyToItems(long totalMoney) {
        List<ItemStack> items = new ArrayList<>();
        if (totalMoney <= 0) return items;

        int[] values = {100000, 50000, 10000, 1000, 100};
        String[] cmds = {"1014", "1013", "1012", "1011", "1010"};

        long remaining = totalMoney;
        for (int i = 0; i < values.length; i++) {
            int val = values[i];
            String cmd = cmds[i];

            long count = remaining / val;
            if (count > 0) {
                remaining %= val;

                // 64개 단위로 분할 생성
                long toGive = count;
                while (toGive > 0) {
                    int stackAmount = (int) Math.min(toGive, 64);
                    items.add(createMoneyItem(cmd, stackAmount));
                    toGive -= stackAmount;
                }
            }
        }

        return items;
    }
}
