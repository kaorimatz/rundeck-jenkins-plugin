package org.kaorimatz.rundeck.jenkins;

import java.io.IOException;

public interface ConsoleOutputLogger {

    void log(String consoleOutput) throws IOException;
}
