package com.nexo.pedagogicoapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O contexto do aluno que o gamificacao-api consulta (turma, etapa, matérias fora da etapa). */
class ContextoAlunoTest extends TesteApiBase {

    @Test
    @DisplayName("aluno do médio: devolve a turma e só Ciências (do fundamental) como fora da etapa")
    void contextoDoAlunoDoMedio() throws Exception {
        mvc.perform(get("/api/aluno/contexto").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turma").value("2º Ano EM A"))
                .andExpect(jsonPath("$.etapa").value("MEDIO"))
                .andExpect(jsonPath("$.materiasForaDaEtapa.length()").value(1))
                .andExpect(jsonPath("$.materiasForaDaEtapa[0]").value("Ciências"));
    }

    @Test
    @DisplayName("sem token é 401; só o aluno consulta o próprio contexto")
    void acesso() throws Exception {
        mvc.perform(get("/api/aluno/contexto")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/aluno/contexto").header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isForbidden());
    }
}
