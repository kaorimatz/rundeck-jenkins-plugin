package org.kaorimatz.rundeck.jenkins;

import org.apache.commons.codec.EncoderException;
import org.dom4j.Document;

import java.io.IOException;
import java.util.Map;

public interface JenkinsClient {

    long build(String jobName, Map<String, String> parameters, String token) throws JenkinsClientException, EncoderException, IOException;

    void cancelQueueItem(long queueItemId) throws JenkinsClientException, IOException;

    void deliver(Document document) throws JenkinsClientException, IOException;

    Build getBuild(String jobName, int buildNumber) throws JenkinsClientException, EncoderException, IOException;

    LogText getLogText(String jobName, int buildNumber, long start) throws JenkinsClientException, EncoderException, IOException;

    QueueItem getQueueItem(long queueItemId) throws JenkinsClientException, IOException;

    void stopBuild(String jobName, int buildNumber) throws JenkinsClientException, EncoderException, IOException;
}
