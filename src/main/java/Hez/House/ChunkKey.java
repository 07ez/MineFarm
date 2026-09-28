package Hez.House;

public record ChunkKey(String worldName, int x, int z) {

    public static ChunkKey from(org.bukkit.Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public static ChunkKey parse(String str) {
        String[] split = str.split(",");
        if (split.length < 3) return null;

        String worldName = split[0].trim();
        int x = Integer.parseInt(split[1].trim());
        int z = Integer.parseInt(split[2].trim());

        return new ChunkKey(worldName, x, z);
    }

    @Override
    public String toString() {
        return worldName + "," + x + "," + z;
    }
}
