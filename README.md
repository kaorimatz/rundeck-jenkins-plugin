# rundeck-jenkins-plugin

A rundeck plugin to build Jenkins jobs.

## Configurations

### Workflow Step

```yaml
- configuration:
    apiTokenPath: keys/path/to/api_token
    authorizationTokenPath: keys/path/to/authorization_token
    baseUrl: https://example.com/path/to/jenkins
    failureThreshold: FAILURE
    followConsoleOutput: 'true'
    jobName: foo
    logConsoleOutput: 'true'
    parameters: |-
      foo=${option.foo}
      bar=${option.bar}
    pollInterval: '10'
    userId: foo
    waitForBuildToFinish: 'true'
  nodeStep: false
  type: jenkins-build
```

### Notification

```properties
project.plugin.Notification.jenkins-webhook.apiToken=xxx
project.plugin.Notification.jenkins-webhook.baseUrl=https://example.com/path/to/jenkins
project.plugin.Notification.jenkins-webhook.userId=foo
```

## Development

### Build

    ./gradlew jar
