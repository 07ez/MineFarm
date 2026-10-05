package Hez.Display.Crop;

import Hez.Display.DisplayTextInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SellCropTextDisplay {
    private final Map<String, DisplayTextInfo> texts = new HashMap<>();

    public DisplayTextInfo getTexts(Set<String> tags) {
        for (String tag : tags) {
            DisplayTextInfo dt = texts.get(tag);
            if (dt != null) return dt;
        }
        return null;
    }

    public void Init() {
        // [1] 황금 당근 & 독성 감자
        texts.put("goldenCarrot", new DisplayTextInfo(100, 100, 0));
        texts.put("poisonousPotato", new DisplayTextInfo(100, 100, 0));

        // [2] 기본 작물 및 열매
        texts.put("beetroot", new DisplayTextInfo(100, 100, 0));
        texts.put("glowBerries", new DisplayTextInfo(100, 100, 0));
        texts.put("sweetBerries", new DisplayTextInfo(100, 100, 0));
        texts.put("wheat", new DisplayTextInfo(100, 100, 0));

        // [3] 네더/특수 작물
        texts.put("netherWart", new DisplayTextInfo(100, 100, 0));
        texts.put("cocoaBeans", new DisplayTextInfo(100, 100, 0));
        texts.put("pitcherPlant", new DisplayTextInfo(100, 100, 0));
        texts.put("torchflower", new DisplayTextInfo(100, 100, 0));

        // [4] 꿀 관련
        texts.put("honeycombBlock", new DisplayTextInfo(100, 100, 0));
        texts.put("honeycomb", new DisplayTextInfo(100, 100, 0));
        texts.put("honeyBlock", new DisplayTextInfo(100, 100, 0));
        texts.put("honeyBottle", new DisplayTextInfo(100, 100, 0));

        // [5] 기타 작물 및 과일
        texts.put("kelp", new DisplayTextInfo(100, 100, 0));
        texts.put("sugarCane", new DisplayTextInfo(100, 100, 0));
        texts.put("cactus", new DisplayTextInfo(100, 100, 0));
        texts.put("pumpkin", new DisplayTextInfo(100, 100, 0));
        texts.put("melonSlice", new DisplayTextInfo(100, 100, 0));
    }
}
