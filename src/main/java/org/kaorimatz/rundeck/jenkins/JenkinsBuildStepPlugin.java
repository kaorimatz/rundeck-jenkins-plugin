package org.kaorimatz.rundeck.jenkins;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.lang.StringUtils;
import org.rundeck.storage.api.StorageException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Plugin(name = "jenkins-build", service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Jenkins Build", description = "Build a Jenkins job")
public class JenkinsBuildStepPlugin implements StepPlugin {

    @PluginProperty(title = "Job name", description = "The name of the Jenkins job to build", required = true, scope = PropertyScope.Instance)
    private String jobName;

    @PluginProperty(title = "Parameters", description = "The parameters for the build", scope = PropertyScope.Instance, validatorClass = PropertiesPropertyValidator.class)
    @RenderingOptions({
            @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = "CODE"),
            @RenderingOption(key = StringRenderingConstants.CODE_SYNTAX_MODE, value = "properties")
    })
    private String parameters;

    @PluginProperty(title = "Key storage path for authorization token", description = "The key storage path for the authorization token to trigger the build", scope = PropertyScope.Instance)
    @RenderingOptions({
            @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = "STORAGE_PATH"),
            @RenderingOption(key = StringRenderingConstants.STORAGE_PATH_ROOT_KEY, value = "keys"),
            @RenderingOption(key = StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, value = "Rundeck-data-type=password"),
    })
    private String authorizationTokenPath;

    @PluginProperty(title = "Wait for the triggered build to finish", scope = PropertyScope.Instance)
    private boolean waitForBuildToFinish;

    @PluginProperty(title = "Poll interval (seconds)", description = "The interval to wait between polling the build until it finishes", defaultValue = "10", scope = PropertyScope.Instance)
    private int pollInterval;

    @PluginProperty(title = "Log the console output of the triggered build", scope = PropertyScope.Instance)
    private boolean logConsoleOutput;

    @PluginProperty(title = "Follow the console output of the triggered build", scope = PropertyScope.Instance)
    private boolean followConsoleOutput;

    @PluginProperty(title = "Failure threshold", description = "Fail the step if the build result is worse or equal to this", scope = PropertyScope.Instance)
    @SelectValues(values = {"SUCCESS", "UNSTABLE", "FAILURE"})
    private String failureThreshold;

    @PluginProperty(title = "Jenkins base URL", description = "The base URL of Jenkins", scope = PropertyScope.Instance, validatorClass = URIPropertyValidator.class)
    @RenderingOption(key = StringRenderingConstants.GROUPING, value = "secondary")
    private String baseUrl;

    @PluginProperty(title = "User ID", description = "The ID of the user to access Jenkins", scope = PropertyScope.Instance)
    @RenderingOption(key = StringRenderingConstants.GROUPING, value = "secondary")
    private String userId;

    @PluginProperty(title = "Key storage path for API token", description = "The key storage path for the API token to access Jenkins", scope = PropertyScope.Instance)
    @RenderingOptions({
            @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = "STORAGE_PATH"),
            @RenderingOption(key = StringRenderingConstants.STORAGE_PATH_ROOT_KEY, value = "keys"),
            @RenderingOption(key = StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, value = "Rundeck-data-type=password"),
            @RenderingOption(key = StringRenderingConstants.GROUPING, value = "secondary")
    })
    private String apiTokenPath;

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        JenkinsClient jenkinsClient = buildJenkinsClient(getApiToken(apiTokenPath, context));
        ConsoleOutputLogger logger = new DefaultConsoleOutputLogger(context.getLogger());
        JenkinsBuildExecutor executor = new JenkinsBuildExecutor(jenkinsClient, logger);
        Build build;
        try {
            build = executor.execute(jobName, parseParameters(parameters), getAuthorizationToken(authorizationTokenPath, context), waitForBuildToFinish, pollInterval, logConsoleOutput, followConsoleOutput);
        } catch (JenkinsBuildCanceledException e) {
            throw new StepException(e.getMessage(), e, JenkinsStepFailureReason.JenkinsBuildCanceled);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StepException(e.getMessage(), e, StepFailureReason.Interrupted);
        } catch (JenkinsClientException e) {
            throw new StepException(e.getMessage(), e, JenkinsStepFailureReason.JenkinsFailure);
        } catch (IOException e) {
            throw new StepException(e.getMessage(), e, StepFailureReason.IOFailure);
        } catch (EncoderException e) {
            throw new StepException(e.getMessage(), e, StepFailureReason.ConfigurationFailure);
        }
        if (build != null && failureThreshold != null && build.getResult().isWorseOrEqualTo(Result.valueOf(failureThreshold))) {
            String message = String.format("Build result is worse or equal to '%s'. result = %s", failureThreshold, build.getResult());
            throw new StepException(message, JenkinsStepFailureReason.JenkinsBuildFailure);
        }
    }

    private String getApiToken(String path, PluginStepContext context) throws StepException {
        try {
            return readValueFromKeyStorage(context, path);
        } catch (StorageException | IOException e) {
            String message = String.format("Failed to get API token from Key Storage, apiTokenPath=%s", path);
            throw new StepException(message, e, StepFailureReason.ConfigurationFailure);
        }
    }

    private String readValueFromKeyStorage(PluginStepContext context, String path) throws IOException {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        ResourceMeta contents = context.getExecutionContext().getStorageTree().getResource(path).getContents();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        contents.writeContent(stream);
        return stream.toString();
    }

    private JenkinsClient buildJenkinsClient(String apiToken) throws StepException {
        if (StringUtils.isBlank(baseUrl)) {
            throw new StepException("baseUrl is required", StepFailureReason.ConfigurationFailure);
        }
        try {
            return new DefaultJenkinsClientBuilder(baseUrl)
                    .setUserId(StringUtils.trimToNull(userId))
                    .setApiToken(StringUtils.trimToNull(apiToken))
                    .build();
        } catch (URISyntaxException e) {
            String message = String.format("Invalid Jenkins base URL. baseUrl=%s", baseUrl);
            throw new StepException(message, e, StepFailureReason.ConfigurationFailure);
        }
    }

    private String getAuthorizationToken(String path, PluginStepContext context) throws StepException {
        try {
            return readValueFromKeyStorage(context, path);
        } catch (StorageException | IOException e) {
            String message = String.format("Failed to get authorization token from Key Storage, authorizationTokenPath=%s", path);
            throw new StepException(message, e, StepFailureReason.ConfigurationFailure);
        }
    }

    private Map<String, String> parseParameters(String parametersString) throws StepException {
        if (StringUtils.isBlank(parametersString)) {
            return Collections.emptyMap();
        }
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(parametersString));
        } catch (IOException e) {
            String message = String.format("Unable to parse parameters. parameters=%s", parametersString);
            throw new StepException(message, e, StepFailureReason.ConfigurationFailure);
        }
        Map<String, String> parameters = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            parameters.put(key, properties.getProperty(key));
        }
        return parameters;
    }
}
