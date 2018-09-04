package org.kaorimatz.rundeck.jenkins;

public class JenkinsClientException extends Exception {

    public JenkinsClientException(String message) {
        super(message);
    }

    public JenkinsClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
