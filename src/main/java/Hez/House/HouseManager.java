package Hez.House;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HouseManager {
    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // 마을 월드 이름 (기본값: "world")
    private String townWorldName = "world";

    // 청크별 소유 하우스 매핑
    private final Map<ChunkKey, HouseData> houseByChunk = new ConcurrentHashMap<>();
    // 플레이어별 하우스 매핑
    private final Map<UUID, HouseData> houseByOwner = new ConcurrentHashMap<>();

    public String getTownWorldName() {
        return townWorldName;
    }

    public void setTownWorldName(String townWorldName) {
        this.townWorldName = townWorldName;
    }

    /**
     * 대상 월드가 보호 대상인 마을 월드인지 확인
     */
    public boolean isTownWorld(World world) {
        if (world == null) return false;
        return world.getName().equalsIgnoreCase(townWorldName);
    }

    public boolean isTownWorld(Location location) {
        if (location == null) return false;
        return isTownWorld(location.getWorld());
    }

    public boolean isAlreadyHasOwner(ChunkKey chunkKey) {
        return houseByChunk.containsKey(chunkKey);
    }

    public boolean isServerChunk(ChunkKey chunkKey) {
        HouseData house = houseByChunk.get(chunkKey);
        return house != null && SERVER_UUID.equals(house.getOwner());
    }

    /**
     * 청크 구매 및 등록 (마을 월드 내 빈 청크 등록)
     */
    public boolean claimChunk(UUID owner, Chunk chunk) {
        if (!isTownWorld(chunk.getWorld())) {
            return false; // 마을 월드에서만 등록 가능
        }

        ChunkKey chunkKey = ChunkKey.from(chunk);
        if (isAlreadyHasOwner(chunkKey)) {
            return false; // 이미 소유자가 있는 경우
        }

        HouseData house = houseByOwner.computeIfAbsent(owner, HouseData::new);
        house.addChunk(chunkKey);
        houseByChunk.put(chunkKey, house);
        return true;
    }

    /**
     * 청크 등록 해제
     */
    public boolean unclaimChunk(UUID owner, Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.from(chunk);
        HouseData house = houseByChunk.get(chunkKey);

        if (house != null && house.isOwner(owner)) {
            house.removeChunk(chunkKey);
            houseByChunk.remove(chunkKey);

            if (house.getChunks().isEmpty()) {
                houseByOwner.remove(owner);
            }
            return true;
        }
        return false;
    }

    // 서버 땅 설정 (스폰, 상점 구역 등)
    public boolean setServerChunk(Chunk chunk) {
        return claimChunk(SERVER_UUID, chunk);
    }

    // 서버 땅 해제
    public boolean removeServerChunk(Chunk chunk) {
        return unclaimChunk(SERVER_UUID, chunk);
    }

    // 하위 호환 메서드 (기존 이름 유지)
    public boolean AddHouseChuck(UUID uuid, Chunk chunk) {
        return claimChunk(uuid, chunk);
    }

    public boolean RemoveChunk(UUID uuid, Chunk chunk) {
        return unclaimChunk(uuid, chunk);
    }

    public boolean SetServerChunk(Chunk chunk) {
        return setServerChunk(chunk);
    }

    public boolean RemoveServerChunk(Chunk chunk) {
        return removeServerChunk(chunk);
    }

    public HouseData GetHouseData(Location location) {
        if (location == null) return null;
        ChunkKey chunkKey = ChunkKey.from(location.getChunk());
        return houseByChunk.get(chunkKey);
    }

    public HouseData getHouseData(ChunkKey chunkKey) {
        return houseByChunk.get(chunkKey);
    }

    public HouseData getHouseByOwner(UUID owner) {
        return houseByOwner.get(owner);
    }

    /**
     * 블록 설치/파괴 권한 확인:
     * - 관리자(OP)는 항상 허용
     * - 야생 월드는 항상 허용
     * - 마을 월드는 전체 기본 보호:
     *   - 미소유 빈 청크는 설치/파괴 불가 (반드시 청크 등록 필요)
     *   - 서버 땅은 관리자 외 불가
     *   - 개인 땅은 소유자 및 멤버만 가능
     */
    public boolean hasBuildPermission(Player player, Location location) {
        if (player.isOp()) return true;
        if (!isTownWorld(location)) return true; // 야생 월드는 자유 건축

        HouseData house = GetHouseData(location);
        if (house == null) {
            // 미소유 빈 청크: 마을 월드 전체 보호 규칙에 따라 건축 차단
            return false;
        }

        if (SERVER_UUID.equals(house.getOwner())) {
            // 서버 땅은 일반 유저 건축 불가
            return false;
        }

        return house.hasFullPermission(player.getUniqueId());
    }

    /**
     * 블록 상호작용(우클릭 등) 권한 확인:
     * - 관리자(OP)는 항상 허용
     * - 야생 월드는 항상 허용
     * - 버튼, 레버는 어디서나(미소유/서버/개인) 통행을 위해 허용
     * - 그 외 상호작용(문, 상자 등)은 소유권/방문자 권한 규칙 검사
     */
    public boolean hasInteractPermission(Player player, Block block) {
        if (player.isOp()) return true;
        if (!isTownWorld(block.getLocation())) return true;

        Material mat = block.getType();
        // 버튼과 레버는 통행/기믹을 위해 어디서나 기본 허용
        if (HouseData.isButtonOrLever(mat)) {
            return true;
        }

        HouseData house = GetHouseData(block.getLocation());
        if (house == null) {
            // 빈 청크에서는 버튼/레버 외 상호작용 차단
            return false;
        }

        if (SERVER_UUID.equals(house.getOwner())) {
            // 서버 땅은 기본적으로 버튼/레버 외 차단
            return false;
        }

        return house.canInteract(player.getUniqueId(), mat);
    }

    /**
     * 엔티티 공격/데미지 권한 확인:
     * - 플레이어 간 공격(PvP)은 마을 월드에서도 허용
     * - 몬스터 공격은 허용
     * - 가축, 동물, 갑옷대, 액자 등 보호 대상은 해당 청크 권한자만 가능
     */
    public boolean hasEntityDamagePermission(Player attacker, Entity victim) {
        if (attacker.isOp()) return true;
        if (!isTownWorld(victim.getLocation())) return true;

        // 플레이어 간 PvP 허용
        if (victim instanceof Player) {
            return true;
        }

        // 몬스터 공격 허용
        if (victim instanceof Monster) {
            return true;
        }

        // 가축, 동물, 갑옷대, 아이템 액자 등 재산 엔티티 보호
        HouseData house = GetHouseData(victim.getLocation());
        if (house == null || SERVER_UUID.equals(house.getOwner())) {
            return false;
        }

        return house.hasFullPermission(attacker.getUniqueId());
    }

    /**
     * 엔티티 상호작용 권한 확인 (동물 탑승, 교배, 젖짜기, 주민 거래 등)
     */
    public boolean hasEntityInteractPermission(Player player, Entity entity) {
        if (player.isOp()) return true;
        if (!isTownWorld(entity.getLocation())) return true;

        // 다른 플레이어나 몬스터와의 상호작용은 허용
        if (entity instanceof Player || entity instanceof Monster) {
            return true;
        }

        HouseData house = GetHouseData(entity.getLocation());
        if (house == null || SERVER_UUID.equals(house.getOwner())) {
            return false;
        }

        return house.hasFullPermission(player.getUniqueId());
    }

    // 기존 hasPermission 메서드 호환
    public boolean hasPermission(UUID uuid, Location location) {
        if (!isTownWorld(location)) return true;
        HouseData house = GetHouseData(location);
        if (house == null) return false;
        return house.hasFullPermission(uuid);
    }

    /**
     * 데이터 저장 (houseData.yml)
     */
    public void saveData(FileConfiguration config) {
        config.set("townWorld", townWorldName);
        config.set("houses", null);

        for (Map.Entry<UUID, HouseData> entry : houseByOwner.entrySet()) {
            String uuidStr = entry.getKey().toString();
            HouseData house = entry.getValue();

            List<String> chunkList = new ArrayList<>();
            for (ChunkKey key : house.getChunks()) {
                chunkList.add(key.toString());
            }

            List<String> memberList = new ArrayList<>();
            for (UUID member : house.getMembers()) {
                memberList.add(member.toString());
            }

            config.set("houses." + uuidStr + ".chunks", chunkList);
            config.set("houses." + uuidStr + ".members", memberList);
            config.set("houses." + uuidStr + ".allowVisitorDoor", house.isAllowVisitorDoor());
            config.set("houses." + uuidStr + ".allowVisitorContainer", house.isAllowVisitorContainer());
        }
    }

    /**
     * 데이터 로드 (houseData.yml)
     */
    public void loadData(FileConfiguration config) {
        houseByChunk.clear();
        houseByOwner.clear();

        townWorldName = config.getString("townWorld", "world");

        ConfigurationSection section = config.getConfigurationSection("houses");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(uuidStr);
                HouseData house = new HouseData(owner);

                house.setAllowVisitorDoor(section.getBoolean(uuidStr + ".allowVisitorDoor", false));
                house.setAllowVisitorContainer(section.getBoolean(uuidStr + ".allowVisitorContainer", false));

                List<String> chunkStrings = section.getStringList(uuidStr + ".chunks");
                for (String chunkStr : chunkStrings) {
                    ChunkKey key = ChunkKey.parse(chunkStr);
                    if (key != null) {
                        house.addChunk(key);
                        houseByChunk.put(key, house);
                    }
                }

                List<String> memberStrings = section.getStringList(uuidStr + ".members");
                for (String memberStr : memberStrings) {
                    try {
                        house.addMember(UUID.fromString(memberStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                houseByOwner.put(owner, house);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
