package Hez.Menu;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class WarpManager {

    public static class WarpPoint {
        private final String name;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public WarpPoint(String name, String worldName, double x, double y, double z, float yaw, float pitch) {
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public String getName() {
            return name;
        }

        public Location getLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    private static final Map<String, WarpPoint> WARPS = new HashMap<>();

    static {
        // 1. 마을
        WARPS.put("town", new WarpPoint("마을", "world", -0.5, 102.0, -31.5, 180.0f, 0.0f));

        // 2. 야생
        WARPS.put("wild", new WarpPoint("야생", "world", -49.5, 101.0, -176.5, 0.0f, 0.0f));

        // 3. 미술관
        WARPS.put("museum", new WarpPoint("미술관", "world", 94.5, 106.0, -66.5, -90.0f, 0.0f));

        // 4. 은행
        WARPS.put("bank", new WarpPoint("은행", "world", -96.5, 100.0, -169.5, 0.0f, 0.0f));

        // 5. 작물상점
        WARPS.put("crops", new WarpPoint("작물상점", "world", -60.5, 100.0, -79.5, -90.0f, 0.0f));

        // 6. 물고기상점
        WARPS.put("fish", new WarpPoint("물고기상점", "world", 61.5, 100.0, -79.5, 90.0f, 0.0f));

        // 7. 광석상점
        WARPS.put("ore", new WarpPoint("광석상점", "world", 62.5, 100.0, -113.5, 90.0f, 0.0f));

        // 8. 나무상점
        WARPS.put("tree", new WarpPoint("나무상점", "world", -60.5, 100.0, -113.5, -90.0f, 0.0f));
    }

    /**
     * 플레이어를 워프 지점으로 이동
     */
    public static boolean teleport(Player player, String actionId) {
        WarpPoint warp = WARPS.get(actionId);
        if (warp == null) {
            return false;
        }

        Location loc = warp.getLocation();
        if (loc == null || loc.getWorld() == null) {
            player.sendMessage("§c[MineFarm] '" + warp.getName() + "' 월드를 찾을 수 없습니다.");
            return false;
        }

        // 인벤토리 창 닫기
        player.closeInventory();

        // 텔레포트
        player.teleport(loc);

        // 엔더맨 텔레포트 효과음
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // 안내 메시지
        player.sendMessage("§a[MineFarm] §f" + warp.getName() + "§a(으)로 이동했습니다.");
        return true;
    }
}
