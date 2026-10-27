package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Inscricao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    /** {@code busca} chega já como padrão LIKE em minúsculo (ex.: "%ana%") ou null. */
    @Query("""
           select i from Inscricao i
           where (:ativo is null or i.ativo = :ativo)
             and (:turmaId is null or i.turma.id = :turmaId)
             and (:busca is null or lower(i.aluno.nome) like :busca)
           order by i.aluno.nome
           """)
    Page<Inscricao> buscar(@Param("ativo") Boolean ativo,
                          @Param("turmaId") Long turmaId,
                          @Param("busca") String busca,
                          Pageable pageable);

    long countByAtivo(boolean ativo);
}
