package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    @Query("""
           select n from Nota n
           where n.turma.id = :turmaId
             and (:disciplina is null or n.disciplina = :disciplina)
             and (:periodo is null or n.periodo = :periodo)
           order by n.aluno.nome
           """)
    List<Nota> buscarPorTurma(@Param("turmaId") Long turmaId,
                              @Param("disciplina") String disciplina,
                              @Param("periodo") String periodo);

    List<Nota> findByAlunoId(Long alunoId);

    /** Períodos letivos que existem de fato nas notas (ex.: "2026-1"), do mais recente ao mais antigo. */
    @Query("select distinct n.periodo from Nota n where n.periodo is not null order by n.periodo desc")
    List<String> listarPeriodos();

    /**
     * A nota exata que a edição altera. Existe a constraint única
     * (aluno_id, disciplina, periodo), então isso devolve no máximo uma linha —
     * antes o boletim inteiro do aluno era carregado só para filtrar em memória.
     */
    Optional<Nota> findByAlunoIdAndDisciplinaAndPeriodo(Long alunoId, String disciplina, String periodo);

    /**
     * Notas cruas de TODOS os alunos numa única query, para calcular médias em lote.
     * Evita o N+1 de chamar {@code findByAlunoId} por aluno em relatórios/evasão —
     * a média em si continua sendo calculada por {@link Nota#calcularMedia}, já que é
     * ponderada e arredondada por nota (não dá para reproduzir com {@code avg()} no SQL).
     */
    interface NotaBruta {
        Long getAlunoId();
        Double getP1();
        Double getP2();
        Double getT1();
        Double getParticipacao();
    }

    @Query("""
           select n.aluno.id as alunoId, n.p1 as p1, n.p2 as p2,
                  n.t1 as t1, n.participacao as participacao
           from Nota n
           """)
    List<NotaBruta> projetarTodas();

    @Query("""
           select n.aluno.id as alunoId, n.p1 as p1, n.p2 as p2,
                  n.t1 as t1, n.participacao as participacao
           from Nota n
           where n.periodo = :periodo
           """)
    List<NotaBruta> projetarPorPeriodo(@Param("periodo") String periodo);

    /**
     * As mesmas notas cruas, mas só dos alunos das turmas indicadas.
     *
     * <p>{@link #projetarTodas()} lê a tabela inteira — todas as notas de todos os
     * alunos da escola — mesmo quando quem pediu enxerga só as próprias turmas, como o
     * dashboard do professor. Em produção (Postgres gerenciado) isso é um seq scan por
     * request, e é o que domina a conta do banco. O recorte é por {@code aluno.turma}, e
     * não por {@code n.turma}, para trazer exatamente as linhas que o scan completo
     * contribuiria para esses alunos — a média por aluno continua idêntica.
     */
    @Query("""
           select n.aluno.id as alunoId, n.p1 as p1, n.p2 as p2,
                  n.t1 as t1, n.participacao as participacao
           from Nota n
           where n.aluno.turma.id in :turmaIds
             and (:periodo is null or n.periodo = :periodo)
           """)
    List<NotaBruta> projetarPorTurmas(@Param("turmaIds") Collection<Long> turmaIds,
                                      @Param("periodo") String periodo);
}
