package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    List<Materia> findAllByOrderByNome();
    List<Materia> findByIdIn(Collection<Long> ids);
}
