package Hez.House;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 개별 플레이어(또는 서버)의 영지 및 소유 청크, 멤버, 권한 데이터
 */
public class HouseData {
    private final UUID owner;
    private final Set<ChunkKey> chunkKeys = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();

    // 방문자(비멤버) 설정 플래그
    private boolean allowVisitorDoor = false;
    private boolean allowVisitorContainer = false;

    public HouseData(UUID owner) {
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<ChunkKey> getChunks() {
        return Collections.unmodifiableSet(chunkKeys);
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addChunk(ChunkKey chunkKey) {
        chunkKeys.add(chunkKey);
    }

    public void removeChunk(ChunkKey chunkKey) {
        chunkKeys.remove(chunkKey);
    }

    public boolean containsChunk(ChunkKey chunkKey) {
        return chunkKeys.contains(chunkKey);
    }

    public boolean addMember(UUID memberUuid) {
        return members.add(memberUuid);
    }

    public boolean removeMember(UUID memberUuid) {
        return members.remove(memberUuid);
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    /**
     * 소유자이거나 등록된 멤버인지 검사 (완전한 건축/수정 권한)
     */
    public boolean hasFullPermission(UUID uuid) {
        return isOwner(uuid) || isMember(uuid);
    }

    public boolean isAllowVisitorDoor() {
        return allowVisitorDoor;
    }

    public void setAllowVisitorDoor(boolean allowVisitorDoor) {
        this.allowVisitorDoor = allowVisitorDoor;
    }

    public boolean isAllowVisitorContainer() {
        return allowVisitorContainer;
    }

    public void setAllowVisitorContainer(boolean allowVisitorContainer) {
        this.allowVisitorContainer = allowVisitorContainer;
    }

    /**
     * 특정 블록에 대한 상호작용(우클릭 등) 권한 검사
     */
    public boolean canInteract(UUID uuid, Material material) {
        // 소유자나 멤버는 항상 모든 상호작용 가능
        if (hasFullPermission(uuid)) {
            return true;
        }

        // 버튼과 레버는 방문자도 항상 허용 (기획 규칙)
        if (isButtonOrLever(material)) {
            return true;
        }

        // 문, 다락문
        if (isDoorOrTrapdoor(material)) {
            return allowVisitorDoor;
        }

        // 보관함(상자, 화로 등)
        if (isContainer(material)) {
            return allowVisitorContainer;
        }

        // 그 외 상호작용(작물, 레드스톤, 침대 등)은 기본 차단
        return false;
    }

    public static boolean isButtonOrLever(Material material) {
        return material == Material.LEVER || Tag.BUTTONS.isTagged(material);
    }

    public static boolean isDoorOrTrapdoor(Material material) {
        return Tag.DOORS.isTagged(material) || Tag.TRAPDOORS.isTagged(material) || Tag.FENCE_GATES.isTagged(material);
    }

    public static boolean isContainer(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.BARREL
                || material == Material.FURNACE
                || material == Material.BLAST_FURNACE
                || material == Material.SMOKER
                || material == Material.HOPPER
                || material == Material.DROPPER
                || material == Material.DISPENSER
                || material == Material.BREWING_STAND
                || Tag.SHULKER_BOXES.isTagged(material);
    }
}
