package Hez.Casino.Holdem;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

public class HoldemCard implements Comparable<HoldemCard> {

    public enum Suit {
        HEARTS("§c♥", 0),
        DIAMONDS("§c◆", 1),
        SPADES("§8♠", 2),
        CLUBS("§8♣", 3);

        private final String symbol;
        private final int index;

        Suit(String symbol, int index) {
            this.symbol = symbol;
            this.index = index;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getIndex() {
            return index;
        }
    }

    public enum Rank {
        ACE("A", 14, 0),
        TWO("2", 2, 1),
        THREE("3", 3, 2),
        FOUR("4", 4, 3),
        FIVE("5", 5, 4),
        SIX("6", 6, 5),
        SEVEN("7", 7, 6),
        EIGHT("8", 8, 7),
        NINE("9", 9, 8),
        TEN("10", 10, 9),
        JACK("J", 11, 10),
        QUEEN("Q", 12, 11),
        KING("K", 13, 12);

        private final String name;
        private final int value; // 2 ~ 14
        private final int orderIndex; // A=0, 2=1, ..., K=12

        Rank(String name, int value, int orderIndex) {
            this.name = name;
            this.value = value;
            this.orderIndex = orderIndex;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public int getOrderIndex() {
            return orderIndex;
        }
    }

    private final Rank rank;
    private final Suit suit;

    public HoldemCard(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Rank getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }

    /**
     * 리소스팩 CustomModelData 계산 (1002 ~ 1053)
     * A: 1002~1005 (하트, 다이아, 스페이드, 클로버)
     * 2: 1006~1009
     * ...
     * K: 1050~1053
     */
    public int getCustomModelData() {
        return 1002 + rank.getOrderIndex() * 4 + suit.getIndex();
    }

    /**
     * 마인크래프트 카드 아이템 생성
     * Material: IRON_NUGGET
     * faceUp: true면 앞면, false면 뒷면(1001)
     */
    public ItemStack createItemStack(boolean faceUp) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (faceUp) {
                meta.setDisplayName(suit.getSymbol() + " §f" + rank.getName());
                setCustomModelData(meta, String.valueOf(getCustomModelData()));
            } else {
                meta.setDisplayName("§7[?] 뒷면 카드");
                setCustomModelData(meta, "1001");
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createBackCard() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7[?] 뒷면 카드");
            setCustomModelData(meta, "1001");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void setCustomModelData(ItemMeta meta, String cmd) {
        try {
            CustomModelDataComponent comp = meta.getCustomModelDataComponent();
            comp.setStrings(List.of(cmd));
            meta.setCustomModelDataComponent(comp);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int compareTo(HoldemCard o) {
        return Integer.compare(this.rank.getValue(), o.rank.getValue());
    }

    @Override
    public String toString() {
        return suit.getSymbol() + rank.getName();
    }
}
