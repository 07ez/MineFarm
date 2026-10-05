package Hez.Display;

import Hez.Display.Crop.BuyCropTextDisplay;
import Hez.Display.Crop.SellCropTextDisplay;
import Hez.Display.Fish.FishTextDisplay;
import Hez.Display.Ore.OreTextDisplay;
import Hez.Display.Tree.TreeTextDisplay;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class DisplayManager {
    private final JavaPlugin plugin;
    // UUID별로 띄운 TextDisplay
    private final Map<UUID, List<TextDisplay>> activeTexts = new HashMap<>();

    Display previousDisplay = null;
    Display currentDisplay = null;

    public DisplayManager (JavaPlugin plugin) { this.plugin = plugin; }

    // 각 디스플레이
    TreeTextDisplay treeTextDisplay = new TreeTextDisplay();
    BuyCropTextDisplay buyCropTextDisplay = new BuyCropTextDisplay();
    SellCropTextDisplay sellCropTextDisplay = new SellCropTextDisplay();
    FishTextDisplay fishTextDisplay = new FishTextDisplay();
    OreTextDisplay oreTextDisplay = new OreTextDisplay();
    public void InitDisplay() {
        treeTextDisplay.Init();
        buyCropTextDisplay.Init();
        sellCropTextDisplay.Init();
        fishTextDisplay.Init();
        oreTextDisplay.Init();
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
                5.0, // 최대 감지 거리 (필요시 조절)
                0.25, // 감지 오차 범위(마우스 조준 보정)
                entity -> (entity instanceof Display) && entity.getType() != EntityType.TEXT_DISPLAY
        );

        RayTraceResult blockResult = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                5.0, // 최대 감지 거리 (필요시 조절)
                FluidCollisionMode.NEVER,
                true
        );

        // 바라보고 있는 target 엔티티가 있는 경우
        if (result != null && result.getHitEntity() != null) {
            // 블럭에 막혀있는지 확인
            if (blockResult != null && blockResult.getHitBlock() != null) {
                double blockDistance = player.getEyeLocation().distance(blockResult.getHitPosition().toLocation(player.getWorld()));
                double entityDistance = player.getEyeLocation().distance(result.getHitPosition().toLocation(player.getWorld()));
                if (blockDistance < entityDistance) return;
            }

            // 바라보는 블럭디스플레이가 다르다면 발광 및 텍스트 삭제
            if (currentDisplay != (Display) result.getHitEntity()) {
                previousDisplay = currentDisplay;
                removeTextDisplay(player, previousDisplay);
            }

            // 텍스트 넣기 및 발광효과
            currentDisplay = (Display) result.getHitEntity();
            Set<String> tags = currentDisplay.getScoreboardTags();
            DisplayTextInfo texts;
            float textYPos;
            if (tags.contains("tree")) {
                texts = treeTextDisplay.getTexts(tags);
                textYPos = 0.7f;
            }
            else if (tags.contains("sellCrop")) {
                texts = sellCropTextDisplay.getTexts(tags);
                textYPos = 0.3f;
            }
            else if (tags.contains("buyCrop")) {
                texts = buyCropTextDisplay.getTexts(tags);
                textYPos = 0.3f;
            }
            else if (tags.contains("fish")) {
                texts = buyCropTextDisplay.getTexts(tags);
                textYPos = 0.7f;
            }
            else if (tags.contains("ore")) {
                texts = buyCropTextDisplay.getTexts(tags);
                textYPos = 0.7f;
            }
            else if(tags.contains("none")) {
                return;
            }
            else {
                texts = new DisplayTextInfo(0, 0, 0);
                textYPos = 0.6f;
            }

            // 아직 이 플레이어에게 텍스트가 안 떠 있다면 생성
            if (!activeTexts.containsKey(player.getUniqueId())) {
                activeTexts.put(player.getUniqueId(), shopTexts(currentDisplay.getLocation(), texts, textYPos));
                currentDisplay.setGlowing(true); // 발광
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 0.8f);
            }
        } else {
            // 바라보지 않고 있다면 텍스트 제거
            removeTextDisplay(player, currentDisplay);
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

    private List <TextDisplay> shopTexts(Location baseLoc, DisplayTextInfo info, float textYPos) {
        return List.of(
                spawnTextDisplay(baseLoc.clone().add(0, textYPos, 0), "판매: " + info.sale() + "원", info.yaw()),
                spawnTextDisplay(baseLoc.clone().add(0, textYPos + 0.3f, 0), "구매: " + info.purchase() + "원", info.yaw())
        );
    }

    private void removeTextDisplay(Player player, Display target) {
        List<TextDisplay> displays = activeTexts.remove(player.getUniqueId());

        if (displays != null) {
            for (TextDisplay display : displays) {
                if (display != null && display.isValid()) {
                    display.remove(); // sale과 purchase텍스트 모두 삭제
                }
            }
        }
        if (target != null)
            target.setGlowing(false); // 발광 삭제
    }
}
