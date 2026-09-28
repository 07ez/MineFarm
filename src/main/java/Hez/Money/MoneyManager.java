package Hez.Money;

import Hez.MineFarm.OpGetMassage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyManager {

    private static final Map<UUID, Integer> moneyData = new ConcurrentHashMap<>();
    private static OpGetMassage opGetMassage = new OpGetMassage();

    /// 플레이어의 잔액을 확인해주는 함수
    /// @param uuid 확인할 플레이어
    /// @return 현재 잔액
    public static int getMoney(UUID uuid) {
        return moneyData.getOrDefault(uuid, 0);
    }

    /// 플레이어 잔액 설정해주는 함수
    /// @param uuid 설정할 플레이어
    /// @param amount 설정할 잔액
    public static boolean setMoney(UUID uuid, int amount) {
        if (amount < 0) return false; // 돈을 음수로 설정 방지
        moneyData.put(uuid, amount);

        // 메세지
        opGetMassage.fromOp(uuid + "님의 돈을 " + amount + "로 설정하였습니다.");
        return true;
    }

    /// 플레이어 잔액 추가해주는 함수
    /// @param uuid 추가할 플레이어
    /// @param amount 추가할 잔액
    public static boolean addMoney(UUID uuid, int amount) {
        if (amount <= 0) return false; // 음수나 0원 추가 방지

        // 돈 추가
        int current = getMoney(uuid);
        moneyData.put(uuid, current + amount);

        // 메세지
        opGetMassage.fromOp(uuid + "님의 돈을 " + amount + "을 추가하였습니다.");
        return true;
    }

    /// 플레이어 잔액 빼주는 함수
    /// @param uuid 뺄 플레이어
    /// @param amount 뺄 잔액
    /// @return 성공여부
    public static boolean removeMoney(UUID uuid, int amount) {
        if (amount <= 0) return false;  // 음수나 0원 빼기 방지

        // 돈 빼기
        int current = getMoney(uuid);
        if (current - amount < 0) return false; // 대출 안됌
        else {
            moneyData.put(uuid, current - amount);

            // 메세지
            opGetMassage.fromOp(uuid + "님의 돈을 " + amount + "을 감소하였습니다.");
            return true;
        }
    }

    // 돈 저장
    public void saveData(FileConfiguration config){
        for (Map.Entry<UUID, Integer> entry : moneyData.entrySet()) {
            config.set("money. " + entry.getKey().toString(), entry.getValue());
        }
    }

    // 돈 불러오기
    public void LoadData(FileConfiguration config) {
        // 예외처리
        if (!config.contains("money")) return;

        moneyData.clear();

        // "data" 하위 섹션이 존재하는지 확인
        ConfigurationSection section = config.getConfigurationSection("data");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key); // UUID 규격 검증
                int amount = section.getInt(key);
                moneyData.put(uuid, amount);
            } catch (IllegalArgumentException e) {
                // UUID 문자열이 아닌 Key는 무시하고 로그 남김 (에러로 인한 플러그인 멈춤 방지)
                System.err.println("[MineFarm] 올바르지 않은 UUID 키 무시됨: " + key);
            }
        }
    }
}
