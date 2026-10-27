package com.nexo.pedagogicoapi.domain;

/**
 * Etapa do ensino básico. Separa o que o aluno do fundamental vê do que é do
 * ensino médio: currículo, matérias e conteúdo não se misturam entre as etapas.
 *
 * <p>{@link #AMBOS} existe só para matéria — Matemática e Português atravessam as
 * duas etapas, enquanto Física e Química são exclusivas do médio e Ciências do
 * fundamental. Turma e aluno nunca são AMBOS: sempre estão em uma etapa só.
 */
public enum SegmentoEnsino {
    FUNDAMENTAL, MEDIO, AMBOS;

    /** Uma matéria deste segmento é ofertada para quem cursa {@code etapa}? */
    public boolean atende(SegmentoEnsino etapa) {
        return this == AMBOS || this == etapa;
    }

    /**
     * Etapa a partir do nome da turma, que é o único lugar onde ela está hoje
     * ("9º Ano B" → fundamental, "1º Ano EM A" → médio). Turma sem padrão
     * reconhecido cai no fundamental, a etapa mais restrita das duas — errar para
     * o lado que mostra menos é melhor do que abrir conteúdo do médio por engano.
     */
    public static SegmentoEnsino daTurma(String nomeDaTurma) {
        if (nomeDaTurma == null) return FUNDAMENTAL;
        return nomeDaTurma.toUpperCase().contains("EM") ? MEDIO : FUNDAMENTAL;
    }
}
