package org.esupportail.esupsignature.service.interfaces.listsearch.impl;

import com.sun.net.httpserver.HttpServer;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;

import static org.mockito.Mockito.mock;
import org.springframework.context.support.StaticMessageSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SympaSoapUserListTest {

    private HttpServer server;
    private SympaSoapUserList userList;
    private GlobalProperties soapProperties;
    private final AtomicReference<String> request = new AtomicReference<>();
    private String response;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/soap", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        GlobalProperties properties = new GlobalProperties();
        soapProperties = properties;
        properties.setSympaUrl("http://localhost:" + server.getAddress().getPort() + "/soap");
        properties.setSympaApp("java-annuaire");
        properties.setSympaToken("secret&value");
        properties.setDomain("example.org");
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("error.sympa.soap.response", Locale.getDefault(), "error.sympa.soap.response");
        messages.addMessage("error.sympa.soap", Locale.getDefault(), "error.sympa.soap");
        userList = new SympaSoapUserList(properties, messages);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void reviewUsesTrustedApplicationAndResolvesEncodedReferences() {
        response = envelope("<s:authenticateRemoteAppAndRunResponse xmlns:s='urn:sympasoap'>"
                + "<return href='#values'/></s:authenticateRemoteAppAndRunResponse>"
                + "<multiRef id='values'><item href='#email'/><item>b@example.org</item></multiRef>"
                + "<multiRef id='email'>a@example.org</multiRef>");
        assertEquals(List.of("a@example.org", "b@example.org"), userList.getUsersEmailFromList("team@example.org"));
        assertTrue(request.get().contains("<appname xsi:type=\"xsd:string\">java-annuaire</appname>"));
        assertTrue(request.get().contains("secret&amp;value"));
        assertTrue(request.get().contains(">review</service>"));
        assertTrue(request.get().contains(">team</item>"));
    }

    @Test
    void autocompleteDoesNotCallSoap() {
        GlobalProperties properties = new GlobalProperties();
        UserListService service = new UserListService(List.of(userList), mock(UserRepository.class), null, properties);

        assertTrue(service.getListsNames("secretariat").isEmpty());
        assertNull(request.get());
    }

    @Test
    void unknownListDoesNotPreventIndividualUserLookup() {
        response = envelope("<soap:Fault><faultcode>soap:Server</faultcode><faultstring>Unknown list</faultstring></soap:Fault>");
        assertTrue(userList.getUsersEmailFromList("person@example.org").isEmpty());
    }

    @Test
    void faultsAndMalformedResponsesAreReported() {
        response = envelope("<soap:Fault><faultcode>soap:Server</faultcode><faultstring>denied</faultstring></soap:Fault>");
        EsupSignatureRuntimeException exception = assertThrows(EsupSignatureRuntimeException.class,
                () -> userList.getUsersEmailFromList("team"));
        assertTrue(exception.getCause().getMessage().contains("denied"));
        response = envelope("<unexpected/>");
        assertThrows(EsupSignatureRuntimeException.class, () -> userList.getUsersEmailFromList("team"));
    }

    @Test
    void emptyArrayAndBlankNamesReturnNoSubscribers() {
        response = envelope("<s:authenticateRemoteAppAndRunResponse xmlns:s='urn:sympasoap'><return/>"
                + "</s:authenticateRemoteAppAndRunResponse>");
        assertTrue(userList.getUsersEmailFromList("team").isEmpty());
        request.set(null);
        assertTrue(userList.getUsersEmailFromList(" ").isEmpty());
        assertNull(request.get());
        assertTrue(userList.getUsersEmailFromAliases("team").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"secretariat.dsi", "secretariat.dsi@listes.univ-rouen.fr", "secretariat.dsi@univ-rouen.fr"})
    void serviceReturnsMembersForConfiguredDomainAndSubdomains(String listName) {
        response = envelope("<s:authenticateRemoteAppAndRunResponse xmlns:s='urn:sympasoap'>"
                + "<return><item>alice@univ-rouen.fr</item><item>bob@univ-rouen.fr</item></return>"
                + "</s:authenticateRemoteAppAndRunResponse>");
        soapProperties.setDomain("univ-rouen.fr");
        GlobalProperties properties = new GlobalProperties();
        properties.setDomain("univ-rouen.fr");
        properties.setForcedExternalsDomainList(List.of());
        UserListService service = new UserListService(List.of(userList), mock(UserRepository.class), null, properties);

        assertEquals(java.util.Set.of("alice@univ-rouen.fr", "bob@univ-rouen.fr"),
                new java.util.HashSet<>(service.getUsersEmailFromList(listName)));
        assertTrue(request.get().contains(">secretariat.dsi</item>"));
        assertFalse(request.get().contains("secretariat.dsi@"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"team@other.org", "team@fakeuniv-rouen.fr", "team@univ-rouen.fr.other.org"})
    void unrelatedDomainsAreRejectedWithoutCallingSoap(String address) {
        soapProperties.setDomain("univ-rouen.fr");
        assertTrue(userList.getUsersEmailFromList(address).isEmpty());
        assertNull(request.get());
    }

    private String envelope(String body) {
        return "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body>"
                + body + "</soap:Body></soap:Envelope>";
    }
}
