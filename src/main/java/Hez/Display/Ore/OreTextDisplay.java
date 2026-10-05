package Hez.Display.Ore;

import Hez.Display.DisplayTextInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OreTextDisplay {
    private final Map<String, DisplayTextInfo> texts = new HashMap<>();

    public DisplayTextInfo getTexts(Set<String> tags) {
        for (String tag : tags) {
            DisplayTextInfo dt = texts.get(tag);
            if (dt != null) return dt;
        }
        return null;
    }
    public void Init() {
        texts.put("netheriteIngot", new DisplayTextInfo(100, 100, 0));
        texts.put("goldIngot", new DisplayTextInfo(100, 100, 0));
        texts.put("copperIngot", new DisplayTextInfo(100, 100, 0));
        texts.put("ironIngot", new DisplayTextInfo(100, 100, 0));
        texts.put("netheriteScrap", new DisplayTextInfo(100, 100, 0));
        texts.put("rawGold", new DisplayTextInfo(100, 100, 0));
        texts.put("rawCopper", new DisplayTextInfo(100, 100, 0));
        texts.put("rawIron", new DisplayTextInfo(100, 100, 0));
        texts.put("emerald", new DisplayTextInfo(100, 100, 0));
        texts.put("diamond", new DisplayTextInfo(100, 100, 0));
        texts.put("coal", new DisplayTextInfo(100, 100, 0));
        texts.put("lapis", new DisplayTextInfo(100, 100, 0));
        texts.put("redstone", new DisplayTextInfo(100, 100, 0));
        texts.put("echoShard", new DisplayTextInfo(100, 100, 0));
        texts.put("quartz", new DisplayTextInfo(100, 100, 0));
        texts.put("amethystShard", new DisplayTextInfo(100, 100, 0));
    }
}
