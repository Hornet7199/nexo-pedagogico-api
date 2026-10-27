package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Frequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FrequenciaRepository extends JpaRepository<Frequencia, Long> {
    List<Frequencia> findByTurmaIdAndData(Long turmaId, LocalDate data);

    /** Todas as chamadas da turma — base do percentual de presença por aluno. */
    List<Frequencia> findByTurmaId(Long turmaId);
    Optional<Frequencia> findByAlunoIdAndTurmaIdAndData(Long alunoId, Long turmaId, LocalDate data);
    long countByAlunoIdAndPresenteFalse(Long alunoId);
    long countByAlunoId(Long alunoId);
    long countByPresenteFalse();
    long count();

    /**
     * Total de aulas e de faltas por aluno, agregado no banco numa única query —
     * substitui o par {@code countByAlunoId}/{@code countByAlunoIdAndPresenteFalse}
     * chamado dentro de laços. Alunos sem nenhum registro não aparecem no resultado.
     */
    interface FrequenciaResumo {
        Long getAlunoId();
        long getTotal();
        long getFaltas();
    }

    @Query("""
           select f.aluno.id as alunoId, count(f) as total,
                  sum(case when f.presente = false then 1L else 0L end) as faltas
           from Frequencia f
           group by f.aluno.id
           """)
    List<FrequenciaResumo> resumoPorAluno();

    /**
     * O mesmo resumo, restrito aos alunos das turmas indicadas.
     *
     * <p>Par de {@code NotaRepository.projetarPorTurmas}: {@link #resumoPorAluno()}
     * agrega a tabela de frequência inteira, o que é desperdício quando o pedido é de
     * um professor que só enxerga as próprias turmas.
     */
    @Query("""
           select f.aluno.id as alunoId, count(f) as total,
                  sum(case when f.presente = false then 1L else 0L end) as faltas
           from Frequencia f
           where f.aluno.turma.id in :turmaIds
           group by f.aluno.id
           """)
    List<FrequenciaResumo> resumoPorTurmas(
            @org.springframework.data.repository.query.Param("turmaIds") Collection<Long> turmaIds);
}
