package com.nexo.pedagogicoapi.repository;

import com.nexo.pedagogicoapi.domain.Questao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {
}
