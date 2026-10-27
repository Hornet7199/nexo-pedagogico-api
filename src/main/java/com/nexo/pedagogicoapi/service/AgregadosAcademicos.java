package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.domain.Nota;
import com.nexo.pedagogicoapi.repository.FrequenciaRepository;
import com.nexo.pedagogicoapi.repository.NotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega médias e frequências de TODOS os alunos em duas queries e entrega o
 * resultado indexado por aluno.
 *
 * <p>Existe para eliminar o N+1 que dominava o tempo de resposta: relatórios, evasão
 * e o dashboard do professor consultavam notas e frequência aluno a aluno, gerando
 * centenas de idas ao banco por request. As médias continuam sendo calculadas em Java
 * por {@link Nota#calcularMedia} — a fórmula é ponderada e arredondada por nota, então
 * agregá-la com {@code avg()} no SQL mudaria os números.
 */
@Service
public class AgregadosAcademicos {

    /** Aulas registradas e faltas de um aluno. */
    public record Presenca(long total, long faltas) {}

    /** Agregados prontos para consulta em memória, sem novas idas ao banco. */
    public record Indices(Map<Long, Double> medias, Map<Long, Presenca> presencas) {

        /** Média do aluno, ou {@code null} se ele não tem nenhuma nota lançada. */
        public Double media(Long alunoId) {
            return medias.get(alunoId);
        }

        /** Média do aluno, com 0.0 para quem não tem nota (regra usada na análise de evasão). */
        public double mediaOuZero(Long alunoId) {
            return medias.getOrDefault(alunoId, 0.0);
        }

        /** Percentual de faltas com uma casa decimal; 0 para aluno sem registros. */
        public double percentualFaltas(Long alunoId) {
            Presenca p = presencas.get(alunoId);
            if (p == null || p.total() == 0) return 0;
            return Math.round(p.faltas() * 1000.0 / p.total()) / 10.0;
        }

        /** Percentual de presença sem arredondar; 0 para aluno sem registros. */
        public double percentualPresenca(Long alunoId) {
            Presenca p = presencas.get(alunoId);
            if (p == null || p.total() == 0) return 0;
            return (p.total() - p.faltas()) * 100.0 / p.total();
        }

        /** Soma de todos os registros de frequência — dispensa um {@code count()} extra. */
        public Presenca totalGeral() {
            long total = 0;
            long faltas = 0;
            for (Presenca p : presencas.values()) {
                total += p.total();
                faltas += p.faltas();
            }
            return new Presenca(total, faltas);
        }
    }

    private final NotaRepository notas;
    private final FrequenciaRepository frequencias;

    public AgregadosAcademicos(NotaRepository notas, FrequenciaRepository frequencias) {
        this.notas = notas;
        this.frequencias = frequencias;
    }

    /**
     * Carrega os agregados de TODA a escola em duas queries.
     *
     * <p>Prefira {@link #carregarDeTurmas} quando o pedido já tem um recorte: este aqui
     * varre as tabelas de nota e frequência inteiras, e é o caminho certo só para as
     * visões institucionais (relatórios do diretor, evasão sem filtro).
     *
     * @param periodo período letivo a considerar nas notas; {@code null}/vazio usa todas.
     */
    @Transactional(readOnly = true)
    public Indices carregar(String periodo) {
        return new Indices(medias(notasBrutas(periodo)), presencas(frequencias.resumoPorAluno()));
    }

    /**
     * Os mesmos agregados, restritos aos alunos das turmas indicadas — também em duas
     * queries, mas lendo apenas as linhas que a resposta vai usar.
     *
     * <p>É o caminho do dashboard do professor e da evasão filtrada por turma. O
     * {@link #carregar} equivalente devolvia os índices da escola inteira e o chamador
     * descartava tudo que não fosse das suas turmas: numa escola com 40 turmas, ~97% das
     * linhas lidas do banco iam para o lixo a cada abertura de tela.
     *
     * <p>Lista vazia não vai ao banco: {@code in ()} não é SQL válido, e um professor
     * sem turma não tem agregado nenhum a carregar.
     */
    @Transactional(readOnly = true)
    public Indices carregarDeTurmas(Collection<Long> turmaIds, String periodo) {
        if (turmaIds == null || turmaIds.isEmpty()) return new Indices(Map.of(), Map.of());
        String filtro = (periodo == null || periodo.isBlank()) ? null : periodo;
        return new Indices(medias(notas.projetarPorTurmas(turmaIds, filtro)),
                presencas(frequencias.resumoPorTurmas(turmaIds)));
    }

    private List<NotaRepository.NotaBruta> notasBrutas(String periodo) {
        return (periodo == null || periodo.isBlank())
                ? notas.projetarTodas()
                : notas.projetarPorPeriodo(periodo);
    }

    /**
     * Acumula soma e contagem num único passo, sem guardar as notas individuais:
     * a versão anterior mantinha um {@code ArrayList<Double>} por aluno e um
     * {@code Double} boxed por nota só para tirar a média depois — lixo proporcional
     * ao total de notas da escola, gerado a cada request de relatório/evasão/dashboard.
     */
    private Map<Long, Double> medias(List<NotaRepository.NotaBruta> brutas) {
        Map<Long, double[]> acumulado = new HashMap<>(capacidadePara(brutas.size()));
        for (NotaRepository.NotaBruta n : brutas) {
            Double media = Nota.calcularMedia(n.getP1(), n.getP2(), n.getT1(), n.getParticipacao());
            if (media == null) continue;
            double[] soma = acumulado.computeIfAbsent(n.getAlunoId(), k -> new double[2]);
            soma[0] += media;
            soma[1]++;
        }

        Map<Long, Double> medias = new HashMap<>(capacidadePara(acumulado.size()));
        acumulado.forEach((alunoId, soma) -> medias.put(alunoId, soma[0] / soma[1]));
        return medias;
    }

    private Map<Long, Presenca> presencas(List<FrequenciaRepository.FrequenciaResumo> resumos) {
        Map<Long, Presenca> presencas = new HashMap<>(capacidadePara(resumos.size()));
        for (FrequenciaRepository.FrequenciaResumo r : resumos) {
            presencas.put(r.getAlunoId(), new Presenca(r.getTotal(), r.getFaltas()));
        }
        return presencas;
    }

    /** Capacidade inicial que evita o rehash do HashMap ao inserir {@code n} chaves. */
    private static int capacidadePara(int n) {
        return Math.max(16, (int) (n / 0.75f) + 1);
    }
}
