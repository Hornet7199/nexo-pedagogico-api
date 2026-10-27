package com.nexo.pedagogicoapi;

import com.nexo.pedagogicoapi.domain.EventoAuditoria;
import com.nexo.pedagogicoapi.repository.EventoAuditoriaRepository;
import com.nexo.pedagogicoapi.service.AuditoriaRetencaoJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A retenção apaga dados de auditoria, então o corte precisa de teste: um erro de sinal
 * ou de unidade aqui não quebra nada visível — só remove o que deveria ficar.
 */
@SpringBootTest
@ActiveProfiles("teste")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:retencao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "nexo.auditoria.retencao-dias=30"})
class AuditoriaRetencaoTest {

    @Autowired EventoAuditoriaRepository eventos;
    @Autowired AuditoriaRetencaoJob job;

    private Long gravarCom(Instant quando) {
        EventoAuditoria e = new EventoAuditoria("teste", EventoAuditoria.Tipo.LOGIN, "Login", null, "127.0.0.1");
        e.setCriadoEm(quando);
        return eventos.save(e).getId();
    }

    @Test
    void apagaOAntigoEPreservaOQueEstaNaJanela() {
        Instant agora = Instant.now();
        Long muitoAntigo = gravarCom(agora.minus(400, ChronoUnit.DAYS));
        Long logoForaDoCorte = gravarCom(agora.minus(31, ChronoUnit.DAYS));
        Long naBorda = gravarCom(agora.minus(29, ChronoUnit.DAYS));
        Long recente = gravarCom(agora.minus(1, ChronoUnit.DAYS));

        job.limpar();

        assertThat(eventos.findById(muitoAntigo)).as("400 dias").isEmpty();
        assertThat(eventos.findById(logoForaDoCorte)).as("31 dias, fora da janela de 30").isEmpty();
        assertThat(eventos.findById(naBorda)).as("29 dias, dentro da janela").isPresent();
        assertThat(eventos.findById(recente)).as("1 dia").isPresent();
    }

    @Test
    void retencaoZeroNaoApagaNada() {
        Long antigo = gravarCom(Instant.now().minus(3650, ChronoUnit.DAYS));
        // Instanciado direto em vez de mexer no campo final por reflexão: o construtor
        // é a própria interface de configuração do job.
        new AuditoriaRetencaoJob(eventos, 0).limpar();
        assertThat(eventos.findById(antigo)).as("0 desliga a limpeza").isPresent();
    }
}
