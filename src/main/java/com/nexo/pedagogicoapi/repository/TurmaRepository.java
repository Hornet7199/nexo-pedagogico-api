package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    Optional<Turma> findByNome(String nome);
    List<Turma> findByProfessorIdOrderByNome(Long professorId);
}
