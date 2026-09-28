package Hez.Placeholder;

import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class MoneyExpansion extends PlaceholderExpansion {

    MoneyManager moneyManager = Main.getInstance().getMoneyManager();

    @Override
    public String getIdentifier(){
        return "mfmoney";
    }

    @Override
    public String getAuthor(){
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
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        // %mfmoney_balance% : 단순 숫자 반환 (예: 15000)
        if (params.equalsIgnoreCase("balance")) {
            int money = moneyManager.getMoney(player.getUniqueId());
            return String.valueOf(money);
        }

        // %mfmoney_formatted% : 콤마가 포함된 예쁜 금액 반환 (예: 15,000)
        if (params.equalsIgnoreCase("formatted")) {
            int money = moneyManager.getMoney(player.getUniqueId());
            return String.format("%,d", money);
        }

        return null; // 정의되지 않은 플레이스홀더일 경우
    }
}
