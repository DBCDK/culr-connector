/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.connector.culr;

import dk.dbc.culrservice.ws.AuthCredentials;
import dk.dbc.culrservice.ws.CulrResponse;
import dk.dbc.culrservice.ws.CulrWebService;
import dk.dbc.culrservice.ws.CulrWebService_Service;
import dk.dbc.culrservice.ws.GetAccountsByAccountIdResponse;
import dk.dbc.culrservice.ws.GlobalUID;
import dk.dbc.culrservice.ws.UserIdValueAndType;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.time.Duration;

/**
 * CULR service connector
 * <p>
 * This class is thread-safe.
 * </p>
 */
public class CulrConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CulrConnector.class);

    static final String CONNECT_TIMEOUT_PROPERTY   = "com.sun.xml.ws.connect.timeout";
    static final String REQUEST_TIMEOUT_PROPERTY   = "com.sun.xml.ws.request.timeout";
    static final String DEFAULT_CONNECT_TIMEOUT_IN_MS = "2000";  //  2 seconds
    static final String DEFAULT_REQUEST_TIMEOUT_IN_MS = "10000"; // 10 seconds

    private static final RetryPolicy<Object> DEFAULT_RETRY_POLICY = new RetryPolicy<>()
            .handle(WebServiceException.class)
            .withDelay(Duration.ofSeconds(3))
            .withMaxRetries(3);

    private final String endpoint;
    private final RetryPolicy<Object> retryPolicy;

    /* web-service proxy */
    private final CulrWebService proxy;

    CulrConnector(String endpoint) {
        this(endpoint, DEFAULT_RETRY_POLICY);
    }

    CulrConnector(String endpoint, int connectTimeoutInMs, int requestTimeoutInMs) {
        this(endpoint, DEFAULT_RETRY_POLICY, connectTimeoutInMs, requestTimeoutInMs);
    }

    CulrConnector(String endpoint, RetryPolicy<Object> retryPolicy) {
        this(endpoint, retryPolicy, Integer.parseInt(DEFAULT_CONNECT_TIMEOUT_IN_MS), Integer.parseInt(DEFAULT_REQUEST_TIMEOUT_IN_MS));
    }

    CulrConnector(String endpoint, RetryPolicy<Object> retryPolicy, int connectTimeoutInMs, int requestTimeoutInMs) {
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(
                endpoint, "endpoint");
        this.retryPolicy = InvariantUtil.checkNotNullOrThrow(
                retryPolicy, "retryPolicy");

        final CulrWebService_Service culrService = new CulrWebService_Service();
        proxy = culrService.getCulrWebServicePort();
        // We don't want to rely on the endpoint from the WSDL
        final BindingProvider bindingProvider = (BindingProvider)proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, connectTimeoutInMs);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, requestTimeoutInMs);

        LOGGER.info("Created CulrConnector for endpoint {} with retry policy {}, connect timeout in ms {} and request timeout in ms {}",
                endpoint, retryPolicy, connectTimeoutInMs, requestTimeoutInMs);
    }

    public GetAccountsByAccountIdResponse getAccountFromProvider(String agencyId, UserIdValueAndType userCredentials,
                                                                 AuthCredentials authCredentials)
            throws CulrConnectorException {
        try {
            return Failsafe.with(retryPolicy).get(() -> proxy.getAccountFromProvider(
                    agencyId, userCredentials, authCredentials));
        } catch (RuntimeException e) {
            throw new CulrConnectorException(e.getMessage(), e);
        }
    }

    public CulrResponse createAccount(String agencyId, UserIdValueAndType userCredentials,
                                      AuthCredentials authCredentials)
            throws CulrConnectorException {
        try {
            return Failsafe.with(retryPolicy).get(() -> proxy.createAccount(
                    agencyId, userCredentials, authCredentials, null, null));
        } catch (RuntimeException e) {
            throw new CulrConnectorException(e.getMessage(), e);
        }
    }

    public CulrResponse createAccount(String agencyId, UserIdValueAndType userCredentials,
                                      AuthCredentials authCredentials, GlobalUID globalUID, String municipalityNo)
            throws CulrConnectorException {
        try {
            return Failsafe.with(retryPolicy).get(() -> proxy.createAccount(
                    agencyId, userCredentials, authCredentials, globalUID, municipalityNo));
        } catch (RuntimeException e) {
            throw new CulrConnectorException(e.getMessage(), e);
        }
    }

    public String getEndpoint() {
        return endpoint;
    }
}
