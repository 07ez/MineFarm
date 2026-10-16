package Hez.Encyclopedia;

import java.util.*;

public class PlayerEncyclopedia {

    // 기본도감(DEFAULT)은 432개 아이템 -> 432 / 32 = 13.5 -> int 14개 필요
    public static final int DEFAULT_MASK_COUNT = 14;
    // 물고기도감, 요리도감 등 기본 마스크 크기
    public static final int FISH_MASK_COUNT = 4;
    public static final int COOK_MASK_COUNT = 4;

    private final Map<EncyclopediaType, int[]> bitmasks = new EnumMap<>(EncyclopediaType.class);

    public PlayerEncyclopedia() {
        bitmasks.put(EncyclopediaType.DEFAULT, new int[DEFAULT_MASK_COUNT]);
        bitmasks.put(EncyclopediaType.FISH, new int[FISH_MASK_COUNT]);
        bitmasks.put(EncyclopediaType.COOK, new int[COOK_MASK_COUNT]);
    }

    /**
     * 특정 도감의 index번(1-based) 아이템이 해금/등록되었는지 비트 연산으로 확인
     */
    public boolean isUnlocked(EncyclopediaType type, int index) {
        if (index <= 0) return false;
        int[] masks = bitmasks.get(type);
        if (masks == null) return false;

        int group = (index - 1) / 32;
        int bit = (index - 1) % 32;

        if (group >= masks.length) return false;

        // 엔드 연산(&)으로 해당 비트 활성화 여부 확인
        return (masks[group] & (1 << bit)) != 0;
    }

    /**
     * 특정 도감의 index번(1-based) 아이템 등록/해제 (비트마스크 세팅)
     */
    public void setUnlocked(EncyclopediaType type, int index, boolean unlocked) {
        if (index <= 0) return;
        int[] masks = bitmasks.get(type);
        if (masks == null) return;

        int group = (index - 1) / 32;
        int bit = (index - 1) % 32;

        // 필요 시 배열 확장
        if (group >= masks.length) {
            int newSize = Math.max(group + 1, masks.length * 2);
            masks = Arrays.copyOf(masks, newSize);
            bitmasks.put(type, masks);
        }

        if (unlocked) {
            // 1 << bit 쉬프트 후 OR 연산
            masks[group] |= (1 << bit);
        } else {
            masks[group] &= ~(1 << bit);
        }
    }

    /**
     * 특정 도감에서 등록 완료된 총 아이템 수 (비트 카운트)
     */
    public int getUnlockedCount(EncyclopediaType type) {
        int[] masks = bitmasks.get(type);
        if (masks == null) return 0;

        int count = 0;
        for (int mask : masks) {
            count += Integer.bitCount(mask);
        }
        return count;
    }

    /**
     * 특정 도감의 마스크 배열 반환
     */
    public int[] getMasks(EncyclopediaType type) {
        return bitmasks.computeIfAbsent(type, k -> new int[DEFAULT_MASK_COUNT]);
    }

    /**
     * List<Integer> 형태로 마스크 목록 반환 (YAML 저장용)
     */
    public List<Integer> getMaskList(EncyclopediaType type) {
        int[] masks = getMasks(type);
        List<Integer> list = new ArrayList<>(masks.length);
        for (int mask : masks) {
            list.add(mask);
        }
        return list;
    }

    /**
     * List<Integer>로부터 마스크 로드 (YAML 로드용)
     */
    public void loadMaskList(EncyclopediaType type, List<Integer> list) {
        if (list == null || list.isEmpty()) return;
        int defaultCount = switch (type) {
            case DEFAULT -> DEFAULT_MASK_COUNT;
            case FISH -> FISH_MASK_COUNT;
            case COOK -> COOK_MASK_COUNT;
        };
        int size = Math.max(defaultCount, list.size());
        int[] masks = new int[size];
        for (int i = 0; i < list.size(); i++) {
            masks[i] = list.get(i);
        }
        bitmasks.put(type, masks);
    }
}
