package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.ConteudoAula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConteudoAulaRepository extends JpaRepository<ConteudoAula, Long> {
    List<ConteudoAula> findByTurmaIdOrderByDataDesc(Long turmaId);
}
