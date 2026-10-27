package com.nexo.pedagogicoapi.web;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.api.PageEnvelope;
import com.nexo.pedagogicoapi.domain.EventoAuditoria;
import com.nexo.pedagogicoapi.domain.Inscricao;
import com.nexo.pedagogicoapi.repository.InscricaoRepository;
import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import com.nexo.pedagogicoapi.service.AuditoriaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Inscrições de alunos nas turmas de estudo.
 *
 * <p>Substitui o antigo /api/matriculas. O que sumiu junto com a matrícula diz o
 * que este sistema deixou de ser: não há mais efetivação nem trancamento, nem
 * situação de documentação, nem rematrícula, nem declaração de vínculo em PDF.
 * Isso é vida escolar, e a vida escolar mora no sistema de aula da escola.
 *
 * <p>Sobraram as duas operações de que o aprendizado depende: ligar/desligar o
 * acesso de um aluno ao conteúdo da turma, e movê-lo de turma.
 */
@RestController
@RequestMapping("/api/inscricoes")
@PreAuthorize("hasAnyRole('DIRETOR','ADMIN')")
@org.springframework.transaction.annotation.Transactional
public class InscricoesController {

    private final InscricaoRepository inscricoes;
    private final com.nexo.pedagogicoapi.repository.TurmaRepository turmas;
    private final com.nexo.pedagogicoapi.repository.AlunoRepository alunos;
    private final AuditoriaService auditoria;

    public InscricoesController(InscricaoRepository inscricoes,
                                com.nexo.pedagogicoapi.repository.TurmaRepository turmas,
                                com.nexo.pedagogicoapi.repository.AlunoRepository alunos,
                                AuditoriaService auditoria) {
        this.inscricoes = inscricoes;
        this.turmas = turmas;
        this.alunos = alunos;
        this.auditoria = auditoria;
    }

    public record InscricaoDTO(Long id, Long alunoId, String aluno, Long turmaId, String turma,
                               boolean ativo, Instant criadaEm) {
        static InscricaoDTO of(Inscricao i) {
            return new InscricaoDTO(i.getId(), i.getAluno().getId(), i.getAluno().getNome(),
                    i.getTurma() != null ? i.getTurma().getId() : null,
                    i.getTurma() != null ? i.getTurma().getNome() : null,
                    i.isAtivo(), i.getCriadaEm());
        }
    }

    @GetMapping
    public PageEnvelope<InscricaoDTO> listar(@RequestParam(required = false) Boolean ativo,
                                             @RequestParam(required = false) Long turma,
                                             @RequestParam(required = false) String busca,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        String filtroBusca = (busca == null || busca.isBlank())
                ? null
                : "%" + busca.trim().toLowerCase() + "%";
        var resultado = inscricoes.buscar(ativo, turma, filtroBusca, PageRequest.of(page, Math.min(size, 100)));
        return PageEnvelope.of(resultado, InscricaoDTO::of);
    }

    @GetMapping("/{id}")
    public InscricaoDTO detalhar(@PathVariable Long id) {
        return inscricoes.findById(id).map(InscricaoDTO::of)
                .orElseThrow(() -> ApiException.notFound("Inscrição não encontrada."));
    }

    public record AtualizarAtivoRequest(Boolean ativo) {}

    /**
     * Liga ou desliga o acesso ao conteúdo da turma. PATCH e não DELETE: desligar
     * não apaga a inscrição, e o progresso já registrado do aluno continua onde
     * está — é o que permite religar sem perder nada.
     */
    @PatchMapping("/{id}/ativo")
    public InscricaoDTO atualizarAtivo(@PathVariable Long id,
                                       @RequestBody AtualizarAtivoRequest request,
                                       @AuthenticationPrincipal UsuarioAutenticado operador) {
        if (request == null || request.ativo() == null) {
            throw ApiException.badRequest("Informe se a inscrição fica ativa.");
        }
        Inscricao inscricao = inscricoes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Inscrição não encontrada."));
        if (inscricao.isAtivo() == request.ativo()) {
            return InscricaoDTO.of(inscricao); // idempotente: repetir o PATCH não é erro
        }
        inscricao.setAtivo(request.ativo());
        inscricoes.save(inscricao);
        auditoria.registrar(operador.nome(), EventoAuditoria.Tipo.ALTERACAO,
                request.ativo() ? "Inscrição reativada" : "Inscrição desativada",
                "Inscrição #" + id, null);
        return InscricaoDTO.of(inscricao);
    }

    public record TransferirTurmaRequest(Long turmaId) {}

    /**
     * Move o aluno de turma. A inscrição e o cadastro do aluno andam juntos: se
     * divergissem, o aluno veria o conteúdo de uma turma e apareceria na lista de
     * outra.
     */
    @PatchMapping("/{id}/turma")
    public InscricaoDTO transferirTurma(@PathVariable Long id,
                                        @RequestBody TransferirTurmaRequest request,
                                        @AuthenticationPrincipal UsuarioAutenticado operador) {
        if (request.turmaId() == null) {
            throw ApiException.badRequest("Informe a turma de destino.");
        }
        Inscricao inscricao = inscricoes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Inscrição não encontrada."));
        var destino = turmas.findById(request.turmaId())
                .orElseThrow(() -> ApiException.notFound("Turma de destino não encontrada."));
        var origem = inscricao.getTurma();
        if (origem != null && origem.getId().equals(destino.getId())) {
            return InscricaoDTO.of(inscricao); // já está lá — repetir não é erro
        }
        long ocupadas = alunos.countByTurmaId(destino.getId());
        if (ocupadas >= destino.getCapacidade()) {
            throw ApiException.validation("A turma de destino está lotada.",
                    java.util.Map.of("turmaId", destino.getNome() + " já tem " + ocupadas
                            + " alunos para " + destino.getCapacidade() + " vagas."));
        }
        inscricao.setTurma(destino);
        var aluno = inscricao.getAluno();
        aluno.setTurma(destino);
        inscricoes.save(inscricao);
        auditoria.registrar(operador.nome(), EventoAuditoria.Tipo.ALTERACAO,
                "Aluno transferido de turma",
                aluno.getNome() + ": " + (origem != null ? origem.getNome() : "sem turma")
                        + " → " + destino.getNome(), null);
        return InscricaoDTO.of(inscricao);
    }
}
