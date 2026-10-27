package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.domain.EventoAuditoria;
import com.nexo.pedagogicoapi.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repository;

    public AuditoriaService(EventoAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String usuarioNome, EventoAuditoria.Tipo tipo, String acao, String detalhe, String ip) {
        repository.save(new EventoAuditoria(usuarioNome, tipo, acao, detalhe, ip));
    }
}
