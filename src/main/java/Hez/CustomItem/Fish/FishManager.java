package Hez.CustomItem.Fish;

import org.bukkit.Material;

import java.util.*;

public class FishManager {

    public final Map<String, FishData> fishByKey = new HashMap<>();

    public FishData fish(String key) {
        return fishByKey.get(key);
    }

    public void Init() {
        if (!fishByKey.isEmpty()) return;

        // ==========================================
        // 1. 바닐라 기본 물고기 (4종)
        // ==========================================
        fishByKey.put("대구", new FishData(Material.COD, null, null, FishRarity.COMMON,
                0, 13000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("연어", new FishData(Material.SALMON, null, null, FishRarity.COMMON,
                0, 13000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("복어", new FishData(Material.PUFFERFISH, null, null, FishRarity.RARE,
                6000, 10000, 7.0,
                FishData.WeatherCondition.CLEAR, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("열대어", new FishData(Material.TROPICAL_FISH, null, null, FishRarity.EPIC,
                0, 13000, 5.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        // ==========================================
        // 2. COMMON (일반 - 16종)
        // 잉어, 청어, 작은입우럭, 멸치, 정어리, 선피쉬, 피라미, 도미, 농어, 해삼, 붉은퉁돔, 월아이, 전어, 무지개송어, 눈동자개, 블롭피쉬
        // ==========================================
        fishByKey.put("잉어", new FishData(Material.COD, "1008", "잉어", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("청어", new FishData(Material.COD, "1019", "청어", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("작은입우럭", new FishData(Material.COD, "1041", "작은입우럭", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("멸치", new FishData(Material.COD, "1002", "멸치", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("정어리", new FishData(Material.COD, "1036", "정어리", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("선피쉬", new FishData(Material.COD, "1047", "선피쉬", FishRarity.COMMON,
                0, 13000, 10.0,
                FishData.WeatherCondition.CLEAR, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("피라미", new FishData(Material.COD, "1010", "피라미", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("도미", new FishData(Material.COD, "1006", "도미", FishRarity.COMMON,
                12000, 0, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("농어", new FishData(Material.COD, "1030", "농어", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("해삼", new FishData(Material.COD, "1038", "해삼", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("붉은퉁돔", new FishData(Material.COD, "1034", "붉은퉁돔", FishRarity.COMMON,
                0, 13000, 10.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("월아이", new FishData(Material.COD, "1053", "월아이", FishRarity.COMMON,
                6000, 22000, 9.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("전어", new FishData(Material.COD, "1039", "전어", FishRarity.COMMON,
                3000, 8000, 10.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("무지개송어", new FishData(Material.COD, "1033", "무지개송어", FishRarity.COMMON,
                0, 13000, 10.0,
                FishData.WeatherCondition.CLEAR, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("눈동자개", new FishData(Material.COD, "1007", "눈동자개", FishRarity.COMMON,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("블롭피쉬", new FishData(Material.COD, "1004", "블롭피쉬", FishRarity.COMMON,
                14000, 22000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        // ==========================================
        // 3. RARE (희귀 - 23종)
        // 가자미, 넙치, 쏠배감펭, 큰입우럭, 귀신물고기, 틸라피아, 숲고기, 슬라임잭, 숭어, 자정잉어, 강꼬치고기, 타이거 송어, 블루디스커스, 날개다랑어, 쑥치, 도루묵, 참치, 장어, 오징어, 메기, 철갑상어, 만새기, 자정 오징어
        // ==========================================
        fishByKey.put("가자미", new FishData(Material.COD, "1014", "가자미", FishRarity.RARE,
                22000, 14000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("넙치", new FishData(Material.COD, "1018", "넙치", FishRarity.RARE,
                4000, 18000, 9.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("쏠배감펭", new FishData(Material.COD, "1025", "쏠배감펭", FishRarity.RARE,
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("큰입우럭", new FishData(Material.COD, "1021", "큰입우럭", FishRarity.RARE,
                22000, 13000, 9.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("귀신물고기", new FishData(Material.COD, "1015", "귀신물고기", FishRarity.RARE,
                18000, 18000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));

        fishByKey.put("틸라피아", new FishData(Material.COD, "1050", "틸라피아", FishRarity.RARE,
                0, 8000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("숲고기", new FishData(Material.COD, "1054", "숲고기", FishRarity.RARE,
                18000, 18000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("슬라임잭", new FishData(Material.COD, "1040", "슬라임잭", FishRarity.RARE,
                18000, 18000, 3.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));

        fishByKey.put("숭어", new FishData(Material.COD, "1028", "숭어", FishRarity.RARE,
                0, 13000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("자정잉어", new FishData(Material.COD, "1026", "자정잉어", FishRarity.RARE,
                13000, 22000, 9.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("강꼬치고기", new FishData(Material.COD, "1031", "강꼬치고기", FishRarity.RARE,
                18000, 18000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("타이거 송어", new FishData(Material.COD, "1049", "타이거 송어", FishRarity.RARE,
                0, 13000, 6.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("블루디스커스", new FishData(Material.COD, "1005", "블루디스커스", FishRarity.RARE,
                18000, 18000, 8.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("날개다랑어", new FishData(Material.COD, "1001", "날개다랑어", FishRarity.RARE,
                22000, 6000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("쑥치", new FishData(Material.COD, "1045", "쑥치", FishRarity.RARE,
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));

        fishByKey.put("도루묵", new FishData(Material.COD, "1035", "도루묵", FishRarity.RARE,
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.DESERT));

        fishByKey.put("참치", new FishData(Material.COD, "1051", "참치", FishRarity.RARE,
                18000, 18000, 3.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("장어", new FishData(Material.COD, "1013", "장어", FishRarity.RARE,
                10000, 20000, 5.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("오징어", new FishData(Material.COD, "1043", "오징어", FishRarity.RARE,
                0, 8000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("메기", new FishData(Material.COD, "1009", "메기", FishRarity.RARE,
                18000, 18000, 10.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("철갑상어", new FishData(Material.COD, "1046", "철갑상어", FishRarity.RARE,
                0, 13000, 3.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("만새기", new FishData(Material.COD, "1012", "만새기", FishRarity.RARE,
                0, 8000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("자정 오징어", new FishData(Material.COD, "1027", "자정 오징어", FishRarity.RARE,
                13000, 22000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        // ==========================================
        // 4. EPIC (영웅 - 9종)
        // 슈퍼 해삼, 가오리, 공허의 연어, 아이스핍, 범노래미, 전갈잉어, 문어, 통안어, 망둑어 (+ 용암장어)
        // ==========================================
        fishByKey.put("슈퍼 해삼", new FishData(Material.COD, "1048", "슈퍼 해삼", FishRarity.EPIC,
                0, 8000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("가오리", new FishData(Material.COD, "1044", "가오리", FishRarity.EPIC,
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("공허의 연어", new FishData(Material.COD, "1052", "공허의 연어", FishRarity.EPIC,
                18000, 18000, 6.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));

        fishByKey.put("아이스핍", new FishData(Material.COD, "1020", "아이스핍", FishRarity.EPIC,
                18000, 18000, 6.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.ICE, BiomeChecker.CustomBiomeCategory.CAVE));

        fishByKey.put("범노래미", new FishData(Material.COD, "1024", "범노래미", FishRarity.EPIC,
                18000, 18000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("전갈잉어", new FishData(Material.COD, "1037", "전갈잉어", FishRarity.EPIC,
                18000, 14000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.DESERT));

        fishByKey.put("문어", new FishData(Material.COD, "1029", "문어", FishRarity.EPIC,
                0, 7000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("통안어", new FishData(Material.COD, "1042", "통안어", FishRarity.EPIC,
                13000, 22000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("망둑어", new FishData(Material.COD, "1017", "망둑어", FishRarity.EPIC,
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("용암장어", new FishData(Material.COD, "1022", "용암장어", FishRarity.EPIC,
                18000, 18000, 0.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        // ==========================================
        // 5. LEGEND (전설 - 5종)
        // ==========================================
        fishByKey.put("전설의 물고기", new FishData(Material.COD, "1023", "전설의 물고기", FishRarity.LEGEND,
                18000, 18000, 0.5,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("크림슨피쉬", new FishData(Material.COD, "1011", "크림슨피쉬", FishRarity.LEGEND,
                18000, 18000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));

        fishByKey.put("아귀", new FishData(Material.COD, "1003", "아귀", FishRarity.LEGEND,
                14000, 22000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("빙하고기", new FishData(Material.COD, "1016", "빙하고기", FishRarity.LEGEND,
                18000, 18000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.ICE));

        fishByKey.put("방사능 잉어", new FishData(Material.COD, "1032", "방사능 잉어", FishRarity.LEGEND,
                18000, 18000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
    }
}
