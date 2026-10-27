package com.nexo.pedagogicoapi.domain;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eventos_auditoria",
       indexes = @Index(name = "idx_eventos_auditoria_criado_em", columnList = "criado_em"))
@Getter
@Setter
public class EventoAuditoria {

    public enum Tipo { LOGIN, LOGOUT, ACESSO, ALTERACAO, ERRO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false)
    private String acao;

    @Column(length = 2000)
    private String detalhe;

    private String ip;

    private Instant criadoEm = Instant.now();

    public EventoAuditoria() {}

    public EventoAuditoria(String usuarioNome, Tipo tipo, String acao, String detalhe, String ip) {
        this.usuarioNome = usuarioNome;
        this.tipo = tipo;
        this.acao = acao;
        this.detalhe = detalhe;
        this.ip = ip;
    }
}
