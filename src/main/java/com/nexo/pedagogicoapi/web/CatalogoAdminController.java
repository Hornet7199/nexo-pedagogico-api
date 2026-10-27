package com.nexo.pedagogicoapi.web;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.ConteudoMateria;
import com.nexo.pedagogicoapi.domain.EventoAuditoria;
import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.repository.ConteudoMateriaRepository;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import com.nexo.pedagogicoapi.repository.TurmaRepository;
import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import com.nexo.pedagogicoapi.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Painel do administrador no que é pedagógico: <b>o que está publicado</b>. A gestão de
 * contas (quem tem acesso) é do usuario-api, e os desafios, da gamificação.
 *
 * <p>O DIRETOR também enxerga, como já enxergava a secretaria.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','DIRETOR')")
@Transactional
public class CatalogoAdminController {

    private final TurmaRepository turmas;
    private final MateriaRepository materias;
    private final ConteudoMateriaRepository conteudos;
    private final AuditoriaService auditoria;

    public CatalogoAdminController(TurmaRepository turmas, MateriaRepository materias,
                                   ConteudoMateriaRepository conteudos, AuditoriaService auditoria) {
        this.turmas = turmas;
        this.materias = materias;
        this.conteudos = conteudos;
        this.auditoria = auditoria;
    }

    public record DashboardAdminDTO(long turmas, long materias, long conteudos, long conteudosDespublicados) {}

    /** Números do catálogo. Os de contas vêm do usuario-api (GET /api/admin/contas/resumo). */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public DashboardAdminDTO dashboard() {
        long total = 0;
        long despublicados = 0;
        for (Object[] linha : conteudos.contarPorPublicado()) {
            long quantidade = ((Number) linha[1]).longValue();
            total += quantidade;
            if (Boolean.FALSE.equals(linha[0])) despublicados += quantidade;
        }
        return new DashboardAdminDTO(turmas.count(), materias.count(), total, despublicados);
    }

    // ── Catálogo de conteúdo ─────────────────────────────────────────────────

    public record MateriaCatalogoDTO(Long id, String nome, String segmento,
                                     long conteudos, long conteudosPublicados) {}

    /**
     * O catálogo por matéria, com quanto de cada uma está no ar. É a resposta para
     * "onde falta conteúdo?" — a pergunta que um sistema de retenção faz antes de
     * qualquer outra.
     */
    @GetMapping("/catalogo")
    @Transactional(readOnly = true)
    public List<MateriaCatalogoDTO> catalogo() {
        Map<Long, List<ConteudoMateria>> porMateria = conteudos.findAll().stream()
                .filter(c -> c.getMateria() != null)
                .collect(Collectors.groupingBy(c -> c.getMateria().getId()));

        return materias.findAllByOrderByNome().stream()
                .map(m -> {
                    List<ConteudoMateria> lista = porMateria.getOrDefault(m.getId(), List.of());
                    long publicados = lista.stream().filter(ConteudoMateria::isPublicado).count();
                    return new MateriaCatalogoDTO(m.getId(), m.getNome(), m.getSegmento().name(),
                            lista.size(), publicados);
                })
                .toList();
    }

    public record ConteudoAdminDTO(Long id, Long materiaId, String materia, String titulo,
                                   String resumo, int minutos, int ordem, boolean publicado) {
        static ConteudoAdminDTO of(ConteudoMateria c) {
            return new ConteudoAdminDTO(c.getId(),
                    c.getMateria() != null ? c.getMateria().getId() : null,
                    c.getMateria() != null ? c.getMateria().getNome() : null,
                    c.getTitulo(), c.getResumo(),
                    c.getMinutos() == null ? 0 : c.getMinutos(),
                    c.getOrdem(), c.isPublicado());
        }
    }

    /** Conteúdos da matéria na ordem em que o aluno os vê, publicados e não. */
    @GetMapping("/catalogo/materias/{materiaId}/conteudos")
    @Transactional(readOnly = true)
    public List<ConteudoAdminDTO> conteudosDaMateria(@PathVariable Long materiaId) {
        exigirMateria(materiaId);
        return conteudos.findByMateriaIdOrderByOrdemAsc(materiaId).stream()
                .map(ConteudoAdminDTO::of).toList();
    }

    public record AtualizarPublicadoRequest(Boolean publicado) {}

    /**
     * Tira do ar ou devolve. Não há DELETE de conteúdo aqui de propósito: apagar
     * levaria junto o registro de quem já o concluiu, e com ele o progresso que
     * este sistema existe para medir.
     */
    @PatchMapping("/catalogo/conteudos/{id}/publicado")
    public ConteudoAdminDTO publicarConteudo(@PathVariable Long id,
                                             @RequestBody AtualizarPublicadoRequest request,
                                             @AuthenticationPrincipal UsuarioAutenticado operador) {
        if (request == null || request.publicado() == null) {
            throw ApiException.badRequest("Informe se o conteúdo fica publicado.");
        }
        ConteudoMateria conteudo = conteudos.findById(id)
                .orElseThrow(() -> ApiException.notFound("Conteúdo não encontrado."));
        if (conteudo.isPublicado() == request.publicado()) {
            return ConteudoAdminDTO.of(conteudo); // idempotente
        }
        conteudo.setPublicado(request.publicado());
        conteudos.save(conteudo);
        auditoria.registrar(operador.nome(), EventoAuditoria.Tipo.ALTERACAO,
                request.publicado() ? "Conteúdo publicado" : "Conteúdo despublicado",
                conteudo.getTitulo(), null);
        return ConteudoAdminDTO.of(conteudo);
    }

    public record ReordenarRequest(List<Long> conteudoIds) {}

    /**
     * Reordena a matéria inteira de uma vez, e não um item por requisição: a ordem é
     * uma propriedade do conjunto, e mover o terceiro para o topo mexe na posição de
     * todos os outros. Receber a lista completa também torna a operação idempotente —
     * reenviar a mesma ordem não desloca nada.
     */
    @PatchMapping("/catalogo/materias/{materiaId}/ordem")
    public List<ConteudoAdminDTO> reordenar(@PathVariable Long materiaId,
                                            @RequestBody ReordenarRequest request,
                                            @AuthenticationPrincipal UsuarioAutenticado operador) {
        Materia materia = exigirMateria(materiaId);
        if (request == null || request.conteudoIds() == null || request.conteudoIds().isEmpty()) {
            throw ApiException.badRequest("Informe a nova ordem dos conteúdos.");
        }

        List<ConteudoMateria> atuais = conteudos.findByMateriaIdOrderByOrdemAsc(materiaId);
        Map<Long, ConteudoMateria> porId = atuais.stream()
                .collect(Collectors.toMap(ConteudoMateria::getId, Function.identity()));

        // A lista precisa ser exatamente a da matéria: uma parcial deixaria os
        // ausentes com a ordem antiga, misturados aos reordenados, e um id de outra
        // matéria mudaria a ordem de uma matéria que ninguém pediu para mexer.
        if (request.conteudoIds().size() != atuais.size()
                || !porId.keySet().containsAll(request.conteudoIds())) {
            throw ApiException.validation("Ordem inválida.",
                    Map.of("conteudoIds", "Envie todos os conteúdos da matéria, uma vez cada."));
        }

        int posicao = 0;
        for (Long id : request.conteudoIds()) {
            porId.get(id).setOrdem(posicao++);
        }
        conteudos.saveAll(atuais);
        auditoria.registrar(operador.nome(), EventoAuditoria.Tipo.ALTERACAO,
                "Conteúdos reordenados", "Matéria: " + materia.getNome(), null);

        return conteudos.findByMateriaIdOrderByOrdemAsc(materiaId).stream()
                .map(ConteudoAdminDTO::of).toList();
    }

    private Materia exigirMateria(Long id) {
        return materias.findById(id)
                .orElseThrow(() -> ApiException.notFound("Matéria não encontrada."));
    }
}
