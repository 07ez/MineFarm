package Hez.BDEngine;

import Hez.MineFarm.Main;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 애니메이션 루프 틱 스케줄러 및 활성 인스턴스 관리자
 */
public class BDEngineAnimationManager {

    private final Main plugin;
    private final Map<UUID, BDEngineModelInstance> activeInstances = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public BDEngineAnimationManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 애니메이션 틱 스케줄러 태스크 시작 (1틱 단위)
     */
    public void start() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /**
     * 매 틱마다 모든 활성화된 모델 인스턴스의 애니메이션을 갱신합니다.
     */
    private void tick() {
        if (activeInstances.isEmpty()) return;

        Iterator<Map.Entry<UUID, BDEngineModelInstance>> it = activeInstances.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BDEngineModelInstance> entry = it.next();
            BDEngineModelInstance instance = entry.getValue();

            boolean alive = instance.stepTick();
            if (!alive) {
                it.remove();
            }
        }
    }

    /**
     * 새로 스폰된 모델 인스턴스를 애니메이션 관리 대상에 등록합니다.
     */
    public void registerInstance(BDEngineModelInstance instance) {
        if (instance == null) return;
        activeInstances.put(instance.getRootUuid(), instance);
    }

    /**
     * 특정 모델의 애니메이션을 중지하고 등록 해제합니다.
     */
    public void unregisterInstance(UUID rootUuid) {
        if (rootUuid == null) return;
        activeInstances.remove(rootUuid);
    }

    public BDEngineModelInstance getInstance(UUID rootUuid) {
        return activeInstances.get(rootUuid);
    }

    public Collection<BDEngineModelInstance> getActiveInstances() {
        return Collections.unmodifiableCollection(activeInstances.values());
    }

    public int getActiveCount() {
        return activeInstances.size();
    }

    /**
     * 플러그인 리로드 / 비활성화 시 모든 태스크 및 인스턴스 정리
     */
    public void cleanup() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        activeInstances.clear();
    }
}
