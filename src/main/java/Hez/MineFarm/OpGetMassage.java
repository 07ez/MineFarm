package Hez.MineFarm;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class OpGetMassage {
    public static void fromOp(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage("§c[!] §f" + message);
            }
        }
    }
}
