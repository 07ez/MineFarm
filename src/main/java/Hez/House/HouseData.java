package Hez.House;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HouseData {
    private final UUID owner;
    private final Set<ChunkKey> chunkKeys = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();

    public HouseData(UUID owner) {
        this.owner = owner;
    }

    public void AddChunk(ChunkKey chunkKey) {
        chunkKeys.add(chunkKey);
    }

    public void RemoveChunk(ChunkKey chunkkey) {
        chunkKeys.remove(chunkkey);
    }

    public boolean containsChunk(ChunkKey chunkKey) {
        return chunkKeys.contains(chunkKey);
    }

    public UUID getOwner() { return owner; }
    public Set<ChunkKey> getChunks() { return chunkKeys; }
    public Set<UUID> getMembers() { return members; }
}
