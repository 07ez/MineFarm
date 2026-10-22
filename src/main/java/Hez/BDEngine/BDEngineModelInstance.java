package Hez.BDEngine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 인게임 월드에 소환되어 실시간으로 애니메이션을 수행하는 BDEngine 모델 인스턴스
 */
public class BDEngineModelInstance {

    private final UUID rootUuid;
    private final Entity rootEntity;
    private final BDEngineModel model;
    private final Map<String, Display> parts;

    private BDEngineAnimation currentAnimation;
    private int currentTick = 0;
    private boolean isFirstTick = true;
    private boolean playing = true;
    private boolean loop = true;

    public BDEngineModelInstance(Entity rootEntity, BDEngineModel model, Map<String, Display> parts, BDEngineAnimation animation) {
        this.rootUuid = rootEntity.getUniqueId();
        this.rootEntity = rootEntity;
        this.model = model;
        this.parts = parts;
        this.currentAnimation = animation;
        this.isFirstTick = true;
    }

    public UUID getRootUuid() {
        return rootUuid;
    }

    public Entity getRootEntity() {
        return rootEntity;
    }

    public BDEngineModel getModel() {
        return model;
    }

    public Display getPart(String tag) {
        return parts.get(tag);
    }

    public Map<String, Display> getAllParts() {
        return Collections.unmodifiableMap(parts);
    }

    public BDEngineAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    public void setAnimation(BDEngineAnimation animation) {
        this.currentAnimation = animation;
        this.currentTick = 0;
        this.isFirstTick = true;
        this.playing = true;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    /**
     * 1틱 단위로 애니메이션을 전진시킵니다.
     *
     * @return 엔티티가 여전히 유효하여 인스턴스를 유지할지 여부 (false면 엔티티 삭제됨을 의미)
     */
    public boolean stepTick() {
        if (rootEntity == null || !rootEntity.isValid()) {
            return false;
        }

        if (!playing || currentAnimation == null) {
            return true;
        }

        List<BDEngineAnimation.KeyframeAction> actions = currentAnimation.getActionsAtTick(currentTick, isFirstTick);
        isFirstTick = false;

        if (!actions.isEmpty()) {
            for (BDEngineAnimation.KeyframeAction action : actions) {
                action.execute(this);
            }
        }

        currentTick++;
        if (currentTick >= currentAnimation.getDurationTicks()) {
            if (loop) {
                currentTick = 0;
            } else {
                playing = false;
            }
        }

        return true;
    }

    /**
     * 서브 명령어를 루트 엔티티의 위치와 컨텍스트로 실행합니다.
     */
    public void dispatchSubCommand(String cmd) {
        if (rootEntity == null || !rootEntity.isValid()) return;
        String fullCmd = "execute as " + rootUuid + " at @s run " + cmd;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fullCmd);
    }
}
