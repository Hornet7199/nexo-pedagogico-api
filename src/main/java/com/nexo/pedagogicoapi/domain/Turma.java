package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "turmas",
       // TurmaRepository.findByProfessorIdOrderByNome — primeira consulta do dashboard
       // do professor, e a que define o escopo de todas as outras.
       indexes = @Index(name = "idx_turmas_professor", columnList = "professor_id"))
@Getter
@Setter
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private int anoLetivo;

    private String turno;

    /**
     * Limite de vagas da turma — controla a ocupação das turmas.
     * Integer (e não int): se a coluna for adicionada a um banco que já tem turmas,
     * as linhas antigas ficam NULL, e o getter cai no padrão.
     */
    @Getter(AccessLevel.NONE)
    private Integer capacidade = CAPACIDADE_PADRAO;

    public static final int CAPACIDADE_PADRAO = 35;

    /** Professor responsável (usado no dashboard/monitoramento docente). */
    @ManyToOne(fetch = FetchType.LAZY)
    private Professor professor;

    /** Etapa de ensino, derivada do nome ("9º Ano B" → fundamental, "1º Ano EM A" → médio). */
    public SegmentoEnsino getSegmento() { return SegmentoEnsino.daTurma(nome); }
    public int getCapacidade() { return capacidade != null ? capacidade : CAPACIDADE_PADRAO; }
}
