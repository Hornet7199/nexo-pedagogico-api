package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.domain.SegmentoEnsino;
import com.nexo.pedagogicoapi.repository.AlunoRepository;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import com.nexo.pedagogicoapi.repository.NotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * As notas do próprio aluno logado. O painel de XP, ranking e desafios que vivia junto
 * daqui é gamificação e não faz parte do pedagógico.
 */
@Service
public class AlunoNotasService {

    public record NotaAlunoDTO(String disciplina, Double media, Double p1, Double p2, Double t1, Double participacao) {}

    /**
     * O que o gamificacao-api precisa saber do aluno: a turma (para o dashboard) e as matérias
     * do catálogo que NÃO são da etapa dele (para esconder os desafios delas).
     */
    public record ContextoAlunoDTO(String turma, String etapa, List<String> materiasForaDaEtapa) {}

    private final AlunoRepository alunos;
    private final NotaRepository notas;
    private final MateriaRepository materias;

    public AlunoNotasService(AlunoRepository alunos, NotaRepository notas, MateriaRepository materias) {
        this.alunos = alunos;
        this.notas = notas;
        this.materias = materias;
    }

    /** Aluno sem turma é tratado como fundamental, a etapa mais restrita (ver SegmentoEnsino.daTurma). */
    @Transactional(readOnly = true)
    public ContextoAlunoDTO contextoDoAluno(Long usuarioId) {
        Aluno aluno = alunos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Aluno não encontrado para o usuário logado."));
        SegmentoEnsino etapa = aluno.getTurma() == null
                ? SegmentoEnsino.FUNDAMENTAL
                : aluno.getTurma().getSegmento();
        List<String> fora = materias.findAllByOrderByNome().stream()
                .filter(m -> !m.getSegmento().atende(etapa))
                .map(m -> m.getNome())
                .toList();
        return new ContextoAlunoDTO(aluno.getTurma() != null ? aluno.getTurma().getNome() : null,
                etapa.name(), fora);
    }

    /** Notas por disciplina do aluno dono da conta {@code usuarioId}. */
    @Transactional(readOnly = true)
    public List<NotaAlunoDTO> notasDoAluno(Long usuarioId) {
        Aluno aluno = alunos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Aluno não encontrado para o usuário logado."));
        return notas.findByAlunoId(aluno.getId()).stream()
                .map(n -> new NotaAlunoDTO(n.getDisciplina(), n.getMedia(), n.getP1(), n.getP2(), n.getT1(),
                        n.getParticipacao()))
                .toList();
    }
}
