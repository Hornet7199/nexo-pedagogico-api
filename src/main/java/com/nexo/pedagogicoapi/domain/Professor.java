package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

/**
 * O docente como o pedagógico precisa dele: cópia local enxuta de quem vive no
 * usuario-api, mais as métricas de produtividade, que são do pedagógico.
 *
 * <p>O {@code id} é o do professor no usuario-api (não é gerado aqui) e a linha nasce
 * quando o usuario-api avisa do cadastro. Nascimento, sexo e e-mail não são guardados.
 */
@Entity
@Table(name = "professores")
@Getter
@Setter
public class Professor {

    /** Id do professor no usuario-api. */
    @Id
    private Long id;

    /** Id da conta de acesso no usuario-api — é o que vem no claim {@code uid} do token. */
    @Column(unique = true)
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    /** Matérias que o docente leciona. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "professor_materias",
            joinColumns = @JoinColumn(name = "professor_id"),
            inverseJoinColumns = @JoinColumn(name = "materia_id"))
    @OrderBy("nome")
    private Set<Materia> materias = new LinkedHashSet<>();

    private String foto;

    /** Turmas atendidas (texto para exibição no monitoramento docente). */
    private String turmas;

    // ── Métricas de produtividade (monitoramento docente) ──
    private int correcoesPendentes;
    private double tempoRespostaDias;
    private int interacoesSemana;
    private double avaliacao;
    private int tarefasConcluidas;
    private int tarefasTotal;

    /** Matérias em uma linha ("História, Geografia"), para o monitoramento e o dashboard. */
    public String getDisciplinas() {
        return materias.stream().map(Materia::getNome).collect(Collectors.joining(", "));
    }
}
