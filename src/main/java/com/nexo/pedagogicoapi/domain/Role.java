package com.nexo.pedagogicoapi.domain;

/**
 * Papéis do sistema. Quem os emite é o usuario-api (no claim {@code role} do JWT); aqui
 * eles só servem para reconhecer o token e para as regras de autorização.
 */
public enum Role {
    ALUNO, PROFESSOR, DIRETOR, ADMIN
}
