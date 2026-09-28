package Hez.Command;

import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {

    private final Main plugin;
    MoneyManager moneyManager;
    public MoneyCommand(Main plugin) {
        this.plugin = plugin;
        moneyManager = plugin.getMoneyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {// 1. /돈 (자신 잔액 조회)
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // money help
        if (args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        // 돈 잔액
        if (args[0].equalsIgnoreCase("잔액")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c콘솔에서는 잔액을 조회할 수 없습니다. /money help을 입력하세요.");
                return true;
            }
            long balance = moneyManager.getMoney(player.getUniqueId());
            player.sendMessage("§f현재 소지금: §e" + String.format("%,d", balance) + "원");
            return true;
        }

        // money give <플레이어> <금액>
        if (args[0].equalsIgnoreCase("give")) {
            if (!checkAdminPermission(sender)) return true;
            if (args.length < 3) {
                sender.sendMessage("§c[사용법] /money give <플레이어> <금액>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount = parseAmount(sender, args[2]);
            // 돈 지급
            boolean success = moneyManager.addMoney(target.getUniqueId(), amount);
            if(!success){
                sender.sendMessage("§c[오류] 해당플레이어에게 음수의 돈을 입금할수 없습니다.");
            }
            return true;
        }

        // money take <플레이어> <금액>
        if (args[0].equalsIgnoreCase("take")) {
            if (!checkAdminPermission(sender)) return true;

            if (args.length < 3) {
                sender.sendMessage("§c[사용법] /money take <플레이어> <금액>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount = parseAmount(sender, args[2]);

            // 돈 차감
            boolean success = moneyManager.removeMoney(target.getUniqueId(), amount);
            if (!success) {
                sender.sendMessage("§c[오류] 해당 플레이어의 잔액이 부족하거나 음수로 차감할 수 없습니다.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage("§c[사용법] /money set <플레이어> <금액>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount = parseAmount(sender, args[2]);

            boolean success = moneyManager.setMoney(target.getUniqueId(), amount);
            if (!success) {
                sender.sendMessage("§c[오류] 해당 플레이어의 잔액을 음수로 설정할수 없습니다.");
            }
            return true;
        }

        // 알 수 없는 서브 명령어 입력 시
        sendHelpMessage(sender);
        return true;
    }

    // --- [ 헬퍼 메서드 ] ---

    // 도움말 출력
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§e=== [ 돈 명령어 도움말 ] ===");
        sender.sendMessage("§f/money : 돈 도움말 확인");
        sender.sendMessage("§f/money 잔액: 돈 잔액 확인");
        if (sender.isOp()) {
            sender.sendMessage("§f/money give <유저> <금액> : 플레이어에게 돈 지급");
            sender.sendMessage("§f/money take <유저> <금액> : 플레이어의 돈 차감");
        }
    }

    // 관리자 권한 체크
    private boolean checkAdminPermission(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return false;
        }
        return true;
    }

    // 입력 문자열을 숫자(int)로 안전하게 변환
    private int parseAmount(CommandSender sender, String input) {
        try {
            int amount = Integer.parseInt(input);
            if (amount <= 0) {
                sender.sendMessage("§c[오류] 금액은 1원 이상이어야 합니다.");
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[오류] 올바른 숫자를 입력해주세요.");
            return -1;
        }
    }
}