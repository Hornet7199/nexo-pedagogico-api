package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.ObservacaoPedagogica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservacaoPedagogicaRepository extends JpaRepository<ObservacaoPedagogica, Long> {
    List<ObservacaoPedagogica> findByAlunoIdOrderByCriadaEmDesc(Long alunoId);
}
