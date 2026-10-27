package com.nexo.pedagogicoapi;

import com.nexo.pedagogicoapi.repository.AlunoRepository;
import com.nexo.pedagogicoapi.repository.ProfessorRepository;
import com.nexo.pedagogicoapi.repository.TurmaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O canal pelo qual o usuario-api mantém a cópia local de aluno e professor em dia. */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:teste-sync;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class SincronizacaoTest extends TesteApiBase {

    @Autowired AlunoRepository alunos;
    @Autowired ProfessorRepository professores;
    @Autowired TurmaRepository turmas;

    private long umaTurma() {
        return turmas.findAll().get(0).getId();
    }

    private String corpoAluno(String nome, long turmaId) {
        return "{\"usuarioId\":900,\"nome\":\"" + nome + "\",\"turmaId\":" + turmaId + ",\"foto\":null}";
    }

    @Test
    @DisplayName("o diretor sincroniza um aluno novo com o id do usuario-api, e repetir não duplica")
    void sincronizaAlunoEmUpsert() throws Exception {
        long turmaId = umaTurma();
        long antes = alunos.count();

        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/sync/alunos/500").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAluno("Aluno Novo Teste", turmaId))
                            .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                    .andExpect(status().isNoContent());
        }

        var aluno = alunos.findById(500L).orElseThrow();
        assertThat(aluno.getNome()).isEqualTo("Aluno Novo Teste");
        assertThat(aluno.getUsuarioId()).isEqualTo(900L);
        assertThat(aluno.getTurma().getId()).isEqualTo(turmaId);
        assertThat(alunos.count()).as("a segunda chamada atualiza, não duplica").isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("aluno e professor não sincronizam cadastro: só a gestão")
    void soAGestaoSincroniza() throws Exception {
        for (String login : new String[]{"aluno", "professor"}) {
            mvc.perform(put("/api/sync/alunos/501").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAluno("Intruso", umaTurma()))
                            .header(HttpHeaders.AUTHORIZATION, bearer(login)))
                    .andExpect(status().isForbidden());
        }
        assertThat(alunos.existsById(501L)).isFalse();
    }

    @Test
    @DisplayName("turma inexistente é recusada com o erro no campo")
    void turmaInexistente() throws Exception {
        mvc.perform(put("/api/sync/alunos/502").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAluno("Sem Turma Valida", 99999))
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.turmaId").value("Turma inexistente."));
    }

    @Test
    @DisplayName("sincroniza professor com as matérias do catálogo e recusa matéria que não existe")
    void sincronizaProfessor() throws Exception {
        mvc.perform(put("/api/sync/professores/600").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":901,\"nome\":\"Profa. Sync\",\"materiaIds\":[999999]}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.materiaIds").exists());

        mvc.perform(put("/api/sync/professores/600").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":901,\"nome\":\"Profa. Sync\",\"materiaIds\":[]}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isNoContent());

        assertThat(professores.findById(600L).orElseThrow().getNome()).isEqualTo("Profa. Sync");
    }

    @Test
    @DisplayName("o dono da conta atualiza o próprio nome e foto; outro usuário não")
    void perfilSoDoDono() throws Exception {
        // aluno do seed: usuário 3 → aluno 1
        mvc.perform(patch("/api/sync/usuarios/3/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Gabriel M. Atualizado\",\"foto\":\"/api/fotos/abc\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isNoContent());

        var gabriel = alunos.findByUsuarioId(3L).orElseThrow();
        assertThat(gabriel.getNome()).isEqualTo("Gabriel M. Atualizado");
        assertThat(gabriel.getFoto()).isEqualTo("/api/fotos/abc");

        mvc.perform(patch("/api/sync/usuarios/3/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Invasor\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isForbidden());
        assertThat(alunos.findByUsuarioId(3L).orElseThrow().getNome()).isEqualTo("Gabriel M. Atualizado");
    }

    @Test
    @DisplayName("sem token a sincronização responde 401")
    void semToken() throws Exception {
        mvc.perform(put("/api/sync/alunos/503").contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAluno("Anonimo", umaTurma())))
                .andExpect(status().isUnauthorized());
    }
}
