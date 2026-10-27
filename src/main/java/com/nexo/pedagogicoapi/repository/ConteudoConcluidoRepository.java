package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.ConteudoConcluido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConteudoConcluidoRepository extends JpaRepository<ConteudoConcluido, Long> {

    Optional<ConteudoConcluido> findByAlunoIdAndConteudoId(Long alunoId, Long conteudoId);

    /** Ids dos conteúdos que o aluno já concluiu numa matéria — marca os itens na tela. */
    @Query("""
           select c.conteudo.id from ConteudoConcluido c
           where c.aluno.id = :alunoId and c.conteudo.materia.id = :materiaId
           """)
    List<Long> idsConcluidosNaMateria(@Param("alunoId") Long alunoId, @Param("materiaId") Long materiaId);

    /**
     * (materiaId, quantidade concluída) do aluno, numa ida só. Sem isto a lista de
     * matérias faria uma consulta por matéria só para montar a barra de progresso.
     */
    @Query("""
           select c.conteudo.materia.id, count(c) from ConteudoConcluido c
           where c.aluno.id = :alunoId
           group by c.conteudo.materia.id
           """)
    List<Object[]> totalPorMateria(@Param("alunoId") Long alunoId);
}
