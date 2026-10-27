package com.nexo.pedagogicoapi.config;

import com.nexo.pedagogicoapi.domain.*;
import com.nexo.pedagogicoapi.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Popula o banco de desenvolvimento com turmas, notas, frequência, avaliações e demais
 * dados pedagógicos de exemplo. Roda só com {@code nexo.seed.enabled=true}, em banco
 * vazio e nunca com o perfil {@code prod}.
 *
 * <p>Não cria conta de acesso: as contas (diretor, professor, aluno, admin) são do
 * usuario-api. O que este seed faz é criar as cópias locais de aluno e professor com os
 * <b>mesmos ids</b> que o seed do usuario-api usa — é o que liga uma nota ao aluno certo
 * e o token do professor ao seu cadastro. Os dois seeds precisam andar juntos: mudar a
 * ordem ou a lista de um sem o outro desalinha os ids.
 *
 * <ul>
 *   <li>usuário 2 (professor) → professor 1; usuário 3 (aluno) → aluno 1;</li>
 *   <li>demais alunos (2–12) e professores (2–5) não têm conta de acesso.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "nexo.seed.enabled", havingValue = "true")
@Order(2) // depois do CatalogoMaterias e do CatalogoTurmas
public class DataSeeder implements CommandLineRunner {

    /** Ids de usuário no seed do usuario-api. */
    private static final long USUARIO_PROFESSOR = 2L;
    private static final long USUARIO_ALUNO = 3L;

    private final TurmaRepository turmas;
    private final AlunoRepository alunos;
    private final ProfessorRepository professores;
    private final MateriaRepository materias;
    private final InscricaoRepository inscricoes;
    private final FrequenciaRepository frequencias;
    private final NotaRepository notas;
    private final AvaliacaoRepository avaliacoes;
    private final QuestaoRepository questoes;
    private final ObservacaoPedagogicaRepository observacoes;
    private final EventoAuditoriaRepository eventos;
    private final AulaAgendadaRepository aulasAgendadas;
    private final AtividadeProfessorRepository atividadesProfessor;
    private final Environment env;

    public DataSeeder(TurmaRepository turmas, AlunoRepository alunos, ProfessorRepository professores,
                      MateriaRepository materias, InscricaoRepository inscricoes,
                      FrequenciaRepository frequencias, NotaRepository notas,
                      AvaliacaoRepository avaliacoes, QuestaoRepository questoes,
                      ObservacaoPedagogicaRepository observacoes, EventoAuditoriaRepository eventos,
                      AulaAgendadaRepository aulasAgendadas, AtividadeProfessorRepository atividadesProfessor,
                      Environment env) {
        this.turmas = turmas;
        this.alunos = alunos;
        this.professores = professores;
        this.materias = materias;
        this.inscricoes = inscricoes;
        this.frequencias = frequencias;
        this.notas = notas;
        this.avaliacoes = avaliacoes;
        this.questoes = questoes;
        this.observacoes = observacoes;
        this.eventos = eventos;
        this.aulasAgendadas = aulasAgendadas;
        this.atividadesProfessor = atividadesProfessor;
        this.env = env;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "nexo.seed.enabled=true com o perfil prod: o seed insere dados de exemplo.");
        }
        if (alunos.count() > 0) return;

        // ── Docentes (ids iguais aos do usuario-api) ──
        Professor roberto = professor(1L, USUARIO_PROFESSOR, "Prof. Roberto Alves", "1º A, 2º A, 3º C",
                2, 1.5, 51, 4.9, 51, 53, "História");
        Professor carla = professor(2L, null, "Profa. Carla Nunes", "1º A, 1º B, 2º A",
                3, 1.8, 47, 4.8, 47, 50, "Matemática");
        professor(3L, null, "Profa. Juliana Costa Santos", "1º C, 2º C, 3º B",
                5, 2.1, 43, 4.6, 43, 48, "Biologia");
        professor(4L, null, "Profa. Mariana Oliveira", "2º B, 3º B, 3º A",
                6, 2.8, 41, 4.4, 44, 50, "Química", "Biologia");
        professor(5L, null, "Prof. Carlos Eduardo Silva", "2º B, 3º A",
                12, 4.2, 28, 4.2, 38, 50, "Física", "Matemática");

        // ── Turmas ──
        Turma t9a = turma("9º Ano A", "Manhã");
        Turma t9b = turma("9º Ano B", "Manhã");
        Turma t1em = turma("1º Ano EM A", "Tarde");
        Turma t2em = turma("2º Ano EM A", "Tarde");

        // Roberto leciona 3 turmas; Carla leciona a outra
        t9a.setProfessor(roberto);
        t9b.setProfessor(roberto);
        t2em.setProfessor(roberto);
        t1em.setProfessor(carla);
        turmas.save(t9a);
        turmas.save(t9b);
        turmas.save(t2em);
        turmas.save(t1em);

        // ── Alunos (nome, turma, engajamento) — a ordem define o id (1 a 12) ──
        String[][] dadosAlunos = {
                {"Gabriel Mendes", "2em", "88"},
                {"Ana Beatriz Souza", "9a", "92"},
                {"Carlos Eduardo Lima", "9a", "45"},
                {"Mariana Ferreira", "9a", "78"},
                {"João Pedro Santos", "9b", "25"},
                {"Larissa Oliveira", "9b", "83"},
                {"Rafael Costa", "9b", "52"},
                {"Beatriz Almeida", "1em", "95"},
                {"Lucas Martins", "1em", "38"},
                {"Julia Rodrigues", "1em", "72"},
                {"Pedro Henrique Silva", "2em", "60"},
                {"Camila Barbosa", "2em", "90"},
        };

        Random random = new Random(42);
        LocalDate hoje = LocalDate.now();
        long proximoId = 1;
        Aluno carlos = null;

        for (String[] d : dadosAlunos) {
            Turma turma = switch (d[1]) {
                case "9a" -> t9a;
                case "9b" -> t9b;
                case "1em" -> t1em;
                default -> t2em;
            };
            Aluno aluno = new Aluno();
            aluno.setId(proximoId++);
            aluno.setNome(d[0]);
            aluno.setTurma(turma);
            aluno.setEngajamento(Integer.parseInt(d[2]));
            aluno.setUltimoAcessoEm(hoje.minusDays(random.nextInt(11)).atStartOfDay(ZoneOffset.UTC).toInstant());
            // Quanto menor o engajamento, mais intervenções pedagógicas registradas
            int qtdIntervencoes = aluno.getEngajamento() < 50 ? 1 + random.nextInt(3) : random.nextInt(2);
            aluno.setIntervencoes(qtdIntervencoes);
            if (qtdIntervencoes > 0) {
                aluno.setUltimaIntervencaoEm(hoje.minusDays(1 + random.nextInt(8)).atStartOfDay(ZoneOffset.UTC).toInstant());
            }
            if (d[0].equals("Gabriel Mendes")) aluno.setUsuarioId(USUARIO_ALUNO);
            aluno = alunos.save(aluno);
            if (d[0].startsWith("Carlos")) carlos = aluno;

            // Inscrição na turma de estudo. Uma em dez nasce inativa, só para que a tela
            // de inscrições tenha os dois estados no banco de exemplo.
            Inscricao inscricao = new Inscricao();
            inscricao.setAluno(aluno);
            inscricao.setTurma(turma);
            inscricao.setAtivo(random.nextInt(10) > 0);
            inscricao.setCriadaEm(hoje.minusMonths(5).plusDays(random.nextInt(30))
                    .atStartOfDay(ZoneOffset.UTC).toInstant());
            inscricoes.save(inscricao);

            // Notas — alunos com engajamento baixo tiram notas menores
            double base = 3.5 + (aluno.getEngajamento() / 100.0) * 6.0;
            for (String disciplina : List.of("História", "Matemática", "Português")) {
                Nota nota = new Nota();
                nota.setAluno(aluno);
                nota.setTurma(turma);
                nota.setDisciplina(disciplina);
                nota.setPeriodo("2026-1");
                nota.setP1(nota10(base + random.nextGaussian()));
                nota.setP2(nota10(base + random.nextGaussian()));
                nota.setT1(nota10(base + 0.5 + random.nextGaussian()));
                nota.setParticipacao(nota10(aluno.getEngajamento() / 10.0));
                notas.save(nota);
            }

            // Frequência das últimas 6 semanas (dias úteis)
            double chancePresenca = 0.55 + (aluno.getEngajamento() / 100.0) * 0.44;
            for (int dia = 1; dia <= 42; dia++) {
                LocalDate data = hoje.minusDays(dia);
                if (data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                Frequencia f = new Frequencia();
                f.setAluno(aluno);
                f.setTurma(turma);
                f.setData(data);
                f.setPresente(random.nextDouble() < chancePresenca);
                frequencias.save(f);
            }
        }

        // ── Avaliações ──
        avaliacao("Prova Bimestral — Revolução Industrial", "História", t9a, "Prova",
                Avaliacao.Status.EM_CORRECAO, hoje.minusDays(3), 28, 12);
        avaliacao("Trabalho — Brasil Colônia", "História", t9b, "Trabalho",
                Avaliacao.Status.PUBLICADA, hoje.plusDays(4), 10, 0);
        avaliacao("Simulado ENEM — 1ª aplicação", "Geral", t2em, "Simulado",
                Avaliacao.Status.CORRIGIDA, hoje.minusDays(15), 30, 0);
        avaliacao("Prova — Idade Média", "História", t1em, "Prova",
                Avaliacao.Status.EM_CORRECAO, hoje.minusDays(1), 25, 25);

        // ── Questões ──
        questao("Explique as principais causas da Primeira Guerra Mundial.", "História",
                Questao.Tipo.DISSERTATIVA, Questao.Dificuldade.DIFICIL);
        questao("Qual evento marcou o início da Idade Contemporânea?", "História",
                Questao.Tipo.OBJETIVA, Questao.Dificuldade.MEDIA);
        questao("Resolva: 2x + 5 = 15. Qual o valor de x?", "Matemática",
                Questao.Tipo.OBJETIVA, Questao.Dificuldade.FACIL);

        // ── Observação pedagógica ──
        ObservacaoPedagogica obs = new ObservacaoPedagogica();
        obs.setAluno(carlos);
        obs.setAutorNome(roberto.getNome());
        obs.setTexto("Aluno apresentou queda de rendimento no último mês. Encaminhado para acompanhamento.");
        observacoes.save(obs);

        eventos.save(new EventoAuditoria("Diretor Silva", EventoAuditoria.Tipo.ALTERACAO,
                "Catálogo atualizado", "Conteúdos publicados", "127.0.0.1"));
        eventos.save(new EventoAuditoria(roberto.getNome(), EventoAuditoria.Tipo.ALTERACAO,
                "Notas lançadas", "Turma 9º Ano A — História", "127.0.0.1"));

        // ── Grade de horários (próximas aulas) do professor Roberto ──
        Instant agora = Instant.now();
        aulasAgendadas.save(new AulaAgendada(roberto, t9a, "História", hoje, "14:00", "204", 3));
        aulasAgendadas.save(new AulaAgendada(roberto, t9b, "História", hoje, "15:50", "101", 3));
        aulasAgendadas.save(new AulaAgendada(roberto, t2em, "História", hoje.plusDays(1), "08:00", "205", 3));
        aulasAgendadas.save(new AulaAgendada(roberto, t9a, "História", hoje.plusDays(1), "10:00", "202", 3));

        // ── Feed de atividades recentes do professor ──
        atividadesProfessor.save(new AtividadeProfessor(roberto, "correcao",
                "Prova Bimestral — 9º Ano A corrigida", "9º Ano A", "bi-pencil-square", "text-purple", agora.minus(2, ChronoUnit.HOURS)));
        atividadesProfessor.save(new AtividadeProfessor(roberto, "diario",
                "Frequência registrada", "2º Ano EM", "bi-journal-check", "text-green", agora.minus(5, ChronoUnit.HOURS)));
        atividadesProfessor.save(new AtividadeProfessor(roberto, "avaliacao",
                "Nova avaliação criada: Revolução Industrial", "9º Ano A", "bi-file-earmark-plus", "text-orange", agora.minus(1, ChronoUnit.DAYS)));
    }

    private Turma turma(String nome, String turno) {
        Turma t = new Turma();
        t.setNome(nome);
        t.setAnoLetivo(2026);
        t.setTurno(turno);
        return turmas.save(t);
    }

    /** Docente com as métricas de produtividade; o id é o do professor no usuario-api. */
    private Professor professor(long id, Long usuarioId, String nome, String turmasAtendidas, int pendentes,
                                double tempo, int interacoes, double avaliacao, int concluidas, int total,
                                String... disciplinas) {
        Professor p = new Professor();
        p.setId(id);
        p.setUsuarioId(usuarioId);
        p.setNome(nome);
        p.setMaterias(materiasPorNome(disciplinas));
        p.setFoto(null); // avatar padrão até o docente enviar a própria foto
        p.setTurmas(turmasAtendidas);
        p.setCorrecoesPendentes(pendentes);
        p.setTempoRespostaDias(tempo);
        p.setInteracoesSemana(interacoes);
        p.setAvaliacao(avaliacao);
        p.setTarefasConcluidas(concluidas);
        p.setTarefasTotal(total);
        return professores.save(p);
    }

    /** Matérias já semeadas, pelo nome — usado para montar o vínculo dos docentes. */
    private Set<Materia> materiasPorNome(String... nomes) {
        List<String> procurados = List.of(nomes);
        return materias.findAllByOrderByNome().stream()
                .filter(m -> procurados.contains(m.getNome()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void avaliacao(String titulo, String disciplina, Turma turma, String tipo,
                           Avaliacao.Status status, LocalDate data, int entregas, int pendentes) {
        Avaliacao a = new Avaliacao();
        a.setTitulo(titulo);
        a.setDisciplina(disciplina);
        a.setTurma(turma);
        a.setTipo(tipo);
        a.setStatus(status);
        a.setData(data);
        a.setEntregas(entregas);
        a.setPendentesCorrecao(pendentes);
        avaliacoes.save(a);
    }

    private void questao(String enunciado, String disciplina, Questao.Tipo tipo, Questao.Dificuldade dif) {
        Questao q = new Questao();
        q.setEnunciado(enunciado);
        q.setDisciplina(disciplina);
        q.setTipo(tipo);
        q.setDificuldade(dif);
        questoes.save(q);
    }

    private static double nota10(double v) {
        return Math.max(0, Math.min(10, Math.round(v * 10.0) / 10.0));
    }
}
