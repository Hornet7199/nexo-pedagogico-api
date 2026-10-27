package com.nexo.pedagogicoapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Valida os access tokens emitidos pelo usuario-api. Este serviço <b>não emite</b> token
 * nenhum: login, renovação e logout são do usuario-api. O segredo precisa ser o mesmo
 * valor nos dois ({@code NEXO_JWT_SECRET}).
 */
@Service
public class JwtService {

    /** HMAC-SHA256 exige chave de pelo menos 256 bits — 32 caracteres ASCII. */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final SecretKey key;

    public JwtService(@Value("${nexo.jwt.secret}") String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "nexo.jwt.secret tem " + bytes.length + " bytes; o mínimo é " + TAMANHO_MINIMO_SEGREDO
                    + " (requisito do HMAC-SHA, que precisa de uma chave de 256 bits). "
                    + "Defina NEXO_JWT_SECRET no ambiente, com o mesmo valor do usuario-api.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** Retorna as claims se o token for válido e não expirado; vazio caso contrário. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
