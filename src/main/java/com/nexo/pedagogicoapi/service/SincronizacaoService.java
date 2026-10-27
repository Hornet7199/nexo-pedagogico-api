package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.domain.Professor;
import com.nexo.pedagogicoapi.domain.Turma;
import com.nexo.pedagogicoapi.repository.AlunoRepository;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import com.nexo.pedagogicoapi.repository.ProfessorRepository;
import com.nexo.pedagogicoapi.repository.TurmaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mantém a cópia local de aluno e professor em dia com o usuario-api, que é a fonte de
 * verdade. É chamado por ele quando alguém é cadastrado ou muda de nome, turma ou foto.
 *
 * <p>Todas as operações são <i>upsert</i> pelo id do usuario-api, então repetir a chamada
 * (uma nova tentativa depois de falha de rede, por exemplo) não duplica nada.
 */
@Service
public class SincronizacaoService {

    private final AlunoRepository alunos;
    private final ProfessorRepository professores;
    private final TurmaRepository turmas;
    private final MateriaRepository materias;
    private final AuditoriaService auditoria;

    public SincronizacaoService(AlunoRepository alunos, ProfessorRepository professores,
                                TurmaRepository turmas, MateriaRepository materias,
                                AuditoriaService auditoria) {
        this.alunos = alunos;
        this.professores = professores;
        this.turmas = turmas;
        this.materias = materias;
        this.auditoria = auditoria;
    }

    @Transactional
    public void sincronizarAluno(Long id, Long usuarioId, String nome, Long turmaId, String foto, String operador) {
        Turma turma = null;
        if (turmaId != null) {
            turma = turmas.findById(turmaId)
                    .orElseThrow(() -> ApiException.validation("Dados inválidos.",
                            Map.of("turmaId", "Turma inexistente.")));
        }
        boolean novo = !alunos.existsById(id);
        Aluno aluno = alunos.findById(id).orElseGet(Aluno::new);
        aluno.setId(id);
        aluno.setUsuarioId(usuarioId);
        aluno.setNome(nome.trim());
        aluno.setTurma(turma);
        aluno.setFoto(foto);
        alunos.save(aluno);
        if (novo) {
            auditoria.registrar(operador, com.nexo.pedagogicoapi.domain.EventoAuditoria.Tipo.ALTERACAO,
                    "Aluno sincronizado", "Aluno: " + aluno.getNome(), null);
        }
    }

    @Transactional
    public void sincronizarProfessor(Long id, Long usuarioId, String nome, String foto, List<Long> materiaIds,
                                     String operador) {
        Set<Long> ids = new LinkedHashSet<>(materiaIds == null ? List.of() : materiaIds);
        List<Materia> encontradas = materias.findAllById(ids);
        if (encontradas.size() != ids.size()) {
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("materiaIds", "Alguma matéria informada não existe.");
            throw ApiException.validation("Dados inválidos.", erros);
        }
        boolean novo = !professores.existsById(id);
        Professor professor = professores.findById(id).orElseGet(Professor::new);
        professor.setId(id);
        professor.setUsuarioId(usuarioId);
        professor.setNome(nome.trim());
        professor.setFoto(foto);
        professor.setMaterias(new LinkedHashSet<>(encontradas));
        professores.save(professor);
        if (novo) {
            auditoria.registrar(operador, com.nexo.pedagogicoapi.domain.EventoAuditoria.Tipo.ALTERACAO,
                    "Professor sincronizado", "Professor: " + professor.getNome(), null);
        }
    }

    /**
     * Nome e foto de uma conta que mudaram no usuario-api. Aplica ao aluno ou ao professor
     * dono da conta; conta sem registro aqui (um diretor, por exemplo) é ignorada, e não
     * é erro: o usuario-api não sabe quem tem registro pedagógico.
     */
    @Transactional
    public void sincronizarPerfil(Long usuarioId, String nome, String foto) {
        alunos.findByUsuarioId(usuarioId).ifPresent(a -> {
            if (nome != null && !nome.isBlank()) a.setNome(nome.trim());
            a.setFoto(foto);
            alunos.save(a);
        });
        professores.findByUsuarioId(usuarioId).ifPresent(p -> {
            if (nome != null && !nome.isBlank()) p.setNome(nome.trim());
            p.setFoto(foto);
            professores.save(p);
        });
    }
}
