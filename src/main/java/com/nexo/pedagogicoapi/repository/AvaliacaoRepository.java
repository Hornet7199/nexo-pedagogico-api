package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("""
           select a from Avaliacao a
           where (:turmaId is null or a.turma.id = :turmaId)
             and (:status is null or a.status = :status)
           order by a.data desc
           """)
    List<Avaliacao> buscar(@Param("turmaId") Long turmaId, @Param("status") Avaliacao.Status status);

    List<Avaliacao> findByStatusInOrderByDataAsc(List<Avaliacao.Status> status);

    /**
     * Mesma busca de {@link #buscar}, restrita às turmas do professor logado. Avaliações
     * sem turma são incluídas: não pertencem a ninguém, então continuam visíveis para
     * todos, como antes desta restrição existir.
     */
    @Query("""
           select a from Avaliacao a
           where (a.turma.id in :turmaIds or a.turma is null)
             and (:status is null or a.status = :status)
           order by a.data desc
           """)
    List<Avaliacao> buscarPorTurmas(@Param("turmaIds") Collection<Long> turmaIds,
                                    @Param("status") Avaliacao.Status status);

    /** Fila de correção limitada às turmas do professor — ver {@link #buscarPorTurmas}. */
    @Query("""
           select a from Avaliacao a
           where (a.turma.id in :turmaIds or a.turma is null)
             and a.status in :status
           order by a.data asc
           """)
    List<Avaliacao> findByStatusInAndTurmas(@Param("status") List<Avaliacao.Status> status,
                                            @Param("turmaIds") Collection<Long> turmaIds);

    /**
     * Correções pendentes somadas no banco para todas as turmas de uma vez. O dashboard
     * do professor só precisa desse total: antes ele rodava uma query por turma e
     * materializava todas as avaliações como entidades gerenciadas para somar um int.
     */
    @Query("select coalesce(sum(a.pendentesCorrecao), 0) from Avaliacao a where a.turma.id in :turmaIds")
    long somarPendentesCorrecao(@Param("turmaIds") Collection<Long> turmaIds);

    /** Avaliações das turmas dentro do intervalo — contadas no banco, sem carregar linhas. */
    @Query("""
           select count(a) from Avaliacao a
           where a.turma.id in :turmaIds and a.data between :inicio and :fim
           """)
    long contarPorTurmasNoPeriodo(@Param("turmaIds") Collection<Long> turmaIds,
                                  @Param("inicio") LocalDate inicio,
                                  @Param("fim") LocalDate fim);
}
