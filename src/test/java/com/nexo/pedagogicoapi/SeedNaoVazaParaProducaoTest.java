package com.nexo.pedagogicoapi;

import com.nexo.pedagogicoapi.config.DataSeeder;
import com.nexo.pedagogicoapi.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:teste-seed-guard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class SeedNaoVazaParaProducaoTest extends TesteApiBase {

    @Autowired
    private AlunoRepository alunos;

    @Autowired
    private ProfessorRepository professores;

    @Test
    @DisplayName("o seed liga o aluno e o professor às contas pelos ids de usuário do usuario-api")
    void seedAlinhadoComOUsuarioApi() {
        var gabriel = alunos.findByUsuarioId(3L).orElseThrow();
        assertThat(gabriel.getId()).as("aluno 1 = Gabriel, como no seed do usuario-api").isEqualTo(1L);
        assertThat(gabriel.getNome()).isEqualTo("Gabriel Mendes");

        var roberto = professores.findByUsuarioId(2L).orElseThrow();
        assertThat(roberto.getId()).isEqualTo(1L);
        assertThat(roberto.getNome()).isEqualTo("Prof. Roberto Alves");

        assertThat(alunos.count()).as("12 alunos, ids 1 a 12").isEqualTo(12);
        assertThat(professores.count()).as("5 docentes, ids 1 a 5").isEqualTo(5);
    }

    @Test
    @DisplayName("com o perfil prod o seed se recusa a rodar, mesmo ligado pela flag")
    void seedRecusaPerfilProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        var seeder = new DataSeeder(mock(TurmaRepository.class), mock(AlunoRepository.class),
                mock(ProfessorRepository.class), mock(MateriaRepository.class), mock(InscricaoRepository.class),
                mock(FrequenciaRepository.class), mock(NotaRepository.class), mock(AvaliacaoRepository.class),
                mock(QuestaoRepository.class), mock(ObservacaoPedagogicaRepository.class),
                mock(EventoAuditoriaRepository.class), mock(AulaAgendadaRepository.class),
                mock(AtividadeProfessorRepository.class), prod);

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }
}
