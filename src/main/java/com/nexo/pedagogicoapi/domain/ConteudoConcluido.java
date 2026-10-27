package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Marca que um aluno concluiu um conteúdo de matéria.
 *
 * <p>É desta tabela que sai o progresso por matéria: concluídos ÷ total de
 * conteúdos daquela matéria. Antes o percentual vinha de número fixo no client
 * (materias.data.ts), igual para todo mundo e imune ao que o aluno fazia.
 *
 * <p>Uma linha por par aluno+conteúdo, garantido por índice único: marcar duas
 * vezes é a mesma conclusão, não duas — e sem isso o percentual passaria de 100%.
 */
@Entity
@Table(name = "conteudos_concluidos",
        uniqueConstraints = @UniqueConstraint(name = "uk_conteudo_por_aluno",
                columnNames = {"aluno_id", "conteudo_id"}))
@Getter
@Setter
public class ConteudoConcluido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteudo_id", nullable = false)
    private ConteudoMateria conteudo;

    private Instant concluidoEm = Instant.now();

    public ConteudoConcluido() {}

    public ConteudoConcluido(Aluno aluno, ConteudoMateria conteudo) {
        this.aluno = aluno;
        this.conteudo = conteudo;
    }
}
