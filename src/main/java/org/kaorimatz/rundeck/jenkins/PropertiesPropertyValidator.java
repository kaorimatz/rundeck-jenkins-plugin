package org.kaorimatz.rundeck.jenkins;

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class PropertiesPropertyValidator implements PropertyValidator {

    @Override
    public boolean isValid(String value) throws ValidationException {
        try {
            new Properties().load(new StringReader(value));
        } catch (IOException | IllegalArgumentException e) {
            throw new ValidationException(e.getMessage(), e);
        }
        return true;
    }
}
