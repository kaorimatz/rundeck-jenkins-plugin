package org.kaorimatz.rundeck.jenkins;

public class LogText {

    private final String content;

    private final long position;

    private final boolean complete;

    public LogText(String content, long position, boolean complete) {
        this.content = content;
        this.position = position;
        this.complete = complete;
    }

    public String getContent() {
        return content;
    }

    public long getPosition() {
        return position;
    }

    public boolean isComplete() {
        return complete;
    }
}
