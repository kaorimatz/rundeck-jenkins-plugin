package org.kaorimatz.rundeck.jenkins;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.plugins.PluginLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class DefaultConsoleOutputLogger implements ConsoleOutputLogger {

    private final PluginLogger pluginLogger;

    public DefaultConsoleOutputLogger(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    @Override
    public void log(String consoleOutput) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(consoleOutput))) {
            String line;
            while ((line = reader.readLine()) != null) {
                pluginLogger.log(Constants.INFO_LEVEL, line);
            }
        }
    }
}
