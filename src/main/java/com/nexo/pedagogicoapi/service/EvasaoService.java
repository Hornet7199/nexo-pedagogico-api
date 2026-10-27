package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.repository.AlunoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Risco de evasão calculado no servidor a partir de dados reais
 * (frequência, notas e engajamento) — substitui os arrays fixos de gestao-evasao.ts.
 */
@Service
public class EvasaoService {

    public enum Risco { BAIXO, MEDIO, ALTO }

    public record AlunoRiscoDTO(Long alunoId, String nome, String turma, String matricula,
                                double media, double percentualFaltas, int engajamento, Risco risco,
                                String foto, String motivoPrincipal, java.time.Instant ultimoAcessoEm,
                                int intervencoes, java.time.Instant ultimaIntervencaoEm) {}

    private final AlunoRepository alunos;
    private final AgregadosAcademicos agregados;

    public EvasaoService(AlunoRepository alunos, AgregadosAcademicos agregados) {
        this.alunos = alunos;
        this.agregados = agregados;
    }

    @Transactional(readOnly = true)
    public List<AlunoRiscoDTO> calcularRisco(Risco filtroRisco, Long turmaId, String busca) {
        // Com filtro de turma, tanto os alunos quanto os agregados saem recortados do
        // banco. Antes o filtro era só de memória: a escola inteira vinha em três
        // varreduras (alunos, notas, frequência) e a turma pedida era selecionada
        // depois — o custo no banco era o mesmo com ou sem filtro na tela.
        List<Aluno> lista = turmaId == null
                ? alunos.findAllComTurma()
                : alunos.findByTurmaIdComTurma(turmaId);
        var indices = turmaId == null
                ? agregados.carregar(null)
                : agregados.carregarDeTurmas(List.of(turmaId), null);

        // Normaliza o termo uma única vez: dentro do filtro ele era reprocessado
        // (trim + toLowerCase, duas Strings novas) para cada aluno da escola.
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim().toLowerCase();

        return lista.stream()
                .filter(a -> termo == null || a.getNome().toLowerCase().contains(termo))
                .map(a -> avaliar(a, indices))
                .filter(dto -> filtroRisco == null || dto.risco() == filtroRisco)
                .sorted((a, b) -> b.risco().compareTo(a.risco()))
                .toList();
    }

    /** Classificação pura: não toca no banco, só consulta os agregados já carregados. */
    public AlunoRiscoDTO avaliar(Aluno aluno, AgregadosAcademicos.Indices indices) {
        double media = indices.mediaOuZero(aluno.getId());
        double percentualFaltas = indices.percentualFaltas(aluno.getId());

        int engajamento = aluno.getEngajamento();
        Risco risco;
        if (percentualFaltas > 25 || (media > 0 && media < 5.0) || engajamento < 30) {
            risco = Risco.ALTO;
        } else if (percentualFaltas > 15 || (media > 0 && media < 6.5) || engajamento < 55) {
            risco = Risco.MEDIO;
        } else {
            risco = Risco.BAIXO;
        }

        return new AlunoRiscoDTO(aluno.getId(), aluno.getNome(),
                aluno.getTurma() != null ? aluno.getTurma().getNome() : null,
                matricula(aluno.getId()),
                Math.round(media * 10.0) / 10.0, percentualFaltas, engajamento, risco,
                aluno.getFoto(), motivoPrincipal(percentualFaltas, media, engajamento),
                aluno.getUltimoAcessoEm(), aluno.getIntervencoes(), aluno.getUltimaIntervencaoEm());
    }

    /**
     * Mesmo resultado de {@code String.format("2024%03d", id)}, sem o custo de compilar
     * a máscara e montar um Formatter — este método roda para cada aluno da escola em
     * toda abertura da tela de evasão e do dashboard do professor.
     */
    private static String matricula(Long id) {
        String sufixo = String.valueOf(id);
        return switch (sufixo.length()) {
            case 1 -> "202400" + sufixo;
            case 2 -> "20240" + sufixo;
            default -> "2024" + sufixo;
        };
    }

    /** Motivo dominante do risco, derivado dos mesmos fatores usados na classificação. */
    private String motivoPrincipal(double percentualFaltas, double media, int engajamento) {
        if (percentualFaltas > 25) return "Baixa frequência";
        if (media > 0 && media < 5.0) return "Baixo desempenho acadêmico";
        if (engajamento < 30) return "Queda acentuada no engajamento";
        if (percentualFaltas > 15) return "Frequência irregular";
        if (media > 0 && media < 6.5) return "Notas abaixo da média";
        if (engajamento < 55) return "Engajamento em queda";
        return "Acompanhamento preventivo";
    }
}
