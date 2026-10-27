package Hez.Placeholder;

import Hez.House.HouseData;
import Hez.House.HouseManager;
import Hez.MineFarm.Main;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HouseExpansion extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "mfhouse";
    }

    @Override
    public String getAuthor() {
        return "Hez";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) return "";

        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        HouseManager houseManager = Main.getInstance().getHouseManager();
        if (houseManager == null) return "";

        Location loc = player.getLocation();
        boolean inTownWorld = houseManager.isTownWorld(loc);
        HouseData house = inTownWorld ? houseManager.GetHouseData(loc) : null;

        // 토지 상태 판정
        // 1. 마을땅 (서버 공용 구역이거나 마을 월드가 아닌 곳)
        boolean isVillageLand = !inTownWorld || (house != null && HouseManager.SERVER_UUID.equals(house.getOwner()));
        // 2. 빈 땅 (소유자가 없는 청크)
        boolean isEmptyLand = inTownWorld && (house == null);
        // 3. 개인 영지 (누군가의 땅)
        boolean isPersonalLand = inTownWorld && house != null && !HouseManager.SERVER_UUID.equals(house.getOwner());

        String ownerName = "";
        if (isPersonalLand) {
            UUID ownerUuid = house.getOwner();
            OfflinePlayer ownerOp = Bukkit.getOfflinePlayer(ownerUuid);
            ownerName = (ownerOp.getName() != null) ? ownerOp.getName() : ownerUuid.toString().substring(0, 8);
        }

        String paramLower = (params == null) ? "" : params.toLowerCase();

        // 1) 색상 없는 순수 텍스트 (%mfhouse_status_raw%, %mfhouse_land_raw%, %mfhouse_raw%)
        if (paramLower.equals("status_raw") || paramLower.equals("land_raw") || paramLower.equals("raw")) {
            if (isVillageLand) {
                return "구매 불가";
            } else if (isEmptyLand) {
                return "구매가능";
            } else {
                return ownerName + "의 땅";
            }
        }

        // 2) 소유자 이름 반환 (%mfhouse_owner%)
        if (paramLower.equals("owner")) {
            if (isVillageLand) {
                return "마을";
            } else if (isEmptyLand) {
                return "없음";
            } else {
                return ownerName;
            }
        }

        // 3) 내가 소유자인지 여부 (%mfhouse_is_owner%)
        if (paramLower.equals("is_owner")) {
            if (isPersonalLand && house.isOwner(player.getUniqueId())) {
                return "true";
            }
            return "false";
        }

        // 4) 내가 멤버 또는 소유자인지 여부 (%mfhouse_is_member%)
        if (paramLower.equals("is_member")) {
            if (isPersonalLand && house.hasFullPermission(player.getUniqueId())) {
                return "true";
            }
            return "false";
        }

        // 5) 기본/상태 반환 (%mfhouse%, %mfhouse_status%, %mfhouse_land%, %mfhouse_current%, %mfhouse_info% 등)
        // 마을땅: 빨간색 "구매 불가" (§c구매 불가)
        // 빈 땅: 초록색 "구매가능" (§a구매가능)
        // 누군가의 땅: 검은색 "000의 땅" (§0000의 땅)
        if (isVillageLand) {
            return "§c구매 불가";
        } else if (isEmptyLand) {
            return "§a구매가능";
        } else {
            return "§0" + ownerName + "의 땅";
        }
    }
}
