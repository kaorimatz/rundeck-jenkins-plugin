package org.kaorimatz.rundeck.jenkins;

import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Map;

public class JenkinsBuildExecutor {

    private final JenkinsClient jenkinsClient;

    private final ConsoleOutputLogger consoleOutputLogger;

    public JenkinsBuildExecutor(JenkinsClient jenkinsClient, ConsoleOutputLogger consoleOutputLogger) {
        this.jenkinsClient = jenkinsClient;
        this.consoleOutputLogger = consoleOutputLogger;
    }

    public Build execute(String jobName, Map<String, String> parameters, String token, boolean waitForBuildToFinish, int pollInterval, boolean logConsoleOutput, boolean followConsoleOutput)
            throws JenkinsBuildCanceledException, JenkinsClientException, EncoderException, InterruptedException, IOException {

        long queueItemId = jenkinsClient.build(jobName, parameters, token);
        if (!waitForBuildToFinish) {
            return null;
        }
        int buildNumber = waitForBuildToBeCreated(jobName, queueItemId, pollInterval);

        if (followConsoleOutput) {
            followConsoleOutput(jobName, buildNumber, pollInterval);
            return jenkinsClient.getBuild(jobName, buildNumber);
        } else {
            Build build = waitForBuildToFinish(jobName, buildNumber, pollInterval);
            if (logConsoleOutput) {
                followConsoleOutput(jobName, buildNumber, pollInterval);
            }
            return build;
        }
    }

    private int waitForBuildToBeCreated(String jobName, long queueItemId, int pollInterval) throws JenkinsBuildCanceledException, JenkinsClientException, InterruptedException, IOException {
        while (true) {
            try {
                QueueItem queueItem = jenkinsClient.getQueueItem(queueItemId);
                if (queueItem.isCancelled()) {
                    String message = String.format("Job %s has been canceled", jobName);
                    throw new JenkinsBuildCanceledException(message);
                }
                Executable executable = queueItem.getExecutable();
                if (executable != null) {
                    return executable.getNumber();
                }
                Thread.sleep(pollInterval * 1000);
            } catch (InterruptedIOException | InterruptedException e) {
                jenkinsClient.cancelQueueItem(queueItemId);
                throw e;
            }
        }
    }

    private Build waitForBuildToFinish(String jobName, int buildNumber, int pollInterval) throws InterruptedException, JenkinsClientException, EncoderException, IOException {
        while (true) {
            try {
                Build build = jenkinsClient.getBuild(jobName, buildNumber);
                if (!build.isBuilding()) {
                    return build;
                }
                Thread.sleep(pollInterval * 1000);
            } catch (InterruptedIOException | InterruptedException e) {
                jenkinsClient.stopBuild(jobName, buildNumber);
                throw e;
            }
        }
    }

    private void followConsoleOutput(String jobName, int buildNumber, int pollInterval) throws InterruptedException, JenkinsClientException, EncoderException, IOException {
        long position = 0;
        while (true) {
            try {
                LogText logText = jenkinsClient.getLogText(jobName, buildNumber, position);
                consoleOutputLogger.log(logText.getContent());
                if (logText.isComplete()) {
                    return;
                }
                position = logText.getPosition();
                Thread.sleep(pollInterval * 1000);
            } catch (InterruptedIOException | InterruptedException e) {
                jenkinsClient.stopBuild(jobName, buildNumber);
                throw e;
            }
        }
    }
}