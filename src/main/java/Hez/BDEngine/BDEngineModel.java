package Hez.BDEngine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BDEngine에서 다운로드받은 모델 데이터 및 태그 메타데이터 모델
 */
public class BDEngineModel {

    private final String name;
    private final String tag1;
    private final String tag2;
    private final long savedAt;
    private final String rawJson;
    private JsonObject parsedContent;
    private Map<String, BDEngineAnimation> animations = new HashMap<>();

    public BDEngineModel(String name, long savedAt, String rawJson) {
        this(name, name, name, savedAt, rawJson);
    }

    public BDEngineModel(String name, String tag1, String tag2, long savedAt, String rawJson) {
        this.name = name;
        this.tag1 = tag1;
        this.tag2 = tag2;
        this.savedAt = savedAt;
        this.rawJson = rawJson;
        try {
            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
            if (root.has("content")) {
                this.parsedContent = root.getAsJsonObject("content");
            } else {
                this.parsedContent = root;
            }
        } catch (Exception e) {
            this.parsedContent = new JsonObject();
        }

        // 애니메이션 파싱 및 캐싱
        loadAnimations();
    }

    private void loadAnimations() {
        if (parsedContent != null && parsedContent.has("datapack")) {
            JsonObject datapack = parsedContent.getAsJsonObject("datapack");
            if (datapack.has("anim_keyframes") && datapack.get("anim_keyframes").isJsonObject()) {
                JsonObject meta = parsedContent.has("meta") ? parsedContent.getAsJsonObject("meta") : null;
                this.animations = BDEngineAnimation.parseAll(datapack.getAsJsonObject("anim_keyframes"), meta);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getTag1() {
        return tag1;
    }

    public String getTag2() {
        return tag2;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public String getRawJson() {
        return rawJson;
    }

    public JsonObject getParsedContent() {
        return parsedContent;
    }

    public boolean hasAnimations() {
        return !animations.isEmpty();
    }

    public Map<String, BDEngineAnimation> getAnimations() {
        return Collections.unmodifiableMap(animations);
    }

    public BDEngineAnimation getAnimation(String animName) {
        if (animName == null) return getDefaultAnimation();
        return animations.get(animName.toLowerCase().trim());
    }

    public BDEngineAnimation getDefaultAnimation() {
        if (animations.isEmpty()) return null;
        if (animations.containsKey("default")) {
            return animations.get("default");
        }
        return animations.values().iterator().next();
    }

    /**
     * 메타데이터를 포함한 저장용 JSON 문자열로 변환합니다.
     */
    public String toStorageJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("tag1", tag1);
        obj.addProperty("tag2", tag2);
        obj.addProperty("savedAt", savedAt);
        obj.add("content", parsedContent);
        return obj.toString();
    }

    /**
     * 저장된 파일의 JSON으로부터 BDEngineModel 객체를 역직렬화합니다.
     */
    public static BDEngineModel fromStorageJson(String jsonString) {
        try {
            JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
            String name = obj.has("name") ? obj.get("name").getAsString() : "unknown";
            String tag1 = obj.has("tag1") ? obj.get("tag1").getAsString() : "default";
            String tag2 = obj.has("tag2") ? obj.get("tag2").getAsString() : name;
            long savedAt = obj.has("savedAt") ? obj.get("savedAt").getAsLong() : System.currentTimeMillis();
            return new BDEngineModel(name, tag1, tag2, savedAt, jsonString);
        } catch (Exception e) {
            return null;
        }
    }
}
