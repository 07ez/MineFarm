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
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager {
    private final JavaPlugin plugin;

    // 플레이어별 현재 바라보고 있는 Display 추적 (전역 공유 버그 해결!)
    private final Map<UUID, Display> currentDisplays = new ConcurrentHashMap<>();
    // 플레이어별 띄운 개인 전용 TextDisplay 목록
    private final Map<UUID, List<TextDisplay>> activeTexts = new ConcurrentHashMap<>();
    // 플레이어별 개인 전용 클론 발광 Display 엔티티
    private final Map<UUID, Display> activeGlowDisplays = new ConcurrentHashMap<>();

    public DisplayManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

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
        // 1틱(0.05초)마다 서버의 모든 온라인 플레이어 시선 개별 검사
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Set<UUID> onlineUUIDs = new HashSet<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                onlineUUIDs.add(player.getUniqueId());
                checkPlayerLook(player);
            }

            // 퇴장한 플레이어의 잔여 디스플레이 정리
            for (UUID uuid : new HashSet<>(activeTexts.keySet())) {
                if (!onlineUUIDs.contains(uuid)) {
                    cleanupPlayerDisplay(uuid);
                }
            }
        }, 0L, 1L);
    }

    public Display getCurrentDisplay(UUID uuid) {
        return currentDisplays.get(uuid);
    }

    public Display rayTraceShopDisplay(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                5.0,
                0.25,
                entity -> (entity instanceof Display)
                        && entity.getType() != EntityType.TEXT_DISPLAY
                        && !entity.getScoreboardTags().contains("private_glow")
        );

        RayTraceResult blockResult = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                5.0,
                FluidCollisionMode.NEVER,
                true
        );

        if (result != null && result.getHitEntity() instanceof Display hitDisplay) {
            if (blockResult != null && blockResult.getHitBlock() != null) {
                double blockDistance = player.getEyeLocation().distance(blockResult.getHitPosition().toLocation(player.getWorld()));
                double entityDistance = player.getEyeLocation().distance(result.getHitPosition().toLocation(player.getWorld()));
                if (blockDistance < entityDistance) {
                    return null;
                }
            }
            if (hitDisplay.getScoreboardTags().contains("none")) {
                return null;
            }
            return hitDisplay;
        }
        return null;
    }

    public DisplayTextInfo getDisplayTextInfo(Set<String> tags) {
        if (tags == null) return null;
        if (tags.contains("tree")) {
            return treeTextDisplay.getTexts(tags);
        } else if (tags.contains("sellCrop")) {
            return sellCropTextDisplay.getTexts(tags);
        } else if (tags.contains("buyCrop")) {
            return buyCropTextDisplay.getTexts(tags);
        } else if (tags.contains("fish")) {
            return fishTextDisplay.getTexts(tags);
        } else if (tags.contains("ore")) {
            return oreTextDisplay.getTexts(tags);
        }
        return null;
    }

    private void checkPlayerLook(Player player) {
        UUID uuid = player.getUniqueId();
        Display prevTarget = currentDisplays.get(uuid);
        Display hitDisplay = rayTraceShopDisplay(player);

        // 바라보고 있는 target 엔티티가 있는 경우
        if (hitDisplay != null) {
            // 바라보는 디스플레이가 이전과 달라졌다면 기존 개인 텍스트/발광 제거
            if (prevTarget != null && !prevTarget.equals(hitDisplay)) {
                removeTextDisplay(player);
            }

            // 이미 현재 타깃에 대한 개인 텍스트가 떠 있다면 유지
            if (activeTexts.containsKey(uuid)) {
                return;
            }

            Set<String> tags = hitDisplay.getScoreboardTags();
            DisplayTextInfo texts = getDisplayTextInfo(tags);
            float textYPos;
            if (tags.contains("tree")) {
                textYPos = 0.7f;
            } else if (tags.contains("sellCrop")) {
                textYPos = 0.3f;
            } else if (tags.contains("buyCrop")) {
                textYPos = 0.3f;
            } else if (tags.contains("fish")) {
                textYPos = 0.7f;
            } else if (tags.contains("ore")) {
                textYPos = 0.7f;
            } else {
                if (texts == null) texts = new DisplayTextInfo(0, 0, 0);
                textYPos = 0.6f;
            }

            if (texts == null) {
                texts = new DisplayTextInfo(0, 0, 0);
            }

            // 이 플레이어에게만 보이는 개인 전용 텍스트 및 개인 전용 발광 적용!
            currentDisplays.put(uuid, hitDisplay);
            activeTexts.put(uuid, shopTexts(player, hitDisplay.getLocation(), texts, textYPos));
            spawnPrivateGlow(player, hitDisplay);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 0.8f);
        } else {
            // 바라보지 않고 있다면 해당 플레이어의 개인 텍스트 및 발광만 제거
            if (prevTarget != null || activeTexts.containsKey(uuid)) {
                removeTextDisplay(player);
            }
        }
    }

    /**
     * 해당 플레이어에게만 보이는 개인 전용 TextDisplay 생성
     */
    private TextDisplay spawnPrivateTextDisplay(Player viewer, Location loc, String content, int yaw) {
        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, text -> {
            text.setVisibleByDefault(false); // 다른 플레이어에게는 보이지 않음!
            text.setPersistent(false);       // 서버 재시작 시 잔류 방지
            text.setText(content);
            text.setSeeThrough(false);
            text.setBackgroundColor(Color.fromARGB(50, 0, 0, 0));
            text.setRotation(yaw, 0);

            org.bukkit.util.Transformation transformation = text.getTransformation();
            transformation.getScale().set(0.7f, 0.7f, 0.7f);
            text.setTransformation(transformation);
        });
        viewer.showEntity(plugin, display); // 오직 바라본 본인에게만 표시
        return display;
    }

    private List<TextDisplay> shopTexts(Player viewer, Location baseLoc, DisplayTextInfo info, float textYPos) {
        return List.of(
                spawnPrivateTextDisplay(viewer, baseLoc.clone().add(0, textYPos, 0), "판매: " + info.sale() + "원", info.yaw()),
                spawnPrivateTextDisplay(viewer, baseLoc.clone().add(0, textYPos + 0.3f, 0), "구매: " + info.purchase() + "원", info.yaw())
        );
    }

    /**
     * 해당 플레이어에게만 발광(Glowing)이 보이도록 개인용 오버레이 Display 생성
     */
    private void spawnPrivateGlow(Player viewer, Display target) {
        // 혹시 원본에 발광이 켜져 있었다면 꺼서 다른 사람에게 안 보이게 초기화
        if (target.isGlowing()) {
            target.setGlowing(false);
        }

        Location loc = target.getLocation();
        Display glowClone = null;

        if (target instanceof ItemDisplay itemTarget) {
            glowClone = loc.getWorld().spawn(loc, ItemDisplay.class, clone -> {
                clone.setVisibleByDefault(false);
                clone.setPersistent(false);
                clone.addScoreboardTag("private_glow");
                clone.setItemStack(itemTarget.getItemStack());
                clone.setItemDisplayTransform(itemTarget.getItemDisplayTransform());
                clone.setBillboard(itemTarget.getBillboard());
                clone.setTransformation(itemTarget.getTransformation());
                clone.setGlowing(true);
            });
        } else if (target instanceof BlockDisplay blockTarget) {
            glowClone = loc.getWorld().spawn(loc, BlockDisplay.class, clone -> {
                clone.setVisibleByDefault(false);
                clone.setPersistent(false);
                clone.addScoreboardTag("private_glow");
                clone.setBlock(blockTarget.getBlock());
                clone.setBillboard(blockTarget.getBillboard());
                clone.setTransformation(blockTarget.getTransformation());
                clone.setGlowing(true);
            });
        }

        if (glowClone != null) {
            viewer.showEntity(plugin, glowClone);
            activeGlowDisplays.put(viewer.getUniqueId(), glowClone);
        }
    }

    private void removeTextDisplay(Player player) {
        cleanupPlayerDisplay(player.getUniqueId());
    }

    private void cleanupPlayerDisplay(UUID uuid) {
        currentDisplays.remove(uuid);

        List<TextDisplay> displays = activeTexts.remove(uuid);
        if (displays != null) {
            for (TextDisplay display : displays) {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            }
        }

        Display glowClone = activeGlowDisplays.remove(uuid);
        if (glowClone != null && glowClone.isValid()) {
            glowClone.remove();
        }
    }

    public void clearAllDisplays() {
        for (UUID uuid : new HashSet<>(activeTexts.keySet())) {
            cleanupPlayerDisplay(uuid);
        }
    }
}
