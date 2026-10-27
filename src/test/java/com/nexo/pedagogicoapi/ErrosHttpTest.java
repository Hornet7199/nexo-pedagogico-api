package com.nexo.pedagogicoapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de status HTTP para requisição malfeita. Todos os casos abaixo
 * respondiam 500 antes de os handlers específicos existirem — este teste existe
 * para que não voltem a responder.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "nexo.seed.enabled=true"
})
class ErrosHttpTest extends TesteApiBase {

    @Test
    @DisplayName("valor inválido em parâmetro tipado devolve 400, não 500")
    void parametroComTipoErrado() throws Exception {
        String diretor = bearer("diretor");

        mvc.perform(get("/api/inscricoes").param("ativo", "NAO_EXISTE")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mvc.perform(get("/api/inscricoes").param("turma", "abc")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("parâmetro obrigatório ausente devolve 400")
    void parametroObrigatorioAusente() throws Exception {
        mvc.perform(get("/api/relatorios/desempenho/export")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.formato").exists());
    }

    @Test
    @DisplayName("JSON malformado e corpo ausente devolvem 400")
    void corpoIlegivel() throws Exception {
        String diretor = bearer("diretor");

        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":")
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, diretor))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("método não suportado devolve 405 com o cabeçalho Allow")
    void metodoNaoSuportado() throws Exception {
        mvc.perform(delete("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists(HttpHeaders.ALLOW));
    }

    @Test
    @DisplayName("Content-Type não suportado devolve 415")
    void mediaTypeNaoSuportado() throws Exception {
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.TEXT_PLAIN).content("x")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("rota de API inexistente devolve 404, não o index.html da SPA")
    void rotaInexistente() throws Exception {
        mvc.perform(get("/api/nao-existe").header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("constraint declarada no DTO é aplicada de fato")
    void validacaoDeCorpo() throws Exception {
        // Havia @NotBlank no record mas faltava @Valid no handler: texto em branco passava.
        mvc.perform(post("/api/alunos/1/observacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"   \"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.texto").exists());
    }
}
