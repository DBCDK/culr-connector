/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.connector.culr;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.culrservice.ws.AuthCredentials;
import dk.dbc.culrservice.ws.CulrResponse;
import dk.dbc.culrservice.ws.GetAccountsByAccountIdResponse;
import dk.dbc.culrservice.ws.GlobalUID;
import dk.dbc.culrservice.ws.GlobalUidTypes;
import dk.dbc.culrservice.ws.ResponseCodes;
import dk.dbc.culrservice.ws.UserIdTypes;
import dk.dbc.culrservice.ws.UserIdValueAndType;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CulrConnectorTest {
    private static WireMockServer wireMockServer;
    private static String wireMockHost;

    static final RetryPolicy<Object> NO_RETRY_POLICY = new RetryPolicy<>();
    static CulrConnector culrConnector;
    static AuthCredentials authCredentials;

    static {
        // Obfuscated credentials
        authCredentials = new AuthCredentials();
        authCredentials.setUserIdAut("connector");
        authCredentials.setGroupIdAut("190976");
        authCredentials.setPasswordAut("connector-pass");
    }

    @BeforeAll
    static void startWireMockServer() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        wireMockHost = "http://localhost:" + wireMockServer.port() + "/1.6/CulrWebService";
        configureFor("localhost", wireMockServer.port());
    }

    @BeforeAll
    static void setupConnector() {
        culrConnector = new CulrConnector(wireMockHost, NO_RETRY_POLICY);
    }

    @AfterAll
    static void stopWireMockServer() {
        wireMockServer.stop();
    }

    @Test
    void getAccountFromProvider() throws CulrConnectorException {
        final UserIdValueAndType userCredentials = new UserIdValueAndType();
        userCredentials.setUserIdType(UserIdTypes.LOCAL);
        userCredentials.setUserIdValue("1111");
        final GetAccountsByAccountIdResponse accountFromProvider = culrConnector.getAccountFromProvider(
                "190976", userCredentials, authCredentials);
        assertThat(accountFromProvider.getResponseStatus().getResponseCode(), is(ResponseCodes.OK_200));
        assertThat("Entry should be cached", culrConnector.getCacheSize(), is(1));
    }

    @Test
    void getAccountFromProvider_notFound() throws CulrConnectorException {
        final UserIdValueAndType userCredentials = new UserIdValueAndType();
        userCredentials.setUserIdType(UserIdTypes.LOCAL);
        userCredentials.setUserIdValue("9999");
        final GetAccountsByAccountIdResponse accountFromProvider = culrConnector.getAccountFromProvider(
                "190976", userCredentials, authCredentials);
        assertThat(accountFromProvider.getResponseStatus().getResponseCode(), is(ResponseCodes.ACCOUNT_DOES_NOT_EXIST));
        assertThat("Entry should not be cached", culrConnector.getCacheSize(), is(0));
    }

    @Test
    void createAccount() throws CulrConnectorException {
        final UserIdValueAndType userCredentialsCPR = new UserIdValueAndType();
        userCredentialsCPR.setUserIdType(UserIdTypes.CPR);
        userCredentialsCPR.setUserIdValue("2407776666");
        final CulrResponse culrResponse1 = culrConnector.createAccount("190976", userCredentialsCPR, authCredentials);
        assertThat("create for type CPR", culrResponse1.getResponseStatus().getResponseCode(), is(ResponseCodes.OK_200));

        final UserIdValueAndType userCredentialsLocal = new UserIdValueAndType();
        userCredentialsLocal.setUserIdType(UserIdTypes.LOCAL);
        userCredentialsLocal.setUserIdValue("6666");
        final GlobalUID globalUID = new GlobalUID();
        globalUID.setUidType(GlobalUidTypes.CPR);
        globalUID.setUidValue("2407776666");
        final CulrResponse culrResponse2 = culrConnector.createAccount(
                "190976", userCredentialsLocal, authCredentials, globalUID, null);
        assertThat("create for type LOCAL", culrResponse2.getResponseStatus().getResponseCode(), is(ResponseCodes.OK_200));
    }

    @Test
    void createAccount_fail() throws CulrConnectorException {
        final UserIdValueAndType userCredentialsCPR = new UserIdValueAndType();
        userCredentialsCPR.setUserIdType(UserIdTypes.CPR);
        userCredentialsCPR.setUserIdValue("2407774444");
        final CulrResponse culrResponse = culrConnector.createAccount("190976", userCredentialsCPR, authCredentials);
        assertThat(culrResponse.getResponseStatus().getResponseCode(), is(ResponseCodes.TRANSACTION_ERROR));
    }
}