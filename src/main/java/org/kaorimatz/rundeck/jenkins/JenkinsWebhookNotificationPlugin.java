package org.kaorimatz.rundeck.jenkins;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.Password;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Plugin(name = "jenkins-webhook", service = ServiceNameConstants.Notification)
@PluginDescription(title = "Jenkins Webhook", description = "Send execution events to Jenkins webhook to trigger builds")
public class JenkinsWebhookNotificationPlugin implements NotificationPlugin {

    private static final Logger logger = Logger.getLogger(JenkinsWebhookNotificationPlugin.class);

    @PluginProperty(title = "Jenkins Base URL", description = "The base URL of Jenkins", scope = PropertyScope.Project, validatorClass = URIPropertyValidator.class)
    private String baseUrl;

    @PluginProperty(title = "User ID", description = "The ID of the user to access Jenkins", scope = PropertyScope.Project)
    private String userId;

    @PluginProperty(title = "API token", description = "The API token to access Jenkins", scope = PropertyScope.Project)
    @Password
    private String apiToken;

    @Override
    public boolean postNotification(String trigger, Map executionData, Map config) {
        if (StringUtils.isBlank(baseUrl)) {
            logger.error("baseUrl is required");
            return false;
        }

        JenkinsClient jenkinsClient;
        try {
            jenkinsClient = new DefaultJenkinsClientBuilder(baseUrl)
                    .setUserId(StringUtils.trimToNull(userId))
                    .setApiToken(StringUtils.trimToNull(apiToken))
                    .build();
        } catch (URISyntaxException e) {
            logger.error(String.format("Invalid Jenkins base URL. baseUrl=%s", baseUrl), e);
            return false;
        }

        Document document = createNotificationDocument(executionData);

        try {
            jenkinsClient.deliver(document);
        } catch (IOException | JenkinsClientException e) {
            logger.error(String.format("Failed to deliver an event. trigger=%s", trigger), e);
            return false;
        }

        return true;
    }

    private Document createNotificationDocument(Map executionData) {
        Document document = DocumentHelper.createDocument();
        Element notificationElement = document.addElement("notification");
        Element executionsElement = notificationElement.addElement("executions");
        Element executionElement = executionsElement.addElement("execution");
        addAttribute(executionElement, "id", ObjectUtils.toString(executionData.get("id"), null));
        addAttribute(executionElement, "href", (String) executionData.get("href"));
        addAttribute(executionElement, "status", (String) executionData.get("status"));
        addAttribute(executionElement, "project", (String) executionData.get("project"));
        addElement(executionElement, "argstring", (String) executionData.get("argstring"));
        addElement(executionElement, "user", (String) executionData.get("user"));
        addElement(executionElement, "abortedby", (String) executionData.get("abortedby"));
        addElement(executionElement, "data-started", "unixtime", ObjectUtils.toString(executionData.get("dateStartedUnixtime")));
        addElement(executionElement, "date-ended", "unixtime", ObjectUtils.toString(executionData.get("dateEndedUnixtime")));
        Map jobData = (Map) executionData.get("job");
        Element jobElement = executionElement.addElement("job");
        addAttribute(jobElement, "id", ObjectUtils.toString(jobData.get("id"), null));
        addAttribute(jobElement, "averageDuration", ObjectUtils.toString(jobData.get("averageDuration")));
        addElement(jobElement, "name", (String) jobData.get("name"));
        addElement(jobElement, "description", (String) jobData.get("description"));
        addElement(jobElement, "group", (String) jobData.get("group"));
        addElement(jobElement, "project", (String) jobData.get("project"));
        return document;
    }

    private void addAttribute(Element element, String name, String value) {
        if (value != null) {
            element.addAttribute(name, value);
        }
    }

    private void addElement(Element element, String name, String text) {
        if (text != null) {
            element.addElement(name).addText(text);
        }
    }

    private void addElement(Element element, String name, String attributeName, String attributeValue) {
        if (attributeValue != null) {
            element.addElement(name).addAttribute(attributeName, attributeValue);
        }
    }
}
