package Hez.BDEngine;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * BDEngine Server API 연동, 로컬 모델, 태그 및 애니메이션 통합 관리자
 */
public class BDEngineManager {

    private final Main plugin;
    private final File modelsFolder;
    private final File tagsFile;

    private final Map<String, BDEngineModel> cachedModels = new ConcurrentHashMap<>();
    private final Set<String> registeredTags = Collections.synchronizedSet(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
    private final HttpClient httpClient;
    private final BDEngineAnimationManager animationManager;

    public BDEngineManager(Main plugin) {
        this.plugin = plugin;
        this.modelsFolder = new File(plugin.getDataFolder(), "models");
        this.tagsFile = new File(plugin.getDataFolder(), "models/tags.yml");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.animationManager = new BDEngineAnimationManager(plugin);
    }

    /**
     * 모델 폴더, 태그 파일 로드 및 애니메이션 스케줄러 가동
     */
    public void init() {
        if (!modelsFolder.exists()) {
            modelsFolder.mkdirs();
        }

        loadTags();
        loadAllModels();
        animationManager.start();
    }

    /**
     * tags.yml 로드
     */
    public void loadTags() {
        registeredTags.clear();
        if (tagsFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(tagsFile);
            List<String> list = config.getStringList("tags");
            registeredTags.addAll(list);
        }
    }

    /**
     * tags.yml 저장
     */
    public void saveTags() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("tags", new ArrayList<>(registeredTags));
            config.save(tagsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("BDEngine tags.yml 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 새 태그1을 등록하고 저장합니다.
     */
    public void registerTag(String tag1) {
        if (tag1 != null && !tag1.isBlank()) {
            if (registeredTags.add(tag1.trim())) {
                saveTags();
            }
        }
    }

    /**
     * 로컬 models 디렉터리의 모든 .json 모델 파일을 읽어 캐시에 적재합니다.
     */
    public void loadAllModels() {
        cachedModels.clear();
        if (!modelsFolder.exists()) return;

        File[] files = modelsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return;

        int loadedCount = 0;
        int animCount = 0;
        for (File file : files) {
            try {
                String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                BDEngineModel model = BDEngineModel.fromStorageJson(json);
                if (model != null) {
                    cachedModels.put(model.getName().toLowerCase(), model);
                    registerTag(model.getTag1());
                    loadedCount++;
                    if (model.hasAnimations()) {
                        animCount += model.getAnimations().size();
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("BDEngine 모델 로드 실패 (" + file.getName() + "): " + e.getMessage());
            }
        }
        plugin.getLogger().info("[BDEngine] 총 " + loadedCount + "개의 모델(" + animCount + "개 애니메이션)과 " + registeredTags.size() + "개의 태그가 로드되었습니다.");
    }

    /**
     * BDEngine 웹 Server API(https://www.block-display.com/server-api?id={id}&tag={modelName})에서
     * 비동기로 모델을 다운로드받아 로컬에 영구 저장합니다. (태그1/태그2 생략, 모델명을 기본 태그로 사용)
     */
    public void downloadModel(String exportId, String modelName, BiConsumer<Boolean, String> callback) {
        if (exportId == null || exportId.isBlank()) {
            callback.accept(false, "Export ID가 올바르지 않습니다.");
            return;
        }
        if (modelName == null || modelName.isBlank()) {
            callback.accept(false, "모델명이 올바르지 않습니다.");
            return;
        }

        String uri = String.format("https://www.block-display.com/server-api?id=%s&tag=%s",
                exportId.trim(), modelName.trim());

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                String body = response.body();

                if (status == 200) {
                    BDEngineModel model = new BDEngineModel(modelName, System.currentTimeMillis(), body);
                    if (model.getParsedContent() == null || model.getParsedContent().isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                callback.accept(false, "응답 JSON에 유효한 모델 데이터가 없습니다."));
                        return;
                    }

                    // 파일 영구 저장
                    File targetFile = new File(modelsFolder, modelName + ".json");
                    Files.writeString(targetFile.toPath(), model.toStorageJson(), StandardCharsets.UTF_8);

                    // 캐시 및 태그 갱신
                    cachedModels.put(modelName.toLowerCase(), model);
                    registerTag(modelName);

                    String animInfo = model.hasAnimations() ? " (" + model.getAnimations().size() + "개 애니메이션 포함)" : "";
                    Bukkit.getScheduler().runTask(plugin, () ->
                            callback.accept(true, "모델 '" + modelName + "'" + animInfo + " 다운로드 및 영구 저장 완료!"));
                } else if (status == 404 || body.contains("outdated") || body.contains("exist")) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            callback.accept(false, "ID가 존재하지 않거나 만료되었습니다 (10분 유효 시간 경과). BDEngine 에디터에서 다시 Export 해주세요."));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            callback.accept(false, "API 요청 실패 (HTTP " + status + "): " + body));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        callback.accept(false, "네트워크 오류 발생: " + e.getMessage()));
            }
        });
    }

    /**
     * BDEngine 웹 Server API(https://www.block-display.com/server-api?id={id}&tag={tag1})에서
     * 비동기로 모델을 다운로드받아 로컬에 영구 저장합니다.
     */
    public void downloadModel(String exportId, String modelName, String tag1, String tag2, BiConsumer<Boolean, String> callback) {
        if (exportId == null || exportId.isBlank()) {
            callback.accept(false, "Export ID가 올바르지 않습니다.");
            return;
        }
        if (modelName == null || modelName.isBlank()) {
            callback.accept(false, "모델명이 올바르지 않습니다.");
            return;
        }
        if (tag1 == null || tag1.isBlank() || tag2 == null || tag2.isBlank()) {
            callback.accept(false, "태그1(그룹)과 태그2(개별)는 필수 입력 항목입니다.");
            return;
        }

        String uri = String.format("https://www.block-display.com/server-api?id=%s&tag=%s",
                exportId.trim(), tag1.trim());

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                String body = response.body();

                if (status == 200) {
                    BDEngineModel model = new BDEngineModel(modelName, tag1, tag2, System.currentTimeMillis(), body);
                    if (model.getParsedContent() == null || model.getParsedContent().size() == 0) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                callback.accept(false, "응답 JSON에 유효한 모델 데이터가 없습니다."));
                        return;
                    }

                    // 파일 영구 저장
                    File targetFile = new File(modelsFolder, modelName + ".json");
                    Files.writeString(targetFile.toPath(), model.toStorageJson(), StandardCharsets.UTF_8);

                    // 캐시 및 태그 갱신
                    cachedModels.put(modelName.toLowerCase(), model);
                    registerTag(tag1);

                    String animInfo = model.hasAnimations() ? " (" + model.getAnimations().size() + "개 애니메이션 포함)" : "";
                    Bukkit.getScheduler().runTask(plugin, () ->
                            callback.accept(true, "모델 '" + modelName + "' (태그1: " + tag1 + ", 태그2: " + tag2 + ")" + animInfo + " 다운로드 및 영구 저장 완료!"));
                } else if (status == 404 || body.contains("outdated") || body.contains("exist")) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            callback.accept(false, "ID가 존재하지 않거나 만료되었습니다 (10분 유효 시간 경과). BDEngine 에디터에서 다시 Export 해주세요."));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            callback.accept(false, "API 요청 실패 (HTTP " + status + "): " + body));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        callback.accept(false, "네트워크 오류 발생: " + e.getMessage()));
            }
        });
    }

    /**
     * 로컬에 저장된 모델을 플레이어 위치에 소환합니다. (애니메이션이 있는 경우 자동 재생)
     */
    public boolean spawnModel(Location loc, String modelName) {
        if (modelName == null) return false;
        BDEngineModel model = cachedModels.get(modelName.toLowerCase().trim());
        if (model == null) {
            return false;
        }
        return BDEngineSpawner.spawn(loc, model, plugin);
    }

    /**
     * 주변 모델을 안전하게 제거하고 애니메이션도 정리합니다.
     */
    public int removeModels(Location center, String targetTag, double radius) {
        return BDEngineSpawner.remove(center, targetTag, radius, plugin);
    }

    public BDEngineModel getModel(String modelName) {
        if (modelName == null) return null;
        return cachedModels.get(modelName.toLowerCase().trim());
    }

    public List<String> getModelNames() {
        return new ArrayList<>(cachedModels.keySet());
    }

    public Set<String> getRegisteredTags() {
        return Collections.unmodifiableSet(registeredTags);
    }

    public BDEngineAnimationManager getAnimationManager() {
        return animationManager;
    }

    public void cleanup() {
        if (animationManager != null) {
            animationManager.cleanup();
        }
    }
}
