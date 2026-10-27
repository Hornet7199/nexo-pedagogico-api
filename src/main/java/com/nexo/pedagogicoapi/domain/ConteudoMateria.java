package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Conteúdo/documento de uma matéria (texto estruturado, sem upload de arquivo
 * nesta etapa). Substitui gradualmente os tópicos hardcoded de materias.data.ts.
 */
@Entity
@Table(name = "conteudos_materia")
@Getter
@Setter
public class ConteudoMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Materia materia;

    @Column(nullable = false)
    private String titulo;

    /**
     * Uma frase dizendo do que se trata. Aparece já no card, antes de abrir:
     * é o que deixa o aluno escolher o que estudar sem ter de abrir tudo.
     *
     * Os três campos abaixo aceitam nulo no banco de propósito. O projeto não
     * tem ferramenta de migração e roda com ddl-auto: update, que não consegue
     * adicionar coluna NOT NULL a uma tabela que já tem linhas — declará-los
     * obrigatórios quebrava a subida em qualquer banco já existente. Quem
     * garante o preenchimento é o ConteudoMateriaSeed, que reescreve o conjunto.
     */
    @Column(length = 500)
    private String resumo;

    @Column(nullable = false, length = 4000)
    private String texto;

    /** Exemplo concreto ou macete — fecha a leitura com algo que gruda. */
    @Column(length = 1000)
    private String exemplo;

    /** Minutos estimados de leitura. Mostrado no card para não assustar. */
    @Column(nullable = true)
    private Integer minutos;

    /** Ordem de exibição dentro da matéria. */
    private int ordem;

    /**
     * Conteúdo despublicado some da tela do aluno mas continua no banco, com o
     * progresso de quem já leu intacto. É a alternativa a apagar para corrigir:
     * o administrador tira de circulação, ajusta e devolve.
     *
     * <p>Boolean (e não boolean) porque ddl-auto=update cria a coluna sem valor:
     * linha gravada antes dela existir fica nula, e o getter a trata como publicada
     * — que é o estado em que ela já estava.
     */
    @Column(name = "publicado")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean publicado = Boolean.TRUE;

    /** Nunca nulo: conteúdo gravado antes da coluna existir já estava no ar. */
    public boolean isPublicado() { return publicado == null || publicado; }
    /** Primitivo de propósito: o campo é wrapper só por causa de linha legada nula. */
    public void setPublicado(boolean publicado) { this.publicado = publicado; }
}
