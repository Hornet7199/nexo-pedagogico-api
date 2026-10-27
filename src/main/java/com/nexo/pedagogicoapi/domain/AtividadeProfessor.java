package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;

/** Atividade recente do professor (feed do dashboard). */
@Entity
@Table(name = "atividades_professor",
       // findTop6ByProfessorIdOrderByCriadaEmDesc: a data entra no índice para o
       // "top 6" sair dele já ordenado, sem sort. O B-tree é lido de trás para frente.
       indexes = @Index(name = "idx_atividades_prof_criada", columnList = "professor_id, criada_em"))
@Getter
public class AtividadeProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Professor professor;

    /** correcao | avaliacao | aviso | diario */
    private String tipo;

    @Column(nullable = false)
    private String descricao;

    private String turma;

    private String icone;

    private String cor;

    private Instant criadaEm;

    public AtividadeProfessor() {}

    public AtividadeProfessor(Professor professor, String tipo, String descricao, String turma,
                              String icone, String cor, Instant criadaEm) {
        this.professor = professor;
        this.tipo = tipo;
        this.descricao = descricao;
        this.turma = turma;
        this.icone = icone;
        this.cor = cor;
        this.criadaEm = criadaEm;
    }
}
