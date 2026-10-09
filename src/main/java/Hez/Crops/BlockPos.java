package Hez.Crops;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * 작물 블록의 월드 및 x, y, z 좌표를 표현하는 불변 레코드
 */
public record BlockPos(String worldName, int x, int y, int z) {

    public static BlockPos from(Block block) {
        return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockPos from(Location location) {
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "";
        return new BlockPos(worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockPos parse(String str) {
        if (str == null) return null;
        String[] split = str.split(",");
        if (split.length < 4) return null;
        try {
            return new BlockPos(split[0].trim(), Integer.parseInt(split[1].trim()), Integer.parseInt(split[2].trim()), Integer.parseInt(split[3].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return worldName + "," + x + "," + y + "," + z;
    }
}
