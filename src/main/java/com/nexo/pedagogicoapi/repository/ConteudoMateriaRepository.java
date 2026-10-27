package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.ConteudoMateria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConteudoMateriaRepository extends JpaRepository<ConteudoMateria, Long> {
    List<ConteudoMateria> findByMateriaIdOrderByOrdemAsc(Long materiaId);

    /**
     * Só o que está no ar — é o recorte da tela do aluno. O {@code is null} cobre o
     * conteúdo gravado antes da coluna existir, que vale como publicado.
     */
    @org.springframework.data.jpa.repository.Query("""
           select c from ConteudoMateria c
           where c.materia.id = :materiaId and (c.publicado is null or c.publicado = true)
           order by c.ordem asc
           """)
    List<ConteudoMateria> publicadosDaMateria(
            @org.springframework.data.repository.query.Param("materiaId") Long materiaId);

    /** Sem uso em produção desde o group by de contarPorPublicado(): fica como referência independente no AgregadosEContagensTest. */
    long countByPublicado(Boolean publicado);

    /**
     * (publicado, total) numa consulta só — o painel do administrador mostra o total e
     * os despublicados lado a lado, e eram dois {@code count(*)} sobre a mesma tabela.
     * O {@code publicado} nulo (registro anterior à coluna) vem como chave própria e
     * conta como publicado, igual ao resto do sistema.
     */
    @org.springframework.data.jpa.repository.Query("select c.publicado, count(c) from ConteudoMateria c group by c.publicado")
    java.util.List<Object[]> contarPorPublicado();
    boolean existsByMateriaId(Long materiaId);

    /**
     * (materiaId, total de conteúdos publicados) — denominador do progresso, numa
     * consulta só. Conta só o que está no ar: com o despublicado no denominador o
     * aluno ficaria travado abaixo de 100% por causa de um conteúdo que ele não
     * tem como abrir.
     */
    @org.springframework.data.jpa.repository.Query("""
           select c.materia.id, count(c) from ConteudoMateria c
           where c.publicado is null or c.publicado = true
           group by c.materia.id
           """)
    List<Object[]> totalPorMateria();
}
