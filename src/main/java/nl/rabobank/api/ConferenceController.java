package nl.rabobank.api;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.split.client.SplitClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.rabobank.conference.Conference;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ConferenceController
{
    private static final String MAP_NAME = "CONFERENCES";
    private final RestTemplate restTemplate;
    private final Cache cache;
    private final SplitClient splitClient;

    @GetMapping("/proxy/api/conferences")
    public ResponseEntity<String> getConferences(@RequestHeader("X_AUTH_HEADER") String jwtToken)
    {
        verifyJwt(jwtToken);
        final var response =  restTemplate.getForObject("http://localhost:9090/external/api/conferences", String.class);
        return ResponseEntity.ok().contentType(APPLICATION_JSON).body(response);
    }

    private String verifyJwt(final String jwt)
    {
        final var key = restTemplate.getForObject("http://localhost:9090/privatekey", String.class);
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("Rabobank")
                .build();

        DecodedJWT decodedJWT = verifier.verify(jwt);
        Claim claim = decodedJWT.getClaim("customerId");
        return claim.asString();
    }

    @PostMapping("/proxy/api/conferences")
    public ResponseEntity<List<Conference>> addConference(@RequestBody final Conference conference)
    {
        final Conference[] conferences = cache.get(MAP_NAME, Conference[].class);

        final List<Conference> conferenceList;
        if (conferences == null)
        {
            conferenceList = List.of(conference);
        } else
        {
            conferenceList = new ArrayList<>(Arrays.asList(conferences));
            conferenceList.add(conference);
        }

        cache.put(MAP_NAME, conferenceList.toArray());

        return ResponseEntity.status(CREATED).contentType(APPLICATION_JSON).body(conferenceList);
    }

    @GetMapping("/split/api/conferences")
    public ResponseEntity<String> returnSplitResponse()
    {
        final var treatment = splitClient.getTreatment("d5c9f9f2d5cc18be7dba99daad2ed36b5175db9d911b96acd6fda1360c96aec9", "TestAutomationDemo");
        if ("on".equals(treatment))
        {
            final var response =  restTemplate.getForObject("http://localhost:9090/external/api/conferences/v2", String.class);
            return ResponseEntity.ok().contentType(APPLICATION_JSON).body(response);
        }
        final var response =  restTemplate.getForObject("http://localhost:9090/external/api/conferences", String.class);
        return ResponseEntity.ok().contentType(APPLICATION_JSON).body(response);
    }

}
