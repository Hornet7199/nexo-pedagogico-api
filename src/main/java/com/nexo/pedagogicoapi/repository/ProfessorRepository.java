package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    Optional<Professor> findByUsuarioId(Long usuarioId);

    /** Docentes com as matérias já carregadas — evita um SELECT por professor no monitoramento. */
    @Query("select distinct p from Professor p left join fetch p.materias order by p.nome")
    List<Professor> findAllComMaterias();
}
