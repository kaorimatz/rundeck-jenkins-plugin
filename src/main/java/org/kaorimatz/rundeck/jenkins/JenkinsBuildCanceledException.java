package org.kaorimatz.rundeck.jenkins;

public class JenkinsBuildCanceledException extends Exception {

    public JenkinsBuildCanceledException(String message) {
        super(message);
    }
}
