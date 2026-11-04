package edu.eci.dosw.todo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import edu.eci.dosw.todo.config.JwtConfig;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.entity.UserRole;
import edu.eci.dosw.todo.security.JwtServiceImpl;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Emision y validacion del JWT con la misma configuracion criptografica (JwtConfig + JwtServiceImpl).
 * La clave de este archivo es un valor ficticio solo para pruebas; la clave real llega por JWT_SECRET.
 */
public class JwtTokenTest {

    private static final String SECRET = "clave-ficticia-solo-para-pruebas-0123456789";
    private static final String ISSUER = "todo-api";

    // El TokenResponse se lee como JSON (lo mismo que recibe el cliente): no depende de si es record o clase.
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final JwtConfig config = new JwtConfig();
    private SecretKey key;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    public void setUp() {
        key = config.jwtSecretKey(SECRET);
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key, ISSUER);
    }

    private UserEntity user(long id, UserRole role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("Usuario " + id);
        user.setEmail("usuario" + id + "@example.com");
        user.setRole(role);
        return user;
    }

    /** 
     * Emite un token real con JwtServiceImpl y devuelve solo el valor del access token. 
     */
    private String issueToken(UserEntity user) {
        JsonNode body = MAPPER.valueToTree(new JwtServiceImpl(encoder, ISSUER, 1800).generateToken(user));
        return body.get("accessToken").stringValue();
    }

    /** Firma un JWT arbitrario con el encoder dado, para fabricar tokens expirados o con claims incorrectos. */
    private String sign(JwtEncoder signer, String issuer, String subject, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", "USER");
        if (subject != null) {
            builder.subject(subject);
        }
        return signer.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), builder.build()))
                .getTokenValue();
    }

    @Test
    public void secretKey_shouldUseConfiguredSecret() {
        SecretKey result = config.jwtSecretKey(SECRET);

        assertEquals("HmacSHA256", result.getAlgorithm());
        assertArrayEquals(SECRET.getBytes(StandardCharsets.UTF_8), result.getEncoded());
    }

    @Test
    public void secretKey_shouldRejectShortSecret() {
        assertThrows(IllegalStateException.class, () -> config.jwtSecretKey("corta"));
    }

    @Test
    public void generateToken_shouldProduceSignedTokenWithExpectedClaims() {
        JwtServiceImpl service = new JwtServiceImpl(encoder, ISSUER, 1800);

        JsonNode body = MAPPER.valueToTree(service.generateToken(user(5L, UserRole.ADMIN)));
        Jwt jwt = decoder.decode(body.get("accessToken").stringValue());

        assertEquals("Bearer", body.get("tokenType").stringValue());
        assertEquals(1800L, body.get("expiresIn").longValue());
        assertEquals("5", jwt.getSubject());
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertEquals(1800, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
    }

    /** Modificar un caracter de la firma invalida el token. */
    @Test
    public void decode_shouldRejectTokenWithModifiedSignature() {
        String token = issueToken(user(1L, UserRole.USER));
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        char replacement = first == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1);

        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }

    /** Escalada de privilegios: payload de un ADMIN con la firma de un USER no debe ser aceptado. */
    @Test
    public void decode_shouldRejectPayloadSwappedFromAnotherToken() {
        String[] userToken = issueToken(user(1L, UserRole.USER)).split("\\.");
        String[] adminToken = issueToken(user(2L, UserRole.ADMIN)).split("\\.");
        String forged = userToken[0] + "." + adminToken[1] + "." + userToken[2];

        assertThrows(JwtException.class, () -> decoder.decode(forged));
    }

    @Test
    public void decode_shouldRejectExpiredToken() {
        Instant now = Instant.now();
        String expired = sign(encoder, ISSUER, "1", now.minusSeconds(100), now.minusSeconds(10));

        assertThrows(JwtException.class, () -> decoder.decode(expired));
    }

    @Test
    public void decode_shouldRejectWrongIssuer() {
        Instant now = Instant.now();
        String token = sign(encoder, "otro-emisor", "1", now, now.plusSeconds(600));

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    public void decode_shouldRejectTokenWithoutSubject() {
        Instant now = Instant.now();
        String token = sign(encoder, ISSUER, null, now, now.plusSeconds(600));

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    public void decode_shouldRejectTokenSignedWithAnotherKey() {
        SecretKey otherKey = new SecretKeySpec(
                "otra-clave-distinta-para-pruebas-9876543210".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant now = Instant.now();
        String token = sign(config.jwtEncoder(otherKey), ISSUER, "1", now, now.plusSeconds(600));

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    /** Un texto que solo parece un JWT (o ni eso) nunca es valido: no basta con decodificar Base64. */
    @Test
    public void decode_shouldRejectMalformedToken() {
        assertThrows(JwtException.class, () -> decoder.decode("esto-no-es-un-jwt"));
    }
}
