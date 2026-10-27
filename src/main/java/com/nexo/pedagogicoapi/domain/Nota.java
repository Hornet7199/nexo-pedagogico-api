package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "disciplina", "periodo"}),
       indexes = @Index(name = "idx_notas_turma", columnList = "turma_id"))
@Getter
@Setter
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    private Turma turma;

    @Column(nullable = false)
    private String disciplina;

    /** Período letivo, ex.: "2026-1". */
    @Column(nullable = false)
    private String periodo;

    private Double p1;

    private Double p2;

    private Double t1;

    private Double participacao;

    /** Média ponderada oficial calculada no servidor. */
    public Double getMedia() {
        return calcularMedia(p1, p2, t1, participacao);
    }

    /**
     * Mesma fórmula de {@link #getMedia()}, aplicável a projeções — permite calcular
     * a média sem carregar a entidade inteira (usado nas agregações de relatórios/evasão).
     */
    public static Double calcularMedia(Double p1, Double p2, Double t1, Double participacao) {
        double soma = 0;
        double pesos = 0;
        if (p1 != null) { soma += p1 * 3; pesos += 3; }
        if (p2 != null) { soma += p2 * 3; pesos += 3; }
        if (t1 != null) { soma += t1 * 2; pesos += 2; }
        if (participacao != null) { soma += participacao * 2; pesos += 2; }
        if (pesos == 0) return null;
        return Math.round((soma / pesos) * 10.0) / 10.0;
    }
}
