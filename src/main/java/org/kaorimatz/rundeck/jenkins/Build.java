package org.kaorimatz.rundeck.jenkins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Build {

    private final boolean building;

    private final Result result;

    @JsonCreator
    public Build(@JsonProperty("building") boolean building, @JsonProperty("result") Result result) {
        this.building = building;
        this.result = result;
    }

    public boolean isBuilding() {
        return building;
    }

    public Result getResult() {
        return result;
    }
}
