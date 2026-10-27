package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conteudos_aula",
       indexes = @Index(name = "idx_conteudos_aula_turma_data", columnList = "turma_id, data"))
@Getter
@Setter
public class ConteudoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY)
    private Professor professor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 4000)
    private String descricao;

    @Column(length = 4000)
    private String observacoes;
}
