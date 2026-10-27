package com.nexo.pedagogicoapi.web;

import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import com.nexo.pedagogicoapi.service.ProfessorDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O que é do professor logado: painel, matérias que leciona e alunos em atenção. */
@RestController
@RequestMapping("/api/professor")
@PreAuthorize("hasRole('PROFESSOR')")
public class ProfessorDashboardController {

    private final ProfessorDashboardService service;
    private final com.nexo.pedagogicoapi.service.EscopoDocente escopoDocente;

    public ProfessorDashboardController(ProfessorDashboardService service,
                                        com.nexo.pedagogicoapi.service.EscopoDocente escopoDocente) {
        this.service = service;
        this.escopoDocente = escopoDocente;
    }

    @GetMapping("/dashboard")
    public ProfessorDashboardService.ProfessorDashboardDTO dashboard(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return service.montar(principal.id());
    }

    /**
     * As matérias que o docente leciona. As telas de nota e de diário montavam esse
     * seletor com uma lista fixa no código ("História, Matemática, Português"), o que
     * deixava um professor de Química sem conseguir lançar nada e oferecia matéria
     * alheia — que o servidor recusa no salvamento. Com a lista vinda daqui, o que
     * está na tela é exatamente o que ele pode lançar.
     */
    @GetMapping("/materias")
    public java.util.Collection<String> materias(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return escopoDocente.nomesDe(principal);
    }

}
