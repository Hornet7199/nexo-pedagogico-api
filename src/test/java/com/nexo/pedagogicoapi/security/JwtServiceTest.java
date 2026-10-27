package com.nexo.pedagogicoapi.security;

import com.nexo.pedagogicoapi.TokenDeTeste;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-abcdef";
    private static final String OUTRO_SEGREDO = "outro-segredo-completamente-diferente-0123456789";

    private final JwtService jwtService = new JwtService(SEGREDO);

    private static String token(String segredo) {
        return TokenDeTeste.gerar(segredo, "ana.professora", 42L, "PROFESSOR", "Ana");
    }

    @Test
    void tokenValidoCarregaAsClaims() {
        Optional<Claims> claims = jwtService.validar(token(SEGREDO));

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("ana.professora");
        assertThat(claims.get().get("uid", Long.class)).isEqualTo(42L);
        assertThat(claims.get().get("role", String.class)).isEqualTo("PROFESSOR");
        assertThat(claims.get().get("nome", String.class)).isEqualTo("Ana");
    }

    @Test
    void tokenAssinadoComOutraChaveEhRejeitado() {
        assertThat(jwtService.validar(token(OUTRO_SEGREDO))).isEmpty();
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        assertThat(jwtService.validar(tokenExpirado(SEGREDO))).isEmpty();
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        String[] partes = token(SEGREDO).split("\\.");
        String payloadAdulterado = partes[1].substring(0, partes[1].length() - 2)
                + (partes[1].endsWith("A") ? "B" : "A");

        assertThat(jwtService.validar(partes[0] + "." + payloadAdulterado + "." + partes[2])).isEmpty();
    }

    @Test
    void lixoNoLugarDoTokenNaoExplode() {
        assertThat(jwtService.validar("nem-parece-um-jwt")).isEmpty();
        assertThat(jwtService.validar("")).isEmpty();
    }

    @Test
    void segredoCurtoDerrubaAConstrucao() {
        assertThatThrownBy(() -> new JwtService("curto-demais"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nexo.jwt.secret")
                .hasMessageContaining("32");
    }

    @Test
    void segredoAusenteDerrubaAConstrucao() {
        assertThatThrownBy(() -> new JwtService(null))
                .isInstanceOf(IllegalStateException.class);
    }

    static String tokenExpirado(String segredo) {
        SecretKey key = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);
        return Jwts.builder()
                .subject("ana.professora")
                .claim("uid", 42L)
                .claim("role", "PROFESSOR")
                .claim("nome", "Ana")
                .issuedAt(Date.from(ontem))
                .expiration(Date.from(ontem.plus(15, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }
}
