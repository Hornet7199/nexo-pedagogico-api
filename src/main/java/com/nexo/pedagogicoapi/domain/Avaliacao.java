package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "avaliacoes",
       // Os dois KPIs do dashboard (somarPendentesCorrecao, contarPorTurmasNoPeriodo)
       // e buscarPorTurmas filtram por turma.id in (...).
       indexes = @Index(name = "idx_avaliacoes_turma", columnList = "turma_id"))
@Getter
@Setter
public class Avaliacao {

    public enum Status { RASCUNHO, PUBLICADA, EM_CORRECAO, CORRIGIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String disciplina;

    @ManyToOne(fetch = FetchType.LAZY)
    private Turma turma;

    /** Prova, Trabalho, Simulado... */
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RASCUNHO;

    private LocalDate data;

    private int entregas;

    private int pendentesCorrecao;
}
