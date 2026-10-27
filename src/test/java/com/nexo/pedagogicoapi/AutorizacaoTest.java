package com.nexo.pedagogicoapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de papéis e escopo por turma. Trava as regras corrigidas em
 * "Professor só age sobre a turma que leciona" e na revisão de autorização.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-authz;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "nexo.seed.enabled=true"
})
class AutorizacaoTest extends TesteApiBase {

    @Test
    @DisplayName("sem token e com token inválido a API responde 401")
    void semCredencial() throws Exception {
        mvc.perform(get("/api/avaliacoes")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());

        // Assinatura válida não basta: sem o conjunto de claims esperado não autentica.
        mvc.perform(get("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("aluno não alcança endpoints de diretor nem de professor")
    void alunoNaoEscala() throws Exception {
        String aluno = bearer("aluno");
        for (String rota : new String[]{
                "/api/evasao/risco", "/api/relatorios/desempenho", "/api/auditoria/eventos",
                "/api/monitoramento/professores", "/api/inscricoes", "/api/avaliacoes",
                "/api/admin/dashboard"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, aluno))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("professor não alcança endpoints exclusivos de diretor")
    void professorNaoEscala() throws Exception {
        String professor = bearer("professor");
        for (String rota : new String[]{
                "/api/evasao/risco", "/api/relatorios/desempenho", "/api/auditoria/eventos",
                "/api/inscricoes", "/api/admin/dashboard"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, professor))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("cada perfil acessa o próprio painel")
    void painelDoProprioPerfil() throws Exception {
        mvc.perform(get("/api/aluno/notas").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/professor/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/evasao/risco").header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrador cuida do catálogo e das inscrições, mas não escala para o resto")
    void escopoDoAdministrador() throws Exception {
        String admin = bearer("admin");

        // O trabalho de quem administra: acesso e catálogo.
        for (String rota : new String[]{
                "/api/admin/dashboard", "/api/admin/catalogo", "/api/inscricoes"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, admin))
                    .andExpect(status().isOk());
        }

        // Gestão pedagógica e auditoria continuam sendo do diretor.
        for (String rota : new String[]{
                "/api/evasao/risco", "/api/relatorios/desempenho", "/api/auditoria/eventos",
                "/api/monitoramento/professores", "/api/avaliacoes"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, admin))
                    .andExpect(status().isForbidden());
        }

        // E os demais perfis não alcançam o painel do administrador (diretor supervisiona).
        mvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor só enxerga avaliações das turmas que leciona")
    void escopoDeAvaliacoesPorTurma() throws Exception {
        String diretor = bearer("diretor");
        String professor = bearer("professor");

        // "1º Ano EM A" é da professora de matemática no seed, não do "professor".
        JsonNode turmas = json.readTree(mvc.perform(get("/api/turmas")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        long turmaDeOutroDocente = -1;
        for (JsonNode t : turmas) {
            if ("1º Ano EM A".equals(t.get("nome").asText())) {
                turmaDeOutroDocente = t.get("id").asLong();
            }
        }
        assertThat(turmaDeOutroDocente)
                .as("o seed precisa conter a turma 1º Ano EM A, atribuída a outro docente")
                .isGreaterThan(0);

        // Pedir explicitamente a turma alheia é barrado...
        mvc.perform(get("/api/avaliacoes").param("turma", String.valueOf(turmaDeOutroDocente))
                        .header(HttpHeaders.AUTHORIZATION, professor))
                .andExpect(status().isForbidden());

        // ...e a listagem sem filtro não pode vazar mais do que a do diretor.
        int vistasPeloProfessor = json.readTree(mvc.perform(get("/api/avaliacoes")
                        .header(HttpHeaders.AUTHORIZATION, professor))
                .andReturn().getResponse().getContentAsString()).size();
        int vistasPeloDiretor = json.readTree(mvc.perform(get("/api/avaliacoes")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andReturn().getResponse().getContentAsString()).size();

        assertThat(vistasPeloProfessor)
                .as("professor não pode ver a escola inteira")
                .isLessThan(vistasPeloDiretor);
    }

    @Test
    @DisplayName("professor não registra conteúdo em turma que não leciona")
    void conteudoSomenteNaPropriaTurma() throws Exception {
        String diretor = bearer("diretor");
        JsonNode turmas = json.readTree(mvc.perform(get("/api/turmas")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andReturn().getResponse().getContentAsString());

        long alheia = -1;
        for (JsonNode t : turmas) {
            if ("1º Ano EM A".equals(t.get("nome").asText())) alheia = t.get("id").asLong();
        }

        mvc.perform(get("/api/turmas/" + alheia + "/frequencia")
                        .header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isForbidden());
    }
}
