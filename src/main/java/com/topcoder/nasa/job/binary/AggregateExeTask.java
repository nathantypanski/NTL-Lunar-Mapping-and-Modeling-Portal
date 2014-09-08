package com.topcoder.nasa.job.binary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topcoder.nasa.job.LmmpJob;

public class AggregateExeTask implements ExeTask, ExeTaskCompletedListener {
    private static final Logger LOG = LoggerFactory.getLogger(AggregateExeTask.class);

    private List<ExeTask> list = new ArrayList<ExeTask>();

    private ExeTaskCompletedListener runCompleteListener;

    private Map<LmmpJob, AtomicInteger> jobIndexMap = new ConcurrentHashMap<LmmpJob, AtomicInteger>();

    public void addExecutableJobProcessor(ExeTask jobProcessor) {
        list.add(jobProcessor);
    }

    @Override
    public Process runTaskFor(LmmpJob lmmpJob) {
        LOG.info("Running aggregate executable chain for job uuid {}", lmmpJob.getUuid());

        jobIndexMap.put(lmmpJob, new AtomicInteger(0));

        return doRunFor(lmmpJob);
    }

    private Process doRunFor(LmmpJob lmmpJob) {
        Integer index = jobIndexMap.get(lmmpJob).get();

        ExeTask executableJobProcessor = list.get(index);

        executableJobProcessor.setRunCompleteListener(this);

        return executableJobProcessor.runTaskFor(lmmpJob);
    }

    @Override
    public void onTaskCompleted(LmmpJob lmmpJob) {
        Integer newIndex = jobIndexMap.get(lmmpJob).incrementAndGet();

        if (newIndex < list.size()) {
            doRunFor(lmmpJob);
            return;
        }

        LOG.info("All done for job uuid {}", lmmpJob.getUuid());
        runCompleteListener.onTaskCompleted(lmmpJob);
        jobIndexMap.remove(lmmpJob);
    }

    @Override
    public void setRunCompleteListener(ExeTaskCompletedListener runCompleteListener) {
        this.runCompleteListener = runCompleteListener;
    }
}
