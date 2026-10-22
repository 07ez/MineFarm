package Hez.BDEngine;

import Hez.MineFarm.Main;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

import java.util.*;

/**
 * BDEngine 모델 엔티티 소환, 애니메이션 자동 활성화 및 태그 안전 제거 헬퍼
 */
public class BDEngineSpawner {

    /**
     * 지정한 위치에 BDEngine 모델을 소환합니다.
     * 최상위 외곽 디스플레이(Root)와 모든 하위 승마(Passenger) 엔티티에 태그가 부착되며,
     * 모델에 애니메이션이 존재할 경우 스폰 즉시 자동으로 애니메이션 재생을 시작합니다.
     *
     * @param loc 소환할 위치
     * @param model 소환할 BDEngine 모델
     * @param plugin 메인 플러그인 인스턴스
     * @return 소환 성공 여부
     */
    public static boolean spawn(Location loc, BDEngineModel model, Main plugin) {
        if (loc == null || loc.getWorld() == null || model == null) {
            return false;
        }

        JsonObject content = model.getParsedContent();
        if (content == null) {
            return false;
        }

        // 1. passengers 배열 추출
        JsonArray passengers = null;
        if (content.has("passengers") && content.get("passengers").isJsonArray()) {
            passengers = content.getAsJsonArray("passengers");
        }

        if (passengers == null || passengers.isEmpty()) {
            return false;
        }

        String tag1 = model.getTag1();
        String tag2 = model.getTag2();

        StringBuilder passengerSnbt = new StringBuilder();
        for (int i = 0; i < passengers.size(); i++) {
            JsonElement el = passengers.get(i);
            String snbt = el.isJsonPrimitive() ? el.getAsString() : el.toString();

            // [BDESERVERTAG] 치환
            snbt = snbt.replace("[BDESERVERTAG]", tag1);

            // 각 하위 엔티티의 Tags:[...] 에 bde_model, tag1, tag2 직접 주입
            snbt = snbt.replaceAll("(?i)Tags:\\[", "Tags:[\"bde_model\",\"" + tag1 + "\",\"" + tag2 + "\",");

            if (i > 0) {
                passengerSnbt.append(",");
            }
            passengerSnbt.append(snbt);
        }

        // 2. execute positioned 명령어를 통해 바닐라 NBT 엔진으로 최상위 외곽 디스플레이 및 하위 승마 계층 구조 소환
        String summonCommand = String.format(Locale.US,
                "execute positioned %.4f %.4f %.4f in %s run summon text_display ~ ~ ~ {Tags:[\"bde_model\",\"%s\",\"%s\",\"%s_root\"],Passengers:[%s]}",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld().getKey().toString(),
                tag1, tag2, tag1,
                passengerSnbt.toString()
        );

        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), summonCommand);

        // 3. 소환된 루트 엔티티 탐색
        Entity root = null;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
            if (entity.getScoreboardTags().contains(tag1 + "_root")) {
                root = entity;
                break;
            }
        }

        if (root == null) {
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
                if (entity.getScoreboardTags().contains("bde_model") && entity.getVehicle() == null) {
                    root = entity;
                    break;
                }
            }
        }

        if (root != null) {
            tagEntityRecursively(root, tag1, tag2);

            // 4. 모델에 애니메이션이 있으면 즉시 자동 활성화 (파츠 디스플레이 매핑 후 스케줄러 등록)
            if (model.hasAnimations() && plugin != null && plugin.getBdEngineManager() != null) {
                Map<String, Display> partsMap = new HashMap<>();
                mapDisplayParts(root, tag1, partsMap);

                BDEngineAnimation anim = model.getDefaultAnimation();
                if (anim != null) {
                    BDEngineModelInstance instance = new BDEngineModelInstance(root, model, partsMap, anim);
                    plugin.getBdEngineManager().getAnimationManager().registerInstance(instance);
                    plugin.getLogger().info("[BDEngine] 모델 '" + model.getName() + "' 애니메이션 '" + anim.getName() + "' 자동 재생 시작 (파츠: " + partsMap.size() + "개, 총 " + anim.getTotalTicks() + "틱)");
                }
            }
        }

        return success;
    }

    private static void mapDisplayParts(Entity entity, String tag1, Map<String, Display> partsMap) {
        if (entity == null) return;
        if (entity instanceof Display display) {
            for (String tag : display.getScoreboardTags()) {
                if (tag.startsWith(tag1 + "_") && !tag.endsWith("_root")) {
                    partsMap.put(tag, display);
                }
            }
        }
        for (Entity passenger : entity.getPassengers()) {
            mapDisplayParts(passenger, tag1, partsMap);
        }
    }

    /**
     * 엔티티 및 그 하위의 모든 탑승(Passenger) 엔티티에 식별 태그들을 재귀적으로 보강합니다.
     */
    public static void tagEntityRecursively(Entity entity, String tag1, String tag2) {
        if (entity == null) return;
        entity.addScoreboardTag("bde_model");
        if (tag1 != null && !tag1.isBlank()) {
            entity.addScoreboardTag(tag1);
        }
        if (tag2 != null && !tag2.isBlank()) {
            entity.addScoreboardTag(tag2);
        }
        for (Entity passenger : entity.getPassengers()) {
            tagEntityRecursively(passenger, tag1, tag2);
        }
    }

    /**
     * 지정한 반경 내에서 해당 태그와 일치하는 BDEngine 모델 엔티티 및 승마 트리를 완전 제거하고,
     * 실행 중이던 애니메이션 인스턴스도 함께 정리합니다.
     */
    public static int remove(Location center, String targetTag, double radius, Main plugin) {
        if (center == null || center.getWorld() == null) return 0;
        int removedCount = 0;
        Set<UUID> removedUuids = new HashSet<>();

        String prefix = (targetTag != null && !targetTag.isBlank()) ? (targetTag.toLowerCase() + "_") : null;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (removedUuids.contains(entity.getUniqueId()) || !entity.isValid()) {
                continue;
            }

            Set<String> tags = entity.getScoreboardTags();
            boolean isBdeEntity = tags.contains("bde_model");
            boolean tagMatches = false;

            if (targetTag == null || targetTag.isBlank() || targetTag.equalsIgnoreCase("all")) {
                tagMatches = isBdeEntity;
            } else {
                for (String tag : tags) {
                    if (tag.equalsIgnoreCase(targetTag)) {
                        tagMatches = true;
                        break;
                    }
                    if (prefix != null && tag.toLowerCase().startsWith(prefix)) {
                        tagMatches = true;
                        break;
                    }
                }
            }

            if (tagMatches) {
                Entity root = entity;
                while (root.getVehicle() != null && root.getVehicle().isValid()) {
                    root = root.getVehicle();
                }
                removedCount += removeTree(root, removedUuids, plugin);
            }
        }

        return removedCount;
    }

    private static int removeTree(Entity entity, Set<UUID> removedUuids, Main plugin) {
        if (entity == null || removedUuids.contains(entity.getUniqueId())) {
            return 0;
        }

        int count = 1;
        removedUuids.add(entity.getUniqueId());

        if (plugin != null && plugin.getBdEngineManager() != null) {
            plugin.getBdEngineManager().getAnimationManager().unregisterInstance(entity.getUniqueId());
        }

        for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
            count += removeTree(passenger, removedUuids, plugin);
        }

        entity.remove();
        return count;
    }
}
