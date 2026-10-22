package Hez.BDEngine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BDEngine 키프레임 애니메이션 데이터 모델 및 고속 변환 파서
 */
public class BDEngineAnimation {

    public interface KeyframeAction {
        void execute(BDEngineModelInstance instance);
    }

    /**
     * Display 엔티티에 직접 Transformation을 0.001ms 단위로 초고속 적용하는 액션
     */
    public static class TransformationAction implements KeyframeAction {
        private final String partTag;
        private final Transformation transformation;
        private final int interpolationDuration;
        private final int interpolationDelay;

        public TransformationAction(String partTag, Transformation transformation, int interpolationDuration, int interpolationDelay) {
            this.partTag = partTag;
            this.transformation = transformation;
            this.interpolationDuration = interpolationDuration;
            this.interpolationDelay = interpolationDelay;
        }

        @Override
        public void execute(BDEngineModelInstance instance) {
            Display display = instance.getPart(partTag);
            if (display != null && display.isValid()) {
                display.setInterpolationDuration(interpolationDuration);
                display.setInterpolationDelay(interpolationDelay);
                display.setTransformation(transformation);
            }
        }

        public String getPartTag() {
            return partTag;
        }

        public Transformation getTransformation() {
            return transformation;
        }
    }

    /**
     * 사운드, 파티클, 또는 텍스처 변경 등 임의의 마인크래프트 명령어를 실행하는 액션
     */
    public static class CommandAction implements KeyframeAction {
        private final String command;

        public CommandAction(String command) {
            this.command = command;
        }

        @Override
        public void execute(BDEngineModelInstance instance) {
            instance.dispatchSubCommand(command);
        }

        public String getCommand() {
            return command;
        }
    }

    private final String name;
    private final int totalTicks;
    private final Map<Integer, List<KeyframeAction>> frames;

    private static final Pattern TRANSFORMATION_PATTERN = Pattern.compile(
            "tag=([A-Za-z0-9_]+).*?transformation:\\[([^\\]]+)\\](?:.*?interpolation_duration:([0-9]+))?(?:.*?start_interpolation:([0-9]+))?"
    );

    public BDEngineAnimation(String name, int totalTicks, Map<Integer, List<KeyframeAction>> frames) {
        this.name = name;
        this.totalTicks = totalTicks;
        this.frames = frames;
    }

    public String getName() {
        return name;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public List<KeyframeAction> getActions(int tick) {
        return frames.getOrDefault(tick, Collections.emptyList());
    }

    /**
     * BDEngine anim_keyframes JSON 객체로부터 애니메이션 데이터를 파싱하여 고속 캐시 모델로 생성합니다.
     */
    public static Map<String, BDEngineAnimation> parseAll(JsonObject animKeyframesRoot) {
        Map<String, BDEngineAnimation> map = new HashMap<>();
        if (animKeyframesRoot == null) return map;

        for (Map.Entry<String, JsonElement> entry : animKeyframesRoot.entrySet()) {
            String animName = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;

            JsonObject ticksObj = entry.getValue().getAsJsonObject();
            Map<Integer, List<KeyframeAction>> frames = new HashMap<>();
            int maxTick = 0;

            for (Map.Entry<String, JsonElement> tickEntry : ticksObj.entrySet()) {
                try {
                    int tick = Integer.parseInt(tickEntry.getKey());
                    if (tick > maxTick) {
                        maxTick = tick;
                    }

                    List<KeyframeAction> actionList = new ArrayList<>();
                    if (tickEntry.getValue().isJsonArray()) {
                        JsonArray cmds = tickEntry.getValue().getAsJsonArray();
                        for (JsonElement cmdElem : cmds) {
                            String cmd = cmdElem.getAsString();
                            KeyframeAction action = parseCommand(cmd);
                            if (action != null) {
                                actionList.add(action);
                            }
                        }
                    }
                    frames.put(tick, actionList);
                } catch (NumberFormatException ignored) {
                }
            }

            int totalDuration = maxTick + 1;
            map.put(animName.toLowerCase(), new BDEngineAnimation(animName, totalDuration, frames));
        }

        return map;
    }

    private static KeyframeAction parseCommand(String cmd) {
        Matcher matcher = TRANSFORMATION_PATTERN.matcher(cmd);
        if (matcher.find()) {
            String partTag = matcher.group(1);
            String rawFloats = matcher.group(2);
            String rawDur = matcher.group(3);
            String rawDelay = matcher.group(4);

            int duration = (rawDur != null) ? Integer.parseInt(rawDur) : 2;
            int delay = (rawDelay != null) ? Integer.parseInt(rawDelay) : 0;

            Transformation trans = parseTransformation(rawFloats);
            if (trans != null) {
                return new TransformationAction(partTag, trans, duration, delay);
            }
        }

        return new CommandAction(cmd);
    }

    /**
     * 16개의 float 행렬 문자열을 JOML을 통해 Bukkit Transformation 객체로 고속 변환합니다.
     */
    private static Transformation parseTransformation(String rawFloats) {
        try {
            String[] parts = rawFloats.split(",");
            if (parts.length != 16) return null;

            float[] f = new float[16];
            for (int i = 0; i < 16; i++) {
                String s = parts[i].trim().replaceAll("[fF]", "");
                f[i] = Float.parseFloat(s);
            }

            // Minecraft Row-major 행렬 설정
            Matrix4f mat = new Matrix4f(
                    f[0], f[1], f[2], f[3],
                    f[4], f[5], f[6], f[7],
                    f[8], f[9], f[10], f[11],
                    f[12], f[13], f[14], f[15]
            );

            Vector3f translation = mat.getTranslation(new Vector3f());
            Vector3f scale = mat.getScale(new Vector3f());
            Quaternionf leftRotation = mat.getUnnormalizedRotation(new Quaternionf());
            Quaternionf rightRotation = new Quaternionf();

            return new Transformation(translation, leftRotation, scale, rightRotation);
        } catch (Exception e) {
            return null;
        }
    }
}
