package com.nexo.pedagogicoapi.security;

import com.nexo.pedagogicoapi.TokenDeTeste;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthFilterTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-abcdef";

    private final JwtAuthFilter filtro = new JwtAuthFilter(new JwtService(SEGREDO));

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void limparDepois() {
        SecurityContextHolder.clearContext();
    }

    /** Roda o filtro e devolve a autenticação resultante (null = requisição anônima). */
    private Authentication executar() throws Exception {
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void comBearer(String token) {
        request.addHeader("Authorization", "Bearer " + token);
    }

    private static String token(String subject, Long uid, String role) {
        return TokenDeTeste.gerar(SEGREDO, subject, uid, role, "Fulano");
    }

    @Test
    void tokenValidoAutenticaComOPapelDoUsuario() throws Exception {
        comBearer(token("carlos.diretor", 7L, "DIRETOR"));

        Authentication auth = executar();

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UsuarioAutenticado.class);
        UsuarioAutenticado principal = (UsuarioAutenticado) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.login()).isEqualTo("carlos.diretor");
        assertThat(principal.role()).isEqualTo("DIRETOR");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_DIRETOR");
    }

    @Test
    void requisicaoSemHeaderSegueAnonima() throws Exception {
        assertThat(executar()).isNull();
    }

    @Test
    void headerSemPrefixoBearerEhIgnorado() throws Exception {
        request.addHeader("Authorization", token("ana", 1L, "ALUNO")); // sem "Bearer "
        assertThat(executar()).isNull();
    }

    @Test
    void tokenDeOutraChaveNaoAutentica() throws Exception {
        comBearer(TokenDeTeste.gerar("outro-segredo-completamente-diferente-0123456789", "ana", 1L, "ALUNO", "Ana"));
        assertThat(executar()).isNull();
    }

    @Test
    void tokenExpiradoNaoAutentica() throws Exception {
        comBearer(JwtServiceTest.tokenExpirado(SEGREDO));
        assertThat(executar()).isNull();
    }

    @Test
    void tokenSemRoleNaoAutentica() throws Exception {
        comBearer(token("ana", 1L, null));
        assertThat(executar()).isNull();
    }

    @Test
    void tokenSemUidNaoAutentica() throws Exception {
        comBearer(token("ana", null, "ALUNO"));
        assertThat(executar()).isNull();
    }

    @Test
    void tokenComSubjectEmBrancoNaoAutentica() throws Exception {
        comBearer(token("   ", 1L, "ALUNO"));
        assertThat(executar()).isNull();
    }

    @Test
    void roleForaDoEnumNaoAutentica() throws Exception {
        comBearer(token("ana", 1L, "SUPERADMIN"));
        assertThat(executar()).isNull();
    }
}
