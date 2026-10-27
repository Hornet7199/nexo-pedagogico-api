package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "observacoes_pedagogicas",
       indexes = @Index(name = "idx_observacoes_aluno_criada", columnList = "aluno_id, criada_em"))
@Getter
@Setter
public class ObservacaoPedagogica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Aluno aluno;

    @Column(nullable = false)
    private String autorNome;

    @Column(nullable = false, length = 4000)
    private String texto;

    private Instant criadaEm = Instant.now();
}
