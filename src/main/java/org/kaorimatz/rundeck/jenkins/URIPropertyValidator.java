package org.kaorimatz.rundeck.jenkins;

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.net.URI;
import java.net.URISyntaxException;

public class URIPropertyValidator implements PropertyValidator {

    @Override
    public boolean isValid(String value) throws ValidationException {
        try {
            new URI(value);
        } catch (URISyntaxException e) {
            throw new ValidationException("Invalid URL: " + e.getMessage(), e);
        }
        return true;
    }
}
