package org.kaorimatz.rundeck.jenkins;

public enum Result {
    SUCCESS, UNSTABLE, FAILURE, NOT_BUILT, ABORTED;

    public boolean isWorseOrEqualTo(Result that) {
        return this.ordinal() >= that.ordinal();
    }
}
