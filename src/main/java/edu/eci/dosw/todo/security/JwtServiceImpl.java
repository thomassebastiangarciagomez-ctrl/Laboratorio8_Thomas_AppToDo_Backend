package edu.eci.dosw.todo.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.entity.UserEntity;

@Service
public class JwtServiceImpl implements JwtService{
    
    private final JwtEncoder encoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtServiceImpl(JwtEncoder encoder, @Value("${app.security.jwt.issuer}") String issuer, @Value("${app.security.jwt.expiration-seconds}") long expirationSeconds) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Este metodo se encarga de generar el token de verificacion a cada usuario al momento en el que este ingresa
     * en la aplicacion web
     */
    @Override 
    public TokenResponse generateToken(UserEntity entity){
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(entity.getId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .claim("role", entity.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse("Bearer", token, expirationSeconds);
    }
}
