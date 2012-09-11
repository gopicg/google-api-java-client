/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.googleapis.services;

import com.google.api.client.googleapis.MethodOverride;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.subscriptions.SubscriptionManager;
import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.ObjectParser;
import com.google.common.base.Preconditions;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Abstract thread-safe Google client.
 *
 * @since 1.12
 * @author Yaniv Inbar
 */
public abstract class AbstractGoogleClient {

  static final Logger LOGGER = Logger.getLogger(AbstractGoogleClient.class.getName());

  /** The request factory for connections to the server. */
  private final HttpRequestFactory requestFactory;

  /**
   * Initializer to use when creating an {@link AbstractGoogleClientRequest} or {@code null} for
   * none.
   */
  private final GoogleClientRequestInitializer googleClientRequestInitializer;

  /**
   * Root URL of the service, for example {@code "https://www.googleapis.com/"}. Must be URL-encoded
   * and must end with a "/".
   */
  private final String rootUrl;

  /**
   * Service path, for example {@code "tasks/v1/"}. Must be URL-encoded and must end with a "/".
   */
  private final String servicePath;

  /**
   * Application name to be sent in the User-Agent header of each request or {@code null} for none.
   */
  private final String applicationName;

  /** Object parser or {@code null} for none. */
  private final ObjectParser objectParser;

  /** Subscription manager. */
  private SubscriptionManager subscriptionManager;

  /** Whether discovery pattern checks should be suppressed on required parameters. */
  private boolean suppressPatternChecks;

  /**
   * Constructor with required parameters.
   *
   * <p>
   * Use {@link Builder} if you need to specify any of the optional parameters.
   * </p>
   *
   * @param transport HTTP transport
   * @param httpRequestInitializer HTTP request initializer or {@code null} for none
   * @param rootUrl root URL of the service
   * @param servicePath service path
   * @param objectParser object parser
   */
  protected AbstractGoogleClient(HttpTransport transport,
      HttpRequestInitializer httpRequestInitializer, String rootUrl, String servicePath,
      ObjectParser objectParser) {
    this(transport, httpRequestInitializer, rootUrl, servicePath, objectParser, null, null, null,
        false);
  }

  /**
   * @param transport HTTP transport
   * @param httpRequestInitializer HTTP request initializer or {@code null} for none
   * @param rootUrl root URL of the service
   * @param servicePath service path
   * @param objectParser object parser or {@code null} for none
   * @param googleClientRequestInitializer Google request initializer or {@code null} for none
   * @param applicationName application name to be sent in the User-Agent header of requests or
   *        {@code null} for none
   * @param subscriptionManager subscription manager
   * @param suppressPatternChecks whether discovery pattern checks should be suppressed on required
   *        parameters
   */
  protected AbstractGoogleClient(HttpTransport transport,
      HttpRequestInitializer httpRequestInitializer, String rootUrl, String servicePath,
      ObjectParser objectParser, GoogleClientRequestInitializer googleClientRequestInitializer,
      String applicationName, SubscriptionManager subscriptionManager,
      boolean suppressPatternChecks) {
    this.googleClientRequestInitializer = googleClientRequestInitializer;
    this.rootUrl = normalizeRootUrl(rootUrl);
    this.servicePath = normalizeServicePath(servicePath);
    this.applicationName = applicationName;
    this.requestFactory = httpRequestInitializer == null
        ? transport.createRequestFactory() : transport.createRequestFactory(httpRequestInitializer);
    this.objectParser = objectParser;
    this.subscriptionManager = subscriptionManager;
    this.suppressPatternChecks = suppressPatternChecks;
  }

  /**
   * Returns the URL-encoded root URL of the service, for example
   * {@code "https://www.googleapis.com/"}.
   *
   * <p>
   * Must end with a "/".
   * </p>
   */
  public final String getRootUrl() {
    return rootUrl;
  }

  /**
   * Returns the URL-encoded service path of the service, for example {@code "tasks/v1/"}.
   *
   * <p>
   * Must end with a "/" and not begin with a "/". It is allowed to be an empty string {@code ""} or
   * a forward slash {@code "/"}, if it is a forward slash then it is treated as an empty string
   * </p>
   */
  public final String getServicePath() {
    return servicePath;
  }

  /**
   * Returns the URL-encoded base URL of the service, for example
   * {@code "https://www.googleapis.com/tasks/v1/"}.
   *
   * <p>
   * Must end with a "/". It is guaranteed to be equal to {@code getRootUrl() + getServicePath()}.
   * </p>
   */
  public final String getBaseUrl() {
    return rootUrl + servicePath;
  }

  /**
   * Returns the application name to be sent in the User-Agent header of each request or
   * {@code null} for none.
   */
  public final String getApplicationName() {
    return applicationName;
  }

  /** Returns the HTTP request factory. */
  public final HttpRequestFactory getRequestFactory() {
    return requestFactory;
  }

  /** Returns the Google client request initializer or {@code null} for none. */
  public final GoogleClientRequestInitializer getGoogleClientRequestInitializer() {
    return googleClientRequestInitializer;
  }

  /**
   * Returns the object parser or {@code null} for none.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public ObjectParser getObjectParser() {
    return objectParser;
  }

  /**
   * Initializes a {@link AbstractGoogleClientRequest} using a
   * {@link GoogleClientRequestInitializer}.
   *
   * <p>
   * Must be called before the Google client request is executed, preferably right after the request
   * is instantiated. Sample usage:
   * </p>
   *
   * <pre>
    public class Get extends HttpClientRequest {
      ...
    }

    public Get get(String userId) throws Exception {
      Get result = new Get(userId);
      initialize(result);
      return result;
    }
   * </pre>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @param httpClientRequest Google client request type
   */
  protected void initialize(AbstractGoogleClientRequest<?> httpClientRequest) throws Exception {
    if (getGoogleClientRequestInitializer() != null) {
      getGoogleClientRequestInitializer().initialize(httpClientRequest);
    }
  }

  /**
   * Create an {@link HttpRequest} suitable for use against this service.
   *
   * <p>
   * Subclasses may override if specific behavior is required, for example if a sequence of requests
   * need to be built instead of a single request then subclasses should throw an
   * {@link UnsupportedOperationException}. Subclasses which override this method can make use of
   * {@link HttpRequest#addParser}, {@link HttpRequest#setContent} and
   * {@link HttpRequest#setEnableGZipContent}.
   * </p>
   *
   * @param requestMethod HTTP request method
   * @param url The complete URL of the service where requests should be sent
   * @param content HTTP content or {@code null} for none
   * @return newly created {@link HttpRequest}
   */
  protected final HttpRequest buildHttpRequest(
      String requestMethod, GenericUrl url, HttpContent content) throws Exception {
    HttpRequest httpRequest = requestFactory.buildRequest(requestMethod, url, content);
    new MethodOverride().intercept(httpRequest);
    httpRequest.setParser(getObjectParser());
    if (getApplicationName() != null) {
      httpRequest.getHeaders().setUserAgent(getApplicationName());
    }
    // custom methods may use POST with no content but require a Content-Length header
    if (content == null && requestMethod.equals(HttpMethods.POST)) {
      httpRequest.setContent(new EmptyContent());
    }
    return httpRequest;
  }

  /**
   * Builds and executes a {@link HttpRequest}. Subclasses may override if specific behavior is
   * required, for example if a sequence of requests need to be built instead of a single request.
   *
   * <p>
   * Callers are responsible for disconnecting the HTTP response by calling
   * {@link HttpResponse#disconnect}. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = client.executeUnparsed(method, url, body);
     try {
       // process response..
     } finally {
       response.disconnect();
     }
   * </pre>
   *
   * @param method HTTP Method type
   * @param url The complete URL of the service where requests should be sent
   * @param content HTTP content or {@code null} for none
   * @return HTTP response
   */
  protected final HttpResponse executeUnparsed(String method, GenericUrl url, HttpContent content)
      throws Exception {
    HttpRequest request = buildHttpRequest(method, url, content);
    return executeUnparsed(request);
  }

  /**
   * Executes the specified {@link HttpRequest}. Subclasses may override if specific behavior is
   * required, for example if a custom error is required to be thrown.
   *
   * <p>
   * Callers are responsible for disconnecting the HTTP response by calling
   * {@link HttpResponse#disconnect}. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = client.executeUnparsed(request);
     try {
       // process response..
     } finally {
       response.disconnect();
     }
   * </pre>
   *
   * <p>
   * Override by calling the super implementation.
   * </p>
   *
   * @param request HTTP Request
   * @return HTTP response
   */
  protected HttpResponse executeUnparsed(HttpRequest request) throws Exception {
    return request.execute();
  }

  /**
   * Builds and executes an {@link HttpRequest} and then returns the content input stream of
   * {@link HttpResponse}. Subclasses may override if specific behavior is required.
   *
   * <p>
   * Callers are responsible for closing the input stream after it is processed. Example usage:
   * </p>
   *
   * <pre>
     InputStream is = client.executeAsInputStream();
     try {
       // Process input stream..
     } finally {
       is.close();
     }
   * </pre>
   *
   * @param method HTTP Method type
   * @param url The complete URL of the service where requests should be sent
   * @param content HTTP content or {@code null} for none
   * @return input stream of the response content
   */
  protected final InputStream executeAsInputStream(
      String method, GenericUrl url, HttpContent content) throws Exception {
    HttpResponse response = executeUnparsed(method, url, content);
    return response.getContent();
  }

  /**
   * Create an {@link BatchRequest} object from this Google API client instance.
   *
   * <p>
   * Sample usage:
   * </p>
   *
   * <pre>
     client.batch()
         .queue(...)
         .queue(...)
         .execute();
   * </pre>
   *
   * @return newly created Batch request
   */
  public final BatchRequest batch() {
    return batch(null);
  }

  /**
   * Create an {@link BatchRequest} object from this Google API client instance.
   *
   * <p>
   * Sample usage:
   * </p>
   *
   * <pre>
     client.batch(httpRequestInitializer)
         .queue(...)
         .queue(...)
         .execute();
   * </pre>
   *
   * @param httpRequestInitializer The initializer to use when creating the top-level batch HTTP
   *        request or {@code null} for none
   * @return newly created Batch request
   */
  public final BatchRequest batch(HttpRequestInitializer httpRequestInitializer) {
    BatchRequest batch =
        new BatchRequest(getRequestFactory().getTransport(), httpRequestInitializer);
    batch.setBatchUrl(new GenericUrl(getRootUrl() + "batch"));
    return batch;
  }

  /** Returns the subscription manager or {@code null} for none. */
  public final SubscriptionManager getSubscriptionManager() {
    return subscriptionManager;
  }

  /** Returns whether discovery pattern checks should be suppressed on required parameters. */
  public final boolean getSuppressPatternChecks() {
    return suppressPatternChecks;
  }

  /** If the specified root URL does not end with a "/" then a "/" is added to the end. */
  static String normalizeRootUrl(String rootUrl) {
    Preconditions.checkNotNull(rootUrl, "root URL cannot be null.");
    if (!rootUrl.endsWith("/")) {
      rootUrl += "/";
    }
    return rootUrl;
  }

  /**
   * If the specified service path does not end with a "/" then a "/" is added to the end. If the
   * specified service path begins with a "/" then the "/" is removed.
   */
  static String normalizeServicePath(String servicePath) {
    Preconditions.checkNotNull(servicePath, "service path cannot be null");
    if (servicePath.length() == 1) {
      Preconditions.checkArgument(
          "/".equals(servicePath), "service path must equal \"/\" if it is of length 1.");
      servicePath = "";
    } else if (servicePath.length() > 0) {
      if (!servicePath.endsWith("/")) {
        servicePath += "/";
      }
      if (servicePath.startsWith("/")) {
        servicePath = servicePath.substring(1);
      }
    }
    return servicePath;
  }

  /**
   * Builder for {@link AbstractGoogleClient}.
   *
   * <p>
   * Implementation is not thread-safe.
   * </p>
   */
  public abstract static class Builder {

    /** HTTP transport. */
    private final HttpTransport transport;

    /**
     * Initializer to use when creating an {@link AbstractGoogleClientRequest} or {@code null} for
     * none.
     */
    private GoogleClientRequestInitializer googleClientRequestInitializer;

    /** HTTP request initializer or {@code null} for none. */
    private HttpRequestInitializer httpRequestInitializer;

    /** Object parser to use for parsing responses. */
    private final ObjectParser objectParser;

    /** The root URL of the service, for example {@code "https://www.googleapis.com/"}. */
    private String rootUrl;

    /** The service path of the service, for example {@code "tasks/v1/"}. */
    private String servicePath;

    /**
     * Application name to be sent in the User-Agent header of each request or {@code null} for
     * none.
     */
    private String applicationName;

    /** Subscription manager used to create subscriptions. */
    private SubscriptionManager subscriptionManager;

    /** Whether discovery pattern checks should be suppressed on required parameters. */
    private boolean suppressPatternChecks;

    /**
     * Returns an instance of a new builder.
     *
     * @param transport The transport to use for requests
     * @param rootUrl root URL of the service. Must end with a "/"
     * @param servicePath service path
     * @param httpRequestInitializer HTTP request initializer or {@code null} for none
     */
    protected Builder(HttpTransport transport, String rootUrl, String servicePath,
        ObjectParser objectParser, HttpRequestInitializer httpRequestInitializer) {
      this.transport = Preconditions.checkNotNull(transport);
      this.objectParser = Preconditions.checkNotNull(objectParser);
      setRootUrl(rootUrl);
      setServicePath(servicePath);
      this.httpRequestInitializer = httpRequestInitializer;
    }

    /** Builds a new instance of {@link AbstractGoogleClient}. */
    public abstract AbstractGoogleClient build();

    /** Returns the HTTP transport. */
    public final HttpTransport getTransport() {
      return transport;
    }

    /**
     * Returns the object parser used or {@code null} if not specified. *
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public ObjectParser getObjectParser() {
      return objectParser;
    }

    /**
     * Returns the URL-encoded root URL of the service, for example
     * {@code https://www.googleapis.com/}.
     *
     * <p>
     * Must be URL-encoded and must end with a "/".
     * </p>
     */
    public final String getRootUrl() {
      return rootUrl;
    }

    /**
     * Sets the URL-encoded root URL of the service, for example {@code https://www.googleapis.com/}
     * .
     * <p>
     * If the specified root URL does not end with a "/" then a "/" is added to the end.
     * </p>
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Builder setRootUrl(String rootUrl) {
      this.rootUrl = normalizeRootUrl(rootUrl);
      return this;
    }

    /**
     * Returns the URL-encoded service path of the service, for example {@code "tasks/v1/"}.
     *
     * <p>
     * Must be URL-encoded and must end with a "/" and not begin with a "/". It is allowed to be an
     * empty string {@code ""}.
     * </p>
     */
    public final String getServicePath() {
      return servicePath;
    }

    /**
     * Sets the URL-encoded service path of the service, for example {@code "tasks/v1/"}.
     *
     * <p>
     * It is allowed to be an empty string {@code ""} or a forward slash {@code "/"}, if it is a
     * forward slash then it is treated as an empty string. This is determined when the library is
     * generated and normally should not be changed.
     * </p>
     *
     * <p>
     * If the specified service path does not end with a "/" then a "/" is added to the end. If the
     * specified service path begins with a "/" then the "/" is removed.
     * </p>
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Builder setServicePath(String servicePath) {
      this.servicePath = normalizeServicePath(servicePath);
      return this;
    }

    /** Returns the Google client request initializer or {@code null} for none. */
    public final GoogleClientRequestInitializer getGoogleClientRequestInitializer() {
      return googleClientRequestInitializer;
    }

    /**
     * Sets the Google client request initializer or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Builder setGoogleClientRequestInitializer(
        GoogleClientRequestInitializer googleClientRequestInitializer) {
      this.googleClientRequestInitializer = googleClientRequestInitializer;
      return this;
    }

    /** Returns the HTTP request initializer or {@code null} for none. */
    public final HttpRequestInitializer getHttpRequestInitializer() {
      return httpRequestInitializer;
    }

    /**
     * Sets the HTTP request initializer or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Builder setHttpRequestInitializer(HttpRequestInitializer httpRequestInitializer) {
      this.httpRequestInitializer = httpRequestInitializer;
      return this;
    }

    /**
     * Returns the application name to be used in the UserAgent header of each request or
     * {@code null} for none.
     */
    public final String getApplicationName() {
      return applicationName;
    }

    /**
     * Sets the application name to be used in the UserAgent header of each request or {@code null}
     * for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Builder setApplicationName(String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    /**
     * Returns the {@link SubscriptionManager} used to make subscription requests, or {@code null}
     * for none.
     */
    public final SubscriptionManager getSubscriptionManager() {
      return subscriptionManager;
    }

    /**
     * Sets the {@link SubscriptionManager} used to make subscription requests, or {@code null} for
     * none.
     */
    public Builder setSubscriptionManager(SubscriptionManager subscriptionManager) {
      this.subscriptionManager = subscriptionManager;
      return this;
    }

    /** Returns whether discovery pattern checks should be suppressed on required parameters. */
    public final boolean getSuppressPatternChecks() {
      return suppressPatternChecks;
    }

    /**
     * Sets whether discovery pattern checks should be suppressed on required parameters.
     *
     * <p>
     * Default value is {@code false}.
     * </p>
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Builder setSuppressPatternChecks(boolean suppressPatternChecks) {
      this.suppressPatternChecks = suppressPatternChecks;
      return this;
    }
  }
}