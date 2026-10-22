package Hez.BDEngine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * BDEngine 모델 엔티티 소환 및 태그 안전 제거 헬퍼
 */
public class BDEngineSpawner {

    /**
     * 지정한 위치에 BDEngine 모델을 소환합니다.
     * 최상위 외곽 디스플레이(Root)와 모든 하위 승마(Passenger) 엔티티에
     * bde_model, tag1, tag2가 확실하게 직접 부여됩니다.
     *
     * @param loc 소환할 위치
     * @param model 소환할 BDEngine 모델
     * @return 소환 성공 여부
     */
    public static boolean spawn(Location loc, BDEngineModel model) {
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

        // 3. 소환된 루트 엔티티 및 하위 탑승 엔티티 전체에 태그 재귀 보강
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
            if (entity.getScoreboardTags().contains("bde_model")
                    || entity.getScoreboardTags().contains(tag1)
                    || entity.getScoreboardTags().contains(tag2)
                    || entity.getScoreboardTags().stream().anyMatch(t -> t.toLowerCase().startsWith(tag1.toLowerCase() + "_"))) {
                tagEntityRecursively(entity, tag1, tag2);
            }
        }

        return success;
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
     * 지정한 반경 내에서 해당 태그(태그1, 태그2, 또는 BDEngine 내부 파츠 접두사 tag1_*)와 일치하는
     * BDEngine 모델 엔티티 및 그 하위의 모든 승마 파츠 디스플레이를 완벽하게 제거합니다.
     * 다른 플러그인의 상점/홀로그램 디스플레이 등 무관한 엔티티는 절대로 건드리지 않습니다.
     *
     * @param center 기준 위치
     * @param targetTag 일치해야 하는 태그1 또는 태그2 (null 또는 "all"이면 bde_model 전체)
     * @param radius 탐색 반경
     * @return 제거된 엔티티 수
     */
    public static int remove(Location center, String targetTag, double radius) {
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
                    // BDEngine 내부 파츠 태그 매칭 (예: Pet_0, Pet_1 등 기존 잔여물까지 포괄)
                    if (prefix != null && tag.toLowerCase().startsWith(prefix)) {
                        tagMatches = true;
                        break;
                    }
                }
            }

            if (tagMatches) {
                // 루트 엔티티(최상위 외곽 디스플레이)를 찾아 전체 승마 트리 일괄 제거
                Entity root = entity;
                while (root.getVehicle() != null && root.getVehicle().isValid()) {
                    root = root.getVehicle();
                }
                removedCount += removeTree(root, removedUuids);
            }
        }

        return removedCount;
    }

    /**
     * 지정된 엔티티와 그 아래에 탑승한 모든 Passenger 엔티티들을 재귀적으로 완전 제거합니다.
     */
    private static int removeTree(Entity entity, Set<UUID> removedUuids) {
        if (entity == null || removedUuids.contains(entity.getUniqueId())) {
            return 0;
        }

        int count = 1;
        removedUuids.add(entity.getUniqueId());

        // 하위 탑승 엔티티들 먼저 재귀 제거
        for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
            count += removeTree(passenger, removedUuids);
        }

        entity.remove();
        return count;
    }
}
