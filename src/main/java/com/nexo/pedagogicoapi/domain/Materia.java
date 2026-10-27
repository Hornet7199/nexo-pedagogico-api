package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Catálogo de matérias da escola. Antes existia apenas como lista hardcoded no
 * client (materias.data.ts) e como texto solto em Professor/Nota/Avaliação;
 * agora é a tabela de referência usada no cadastro de professor.
 */
@Entity
@Table(name = "materias")
@Getter
@Setter
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    /**
     * Etapa em que a matéria é ofertada. Física e Química só existem no médio,
     * Ciências só no fundamental, e o resto atravessa as duas (AMBOS).
     *
     * <p>Coluna como texto e não como enum nativo: o Hibernate congela a lista de
     * valores de um enum nativo ao criar a tabela, e um segmento futuro exigiria
     * migração. Quem valida é o enum no Java.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "segmento", length = 20)
    @Getter(AccessLevel.NONE)
    private SegmentoEnsino segmento = SegmentoEnsino.AMBOS;

    public Materia() {}

    public Materia(String nome) {
        this.nome = nome;
    }

    public Materia(String nome, SegmentoEnsino segmento) {
        this.nome = nome;
        this.segmento = segmento;
    }

    /** Nunca nulo: linha gravada antes da coluna existir vale como AMBOS. */
    public SegmentoEnsino getSegmento() { return segmento != null ? segmento : SegmentoEnsino.AMBOS; }
}
