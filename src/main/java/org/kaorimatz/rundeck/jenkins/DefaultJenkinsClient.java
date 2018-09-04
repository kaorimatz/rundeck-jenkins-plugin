package org.kaorimatz.rundeck.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultJenkinsClient implements JenkinsClient {

    private static final Pattern QUEUE_ITEM_PATH_PATTERN = Pattern.compile("/queue/item/(\\d+)/$");

    private final HttpClient httpClient;

    private final URI baseUri;

    public DefaultJenkinsClient(HttpClient httpClient, URI baseUri) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
    }

    @Override
    public long build(String jobName, Map<String, String> parameters, String token) throws JenkinsClientException, EncoderException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        if (parameters.isEmpty()) {
            uriBuilder.setPath(String.format("%s%s/build", uriBuilder.getPath(), toJobPath(jobName)));
        } else {
            uriBuilder.setPath(String.format("%s%s/buildWithParameters", uriBuilder.getPath(), toJobPath(jobName)));
        }
        if (token != null) {
            uriBuilder.addParameter("token", token);
        }
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            uriBuilder.addParameter(parameter.getKey(), parameter.getValue());
        }
        try (CloseableHttpResponse response = post(uriBuilder.toString(), HttpStatus.SC_CREATED)) {
            Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
            if (locationHeader == null) {
                throw new JenkinsClientException("No HTTP Location header");
            }
            return extractQueueItemIdFromLocation(locationHeader.getValue());
        }
    }

    private String toJobPath(String jobName) throws EncoderException {
        StringBuilder builder = new StringBuilder();
        URLCodec codec = new URLCodec();
        for (String segment : jobName.split("/")) {
            builder.append("/job/");
            builder.append(codec.encode(segment));
        }
        return builder.toString();
    }

    private CloseableHttpResponse post(String uri, int expectedStatusCode) throws JenkinsClientException, IOException {
        return execute(new HttpPost(uri), expectedStatusCode);
    }

    private CloseableHttpResponse execute(HttpUriRequest request, int expectedStatusCode) throws JenkinsClientException, IOException {
        CloseableHttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != expectedStatusCode) {
            String body = EntityUtils.toString(response.getEntity());
            String message = String.format("Unexpected response status code. statusCode=%d, body=%s", statusCode, body);
            throw new JenkinsClientException(message);
        }
        return response;
    }

    private long extractQueueItemIdFromLocation(String location) throws JenkinsClientException {
        URI uri;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            String message = String.format("Invalid HTTP Location header value. location=%s", location);
            throw new JenkinsClientException(message, e);
        }
        Matcher matcher = QUEUE_ITEM_PATH_PATTERN.matcher(uri.getPath());
        if (!matcher.matches()) {
            String message = String.format("Unable to extract queue item ID. location=%s", location);
            throw new JenkinsClientException(message);
        }
        return Integer.parseInt(matcher.group(1));
    }

    @Override
    public void cancelQueueItem(long queueItemId) throws JenkinsClientException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        uriBuilder.setPath(String.format("%s/queue/cancelItem", uriBuilder.getPath()));
        uriBuilder.setParameter("id", String.valueOf(queueItemId));
        post(uriBuilder.toString(), HttpStatus.SC_MOVED_TEMPORARILY);
    }

    @Override
    public void deliver(Document document) throws JenkinsClientException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        uriBuilder.setPath(String.format("%s/plugin/rundeck/webhook/", uriBuilder.getPath()));
        post(uriBuilder.toString(), document, HttpStatus.SC_OK);
    }

    @Override
    public Build getBuild(String jobName, int buildNumber) throws JenkinsClientException, EncoderException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        uriBuilder.setPath(String.format("%s%s/%d/api/json", uriBuilder.getPath(), toJobPath(jobName), buildNumber));
        return get(uriBuilder.toString(), Build.class);
    }

    private <T> T get(String uri, Class<T> responseClass) throws JenkinsClientException, IOException {
        try (CloseableHttpResponse response = get(uri)) {
            return new ObjectMapper().readValue(response.getEntity().getContent(), responseClass);
        }
    }

    private CloseableHttpResponse get(String uri) throws JenkinsClientException, IOException {
        return execute(new HttpGet(uri), HttpStatus.SC_OK);
    }

    @Override
    public LogText getLogText(String jobName, int buildNumber, long start) throws JenkinsClientException, EncoderException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        uriBuilder.setPath(String.format("%s%s/%d/logText/progressiveText", uriBuilder.getPath(), toJobPath(jobName), buildNumber));
        uriBuilder.addParameter("start", String.valueOf(start));
        try (CloseableHttpResponse response = get(uriBuilder.toString())) {
            Header positionHeader = response.getFirstHeader("X-Text-Size");
            if (positionHeader == null) {
                throw new JenkinsClientException("No X-Text-Size header");
            }
            String content = EntityUtils.toString(response.getEntity());
            long position = Long.parseLong(positionHeader.getValue());
            boolean complete = !response.containsHeader("X-More-Data");
            return new LogText(content, position, complete);
        }
    }

    @Override
    public QueueItem getQueueItem(long queueItemId) throws JenkinsClientException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        uriBuilder.setPath(String.format("%s/queue/item/%d/api/json", baseUri.getPath(), queueItemId));
        return get(uriBuilder.toString(), QueueItem.class);
    }

    @Override
    public void stopBuild(String jobName, int buildNumber) throws JenkinsClientException, EncoderException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUri);
        uriBuilder.setPath(String.format("%s%s/%d/stop", uriBuilder.getPath(), toJobPath(jobName), buildNumber));
        post(uriBuilder.toString(), HttpStatus.SC_MOVED_TEMPORARILY);
    }

    private void post(String uri, Document document, int expectedStatusCode) throws JenkinsClientException, IOException {
        HttpPost request = new HttpPost(uri);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter(outputStream, OutputFormat.createCompactFormat());
        writer.write(document);
        request.setEntity(new ByteArrayEntity(outputStream.toByteArray()));
        execute(request, expectedStatusCode);
    }
}
