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

        fishByKey.put("대구", new FishData(Material.COD, null, null,
                0, 13000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN, BiomeChecker.CustomBiomeCategory.LAKE));

        fishByKey.put("연어", new FishData(Material.SALMON, null, null,
                0, 13000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));

        fishByKey.put("열대어", new FishData(Material.TROPICAL_FISH, null, null,
                0, 13000, 5.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        fishByKey.put("복어", new FishData(Material.PUFFERFISH, null, null,
                6000, 10000, 7.0,
                FishData.WeatherCondition.CLEAR, BiomeChecker.CustomBiomeCategory.OCEAN));

        // 1001 ~ 1027
        fishByKey.put("날개다랑어", new FishData(Material.COD, "1001", "날개다랑어",
                22000, 6000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("멸치", new FishData(Material.COD, "1002", "멸치",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("아귀", new FishData(Material.COD, "1003", "아귀",
                14000, 22000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("블롭피쉬", new FishData(Material.COD, "1004", "블롭피쉬",
                14000, 22000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("블루디스커스", new FishData(Material.COD, "1005", "블루디스커스",
                18000, 18000, 8.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE, BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("도미", new FishData(Material.COD, "1006", "도미",
                12000, 0, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("눈동자개", new FishData(Material.COD, "1007", "눈동자개",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("잉어", new FishData(Material.COD, "1008", "잉어",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("메기", new FishData(Material.COD, "1009", "메기",
                18000, 18000, 10.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("피라미", new FishData(Material.COD, "1010", "피라미",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("크림슨피쉬", new FishData(Material.COD, "1011", "크림슨피쉬",
                18000, 18000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("만새기", new FishData(Material.COD, "1012", "만새기",
                0, 8000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("장어", new FishData(Material.COD, "1013", "장어",
                10000, 20000, 5.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("가자미", new FishData(Material.COD, "1014", "가자미",
                22000, 14000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("귀신물고기", new FishData(Material.COD, "1015", "귀신물고기",
                18000, 18000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("빙하고기", new FishData(Material.COD, "1016", "빙하고기",
                18000, 18000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.ICE));
        fishByKey.put("망둑어", new FishData(Material.COD, "1017", "망둑어",
                18000, 18000, 4.0,
               FishData.WeatherCondition.ANY,  BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("넙치", new FishData(Material.COD, "1018", "넙치",
                4000, 18000, 9.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("청어", new FishData(Material.COD, "1019", "청어",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("아이스핍", new FishData(Material.COD, "1020", "아이스핍",
                18000, 18000, 6.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.ICE, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("큰입우럭", new FishData(Material.COD, "1021", "큰입우럭",
                22000, 13000, 9.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("용암장어", new FishData(Material.COD, "1022", "용암장어",
                18000, 18000, 0.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("전설의 물고기", new FishData(Material.COD, "1023", "전설의 물고기",
                18000, 18000, 0.5,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("범노래미", new FishData(Material.COD, "1024", "범노래미",
                18000, 18000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("쏠배감펭", new FishData(Material.COD, "1025", "쏠배감펭",
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("자정잉어", new FishData(Material.COD, "1026", "자정잉어",
                13000, 22000, 9.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("자정 오징어", new FishData(Material.COD, "1027", "자정 오징어",
                13000, 22000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));

        // 1028 ~ 1054
        fishByKey.put("숭어", new FishData(Material.COD, "1028", "숭어",
                0, 13000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("문어", new FishData(Material.COD, "1029", "문어",
                0, 7000, 8.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("농어", new FishData(Material.COD, "1030", "농어",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("강꼬치고기", new FishData(Material.COD, "1031", "강꼬치고기",
                18000, 18000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("방사능 잉어", new FishData(Material.COD, "1032", "방사능 잉어",
                18000, 18000, 0.5,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("무지개송어", new FishData(Material.COD, "1033", "무지개송어",
                0, 13000, 10.0,
                FishData.WeatherCondition.CLEAR, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("붉은퉁돔", new FishData(Material.COD, "1034", "붉은퉁돔",
                0, 13000, 10.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("도루묵", new FishData(Material.COD, "1035", "도루묵",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.DESERT));
        fishByKey.put("정어리", new FishData(Material.COD, "1036", "정어리",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("전갈잉어", new FishData(Material.COD, "1037", "전갈잉어",
                18000, 14000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.DESERT));
        fishByKey.put("해삼", new FishData(Material.COD, "1038", "해삼",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("전어", new FishData(Material.COD, "1039", "전어",
                3000, 8000, 10.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("슬라임잭", new FishData(Material.COD, "1040", "슬라임잭",
                18000, 18000, 3.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("작은입우럭", new FishData(Material.COD, "1041", "작은입우럭",
                18000, 18000, 10.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("통안어", new FishData(Material.COD, "1042", "통안어",
                13000, 22000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("오징어", new FishData(Material.COD, "1043", "오징어",
                0, 8000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("가오리", new FishData(Material.COD, "1044", "가오리",
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("쑥치", new FishData(Material.COD, "1045", "쑥치",
                18000, 18000, 4.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("철갑상어", new FishData(Material.COD, "1046", "철갑상어",
                0, 13000, 3.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("선피쉬", new FishData(Material.COD, "1047", "선피쉬",
                0, 13000, 10.0,
                FishData.WeatherCondition.CLEAR, BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("슈퍼 해삼", new FishData(Material.COD, "1048", "슈퍼 해삼",
                0, 8000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("타이거 송어", new FishData(Material.COD, "1049", "타이거 송어",
                0, 13000, 6.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.RIVER));
        fishByKey.put("틸라피아", new FishData(Material.COD, "1050", "틸라피아",
                0, 8000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("참치", new FishData(Material.COD, "1051", "참치",
                18000, 18000, 3.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.OCEAN));
        fishByKey.put("공허의 연어", new FishData(Material.COD, "1052", "공허의 연어",
                18000, 18000, 6.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.CAVE));
        fishByKey.put("월아이", new FishData(Material.COD, "1053", "월아이",
                6000, 22000, 9.0,
                FishData.WeatherCondition.RAIN, BiomeChecker.CustomBiomeCategory.RIVER, BiomeChecker.CustomBiomeCategory.LAKE));
        fishByKey.put("숲고기", new FishData(Material.COD, "1054", "숲고기",
                18000, 18000, 7.0,
                FishData.WeatherCondition.ANY, BiomeChecker.CustomBiomeCategory.LAKE));
    }
}

