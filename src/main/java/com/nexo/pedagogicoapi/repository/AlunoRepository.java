package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    Optional<Aluno> findByUsuarioId(Long usuarioId);

    List<Aluno> findByTurmaIdOrderByNome(Long turmaId);

    /**
     * Alunos com a turma já carregada — evita um SELECT lazy por turma ao montar DTOs.
     * Ordena por id para que listas como a de evasão, ordenadas por risco com sort
     * estável, tenham desempate previsível.
     */
    @Query("select a from Aluno a left join fetch a.turma order by a.id")
    List<Aluno> findAllComTurma();

    /** Alunos de várias turmas de uma vez — evita uma query por turma dentro de laços. */
    @Query("select a from Aluno a left join fetch a.turma t where t.id in :turmaIds order by a.nome")
    List<Aluno> findByTurmaIdInComTurma(Collection<Long> turmaIds);

    /** Uma turma só, com a mesma ordenação por id de {@link #findAllComTurma()}. */
    @Query("select a from Aluno a left join fetch a.turma t where t.id = :turmaId order by a.id")
    List<Aluno> findByTurmaIdComTurma(Long turmaId);

    long countByTurmaId(Long turmaId);

    /** Ocupação de todas as turmas numa ida só: (turmaId, total de alunos). */
    @Query("select a.turma.id, count(a) from Aluno a where a.turma is not null group by a.turma.id")
    List<Object[]> totalPorTurma();
}
