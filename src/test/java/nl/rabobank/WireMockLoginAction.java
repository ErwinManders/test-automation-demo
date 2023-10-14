package nl.rabobank;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.time.ZoneOffset.UTC;

import static nl.rabobank.CallForPapersWireMockTest.PORT;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class WireMockLoginAction extends PostServeAction
{
    private static final String SECRET = "demo_secret";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);
    private static final ZoneOffset EU_AMS_PARIS = ZoneOffset.of("+02:00");

    static final String NAME = "WIREMOCK_LOGIN";
    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters)
    {
        final var customerId = serveEvent.getRequest().queryParameter("customerId").firstValue();
        final String jwt = createJwtToken(customerId);

        configureFor(PORT);
        stubFor(get("/privatekey").willReturn(ok(SECRET)));
        stubFor(get(urlMatching("/api/conferences"))
                .willReturn(
                        aResponse()
                                .proxiedFrom("http://localhost:8080/proxy")
                                .withAdditionalRequestHeader("X_AUTH_HEADER", jwt)));
    }

    private static String createJwtToken(final String customerId)
    {
        final var localDateTime = LocalDateTime.now();
        String jwtToken = JWT.create()
                .withIssuer("Rabobank")
                .withSubject("JavaCRO Demo")
                .withClaim("customerId", customerId)
                .withIssuedAt(localDateTime.toInstant(EU_AMS_PARIS))
                .withExpiresAt(localDateTime.plusHours(1L).toInstant(EU_AMS_PARIS))
                .withJWTId(UUID.randomUUID().toString())
                //.withNotBefore(localDateTime.plusSeconds(1L).toInstant(UTC))
                .sign(ALGORITHM);
        return jwtToken;
    }
}
