package Hez.Display;

import Hez.Display.Tree.TreeTextDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class DisplayManager {
    private final JavaPlugin plugin;
    // UUID별로 띄운 Textdisplay
    private final Map<UUID, List<TextDisplay>> activeTexts = new HashMap<>();

    BlockDisplay previousDisplay = null;
    BlockDisplay currentDisplay = null;
    public DisplayManager (JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 각 디스플레이
    TreeTextDisplay treeTextDisplay = new TreeTextDisplay();
    public void InitDisplay() {
        treeTextDisplay.Init();
    }

    public void startRaytraceTask() {
        // 1틱(0.05초)마다 서버의 모든 온라인 플레이어 시선 검사
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPlayerLook(player);
            }
        }, 0L, 1L);
    }

    private void checkPlayerLook(Player player) {
        // 플레이어 눈 위치에서 최대 5블록 거리까지 엔티티 레이트레이스 (비대면 처리)
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                7.0, // 최대 감지 거리 (필요시 조절)
                0.25, // 감지 오차 범위(마우스 조준 보정)
                entity -> entity instanceof BlockDisplay
        );

        // 바라보고 있는 target 엔티티가 있는 경우
        if (result != null && result.getHitEntity() != null) {
            if (previousDisplay != currentDisplay) {
                previousDisplay = currentDisplay;
                removeTextDisplay(player, previousDisplay);
            }
            currentDisplay = (BlockDisplay) result.getHitEntity();
            Set<String> tags = currentDisplay.getScoreboardTags();
            DisplayTextInfo texts;

            if (tags.contains("tree")) {
                texts = treeTextDisplay.getTexts(tags);
            }
            else {
                texts = new DisplayTextInfo("오류", "개발자에게 문의하세요", 0);
            }

            // 아직 이 플레이어에게 텍스트가 안 떠 있다면 생성
            if (!activeTexts.containsKey(player.getUniqueId())) {
                activeTexts.put(player.getUniqueId(), shopTexts(currentDisplay.getLocation(), texts));
                currentDisplay.setGlowing(true); // 발광
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 0.8f);
            }
        } else {
            // 바라보지 않고 있다면 텍스트 제거
            removeTextDisplay(player, previousDisplay);
        }
    }

    private TextDisplay spawnTextDisplay(Location loc, String content, int yaw) {
        return loc.getWorld().spawn(loc, TextDisplay.class, text -> {
            text.setText(content);                                                      // 글자 내용 적기
            text.setSeeThrough(false);                                                  // 벽 뒤에서는 안 보임
            text.setBackgroundColor(Color.fromARGB(50, 0, 0, 0));  // 베경 투명도
            text.setRotation(yaw, 0);                                              // 텍스트 디스플레이 돌리기

            org.bukkit.util.Transformation transformation = text.getTransformation();   // 글자 크기
            transformation.getScale().set(0.7f, 0.7f, 0.7f);
            text.setTransformation(transformation);
        });
    }

    private List <TextDisplay> shopTexts(Location baseLoc, DisplayTextInfo info) {
        return List.of(
                spawnTextDisplay(baseLoc.clone().add(0, 0.6f, 0), "판매: " + info.sale(), info.yaw()),
                spawnTextDisplay(baseLoc.clone().add(0, 0.9f, 0), "구매: " + info.purchase(), info.yaw())
        );
    }

    private void removeTextDisplay(Player player, BlockDisplay target) {
        List<TextDisplay> displays = activeTexts.remove(player.getUniqueId());

        if (displays != null) {
            for (TextDisplay display : displays) {
                if (display != null && display.isValid()) {
                    display.remove(); // sale과 purchase텍스트 모두 삭제
                }
            }
        }
        target.setGlowing(false); // 발광 삭제
    }
}
