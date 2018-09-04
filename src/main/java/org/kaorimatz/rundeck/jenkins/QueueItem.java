package org.kaorimatz.rundeck.jenkins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueItem {

    private final Executable executable;

    private final boolean cancelled;

    @JsonCreator
    public QueueItem(@JsonProperty("executable") Executable executable, @JsonProperty("cancelled") boolean cancelled) {
        this.executable = executable;
        this.cancelled = cancelled;
    }

    public Executable getExecutable() {
        return executable;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
