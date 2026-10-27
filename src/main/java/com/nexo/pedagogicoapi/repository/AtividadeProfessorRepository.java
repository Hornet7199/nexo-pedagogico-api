package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.AtividadeProfessor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtividadeProfessorRepository extends JpaRepository<AtividadeProfessor, Long> {
    List<AtividadeProfessor> findTop6ByProfessorIdOrderByCriadaEmDesc(Long professorId);
}
