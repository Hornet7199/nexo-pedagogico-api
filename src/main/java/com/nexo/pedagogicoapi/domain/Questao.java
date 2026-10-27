package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "questoes")
@Getter
@Setter
public class Questao {

    public enum Tipo { OBJETIVA, DISSERTATIVA }

    public enum Dificuldade { FACIL, MEDIA, DIFICIL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4000)
    private String enunciado;

    private String disciplina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo = Tipo.OBJETIVA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dificuldade dificuldade = Dificuldade.MEDIA;

    private Instant criadaEm = Instant.now();
}
