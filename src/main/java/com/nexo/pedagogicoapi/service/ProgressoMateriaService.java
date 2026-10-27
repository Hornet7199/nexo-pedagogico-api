package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.domain.ConteudoConcluido;
import com.nexo.pedagogicoapi.domain.ConteudoMateria;
import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.domain.SegmentoEnsino;
import com.nexo.pedagogicoapi.repository.AlunoRepository;
import com.nexo.pedagogicoapi.repository.ConteudoConcluidoRepository;
import com.nexo.pedagogicoapi.repository.ConteudoMateriaRepository;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Progresso do aluno por matéria.
 *
 * <p>O percentual sai de dado real: conteúdos que o aluno marcou como concluídos
 * ÷ total de conteúdos da matéria. Antes vinha de número fixo no client — todo
 * aluno via "45% em Biologia", e nada do que ele fizesse mudava isso.
 *
 * <p>Matéria sem conteúdo cadastrado fica com total 0 e percentual 0, e a tela
 * mostra "sem conteúdo ainda" em vez de fingir 0% de 0.
 */
@Service
public class ProgressoMateriaService {

    private final AlunoRepository alunos;
    private final MateriaRepository materias;
    private final ConteudoMateriaRepository conteudos;
    private final ConteudoConcluidoRepository concluidos;

    public ProgressoMateriaService(AlunoRepository alunos, MateriaRepository materias,
                                   ConteudoMateriaRepository conteudos,
                                   ConteudoConcluidoRepository concluidos) {
        this.alunos = alunos;
        this.materias = materias;
        this.conteudos = conteudos;
        this.concluidos = concluidos;
    }

    public record MateriaProgressoDTO(Long id, String nome, String segmento,
                                      int totalConteudos, int conteudosConcluidos, int percentual) {}

    /** As matérias da etapa do aluno, cada uma com o progresso dele. */
    @Transactional(readOnly = true)
    public List<MateriaProgressoDTO> materiasDoAluno(Long usuarioId) {
        Aluno aluno = exigirAluno(usuarioId);
        SegmentoEnsino etapa = etapaDe(aluno);

        Map<Long, Long> totais = paresParaMapa(conteudos.totalPorMateria());
        Map<Long, Long> feitos = paresParaMapa(concluidos.totalPorMateria(aluno.getId()));

        return materias.findAllByOrderByNome().stream()
                .filter(m -> m.getSegmento().atende(etapa))
                .map(m -> {
                    int total = totais.getOrDefault(m.getId(), 0L).intValue();
                    int feito = Math.min(feitos.getOrDefault(m.getId(), 0L).intValue(), total);
                    int percentual = total == 0 ? 0 : Math.round(feito * 100f / total);
                    return new MateriaProgressoDTO(m.getId(), m.getNome(), m.getSegmento().name(),
                            total, feito, percentual);
                })
                .toList();
    }

    /** Ids dos conteúdos já concluídos numa matéria — a tela marca os itens da lista. */
    @Transactional(readOnly = true)
    public List<Long> concluidosNaMateria(Long usuarioId, Long materiaId) {
        Aluno aluno = exigirAluno(usuarioId);
        exigirMateriaDaEtapa(materiaId, aluno);
        return concluidos.idsConcluidosNaMateria(aluno.getId(), materiaId);
    }

    /**
     * Marca o conteúdo como concluído. Idempotente: repetir não cria linha nova
     * nem estoura — quem clica duas vezes concluiu uma vez só.
     */
    @Transactional
    public void concluir(Long usuarioId, Long conteudoId) {
        Aluno aluno = exigirAluno(usuarioId);
        ConteudoMateria conteudo = exigirConteudoDaEtapa(conteudoId, aluno);

        if (concluidos.findByAlunoIdAndConteudoId(aluno.getId(), conteudoId).isEmpty()) {
            concluidos.save(new ConteudoConcluido(aluno, conteudo));
        }
    }

    /** Desmarca. Também idempotente: desmarcar o que não estava marcado não é erro. */
    @Transactional
    public void desmarcar(Long usuarioId, Long conteudoId) {
        Aluno aluno = exigirAluno(usuarioId);
        exigirConteudoDaEtapa(conteudoId, aluno);
        concluidos.findByAlunoIdAndConteudoId(aluno.getId(), conteudoId)
                .ifPresent(concluidos::delete);
    }

    // ── Apoio ────────────────────────────────────────────────────────────────

    private Aluno exigirAluno(Long usuarioId) {
        return alunos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.forbidden("Aluno sem cadastro vinculado."));
    }

    private static SegmentoEnsino etapaDe(Aluno aluno) {
        return aluno.getTurma() == null ? SegmentoEnsino.FUNDAMENTAL : aluno.getTurma().getSegmento();
    }

    /** A etapa vale aqui também: sem isto, dava para concluir conteúdo de outra etapa. */
    private void exigirMateriaDaEtapa(Long materiaId, Aluno aluno) {
        Materia materia = materias.findById(materiaId)
                .orElseThrow(() -> ApiException.notFound("Matéria não encontrada."));
        if (!materia.getSegmento().atende(etapaDe(aluno))) {
            throw ApiException.forbidden("Esta matéria não faz parte da sua etapa de ensino.");
        }
    }

    private ConteudoMateria exigirConteudoDaEtapa(Long conteudoId, Aluno aluno) {
        ConteudoMateria conteudo = conteudos.findById(conteudoId)
                .orElseThrow(() -> ApiException.notFound("Conteúdo não encontrado."));
        if (!conteudo.getMateria().getSegmento().atende(etapaDe(aluno))) {
            throw ApiException.forbidden("Este conteúdo não faz parte da sua etapa de ensino.");
        }
        return conteudo;
    }

    private static Map<Long, Long> paresParaMapa(List<Object[]> linhas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] linha : linhas) {
            mapa.put((Long) linha[0], (Long) linha[1]);
        }
        return mapa;
    }
}
