package Hez.Display.Tree;

import Hez.Display.DisplayTextInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TreeTextDisplay {
    private final Map<String, DisplayTextInfo> texts = new HashMap<>();

    public DisplayTextInfo getTexts(Set<String> tags) {
        for (String tag : tags) {
            DisplayTextInfo dt = texts.get(tag);
            if (dt != null) return dt;
        }
        return null;
    }

    public void Init() {
        // [1] 아젤리아
        texts.put("azalea", new DisplayTextInfo(100, 100, 90));
        // [2] 맹그로브
        texts.put("mangrove", new DisplayTextInfo(100, 100, 90));
        // [3] 페일 오크
        texts.put("paleOak", new DisplayTextInfo(100, 100, 90));
        // [4] 벚나무
        texts.put("cherry", new DisplayTextInfo(100, 100, 135));
        // [5] 아카시아
        texts.put("acacia", new DisplayTextInfo(100, 100, 180));
        // [6] 가문비나무
        texts.put("spruce", new DisplayTextInfo(100, 100, 180));
        // [7] 참나무
        texts.put("oak", new DisplayTextInfo(100, 100, 180));
        // [8] 자작나무
        texts.put("birch", new DisplayTextInfo(1000, 100, 180));
        // [9] 짙은 참나무
        texts.put("darkOak", new DisplayTextInfo(100, 100, 180));
        // [10] 정글나무
        texts.put("jungle", new DisplayTextInfo(100, 100, 225));
        // [11] 포플러나무
        texts.put("poplar", new DisplayTextInfo(100, 100, 270));
        // [12] 대나무
        texts.put("bamboo", new DisplayTextInfo(100, 100, 270));
        // [13] 꽃핀 아젤리아
        texts.put("floweringAzalea", new DisplayTextInfo(100, 100, 270));
    }
}
