package Hez.Job;

public class Job {
    private final JobType jobType;
    private int level;

    public Job(JobType jobType, int level) {
        this.jobType = jobType;
        this.level = level;
    }


    public JobType getJobType(){
        return jobType;
    }

    public int getLevel() {
        return level;
    }

    public void SetLevel(int lev) {
        level = lev;
    }
}
