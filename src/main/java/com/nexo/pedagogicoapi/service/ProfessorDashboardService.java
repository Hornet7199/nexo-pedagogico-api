package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.domain.Professor;
import com.nexo.pedagogicoapi.domain.Turma;
import com.nexo.pedagogicoapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monta o dashboard do professor (turmas, próximas aulas, alunos em atenção,
 * feed de atividades e KPIs) — substitui os arrays hardcoded de dashboard-professor.ts.
 */
@Service
public class ProfessorDashboardService {

    public record TurmaResumo(String nome, String disciplina, int alunos, double mediaGeral, int progresso) {}

    public record ProximaAula(String turma, String disciplina, String hora, LocalDate data, String sala, int alunos) {}

    public record AlunoAtencaoDTO(String nome, String turma, double mediaAtual, int frequencia,
                                  String status, String foto, Instant ultimaAtividade) {}

    public record AtividadeDTO(String tipo, String descricao, String turma, String icone, String cor, Instant criadaEm) {}

    public record ProfessorDashboardDTO(int turmasAtivas, int totalAlunos, int correcoesPendentes, int avaliacoesMes,
                                        List<TurmaResumo> turmas, List<ProximaAula> proximasAulas,
                                        List<AlunoAtencaoDTO> alunosAtencao, List<AtividadeDTO> atividades) {}

    private final ProfessorRepository professores;
    private final TurmaRepository turmas;
    private final AlunoRepository alunos;
    private final AvaliacaoRepository avaliacoes;
    private final AulaAgendadaRepository aulas;
    private final AtividadeProfessorRepository atividadesProf;
    private final EvasaoService evasao;
    private final AgregadosAcademicos agregados;

    public ProfessorDashboardService(ProfessorRepository professores, TurmaRepository turmas, AlunoRepository alunos,
                                     AvaliacaoRepository avaliacoes, AulaAgendadaRepository aulas,
                                     AtividadeProfessorRepository atividadesProf, EvasaoService evasao,
                                     AgregadosAcademicos agregados) {
        this.professores = professores;
        this.turmas = turmas;
        this.alunos = alunos;
        this.avaliacoes = avaliacoes;
        this.aulas = aulas;
        this.atividadesProf = atividadesProf;
        this.evasao = evasao;
        this.agregados = agregados;
    }

    @Transactional(readOnly = true)
    public ProfessorDashboardDTO montar(Long usuarioId) {
        Professor prof = professores.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Professor não encontrado para o usuário logado."));

        List<Turma> minhasTurmas = turmas.findByProfessorIdOrderByNome(prof.getId());
        LocalDate hoje = LocalDate.now();
        List<Long> idsTurmas = minhasTurmas.stream().map(Turma::getId).toList();

        // Alunos de todas as turmas e agregados de nota/frequência carregados de uma vez —
        // antes eram uma query por turma mais seis por aluno (evasao.avaliar era chamado duas vezes).
        // O recorte por turma importa tanto quanto a contagem de queries: com carregar(null)
        // as duas eram varreduras das tabelas de nota e frequência da escola inteira, para
        // depois usar só as linhas das turmas deste professor.
        var indices = agregados.carregarDeTurmas(idsTurmas, null);
        Map<Long, List<Aluno>> alunosPorTurma = minhasTurmas.isEmpty() ? Map.of()
                : alunos.findByTurmaIdInComTurma(idsTurmas).stream()
                        .collect(Collectors.groupingBy(a -> a.getTurma().getId()));

        // Os dois KPIs de avaliação saem de duas agregações no banco, para todas as turmas
        // de uma vez. Antes era uma query por turma, cada uma trazendo as avaliações
        // inteiras como entidades gerenciadas só para somar um int e contar linhas.
        int correcoesPendentes = 0;
        int avaliacoesMes = 0;
        if (!idsTurmas.isEmpty()) {
            correcoesPendentes = (int) avaliacoes.somarPendentesCorrecao(idsTurmas);
            avaliacoesMes = (int) avaliacoes.contarPorTurmasNoPeriodo(
                    idsTurmas, hoje.withDayOfMonth(1), hoje.withDayOfMonth(hoje.lengthOfMonth()));
        }

        List<TurmaResumo> turmasResumo = new ArrayList<>();
        List<AlunoAtencaoDTO> atencao = new ArrayList<>();
        int totalAlunos = 0;

        for (Turma t : minhasTurmas) {
            List<Aluno> alunosTurma = alunosPorTurma.getOrDefault(t.getId(), List.of());
            totalAlunos += alunosTurma.size();

            // Um único avaliar() por aluno alimenta a média da turma e a lista de atenção.
            // Antes eram dois, e cada um montava um AlunoRiscoDTO completo (com o
            // String.format da matrícula) que era descartado logo em seguida.
            double soma = 0;
            int comNota = 0;
            for (Aluno a : alunosTurma) {
                var r = evasao.avaliar(a, indices);
                if (r.media() > 0) {
                    soma += r.media();
                    comNota++;
                }
                if (r.risco() != EvasaoService.Risco.BAIXO) {
                    atencao.add(new AlunoAtencaoDTO(a.getNome(), t.getNome(), r.media(),
                            Math.max(0, (int) Math.round(100 - r.percentualFaltas())),
                            r.risco() == EvasaoService.Risco.ALTO ? "critico" : "atencao",
                            a.getFoto(), a.getUltimoAcessoEm()));
                }
            }

            double media = comNota == 0 ? 0 : Math.round(soma / comNota * 10.0) / 10.0;
            turmasResumo.add(new TurmaResumo(t.getNome(), prof.getDisciplinas(),
                    alunosTurma.size(), media, (int) Math.round(media * 10)));
        }

        atencao.sort(Comparator.comparingDouble(AlunoAtencaoDTO::mediaAtual));
        List<AlunoAtencaoDTO> topAtencao = atencao.size() > 5 ? new ArrayList<>(atencao.subList(0, 5)) : atencao;

        List<ProximaAula> proximas = aulas.findTop6ByProfessorIdOrderByDataAscHoraAsc(prof.getId()).stream()
                .map(al -> new ProximaAula(al.getTurma() != null ? al.getTurma().getNome() : "—",
                        al.getDisciplina(), al.getHora(), al.getData(), al.getSala(), al.getQtdAlunos()))
                .toList();

        List<AtividadeDTO> feed = atividadesProf.findTop6ByProfessorIdOrderByCriadaEmDesc(prof.getId()).stream()
                .map(at -> new AtividadeDTO(at.getTipo(), at.getDescricao(), at.getTurma(),
                        at.getIcone(), at.getCor(), at.getCriadaEm()))
                .toList();

        return new ProfessorDashboardDTO(minhasTurmas.size(), totalAlunos, correcoesPendentes, avaliacoesMes,
                turmasResumo, proximas, topAtencao, feed);
    }
}
