package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * O aluno como o pedagógico precisa dele: uma cópia local enxuta de quem vive no
 * usuario-api. Existe para que notas, frequência, inscrições e relatórios continuem
 * podendo ligar o registro acadêmico a um nome e a uma turma sem uma chamada de rede por
 * linha.
 *
 * <p>O {@code id} <b>não é gerado aqui</b>: é o mesmo id que o aluno tem no usuario-api,
 * e é por ele que os dois serviços se referem ao aluno. A linha nasce quando o
 * usuario-api avisa do cadastro (ver {@code SincronizacaoController}) e é atualizada
 * quando nome, turma ou foto mudam lá. Nada de dado pessoal (nascimento, sexo) nem de
 * credencial fica aqui.
 *
 * <p>O engajamento, o último acesso e as intervenções são do pedagógico: alimentam a
 * gestão de evasão.
 */
@Entity
@Table(name = "alunos",
       // Recorte por turma: é o filtro de praticamente toda tela de professor, e a coluna
       // que os agregados de nota/frequência atravessam para chegar à turma.
       indexes = @Index(name = "idx_alunos_turma", columnList = "turma_id"))
@Getter
@Setter
public class Aluno {

    /** Id do aluno no usuario-api. */
    @Id
    private Long id;

    /** Id da conta de acesso no usuario-api — é o que vem no claim {@code uid} do token. */
    @Column(unique = true)
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    private Turma turma;

    /** Foto de perfil (usada em telas de gestão como Evasão). */
    private String foto;

    /** Índice de engajamento (0-100), atualizado a partir de acessos/entregas. */
    private int engajamento;

    /** Último acesso do aluno à plataforma. */
    private Instant ultimoAcessoEm;

    /** Nº de intervenções pedagógicas registradas (gestão de evasão). */
    private int intervencoes;

    /** Data da intervenção mais recente. */
    private Instant ultimaIntervencaoEm;
}
