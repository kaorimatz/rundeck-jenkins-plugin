package org.kaorimatz.rundeck.jenkins;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

public enum JenkinsStepFailureReason implements FailureReason {
    JenkinsBuildCanceled,
    JenkinsBuildFailure,
    JenkinsFailure,
}
