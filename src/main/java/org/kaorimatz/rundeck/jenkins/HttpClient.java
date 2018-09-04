package org.kaorimatz.rundeck.jenkins;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public interface HttpClient {

    CloseableHttpResponse execute(HttpUriRequest request) throws IOException;
}
