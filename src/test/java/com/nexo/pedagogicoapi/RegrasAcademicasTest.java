package com.nexo.pedagogicoapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regras acadêmicas que antes só existiam como intenção: etapa de ensino do aluno,
 * escopo do professor por matéria e progresso de estudo por matéria.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-regras;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "nexo.seed.enabled=true"
})
class RegrasAcademicasTest extends TesteApiBase {

    private JsonNode json(String rota, String bearer) throws Exception {
        return json.readTree(mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    // ── Etapa de ensino do aluno ─────────────────────────────────────────────

    @Test
    @DisplayName("aluno só enxerga as matérias da própria etapa, e o catálogo inteiro é do diretor")
    void materiasPorEtapaDoAluno() throws Exception {
        String aluno = bearer("aluno");
        String diretor = bearer("diretor");

        JsonNode catalogo = json("/api/materias", diretor);
        JsonNode doAluno = json("/api/materias", aluno);

        assertThat(doAluno.size())
                .as("o recorte por etapa tem que devolver menos que o catálogo inteiro")
                .isLessThan(catalogo.size());

        // O seed põe o aluno no 2º ano do médio: entram Física/Química/Biologia,
        // sai Ciências, que é exclusiva do fundamental.
        for (JsonNode m : doAluno) {
            assertThat(m.get("segmento").asText())
                    .as("matéria de outra etapa vazou para a lista do aluno: %s", m.get("nome").asText())
                    .isIn("MEDIO", "AMBOS");
        }
        assertThat(doAluno.toString()).doesNotContain("Ciências");
    }

    @Test
    @DisplayName("aluno não abre o conteúdo de matéria de outra etapa nem pedindo pelo id")
    void conteudoDeOutraEtapaEhBarrado() throws Exception {
        String aluno = bearer("aluno");
        String diretor = bearer("diretor");

        long ciencias = -1;
        long fisica = -1;
        for (JsonNode m : json("/api/materias", diretor)) {
            if ("Ciências".equals(m.get("nome").asText())) ciencias = m.get("id").asLong();
            if ("Física".equals(m.get("nome").asText())) fisica = m.get("id").asLong();
        }
        assertThat(ciencias).isGreaterThan(0);

        // Esconder da listagem não basta: o id continua adivinhável.
        mvc.perform(get("/api/materias/" + ciencias + "/conteudos")
                        .header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(status().isForbidden());

        // E a matéria da etapa dele segue aberta.
        mvc.perform(get("/api/materias/" + fisica + "/conteudos")
                        .header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(status().isOk());
    }

    // ── Escopo do professor por matéria ──────────────────────────────────────

    /** Um aluno de turma que o professor do seed (História) leciona. */
    private long alunoDaTurmaDoProfessor(String professor, String diretor) throws Exception {
        long turmaId = -1;
        for (JsonNode t : json("/api/turmas", diretor)) {
            if ("9º Ano A".equals(t.get("nome").asText())) turmaId = t.get("id").asLong();
        }
        assertThat(turmaId).as("o seed precisa da turma 9º Ano A").isGreaterThan(0);
        JsonNode presencas = json("/api/turmas/" + turmaId + "/frequencia", professor);
        assertThat(presencas.size()).isGreaterThan(0);
        return presencas.get(0).get("alunoId").asLong();
    }

    @Test
    @DisplayName("professor lança nota só da matéria que leciona, mesmo na turma dele")
    void notaSomenteNaPropriaMateria() throws Exception {
        String professor = bearer("professor"); // leciona História
        long alunoId = alunoDaTurmaDoProfessor(professor, bearer("diretor"));

        // Turma certa, matéria alheia: antes passava, porque disciplina era texto livre.
        mvc.perform(patch("/api/alunos/" + alunoId + "/notas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disciplina\":\"Matemática\",\"p1\":9.0}")
                        .header(HttpHeaders.AUTHORIZATION, professor))
                .andExpect(status().isForbidden());

        // Sem disciplina também não: virava a gaveta "Geral", fora de qualquer matéria.
        mvc.perform(patch("/api/alunos/" + alunoId + "/notas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"p1\":9.0}")
                        .header(HttpHeaders.AUTHORIZATION, professor))
                .andExpect(status().isBadRequest());

        // A dele passa — com acento ou sem, é a mesma matéria.
        mvc.perform(patch("/api/alunos/" + alunoId + "/notas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disciplina\":\"historia\",\"p1\":9.0}")
                        .header(HttpHeaders.AUTHORIZATION, professor))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("professor não cria avaliação de matéria que não leciona; diretor cria de qualquer uma")
    void avaliacaoSomenteNaPropriaMateria() throws Exception {
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Prova de Química\",\"disciplina\":\"Química\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Prova de História\",\"disciplina\":\"História\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isCreated());

        // O diretor responde pela escola inteira e corrige lançamento errado.
        mvc.perform(post("/api/avaliacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Simulado geral\",\"disciplina\":\"Química\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isCreated());
    }

    // ── Progresso por matéria ────────────────────────────────────────────────

    @Test
    @DisplayName("progresso da matéria sai dos conteúdos concluídos e marcar é idempotente")
    void progressoPorMateria() throws Exception {
        String aluno = bearer("aluno");

        JsonNode materias = json("/api/aluno/materias", aluno);
        assertThat(materias.size()).isGreaterThan(0);

        // Uma matéria com conteúdo publicado — é onde o progresso faz sentido.
        JsonNode alvo = null;
        for (JsonNode m : materias) {
            if (m.get("totalConteudos").asInt() > 0) { alvo = m; break; }
        }
        assertThat(alvo).as("o seed precisa de matéria com conteúdo").isNotNull();
        long materiaId = alvo.get("id").asLong();
        assertThat(alvo.get("percentual").asInt()).isZero();

        JsonNode conteudos = json("/api/materias/" + materiaId + "/conteudos", aluno);
        long conteudoId = conteudos.get(0).get("id").asLong();

        // Marcar duas vezes conta uma só — senão o percentual passaria de 100%.
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/aluno/conteudos/" + conteudoId + "/concluido")
                            .header(HttpHeaders.AUTHORIZATION, aluno))
                    .andExpect(status().isNoContent());
        }

        assertThat(json("/api/aluno/materias/" + materiaId + "/concluidos", aluno).size()).isEqualTo(1);

        int percentual = 0;
        for (JsonNode m : json("/api/aluno/materias", aluno)) {
            if (m.get("id").asLong() == materiaId) {
                assertThat(m.get("conteudosConcluidos").asInt()).isEqualTo(1);
                percentual = m.get("percentual").asInt();
            }
        }
        assertThat(percentual).isGreaterThan(0);

        // Desmarcar volta ao zero, e repetir também não é erro.
        for (int i = 0; i < 2; i++) {
            mvc.perform(delete("/api/aluno/conteudos/" + conteudoId + "/concluido")
                            .header(HttpHeaders.AUTHORIZATION, aluno))
                    .andExpect(status().isNoContent());
        }
        assertThat(json("/api/aluno/materias/" + materiaId + "/concluidos", aluno).size()).isZero();
    }

    @Test
    @DisplayName("aluno não conclui conteúdo de matéria fora da etapa dele")
    void progressoRespeitaAEtapa() throws Exception {
        String aluno = bearer("aluno");
        long ciencias = -1;
        for (JsonNode m : json("/api/materias", bearer("diretor"))) {
            if ("Ciências".equals(m.get("nome").asText())) ciencias = m.get("id").asLong();
        }
        mvc.perform(get("/api/aluno/materias/" + ciencias + "/concluidos")
                        .header(HttpHeaders.AUTHORIZATION, aluno))
                .andExpect(status().isForbidden());
    }

    // ── Escopo das telas do professor ────────────────────────────────────────

    @Test
    @DisplayName("professor vê só as turmas que leciona e só as matérias dele")
    void telasDoProfessorVemRecortadas() throws Exception {
        String professor = bearer("professor"); // leciona História, em 3 turmas
        String diretor = bearer("diretor");

        JsonNode todas = json("/api/turmas", diretor);
        JsonNode minhas = json("/api/turmas", professor);

        assertThat(minhas.size())
                .as("o seletor do diário e o de notas listavam a escola inteira")
                .isLessThan(todas.size());
        assertThat(minhas.size()).isGreaterThan(0);

        // Toda turma listada tem que ser acionável: antes, escolher a turma de um
        // colega só rendia 403 no primeiro clique seguinte.
        for (JsonNode t : minhas) {
            mvc.perform(get("/api/turmas/" + t.get("id").asLong() + "/frequencia")
                            .header(HttpHeaders.AUTHORIZATION, professor))
                    .andExpect(status().isOk());
        }

        JsonNode materias = json("/api/professor/materias", professor);
        assertThat(materias.size()).isEqualTo(1);
        assertThat(materias.get(0).asText()).isEqualTo("História");
    }
}
