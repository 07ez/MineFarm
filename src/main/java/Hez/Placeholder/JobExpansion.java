package Hez.Placeholder;

import Hez.Job.JobManager;
import Hez.Job.JobType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class JobExpansion extends PlaceholderExpansion {

    @Override
    public String getIdentifier(){
        return "mfjob";
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
        if (player == null) return "없음";

        // 1. %mfjob_main% 또는 %mfjob_primary% -> 대표(메인) 직업 이름 반환 (예: "농부", "무직")
        if (params.equalsIgnoreCase("main") || params.equalsIgnoreCase("primary")) {
            return JobManager.GetPrimaryJobName(player.getUniqueId());
        }

        // 2. %mfjob_level_<직업명>% -> 레벨 동적 파싱 (일일이 if문 안 써도 됨)
        // 사용 예시: %mfjob_level_farmer%, %mfjob_level_miner%, %mfjob_level_농부%
        if (params.startsWith("level_")) {
            String jobInput = params.substring(6); // "level_" 뒤의 텍스트만 추출 (예: "farmer")
            JobType type = JobType.fromString(jobInput);

            int level = JobManager.GetJobLevel(player.getUniqueId(), type);
            return String.valueOf(level);
        }

        // %mfjob_has_<직업명>%
        if (params.startsWith("has_")) {
            String jobInput = params.substring(4); // "has_" 제거
            JobType type = JobType.fromString(jobInput);

            if (type != null && JobManager.HasJob(player.getUniqueId(), type)) {
                return "O";
            }
            return "X";
        }

        return null; // 정의되지 않은 인자일 경우 null 리턴
    }
}
