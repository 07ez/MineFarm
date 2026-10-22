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
     * Mojang 공식 NBT UUID를 고정 부여하여 소환 후 1틱 딜레이로 루트 및 하위 파츠를 100% 탐색하고,
     * 애니메이션이 존재할 경우 자동으로 실시간 틱 루프 재생을 시작합니다.
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

        String modelTag = model.getName();

        StringBuilder passengerSnbt = new StringBuilder();
        for (int i = 0; i < passengers.size(); i++) {
            JsonElement el = passengers.get(i);
            String snbt = el.isJsonPrimitive() ? el.getAsString() : el.toString();

            // [BDESERVERTAG] 치환 (모델명으로 단일 태그 치환)
            snbt = snbt.replace("[BDESERVERTAG]", modelTag);

            // 각 하위 엔티티의 Tags:[...] 에 bde_model 과 모델명 태그 주입
            snbt = snbt.replaceAll("(?i)Tags:\\[", "Tags:[\"bde_model\",\"" + modelTag + "\",");

            if (i > 0) {
                passengerSnbt.append(",");
            }
            passengerSnbt.append(snbt);
        }

        // 2. 고유 UUID 생성 (Mojang int[4] NBT 배열 포맷)
        UUID rootUuid = UUID.randomUUID();
        long msb = rootUuid.getMostSignificantBits();
        long lsb = rootUuid.getLeastSignificantBits();
        int i1 = (int) (msb >> 32);
        int i2 = (int) msb;
        int i3 = (int) (lsb >> 32);
        int i4 = (int) lsb;

        String summonCommand = String.format(Locale.US,
                "execute positioned %.4f %.4f %.4f in %s run summon text_display ~ ~ ~ {UUID:[I;%d,%d,%d,%d],Tags:[\"bde_model\",\"%s\",\"%s_root\"],Passengers:[%s]}",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld().getKey().toString(),
                i1, i2, i3, i4,
                modelTag, modelTag,
                passengerSnbt.toString()
        );

        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), summonCommand);

        // 3. 소환 직후 및 1틱 뒤 루트 엔티티 및 파츠들을 100% 포착하여 애니메이션 바인딩
        Runnable initTask = () -> {
            Entity root = Bukkit.getEntity(rootUuid);

            // fallback: 위치 기반 검색
            if (root == null && loc.getWorld() != null) {
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 6.0, 6.0, 6.0)) {
                    if (entity.getScoreboardTags().contains(modelTag + "_root")) {
                        root = entity;
                        break;
                    }
                }
            }

            if (root != null) {
                // 하위 파츠 엔티티들에도 모델명 태그 재귀적 보강
                tagEntityRecursively(root, modelTag);

                if (model.hasAnimations() && plugin != null && plugin.getBdEngineManager() != null) {
                    Map<String, Display> partsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    mapDisplayParts(root, partsMap);

                    BDEngineAnimation anim = model.getDefaultAnimation();
                    if (anim != null) {
                        BDEngineModelInstance instance = new BDEngineModelInstance(root, model, partsMap, anim);
                        plugin.getBdEngineManager().getAnimationManager().registerInstance(instance);
                        plugin.getLogger().info("[BDEngine] 모델 '" + model.getName() + "' 애니메이션 '" + anim.getName() + "' 자동 재생 시작! (바인딩된 파츠: " + partsMap.size() + "개, 총 " + anim.getTotalTicks() + "틱)");
                    }
                }
            } else {
                plugin.getLogger().warning("[BDEngine] 모델 '" + model.getName() + "' 소환 후 루트 엔티티를 찾지 못했습니다.");
            }
        };

        // 다음 틱에 엔티티가 월드 및 청크에 완전히 마운트된 상태에서 바인딩 실행
        Bukkit.getScheduler().runTask(plugin, initTask);

        return success;
    }

    public static void mapDisplayParts(Entity entity, Map<String, Display> partsMap) {
        if (entity == null) return;
        if (entity instanceof Display display) {
            for (String tag : display.getScoreboardTags()) {
                if (tag.contains("_") && !tag.endsWith("_root")) {
                    partsMap.put(tag, display);
                }
            }
        }
        for (Entity passenger : entity.getPassengers()) {
            mapDisplayParts(passenger, partsMap);
        }
    }

    /**
     * 이미 월드에 소환되어 있는 모델에 대해 애니메이션을 즉시 가동합니다.
     */
    public static boolean startAnimationForExisting(Entity root, BDEngineModel model, Main plugin) {
        if (root == null || !root.isValid() || model == null || !model.hasAnimations()) {
            return false;
        }
        Map<String, Display> partsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        mapDisplayParts(root, partsMap);
        BDEngineAnimation anim = model.getDefaultAnimation();
        if (anim != null && plugin != null && plugin.getBdEngineManager() != null) {
            BDEngineModelInstance instance = new BDEngineModelInstance(root, model, partsMap, anim);
            plugin.getBdEngineManager().getAnimationManager().registerInstance(instance);
            plugin.getLogger().info("[BDEngine] 기존 모델 '" + model.getName() + "' 애니메이션 재생 시작! (파츠: " + partsMap.size() + "개)");
            return true;
        }
        return false;
    }

    /**
     * 엔티티 및 그 하위의 모든 탑승(Passenger) 엔티티에 단일 모델 태그를 재귀적으로 보강합니다.
     */
    public static void tagEntityRecursively(Entity entity, String modelTag) {
        if (entity == null) return;
        entity.addScoreboardTag("bde_model");
        if (modelTag != null && !modelTag.isBlank()) {
            entity.addScoreboardTag(modelTag);
        }
        for (Entity passenger : entity.getPassengers()) {
            tagEntityRecursively(passenger, modelTag);
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
