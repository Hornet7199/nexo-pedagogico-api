package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "frequencias",
       uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "turma_id", "data"}),
       indexes = @Index(name = "idx_frequencias_turma_data", columnList = "turma_id, data"))
@Getter
@Setter
public class Frequencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Aluno aluno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Turma turma;

    @Column(nullable = false)
    private LocalDate data;

    private boolean presente;
}
