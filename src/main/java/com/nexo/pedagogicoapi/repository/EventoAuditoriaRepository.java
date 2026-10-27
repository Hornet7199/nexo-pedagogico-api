package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    List<EventoAuditoria> findByCriadoEmAfterOrderByCriadoEmDesc(Instant desde);
    List<EventoAuditoria> findTop200ByOrderByCriadoEmDesc();

    /**
     * Apaga os eventos anteriores ao corte — ver {@code AuditoriaRetencaoJob}.
     *
     * <p>Usa o índice {@code idx_eventos_auditoria_criado_em}, que já existia: o
     * DELETE alcança direto a faixa antiga em vez de varrer a tabela.
     */
    @Modifying
    @Query("delete from EventoAuditoria e where e.criadoEm < :corte")
    int removerAnterioresA(@Param("corte") Instant corte);
}
