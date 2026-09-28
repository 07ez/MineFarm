package Hez.Command;

import Hez.Job.JobManager;
import Hez.Job.JobType;
import Hez.MineFarm.Main;
import Hez.Money.MoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class JobCommand implements CommandExecutor {
    private final Main plugin;

    JobManager jobManager;

    public JobCommand(Main plugin) {
        this.plugin = plugin;
        jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // 직업 도움말
        if (args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        // job check <플레이어>
        if (args[0].equalsIgnoreCase("check")) {
            if (!checkAdminPermission(sender)) return true;

            if (args.length < 2) {
                sender.sendMessage("§c[사용법] /job check <플레이어>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            List<JobType> types = jobManager.CheckJob(target.getUniqueId());

            sender.sendMessage(target + "님의 직업");
            for (JobType type : types) {
                sender.sendMessage(type.toString());
            }

            return true;
        }

        // job add <플레이어> <직업>
        if (args[0].equalsIgnoreCase("add")) {
            if (!checkAdminPermission(sender)) return true;

            if (args.length < 3) {
                sender.sendMessage("§c[사용법] /job add <플레이어> <직업>");
                return true;
            }

            // 직업 추가
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            JobType type = JobType.fromString(args[2]);
            jobManager.AddJob(type, target.getUniqueId());

            return true;
        }

        // job remove <플레이어> <직업>
        if (args[0].equalsIgnoreCase("remove")) {
            if (!checkAdminPermission(sender)) return true;

            if (args.length < 3) {
                sender.sendMessage("§c[사용법] /job remove <플레이어> <직업>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            JobType type = JobType.fromString(args[2]);
            jobManager.RemoveJob(type, target.getUniqueId());

            return true;
        }

        // 알 수 없는 서브 명령어 입력 시
        sendHelpMessage(sender);
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§e=== [ 직업 명령어 도움말 ] ===");
        sender.sendMessage("§f/직업 : 직업 도움말 확인");
        if (sender.isOp()) {
            sender.sendMessage("§f/job add <유저> <직업> : 플레이어에게 직업 지급");
            sender.sendMessage("§f/job remove <유저> <직업> : 플레이어의 직업 빼기");
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
}
