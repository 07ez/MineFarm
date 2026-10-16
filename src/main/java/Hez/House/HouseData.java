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
    public enum AccessMode {
        NONE("아무도 허용x", "§c"),
        MEMBER("멤버만 가능", "§a"),
        ALL("모두다 가능", "§e");

        private final String displayName;
        private final String color;

        AccessMode(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public String getFormatted() {
            return color + displayName;
        }

        public AccessMode next() {
            switch (this) {
                case NONE: return MEMBER;
                case MEMBER: return ALL;
                case ALL: return NONE;
                default: return MEMBER;
            }
        }
    }

    private final UUID owner;
    private final Set<ChunkKey> chunkKeys = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();

    // 문 / 상자 권한 모드 (기본값: 멤버만 가능)
    private AccessMode doorAccessMode = AccessMode.MEMBER;
    private AccessMode containerAccessMode = AccessMode.MEMBER;

    // 집 TP(스폰) 위치
    private org.bukkit.Location spawnLocation;

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

    public AccessMode getDoorAccessMode() {
        return doorAccessMode;
    }

    public void setDoorAccessMode(AccessMode doorAccessMode) {
        this.doorAccessMode = (doorAccessMode != null) ? doorAccessMode : AccessMode.MEMBER;
    }

    public AccessMode getContainerAccessMode() {
        return containerAccessMode;
    }

    public void setContainerAccessMode(AccessMode containerAccessMode) {
        this.containerAccessMode = (containerAccessMode != null) ? containerAccessMode : AccessMode.MEMBER;
    }

    public org.bukkit.Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(org.bukkit.Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    // 기존 boolean 플래그 하위 호환
    public boolean isAllowVisitorDoor() {
        return doorAccessMode == AccessMode.ALL;
    }

    public void setAllowVisitorDoor(boolean allowVisitorDoor) {
        this.doorAccessMode = allowVisitorDoor ? AccessMode.ALL : AccessMode.MEMBER;
    }

    public boolean isAllowVisitorContainer() {
        return containerAccessMode == AccessMode.ALL;
    }

    public void setAllowVisitorContainer(boolean allowVisitorContainer) {
        this.containerAccessMode = allowVisitorContainer ? AccessMode.ALL : AccessMode.MEMBER;
    }

    /**
     * 특정 블록에 대한 상호작용(우클릭 등) 권한 검사
     */
    public boolean canInteract(UUID uuid, Material material) {
        // 버튼과 레버는 통행을 위해 누구나 허용 (기획 규칙)
        if (isButtonOrLever(material)) {
            return true;
        }

        // 문, 다락문, 울타리문
        if (isDoorOrTrapdoor(material)) {
            switch (doorAccessMode) {
                case NONE:
                    return isOwner(uuid); // 소유자만 가능
                case MEMBER:
                    return hasFullPermission(uuid); // 소유자 및 멤버
                case ALL:
                    return true; // 모두다 가능
            }
        }

        // 보관함(상자, 화로 등)
        if (isContainer(material)) {
            switch (containerAccessMode) {
                case NONE:
                    return isOwner(uuid); // 소유자만 가능
                case MEMBER:
                    return hasFullPermission(uuid); // 소유자 및 멤버
                case ALL:
                    return true; // 모두다 가능
            }
        }

        // 그 외 상호작용은 소유자나 멤버만 가능
        return hasFullPermission(uuid);
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
