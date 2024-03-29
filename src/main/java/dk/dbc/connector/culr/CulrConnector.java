package dk.dbc.connector.culr;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.culrservice.ws.AuthCredentials;
import dk.dbc.culrservice.ws.CulrResponse;
import dk.dbc.culrservice.ws.CulrWebService;
import dk.dbc.culrservice.ws.CulrWebService_Service;
import dk.dbc.culrservice.ws.GetAccountsByAccountIdResponse;
import dk.dbc.culrservice.ws.GlobalUID;
import dk.dbc.culrservice.ws.ResponseCodes;
import dk.dbc.culrservice.ws.UserIdTypes;
import dk.dbc.culrservice.ws.UserIdValueAndType;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import java.time.Duration;
import java.util.Objects;

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
    static final String DEFAULT_CACHE_TTL_DURATION = "PT8H";
    private final Cache<CacheKey, GetAccountsByAccountIdResponse> cache;

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

    CulrConnector(String endpoint, int connectTimeoutInMs, int requestTimeoutInMs, Duration cacheTtl) {
        this(endpoint, DEFAULT_RETRY_POLICY, connectTimeoutInMs, requestTimeoutInMs, cacheTtl);
    }

    CulrConnector(String endpoint, RetryPolicy<Object> retryPolicy) {
        this(endpoint, retryPolicy, Integer.parseInt(DEFAULT_CONNECT_TIMEOUT_IN_MS), Integer.parseInt(DEFAULT_REQUEST_TIMEOUT_IN_MS), Duration.parse(DEFAULT_CACHE_TTL_DURATION));
    }

    CulrConnector(String endpoint, RetryPolicy<Object> retryPolicy, int connectTimeoutInMs, int requestTimeoutInMs, Duration cacheTtl) {
        if (endpoint == null) {
            throw new NullPointerException("Parameter 'endpoint' can not be null in call to CulrConnector(...)");
        }
        if (endpoint.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'endpoint' can not be empty in call to CulrConnector(...)");
        }
        if (retryPolicy == null) {
            throw new NullPointerException("Parameter 'retryPolicy' can not be null in call to CulrConnector(...)");
        }
        this.endpoint = endpoint;
        this.retryPolicy = retryPolicy;

        CulrWebService_Service culrService = new CulrWebService_Service();
        proxy = culrService.getCulrWebServicePort();
        // We don't want to rely on the endpoint from the WSDL
        BindingProvider bindingProvider = (BindingProvider)proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, connectTimeoutInMs);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, requestTimeoutInMs);
        cache = CacheBuilder.newBuilder().expireAfterWrite(cacheTtl).recordStats().build();
        LOGGER.info("Created CulrConnector for endpoint {} with retry policy {}, connect timeout in ms {} and request timeout in ms {}",
                endpoint, retryPolicy, connectTimeoutInMs, requestTimeoutInMs);
    }

    public GetAccountsByAccountIdResponse getAccountFromProvider(String agencyId, UserIdValueAndType userCredentials,
                                                                 AuthCredentials authCredentials) throws CulrConnectorException {
        if(cache == null) {
            return getAccountFromProviderNoCache(agencyId, userCredentials, authCredentials);
        }
        CacheKey key = new CacheKey(agencyId, userCredentials, authCredentials);
        GetAccountsByAccountIdResponse response = cache.getIfPresent(key);
        if(response == null) {
            response = getAccountFromProviderNoCache(agencyId, userCredentials, authCredentials);
            if(response.getResponseStatus().getResponseCode() == ResponseCodes.OK_200) cache.put(key, response);
        }
        return response;
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

    @SuppressWarnings("unused")
    public String getEndpoint() {
        return endpoint;
    }

    public long getCacheSize() {
        return cache.size();
    }

    private GetAccountsByAccountIdResponse getAccountFromProviderNoCache(String agencyId, UserIdValueAndType userCredentials,
                                                                         AuthCredentials authCredentials) throws CulrConnectorException {
        try {
            return Failsafe.with(retryPolicy).get(() -> proxy.getAccountFromProvider(agencyId, userCredentials, authCredentials));
        } catch (RuntimeException e) {
            throw new CulrConnectorException(e.getMessage(), e);
        }
    }

    private static class CacheKey {
        private final String agencyId;
        private final UserIdTypes userIdType;
        private final String userIdValue;
        private final String userIdAut;
        private final String groupIdAut;

        public CacheKey(String agencyId, UserIdValueAndType userCredentials, AuthCredentials authCredentials) {
            this.agencyId = agencyId;
            userIdType = userCredentials.getUserIdType();
            userIdValue = userCredentials.getUserIdValue();
            userIdAut = authCredentials.getUserIdAut();
            groupIdAut = authCredentials.getGroupIdAut();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return userIdValue.equals(cacheKey.userIdValue) && agencyId.equals(cacheKey.agencyId) && userIdType == cacheKey.userIdType && userIdAut.equals(cacheKey.userIdAut) && groupIdAut.equals(cacheKey.groupIdAut);
        }

        @Override
        public int hashCode() {
            return Objects.hash(agencyId, userIdType, userIdValue, userIdAut, groupIdAut);
        }
    }
}
