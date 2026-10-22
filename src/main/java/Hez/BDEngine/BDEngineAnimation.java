package Hez.BDEngine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
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

        public int getInterpolationDuration() {
            return interpolationDuration;
        }
    }

    /**
     * 사운드, 파티클 등 임의의 마인크래프트 명령어를 실행하는 액션
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
    private final int stepTicks;
    private final int durationTicks;
    private final List<KeyframeAction> initialSetupActions;
    private final Map<Integer, List<KeyframeAction>> tickActions;

    private static final Pattern TRANSFORMATION_PATTERN = Pattern.compile(
            "tag=([A-Za-z0-9_]+).*?transformation:\\[([^\\]]+)\\](?:.*?interpolation_duration:([0-9]+))?(?:.*?start_interpolation:([0-9]+))?"
    );
    private static final Pattern TAG_PATTERN = Pattern.compile("tag=([A-Za-z0-9_]+)");

    public BDEngineAnimation(String name, int stepTicks, int durationTicks, Map<Integer, List<KeyframeAction>> tickActions) {
        this(name, stepTicks, durationTicks, Collections.emptyList(), tickActions);
    }

    public BDEngineAnimation(String name, int stepTicks, int durationTicks, List<KeyframeAction> initialSetupActions, Map<Integer, List<KeyframeAction>> tickActions) {
        this.name = name;
        this.stepTicks = Math.max(1, stepTicks);
        this.durationTicks = Math.max(1, durationTicks);
        this.initialSetupActions = (initialSetupActions != null) ? Collections.unmodifiableList(new ArrayList<>(initialSetupActions)) : Collections.emptyList();
        this.tickActions = (tickActions != null) ? tickActions : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public int getStepTicks() {
        return stepTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getTotalTicks() {
        return durationTicks;
    }

    public int getTotalFrames() {
        return tickActions.size();
    }

    public List<KeyframeAction> getInitialSetupActions() {
        return initialSetupActions;
    }

    public List<KeyframeAction> getActions(int tick) {
        return tickActions.getOrDefault(tick, Collections.emptyList());
    }

    /**
     * 해당 틱에 실행할 액션 목록을 반환합니다.
     * 애니메이션의 첫 번째 틱(tick 0)인 경우 초기 설정 액션(initialSetupActions)도 함께 포함합니다.
     */
    public List<KeyframeAction> getActionsAtTick(int tick, boolean isFirstTickOfAnimation) {
        List<KeyframeAction> list = tickActions.get(tick);
        if (isFirstTickOfAnimation && tick == 0 && !initialSetupActions.isEmpty()) {
            if (list == null || list.isEmpty()) {
                return initialSetupActions;
            }
            List<KeyframeAction> combined = new ArrayList<>(initialSetupActions.size() + list.size());
            combined.addAll(initialSetupActions);
            combined.addAll(list);
            return combined;
        }
        return list != null ? list : Collections.emptyList();
    }

    public static Map<String, BDEngineAnimation> parseAll(JsonObject animKeyframesRoot) {
        return parseAll(animKeyframesRoot, null);
    }

    /**
     * BDEngine anim_keyframes 및 meta JSON 객체로부터 애니메이션 데이터를 파싱하여 고속 캐시 모델로 생성합니다.
     */
    public static Map<String, BDEngineAnimation> parseAll(JsonObject animKeyframesRoot, JsonObject metaRoot) {
        Map<String, BDEngineAnimation> map = new HashMap<>();
        if (animKeyframesRoot == null) return map;

        JsonObject animsMeta = (metaRoot != null && metaRoot.has("animations") && metaRoot.get("animations").isJsonObject())
                ? metaRoot.getAsJsonObject("animations") : null;

        for (Map.Entry<String, JsonElement> entry : animKeyframesRoot.entrySet()) {
            String animName = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;

            JsonObject ticksObj = entry.getValue().getAsJsonObject();
            Map<Integer, List<KeyframeAction>> rawFrames = new HashMap<>();
            int maxRawKey = 0;
            int detectedInterpolation = 0;

            for (Map.Entry<String, JsonElement> tickEntry : ticksObj.entrySet()) {
                try {
                    int tick = Integer.parseInt(tickEntry.getKey());
                    if (tick > maxRawKey) {
                        maxRawKey = tick;
                    }

                    List<KeyframeAction> actionList = new ArrayList<>();
                    if (tickEntry.getValue().isJsonArray()) {
                        JsonArray cmds = tickEntry.getValue().getAsJsonArray();
                        for (JsonElement cmdElem : cmds) {
                            String cmd = cmdElem.getAsString();
                            KeyframeAction action = parseCommand(cmd);
                            if (action != null) {
                                actionList.add(action);
                                if (action instanceof TransformationAction ta && detectedInterpolation == 0 && ta.getInterpolationDuration() > 0) {
                                    detectedInterpolation = ta.getInterpolationDuration();
                                }
                            }
                        }
                    }
                    rawFrames.put(tick, actionList);
                } catch (NumberFormatException ignored) {
                }
            }

            // 기본 스텝 틱 및 재생 시간 계산 (meta 우선)
            int stepTicks = (detectedInterpolation > 0) ? detectedInterpolation : 1;
            int durationTicks = (maxRawKey + 1) * stepTicks;

            if (animsMeta != null) {
                JsonObject singleMeta = null;
                for (Map.Entry<String, JsonElement> mEntry : animsMeta.entrySet()) {
                    if (mEntry.getKey().equalsIgnoreCase(animName)) {
                        singleMeta = mEntry.getValue().getAsJsonObject();
                        break;
                    }
                }
                if (singleMeta != null) {
                    if (singleMeta.has("stepTicks")) {
                        stepTicks = singleMeta.get("stepTicks").getAsInt();
                    }
                    if (singleMeta.has("durationTicks")) {
                        durationTicks = singleMeta.get("durationTicks").getAsInt();
                    }
                }
            }

            List<KeyframeAction> initialSetupActions = new ArrayList<>();
            Map<Integer, List<KeyframeAction>> tickActions = new HashMap<>();

            // 원시 키가 스텝 인덱스(0, 1, 2, ... N)인지 판별 (stepTicks > 1 이고 maxRawKey <= durationTicks / stepTicks + 1)
            if (stepTicks > 1 && maxRawKey <= (durationTicks / stepTicks) + 1) {
                // Key 0이 interpolation_duration == 0만 가지는 초기 설정 프레임인지 확인
                boolean key0IsSetup = hasOnlyZeroInterpolation(rawFrames.get(0));
                if (key0IsSetup && rawFrames.containsKey(1)) {
                    if (rawFrames.get(0) != null) {
                        initialSetupActions.addAll(rawFrames.get(0));
                    }
                    // Key 1부터 시작하여 틱 매핑 (tick = (k - 1) * stepTicks)
                    for (int k = 1; k <= maxRawKey; k++) {
                        List<KeyframeAction> actions = rawFrames.get(k);
                        if (actions != null && !actions.isEmpty()) {
                            int targetTick = (k - 1) * stepTicks;
                            if (targetTick < durationTicks) {
                                tickActions.computeIfAbsent(targetTick, t -> new ArrayList<>()).addAll(actions);
                            }
                        }
                    }
                } else {
                    // Key 0부터 스텝 프레임으로 매핑 (tick = k * stepTicks)
                    for (int k = 0; k <= maxRawKey; k++) {
                        List<KeyframeAction> actions = rawFrames.get(k);
                        if (actions != null && !actions.isEmpty()) {
                            int targetTick = k * stepTicks;
                            if (targetTick < durationTicks) {
                                tickActions.computeIfAbsent(targetTick, t -> new ArrayList<>()).addAll(actions);
                            }
                        }
                    }
                }
            } else {
                // 이미 실제 틱 단위로 저장되어 있는 경우
                for (Map.Entry<Integer, List<KeyframeAction>> frameEntry : rawFrames.entrySet()) {
                    int rawTick = frameEntry.getKey();
                    if (rawTick < durationTicks) {
                        tickActions.computeIfAbsent(rawTick, t -> new ArrayList<>()).addAll(frameEntry.getValue());
                    }
                }
            }

            map.put(animName.toLowerCase(), new BDEngineAnimation(animName, stepTicks, durationTicks, initialSetupActions, tickActions));
        }

        return map;
    }

    private static boolean hasOnlyZeroInterpolation(List<KeyframeAction> actions) {
        if (actions == null || actions.isEmpty()) return false;
        boolean hasTransformation = false;
        for (KeyframeAction a : actions) {
            if (a instanceof TransformationAction ta) {
                hasTransformation = true;
                if (ta.getInterpolationDuration() > 0) {
                    return false;
                }
            }
        }
        return hasTransformation;
    }

    private static KeyframeAction parseCommand(String cmd) {
        // 1. transformation 행렬 변경 명령어 파싱 -> Java 직접 주입
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

        // 2. item_display: "none" 같은 디스플레이 모드 설정은 Java API로 직접 처리 (명령어 피드백 채팅 스팸 방지)
        if (cmd.contains("item_display:\"none\"") || cmd.contains("item_display:none")) {
            Matcher tagMatcher = TAG_PATTERN.matcher(cmd);
            if (tagMatcher.find()) {
                String partTag = tagMatcher.group(1);
                return instance -> {
                    Display d = instance.getPart(partTag);
                    if (d instanceof ItemDisplay id) {
                        id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                    }
                };
            }
        }

        // 3. data modify / data merge 명령어는 소환 시 이미 Passenger NBT로 적용되어 있으므로
        // 루프 애니메이션에서는 명령어로 재실행하지 않고 스킵 (채팅창의 "문자 표시: 개체데이터 변경" 스팸 100% 차단)
        if (cmd.startsWith("data modify") || cmd.startsWith("data merge")) {
            return null;
        }

        // 4. 사운드, 파티클 등 실제 인게임 효과 명령어는 실행
        return new CommandAction(cmd);
    }

    /**
     * Minecraft NBT의 16개 Row-major float 행렬을
     * JOML의 Column-major Matrix4f(m<col><row>)로 올바르게 전치(Transpose)하여
     * 왜곡 없이 완벽한 Bukkit Transformation 객체로 변환합니다.
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

            Matrix4f mat = new Matrix4f(
                    f[0], f[4], f[8], f[12],  // 열 0
                    f[1], f[5], f[9], f[13],  // 열 1
                    f[2], f[6], f[10], f[14], // 열 2
                    f[3], f[7], f[11], f[15]  // 열 3 (이동 x, y, z)
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
