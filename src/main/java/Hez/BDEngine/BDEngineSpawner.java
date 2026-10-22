package Hez.BDEngine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Locale;
import java.util.Set;

/**
 * BDEngine 모델 엔티티 소환 및 태그 안전 제거 헬퍼
 */
public class BDEngineSpawner {

    /**
     * 지정한 위치에 BDEngine 모델을 소환합니다.
     * 모든 루트 및 승마(Passenger) 엔티티에 bde_model, tag1, tag2가 자동 부착됩니다.
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

        StringBuilder passengerSnbt = new StringBuilder();
        for (int i = 0; i < passengers.size(); i++) {
            JsonElement el = passengers.get(i);
            String snbt = el.isJsonPrimitive() ? el.getAsString() : el.toString();
            // [BDESERVERTAG] 치환
            snbt = snbt.replace("[BDESERVERTAG]", model.getTag1());
            if (i > 0) {
                passengerSnbt.append(",");
            }
            passengerSnbt.append(snbt);
        }

        // 2. execute positioned 명령어를 통해 바닐라 NBT 엔진으로 정확하게 승마 계층 구조 소환
        String summonCommand = String.format(Locale.US,
                "execute positioned %.4f %.4f %.4f in %s run summon text_display ~ ~ ~ {Tags:[\"bde_model\",\"%s\",\"%s\"],Passengers:[%s]}",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld().getKey().toString(),
                model.getTag1(), model.getTag2(),
                passengerSnbt.toString()
        );

        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), summonCommand);

        // 3. 소환된 루트 엔티티 및 하위 탑승 엔티티 전체에 태그 재귀 부여
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
            if (entity.getScoreboardTags().contains("bde_model")
                    || entity.getScoreboardTags().contains(model.getTag1())
                    || entity.getScoreboardTags().contains(model.getTag2())) {
                tagEntityRecursively(entity, model.getTag1(), model.getTag2());
            }
        }

        return success;
    }

    /**
     * 엔티티 및 그 하위의 모든 탑승(Passenger) 엔티티에 식별 태그들을 재귀적으로 부여합니다.
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
     * 지정한 반경 내에서 bde_model 태그를 가진 엔티티 중 targetTag와 일치하는 엔티티만 안전하게 제거합니다.
     * 다른 플러그인의 디스플레이 엔티티나 무관한 엔티티는 절대로 건드리지 않습니다.
     *
     * @param center 기준 위치
     * @param targetTag 일치해야 하는 태그1 또는 태그2 (null 또는 "all"이면 bde_model 전체)
     * @param radius 탐색 반경
     * @return 제거된 엔티티 수
     */
    public static int remove(Location center, String targetTag, double radius) {
        if (center == null || center.getWorld() == null) return 0;
        int removedCount = 0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            Set<String> tags = entity.getScoreboardTags();
            // 반드시 bde_model 태그를 가지고 있어야 함
            if (!tags.contains("bde_model")) {
                continue;
            }

            boolean matches = false;
            if (targetTag == null || targetTag.isBlank() || targetTag.equalsIgnoreCase("all")) {
                matches = true;
            } else {
                for (String tag : tags) {
                    if (tag.equalsIgnoreCase(targetTag)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                entity.remove();
                removedCount++;
            }
        }

        return removedCount;
    }
}
