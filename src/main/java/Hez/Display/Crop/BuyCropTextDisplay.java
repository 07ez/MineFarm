package Hez.Display.Crop;

import Hez.Display.DisplayTextInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BuyCropTextDisplay {
    private final Map<String, DisplayTextInfo> texts = new HashMap<>();

    public DisplayTextInfo getTexts(Set<String> tags) {
        for (String tag : tags) {
            DisplayTextInfo dt = texts.get(tag);
            if (dt != null) return dt;
        }
        return null;
    }

    public void Init() {
        // [1] 씨앗류
        texts.put("beetrootSeeds", new DisplayTextInfo("100원", "100원", 0));
        texts.put("wheatSeeds", new DisplayTextInfo("100원", "100원", 0));
        texts.put("pitcherPod", new DisplayTextInfo("100원", "100원", 0));
        texts.put("torchflowerSeeds", new DisplayTextInfo("100원", "100원", 0));
        texts.put("melonSeeds", new DisplayTextInfo("100원", "100원", 0));
        texts.put("pumpkinSeeds", new DisplayTextInfo("100원", "100원", 0));

        // [2] 기본 수확물 및 열매
        texts.put("carrot", new DisplayTextInfo("100원", "100원", 0));
        texts.put("potato", new DisplayTextInfo("100원", "100원", 0));
        texts.put("glowBerries", new DisplayTextInfo("100원", "100원", 0));
        texts.put("sweetBerries", new DisplayTextInfo("100원", "100원", 0));

        // [3] 네더/기타 수확물
        texts.put("netherWart", new DisplayTextInfo("100원", "100원", 0));
        texts.put("cocoaBeans", new DisplayTextInfo("100원", "100원", 0));
        texts.put("sugarCane", new DisplayTextInfo("100원", "100원", 0));
        texts.put("cactus", new DisplayTextInfo("100원", "100원", 0));
        texts.put("kelp", new DisplayTextInfo("100원", "100원", 0));

        // [4] 가공품 및 특수 작물
        texts.put("honeycomb", new DisplayTextInfo("100원", "100원", 0));
        texts.put("honeyBottle", new DisplayTextInfo("100원", "100원", 0));
        texts.put("honeyBlock", new DisplayTextInfo("100원", "100원", 0));
        texts.put("honeycombBlock", new DisplayTextInfo("100원", "100원", 0));
    }
}
