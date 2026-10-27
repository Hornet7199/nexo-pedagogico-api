package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.repository.EventoAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Descarta eventos de auditoria antigos.
 *
 * <p>Era a única tabela do sistema sem teto de crescimento. Login e logout geram um
 * evento cada, então o volume acompanha o uso diário da escola, não a administração:
 * mil usuários entrando e saindo num dia letivo são dois mil registros, e nada nunca
 * os removia. Num plano com cota de armazenamento é a tabela que a estoura primeiro.
 *
 * <p>O padrão de um ano é folgado de propósito. A tela de auditoria consulta por
 * {@code ?periodo=<dias>}, escolhido por quem olha, e um corte curto faria a resposta
 * mentir — pareceria "não houve eventos" onde na verdade foram apagados. Ajuste em
 * {@code nexo.auditoria.retencao-dias}; 0 desliga a limpeza e volta ao crescimento
 * ilimitado.
 */
@Component
public class AuditoriaRetencaoJob {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaRetencaoJob.class);

    private final EventoAuditoriaRepository eventos;
    private final int retencaoDias;

    public AuditoriaRetencaoJob(EventoAuditoriaRepository eventos,
                                @Value("${nexo.auditoria.retencao-dias:365}") int retencaoDias) {
        this.eventos = eventos;
        this.retencaoDias = retencaoDias;
    }

    @Scheduled(cron = "0 30 3 * * *") // 03:30 — fora do horário de uso da escola
    @Transactional
    public void limpar() {
        if (retencaoDias <= 0) return;
        Instant corte = Instant.now().minus(retencaoDias, ChronoUnit.DAYS);
        int removidos = eventos.removerAnterioresA(corte);
        if (removidos > 0) {
            log.info("Auditoria: {} eventos anteriores a {} removidos.", removidos, corte);
        }
    }
}
