package Hez.Job;

import Hez.MineFarm.OpGetMassage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class JobManager {

    private static final Map<UUID, List<Job>> playerJobs = new HashMap<>();

    // 메인직업 불러오기
    public static String GetPrimaryJobName(UUID uuid){
        List<Job> jobs = playerJobs.get(uuid);
        // 직업이 없다면 직업없음
        if (jobs == null || jobs.isEmpty()) return JobType.none.getKoreanName();

        for (Job job : jobs){
            // 메인직업 이라면
            if (job.getJobType().isPrimary()){
                return job.getJobType().getKoreanName();
            }
        }

        // 서브직업 밖에 없다면
        return JobType.none.getKoreanName();
    }

    /// 직업 레벨 확인하기
    /// @param uuid 확인할 플레이어
    /// @param type 확인할 직업
    /// @return
    public static int GetJobLevel(UUID uuid, JobType type){
        List<Job> jobs = playerJobs.get(uuid);
        for (Job job : jobs){
            if (job.getJobType() == type){
                return job.getLevel();
            }
        }
        return -1; // 없으면 -1
    }

    /// 플레이어가 직업 가지고 있나 확인하기
    /// @param uuid 확인할 플레이어
    /// @param type 확인할 직업
    /// @return
    public static boolean HasJob(UUID uuid, JobType type) {
        List<Job> jobs = playerJobs.get(uuid);
        for (Job job : jobs){
            if (job.getJobType() == type){
                return true; // 있으면 true
            }
        }
        return false; // 없으면 false
    }

    /// 무슨 직업이 있는지 확인하는 변수
    /// @param uuid 확인할 플레이어
    public static List<JobType> CheckJob(UUID uuid) {
        List<Job> jobs = playerJobs.get(uuid);
        List<JobType> types = new ArrayList<>();

        if (jobs != null) {
            for (Job job : jobs) {
                types.add(job.getJobType());
            }
        }

        return types;
    }
    /// 직업 추가
    /// @param jobType 추가할 직업
    /// @param uuid 추가할 플레이어
    public static void AddJob(JobType jobType, UUID uuid){
        List<Job> jobs = playerJobs.computeIfAbsent(uuid, k -> new ArrayList<>());

        // 직업이 곂친경우
        boolean hasJob = jobs.stream().anyMatch(j -> j.getJobType() == jobType);

        // 메인직업을 추가하는 경우 기존 메인직업 삭제
        jobs.removeIf(job -> job.getJobType().isPrimary());


        // 직업추가
        if (!hasJob)
            jobs.add(new Job(jobType, 0));
        else{
            OpGetMassage.fromOp("이미 " +  uuid + "님은 " + jobType + "직업을 가지고 있습니다.");
        }

        OpGetMassage.fromOp(uuid + "에게" + jobType + "직업을 추가했습니다.");
    }

    /// 직업 삭제
    /// @param jobType 삭제할 직업
    /// @param uuid 삭제할 플레이어
    public static void RemoveJob(JobType jobType, UUID uuid) {
        List<Job> jobs = playerJobs.computeIfAbsent(uuid, k -> new ArrayList<>());

        // 직업이 있는경우 삭제
        jobs.removeIf(job -> job.getJobType() == jobType);

        OpGetMassage.fromOp(uuid + "에게" + jobType + "직업을 삭제했습니다.");
    }
}
