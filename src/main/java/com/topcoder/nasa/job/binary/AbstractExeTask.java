package com.topcoder.nasa.job.binary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topcoder.nasa.job.LmmpJob;

/**
 * Defines a bunch of common functionality for a stereotypical implementaiton of {@link ExeTask}.
 *
 */
public abstract class AbstractExeTask implements ExeTask {
    private static final int MONITOR_PERIOD = 10000;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractExeTask.class);

    /**
     * We maintain a background thread primiarily to check for completion of an ExeTask.
     * <p/>
     * The thread serves a secondary purpose: to periodically consume from Process' OutputStreams
     * and ErrorStreams -- and swallow them. See <a href=
     * "http://docs.oracle.com/javase/7/docs/api/java/lang/ProcessBuilder.html" >ProcessBuilder</a>
     * javadoc.
     * 
     */
    private static Thread monitorThread;

    /**
     * The shell command to execute for this ExeTask.
     */
    private String command;

    /**
     * When this ExeTask has completed, inform this listener.
     */
    private ExeTaskCompletedListener runCompleteListener;

    /**
     * Map of active (running) Processes for the given LmmpJob.
     */
    private static Map<LmmpJob, Process> jobProcessMap = new ConcurrentHashMap<LmmpJob, Process>();

    /**
     * One-off singleton setup of the thread. Synchronize on the only object we know to be good at
     * initialization time - the Logger!
     */
    {
        synchronized (LOG) {
            if (monitorThread == null) {

                LOG.info("Spinning up thread to consume output/error streams for jobs");
                monitorThread = new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                // NOTE: this threads wakes up every
                                // MONITOR_PERIOD ms
                                Thread.sleep(MONITOR_PERIOD);
                            } catch (InterruptedException e) {
                            }

                            drainProcessStreams();
                        }
                    }

                });

                monitorThread.start();
            }
        }
    }

    // =========================================================================

    public AbstractExeTask(String command) {
        this.command = command;
    }

    // =========================================================================

    public Process runTaskFor(LmmpJob lmmpJob) {
        try {
            return doRunTaskFor(lmmpJob);
        } catch (Exception e) {
            throw new IllegalStateException("Exception while running command " + getCommand(), e);
        }
    }

    private Process doRunTaskFor(LmmpJob lmmpJob) throws Exception, IOException {
        // ask concrete implementation for all the args
        List<String> args = new ArrayList<String>(getArgsFor(lmmpJob));

        args.add(0, command);

        LOG.info("Launching command for job uuid {}: {}", lmmpJob.getUuid(), args);

        Process process = new ProcessBuilder(args.toArray(new String[0])).start();

        jobProcessMap.put(lmmpJob, process);

        return process;
    }

    // =========================================================================

    /**
     * Implementations are expected to provide all the arguments for this command.
     */
    protected abstract List<String> getArgsFor(LmmpJob lmmpJob) throws Exception;

    // =========================================================================

    private void drainProcessStreams() {
        // for each process that's running...
        for (Iterator<Entry<LmmpJob, Process>> it = jobProcessMap.entrySet().iterator(); it
                .hasNext();) {
            Entry<LmmpJob, Process> entry = it.next();

            LmmpJob job = entry.getKey();
            Process process = entry.getValue();

            // ...if it has finished - remove from the active map and continue
            if (Processes.hasFinished(process)) {
                LOG.info("LmmpJob UUID {}, command {} completed", job.getUuid(), command);
                it.remove();

                runCompleteListener.onTaskCompleted(job);
                continue;
            }

            // ...otherwise drain the streams to a buffer and do nothing with
            // their contents.
            drainStream("stdout", process.getInputStream());
            drainStream("stderr", process.getErrorStream());
        }
    }

    private void drainStream(String streamName, InputStream stream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                LOG.info("{}: {}", streamName, line);
            }

            bufferedReader.close();

        } catch (IOException e) {
            LOG.error("Exception while draining stream?", e);
        }
    }

    // =========================================================================

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setRunCompleteListener(ExeTaskCompletedListener runCompleteListener) {
        this.runCompleteListener = runCompleteListener;
    }
}
