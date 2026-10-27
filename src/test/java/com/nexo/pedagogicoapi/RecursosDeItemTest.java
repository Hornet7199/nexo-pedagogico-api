package com.nexo.pedagogicoapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recursos endereçáveis: todo POST que cria algo aponta o Location para um GET que
 * responde, e cada GET de item respeita o mesmo escopo das demais operações sobre
 * aquele recurso — um GET novo sem essa checagem seria um atalho de leitura.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-itens;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "nexo.seed.enabled=true"
})
class RecursosDeItemTest extends TesteApiBase {

    /** Segue o Location devolvido no 201 e confere que ele resolve. */
    private void locationResolve(String location, String autorizacao) throws Exception {
        assertThat(location).as("o 201 precisa trazer Location").isNotBlank();
        String caminho = location.substring(location.indexOf("/api/"));
        mvc.perform(get(caminho).header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("questão e observação devolvem 201 com Location que resolve")
    void criarDemaisRecursos() throws Exception {
        String diretor = bearer("diretor");

        String questao = mvc.perform(post("/api/questoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enunciado\":\"Quem proclamou a República?\",\"disciplina\":\"História\"}")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
        locationResolve(questao, diretor);

        String observacao = mvc.perform(post("/api/alunos/1/observacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Participou bem da aula.\"}")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
        locationResolve(observacao, diretor);
    }

    @Test
    @DisplayName("GET de item inexistente devolve 404")
    void itemInexistente() throws Exception {
        String diretor = bearer("diretor");
        for (String rota : new String[]{
                "/api/questoes/999999", "/api/avaliacoes/999999"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, diretor))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("aluno não alcança os GETs de item de gestão")
    void alunoNaoLeItensDeGestao() throws Exception {
        String aluno = bearer("aluno");
        for (String rota : new String[]{
                "/api/questoes/1", "/api/avaliacoes/1"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, aluno))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("observação de outro aluno não vaza pelo id na URI")
    void observacaoNaoVazaPorId() throws Exception {
        String diretor = bearer("diretor");

        String location = mvc.perform(post("/api/alunos/1/observacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Observação do aluno 1.\"}")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
        String id = location.substring(location.lastIndexOf('/') + 1);

        // Mesmo id, aluno errado na URI: precisa dar 404, não devolver o conteúdo.
        mvc.perform(get("/api/alunos/2/observacoes/" + id).header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isNotFound());
    }
}
