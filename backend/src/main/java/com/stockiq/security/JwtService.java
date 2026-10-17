package com.stockiq.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stockiq.usuario.model.Usuario;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Emisión y verificación de tokens JWT (autenticación stateless, RNF-02). */
@Service
public class JwtService {

    private static final int LARGO_MINIMO_SECRET = 32; // 256 bits, mínimo para HS256

    private final SecretKey clave;
    private final long minutosDeVigencia;

    public JwtService(@Value("${stockiq.jwt.secret}") String secret,
            @Value("${stockiq.jwt.expiration-minutes}") long minutosDeVigencia) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LARGO_MINIMO_SECRET) {
            throw new IllegalStateException(
                    "stockiq.jwt.secret (JWT_SECRET) debe tener al menos " + LARGO_MINIMO_SECRET + " caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.minutosDeVigencia = minutosDeVigencia;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosDeVigencia, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve el username del token si la firma es válida y no venció.
     *
     * @throws JwtException si el token está vencido, mal formado o la firma no coincide
     */
    public String extraerUsername(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
