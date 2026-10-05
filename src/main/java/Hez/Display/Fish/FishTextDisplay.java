package Hez.Display.Fish;

import Hez.Display.DisplayTextInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FishTextDisplay {

    private final Map<String, DisplayTextInfo> texts = new HashMap<>();

    public DisplayTextInfo getTexts(Set<String> tags) {
        for (String tag : tags) {
            DisplayTextInfo dt = texts.get(tag);
            if (dt != null) return dt;
        }
        return null;
    }
    public void Init() {
        texts.put("대구", new DisplayTextInfo(100, 100, 0));
        texts.put("연어", new DisplayTextInfo(100, 100, 0));
        texts.put("열대어", new DisplayTextInfo(100, 100, 0));
        texts.put("복어", new DisplayTextInfo(100, 100, 0));
        texts.put("날개다랑어", new DisplayTextInfo(100, 100, 0));
        texts.put("멸치", new DisplayTextInfo(100, 100, 0));
        texts.put("아귀", new DisplayTextInfo(100, 100, 0));
        texts.put("블롭피쉬", new DisplayTextInfo(100, 100, 0));
        texts.put("블루디스커스", new DisplayTextInfo(100, 100, 0));
        texts.put("도미", new DisplayTextInfo(100, 100, 0));
        texts.put("눈동자개", new DisplayTextInfo(100, 100, 0));
        texts.put("잉어", new DisplayTextInfo(100, 100, 0));
        texts.put("메기", new DisplayTextInfo(100, 100, 0));
        texts.put("피라미", new DisplayTextInfo(100, 100, 0));
        texts.put("크림스피쉬", new DisplayTextInfo(100, 100, 0));
        texts.put("만새기", new DisplayTextInfo(100, 100, 0));
        texts.put("장어", new DisplayTextInfo(100, 100, 0));
        texts.put("가자미", new DisplayTextInfo(100, 100, 0));
        texts.put("귀신물고기", new DisplayTextInfo(100, 100, 0));
        texts.put("빙하고기", new DisplayTextInfo(100, 100, 0));
        texts.put("망둥어", new DisplayTextInfo(100, 100, 0));
        texts.put("넙치", new DisplayTextInfo(100, 100, 0));
        texts.put("청어", new DisplayTextInfo(100, 100, 0));
        texts.put("아이스핍", new DisplayTextInfo(100, 100, 0));
        texts.put("큰입우럭", new DisplayTextInfo(100, 100, 0));
        texts.put("용암장어", new DisplayTextInfo(100, 100, 0));
        texts.put("전설의물고기", new DisplayTextInfo(100, 100, 0));
        texts.put("범노래미", new DisplayTextInfo(100, 100, 0));
        texts.put("쏠배감펭", new DisplayTextInfo(100, 100, 0));
        texts.put("자정잉어", new DisplayTextInfo(100, 100, 0));
        texts.put("자정오징어", new DisplayTextInfo(100, 100, 0));
        texts.put("숭어", new DisplayTextInfo(100, 100, 0));
        texts.put("문어", new DisplayTextInfo(100, 100, 0));
        texts.put("농어", new DisplayTextInfo(100, 100, 0));
        texts.put("강꼬치고기", new DisplayTextInfo(100, 100, 0));
        texts.put("방사능잉어", new DisplayTextInfo(100, 100, 0));
        texts.put("무지개송어", new DisplayTextInfo(100, 100, 0));
        texts.put("붉은툼돔", new DisplayTextInfo(100, 100, 0));
        texts.put("도루묵", new DisplayTextInfo(100, 100, 0));
        texts.put("정어리", new DisplayTextInfo(100, 100, 0));
        texts.put("전갈잉어", new DisplayTextInfo(100, 100, 0));
        texts.put("해삼", new DisplayTextInfo(100, 100, 0));
        texts.put("전어", new DisplayTextInfo(100, 100, 0));
        texts.put("슬라임잭", new DisplayTextInfo(100, 100, 0));
        texts.put("작은입우럭", new DisplayTextInfo(100, 100, 0));
        texts.put("통안어", new DisplayTextInfo(100, 100, 0));
        texts.put("오징어", new DisplayTextInfo(100, 100, 0));
        texts.put("가오리", new DisplayTextInfo(100, 100, 0));
        texts.put("쑥치", new DisplayTextInfo(100, 100, 0));
        texts.put("철갑상어", new DisplayTextInfo(100, 100, 0));
        texts.put("선피쉬", new DisplayTextInfo(100, 100, 0));
        texts.put("슈퍼해삼", new DisplayTextInfo(100, 100, 0));
        texts.put("타이거송어", new DisplayTextInfo(100, 100, 0));
        texts.put("틸라피아", new DisplayTextInfo(100, 100, 0));
        texts.put("참치", new DisplayTextInfo(100, 100, 0));
        texts.put("공허의연어", new DisplayTextInfo(100, 100, 0));
        texts.put("월아이", new DisplayTextInfo(100, 100, 0));
        texts.put("숲고기", new DisplayTextInfo(100, 100, 0));
    }
}
