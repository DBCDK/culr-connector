package dk.dbc.connector.culr;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class CulrConnectorTestWireMockRecorder {

    /*
        Steps to reproduce wiremock recording:

        * Start standalone runner
            java -jar wiremock-standalone-{WIRE_MOCK_VERSION}.jar --proxy-all="{CULR_SERVICE_URL}" --https-port 8443 --record-mappings --verbose

        * Run the main method of this class

        * OBFUSCATE credentials in the recordings

        * Replace content of src/test/resources/{__files|mappings} with that produced by the standalone runner
     */

    public static void main(String[] args) throws CulrConnectorException, NoSuchAlgorithmException, KeyManagementException {
        trustEveryone();
        CulrConnectorTest.culrConnector = new CulrConnector("https://localhost:8443/1.6/CulrWebService",
                CulrConnectorTest.NO_RETRY_POLICY);
        final CulrConnectorTest culrConnectorTest = new CulrConnectorTest();
        recordRequestsForGetAccountFromProvider(culrConnectorTest);
        recordRequestsForCreateAccount(culrConnectorTest);
    }

    private static void recordRequestsForGetAccountFromProvider(CulrConnectorTest culrConnectorTest)
            throws CulrConnectorException {
        culrConnectorTest.getAccountFromProvider();
        culrConnectorTest.getAccountFromProvider_notFound();
    }

    private static void recordRequestsForCreateAccount(CulrConnectorTest culrConnectorTest)
            throws CulrConnectorException {
        culrConnectorTest.createAccount();
        culrConnectorTest.createAccount_fail();
    }

    private static void trustEveryone() throws NoSuchAlgorithmException, KeyManagementException {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new X509TrustManager[]{new X509TrustManager(){
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }}}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(
                context.getSocketFactory());
    }
}
