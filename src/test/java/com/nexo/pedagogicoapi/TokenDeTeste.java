package com.nexo.pedagogicoapi;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Emite, nos testes, os tokens que em produção vêm do usuario-api: mesma chave, mesmos
 * claims (jti, sub, uid, role, nome). O pedagógico só valida token, então o teste precisa
 * de um emissor próprio.
 *
 * <p>As contas são as do seed do usuario-api, com os mesmos ids de usuário: é o que liga o
 * token do professor ao Professor 1 e o do aluno ao Aluno 1 criados pelo DataSeeder daqui.
 */
public final class TokenDeTeste {

    /** Igual a nexo.jwt.secret do application-teste.yml. */
    public static final String SEGREDO =
            "nexo-gestao-escolar-chave-de-desenvolvimento-nao-usar-em-producao-0123456789";

    public record Conta(long uid, String role, String nome) {}

    /** login → conta, como no seed do usuario-api. */
    public static final Map<String, Conta> CONTAS = Map.of(
            "diretor", new Conta(1, "DIRETOR", "Diretor Silva"),
            "professor", new Conta(2, "PROFESSOR", "Prof. Roberto Alves"),
            "aluno", new Conta(3, "ALUNO", "Gabriel Mendes"),
            "admin", new Conta(4, "ADMIN", "Helena Ramos"));

    private TokenDeTeste() {}

    public static String gerar(String login) {
        Conta c = CONTAS.get(login);
        if (c == null) throw new IllegalArgumentException("Conta de teste desconhecida: " + login);
        return gerar(login, c.uid(), c.role(), c.nome());
    }

    public static String gerar(String login, Long uid, String role, String nome) {
        return gerar(SEGREDO, login, uid, role, nome);
    }

    /** Com a chave informada — para testar token assinado por quem não é o usuario-api. */
    public static String gerar(String segredo, String login, Long uid, String role, String nome) {
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(login)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(900)));
        if (uid != null) builder.claim("uid", uid);
        if (role != null) builder.claim("role", role);
        if (nome != null) builder.claim("nome", nome);
        return builder.signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
