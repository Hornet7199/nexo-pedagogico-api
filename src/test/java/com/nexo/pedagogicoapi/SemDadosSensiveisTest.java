package com.nexo.pedagogicoapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Trava a decisão que motivou a refatoração: este sistema não guarda dado pessoal
 * de aluno.
 *
 * <p>A escola tem dois sistemas. O de aula administra alunos, matrícula e
 * rematrícula, e é lá que a ficha do aluno pertence. Este cuida de aprendizado e
 * retenção de conteúdo — e, sendo novo, é onde uma invasão custaria caro à toa.
 * Endereço, nascimento, sexo e checklist de documentos saíram por isso.
 *
 * <p>O teste ataca pelos dois lados, porque esconder da tela não protege nada: as
 * <b>rotas</b> que serviam esses dados precisam responder 404, e as <b>colunas</b>
 * precisam ter sumido do banco. Uma tela que deixa de exibir e uma coluna que
 * continua gravada é exatamente a situação que a mudança quis evitar.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste-sem-pii;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "nexo.seed.enabled=true"
})
class SemDadosSensiveisTest extends TesteApiBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("as rotas de dado pessoal e de vida escolar não existem mais")
    void rotasRemovidas() throws Exception {
        String diretor = bearer("diretor");

        for (String rota : new String[]{
                "/api/alunos/1/prontuario",          // ficha completa do aluno
                "/api/alunos/1/historico",           // histórico escolar em PDF
                "/api/alunos/1/endereco",            // endereço residencial
                "/api/cep/01310100",                 // consulta de CEP
                "/api/matriculas",                   // vida escolar
                "/api/matriculas/1/declaracao",      // declaração de matrícula
                "/api/matriculas/1/documentos/checklist",
                "/api/secretaria/dashboard"}) {
            mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, diretor))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("a tabela de alunos não tem coluna de dado pessoal")
    void colunasRemovidas() {
        List<String> colunas = colunasDe("alunos");

        for (String proibida : new String[]{
                "sexo", "data_nascimento", "cpf", "telefone",
                "email_responsavel", "cpf_responsavel", "telefone_responsavel",
                "endereco", "complemento", "endereco_cep", "endereco_logradouro",
                "endereco_numero", "endereco_complemento", "endereco_bairro",
                "endereco_cidade", "endereco_uf"}) {
            assertThat(colunas).as("alunos.%s precisa ter sido removida", proibida)
                    .doesNotContain(proibida);
        }

        // O que sobra é o necessário para servir conteúdo e medir retenção.
        assertThat(colunas).contains("nome", "turma_id");
    }

    @Test
    @DisplayName("o checklist de documentos e a matrícula não existem como tabela")
    void tabelasRemovidas() {
        List<String> tabelas = jdbc.queryForList(
                        "select table_name from information_schema.tables", String.class)
                .stream().map(t -> t.toLowerCase(Locale.ROOT)).toList();

        assertThat(tabelas).doesNotContain("documentos_entregues", "matriculas");
        assertThat(tabelas).as("o vínculo aluno-turma continua, enxuto").contains("inscricoes");
    }

    @Test
    @DisplayName("a inscrição guarda só o vínculo, sem status nem documentação")
    void inscricaoEhEnxuta() {
        List<String> colunas = colunasDe("inscricoes");

        assertThat(colunas).contains("aluno_id", "turma_id", "ativo");
        assertThat(colunas).doesNotContain("status", "documentacao", "ano_letivo",
                "origem_matricula_id", "data_matricula");
    }

    private List<String> colunasDe(String tabela) {
        return jdbc.queryForList("""
                        select column_name from information_schema.columns
                        where lower(table_name) = ?
                        """, String.class, tabela)
                .stream().map(c -> c.toLowerCase(Locale.ROOT)).toList();
    }
}
