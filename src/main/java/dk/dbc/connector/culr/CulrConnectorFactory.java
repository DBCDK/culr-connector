package dk.dbc.connector.culr;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.time.Duration;

/**
 * CulrConnector factory
 * <p>
 * Synopsis:
 * </p>
 * <pre>
 *    // New instance
 *    CulrConnector cc = CulrConnectorFactory.create("http://culr-service");
 *
 *    // Singleton instance in CDI enabled environment
 *    {@literal @}Inject
 *    CulrConnectorFactory factory;
 *    ...
 *    CulrConnector cc = factory.getInstance();
 *
 *    // or simply
 *    {@literal @}Inject
 *    CulrConnector cc;
 * </pre>
 * <p>
 * CDI case depends on the CULR service baseurl being defined as
 * the value of either a system property or environment variable
 * named CULR_SERVICE_URL.
 * </p>
 * <p>
 * This class is thread-safe.
 * </p>
 */
@ApplicationScoped
public class CulrConnectorFactory {
    /**
     * @param culrServiceUrl culr service endpoint
     * @return new {@link CulrConnector} instance
     */
    public static CulrConnector create(String culrServiceUrl) {
        return new CulrConnector(culrServiceUrl);
    }

    /**
     * @param culrServiceUrl culr service endpoint
     * @param culrConnectorConnectTimeoutInMs connection timeout in milliseconds
     * @param culrConnectorRequestTimeoutInMs request timeout in milliseconds
     * @param cacheTtl Cache time to live. 0 disables the cache
     * @return new {@link CulrConnector} instance
     */
    public static CulrConnector create(String culrServiceUrl, int culrConnectorConnectTimeoutInMs,
                                       int culrConnectorRequestTimeoutInMs, Duration cacheTtl) {
        return new CulrConnector(culrServiceUrl, culrConnectorConnectTimeoutInMs, culrConnectorRequestTimeoutInMs, cacheTtl);
    }

    /**
     * @param culrServiceUrl culr service endpoint
     * @param retryPolicy retry policy
     * @return new {@link CulrConnector} instance
     */
    public static CulrConnector create(String culrServiceUrl, RetryPolicy<Object> retryPolicy) {
        return new CulrConnector(culrServiceUrl, retryPolicy);
    }

    /**
     * @param culrServiceUrl culr service endpoint
     * @param retryPolicy retry policy
     * @param culrConnectorConnectTimeoutInMs connection timeout in milliseconds
     * @param culrConnectorRequestTimeoutInMs request timeout in milliseconds
     * @param cacheTtl Cache time to live. 0 disables the cache
     * @return new {@link CulrConnector} instance
     */
    public static CulrConnector create(String culrServiceUrl, RetryPolicy<Object> retryPolicy,
                                       int culrConnectorConnectTimeoutInMs, int culrConnectorRequestTimeoutInMs, Duration cacheTtl) {

        return new CulrConnector(culrServiceUrl, retryPolicy, culrConnectorConnectTimeoutInMs, culrConnectorRequestTimeoutInMs, cacheTtl);
    }

    @Inject
    @ConfigProperty(name = "CULR_SERVICE_URL")
    private String culrServiceUrl;

    @Inject
    @ConfigProperty(name = "CULR_CONNECTOR_CONNECT_TIMEOUT_IN_MS",
            defaultValue = CulrConnector.DEFAULT_CONNECT_TIMEOUT_IN_MS)
    private Integer culrConnectorConnectTimeoutInMs;

    @Inject
    @ConfigProperty(name = "CULR_CONNECTOR_REQUEST_TIMEOUT_IN_MS",
            defaultValue = CulrConnector.DEFAULT_REQUEST_TIMEOUT_IN_MS)
    private Integer culrConnectorRequestTimeoutInMs;

    @Inject
    @ConfigProperty(name = "CULR_CONNECTOR_CACHE_TTL_DURATION", defaultValue = CulrConnector.DEFAULT_CACHE_TTL_DURATION)
    private Duration cacheTtl;

    CulrConnector culrConnector;

    @PostConstruct
    public void initializeConnector() {
        culrConnector = CulrConnectorFactory.create(culrServiceUrl,
                culrConnectorConnectTimeoutInMs, culrConnectorRequestTimeoutInMs, cacheTtl);
    }

    @Produces
    public CulrConnector getInstance() {
        return culrConnector;
    }
}
