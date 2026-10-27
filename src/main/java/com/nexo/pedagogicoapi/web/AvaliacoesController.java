package com.nexo.pedagogicoapi.web;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Avaliacao;
import com.nexo.pedagogicoapi.domain.Professor;
import com.nexo.pedagogicoapi.domain.Questao;
import com.nexo.pedagogicoapi.domain.Turma;
import com.nexo.pedagogicoapi.repository.AvaliacaoRepository;
import com.nexo.pedagogicoapi.repository.ProfessorRepository;
import com.nexo.pedagogicoapi.repository.QuestaoRepository;
import com.nexo.pedagogicoapi.repository.TurmaRepository;
import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasAnyRole('PROFESSOR','DIRETOR')")
@org.springframework.transaction.annotation.Transactional
public class AvaliacoesController {

    private final AvaliacaoRepository avaliacoes;
    private final QuestaoRepository questoes;
    private final TurmaRepository turmas;
    private final ProfessorRepository professores;
    private final com.nexo.pedagogicoapi.service.EscopoDocente escopoDocente;

    public AvaliacoesController(AvaliacaoRepository avaliacoes, QuestaoRepository questoes, TurmaRepository turmas,
                                ProfessorRepository professores,
                                com.nexo.pedagogicoapi.service.EscopoDocente escopoDocente) {
        this.avaliacoes = avaliacoes;
        this.questoes = questoes;
        this.turmas = turmas;
        this.professores = professores;
        this.escopoDocente = escopoDocente;
    }

    /** PROFESSOR só cria avaliação para a turma que leciona; DIRETOR tem acesso irrestrito. */
    private void exigirLeciona(Turma turma, UsuarioAutenticado operador) {
        if (!"PROFESSOR".equals(operador.role())) return;
        Professor professor = professores.findByUsuarioId(operador.id()).orElse(null);
        boolean leciona = professor != null
                && turma.getProfessor() != null && turma.getProfessor().getId().equals(professor.getId());
        if (!leciona) {
            throw ApiException.forbidden("Você não leciona esta turma.");
        }
    }

    public record AvaliacaoDTO(Long id, String titulo, String disciplina, String turma, String tipo,
                               String status, LocalDate data, int entregas, int pendentesCorrecao) {
        static AvaliacaoDTO of(Avaliacao a) {
            return new AvaliacaoDTO(a.getId(), a.getTitulo(), a.getDisciplina(),
                    a.getTurma() != null ? a.getTurma().getNome() : null,
                    a.getTipo(), a.getStatus().name(), a.getData(), a.getEntregas(), a.getPendentesCorrecao());
        }
    }

    /**
     * Ids das turmas que o operador pode enxergar, ou {@code null} quando não há
     * restrição (DIRETOR). Mesma regra de {@link #exigirLeciona}, aplicada a listagens.
     */
    private List<Long> turmasVisiveis(UsuarioAutenticado operador) {
        if (!"PROFESSOR".equals(operador.role())) return null;
        Professor professor = professores.findByUsuarioId(operador.id()).orElse(null);
        if (professor == null) return List.of();
        return turmas.findByProfessorIdOrderByNome(professor.getId()).stream().map(Turma::getId).toList();
    }

    @GetMapping("/api/avaliacoes")
    public List<AvaliacaoDTO> listar(@RequestParam(required = false) Long turma,
                                     @RequestParam(required = false) Avaliacao.Status status,
                                     @AuthenticationPrincipal UsuarioAutenticado operador) {
        // A criação já exigia lecionar a turma, mas a listagem não: qualquer professor
        // via as avaliações de todas as turmas da escola, inclusive as dos colegas.
        List<Long> minhas = turmasVisiveis(operador);
        if (minhas == null) {
            return avaliacoes.buscar(turma, status).stream().map(AvaliacaoDTO::of).toList();
        }
        if (turma != null) {
            if (!minhas.contains(turma)) {
                throw ApiException.forbidden("Você não leciona esta turma.");
            }
            return avaliacoes.buscarPorTurmas(List.of(turma), status).stream().map(AvaliacaoDTO::of).toList();
        }
        return avaliacoes.buscarPorTurmas(minhas, status).stream().map(AvaliacaoDTO::of).toList();
    }

    public record NovaAvaliacaoRequest(String titulo, String disciplina, Long turmaId,
                                       String tipo, LocalDate data) {}

    /** Recurso endereçável da avaliação, com o mesmo escopo de turma da listagem. */
    @GetMapping("/api/avaliacoes/{id}")
    public AvaliacaoDTO detalhar(@PathVariable Long id,
                                 @AuthenticationPrincipal UsuarioAutenticado operador) {
        Avaliacao avaliacao = avaliacoes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Avaliação não encontrada."));
        List<Long> minhas = turmasVisiveis(operador);
        // Avaliação sem turma não pertence a ninguém e segue visível para todos,
        // igual ao critério de buscarPorTurmas.
        if (minhas != null && avaliacao.getTurma() != null && !minhas.contains(avaliacao.getTurma().getId())) {
            throw ApiException.forbidden("Você não leciona esta turma.");
        }
        return AvaliacaoDTO.of(avaliacao);
    }

    @PostMapping("/api/avaliacoes")
    public ResponseEntity<AvaliacaoDTO> criar(@RequestBody NovaAvaliacaoRequest request,
                                              @AuthenticationPrincipal UsuarioAutenticado operador) {
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw ApiException.validation("Dados inválidos.", Map.of("titulo", "Informe o título da avaliação."));
        }
        // Mesma regra das notas: a turma pode ser dele, a matéria não.
        escopoDocente.exigirMateria(request.disciplina(), operador);
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setTitulo(request.titulo().trim());
        avaliacao.setDisciplina(request.disciplina());
        avaliacao.setTipo(request.tipo());
        avaliacao.setData(request.data());
        avaliacao.setStatus(Avaliacao.Status.PUBLICADA);
        if (request.turmaId() != null) {
            Turma turma = turmas.findById(request.turmaId())
                    .orElseThrow(() -> ApiException.badRequest("Turma inexistente."));
            exigirLeciona(turma, operador);
            avaliacao.setTurma(turma);
        }
        AvaliacaoDTO dto = AvaliacaoDTO.of(avaliacoes.save(avaliacao));
        return ResponseEntity.created(uriDoItem(dto.id())).body(dto);
    }

    /** URI do recurso recém-criado, a partir do caminho da própria requisição. */
    private static URI uriDoItem(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }

    @GetMapping("/api/avaliacoes/fila-correcao")
    public List<AvaliacaoDTO> filaCorrecao(@AuthenticationPrincipal UsuarioAutenticado operador) {
        List<Avaliacao.Status> pendentes = List.of(Avaliacao.Status.EM_CORRECAO, Avaliacao.Status.PUBLICADA);
        List<Long> minhas = turmasVisiveis(operador);
        List<Avaliacao> lista = minhas == null
                ? avaliacoes.findByStatusInOrderByDataAsc(pendentes)
                : avaliacoes.findByStatusInAndTurmas(pendentes, minhas);
        return lista.stream()
                .filter(a -> a.getPendentesCorrecao() > 0)
                .map(AvaliacaoDTO::of)
                .toList();
    }

    // ── Banco de questões ────────────────────────────────────────────────────

    public record QuestaoDTO(Long id, String enunciado, String disciplina, String tipo,
                             String dificuldade, Instant criadaEm) {
        static QuestaoDTO of(Questao q) {
            return new QuestaoDTO(q.getId(), q.getEnunciado(), q.getDisciplina(),
                    q.getTipo().name(), q.getDificuldade().name(), q.getCriadaEm());
        }
    }

    @GetMapping("/api/questoes")
    public List<QuestaoDTO> listarQuestoes() {
        return questoes.findAll().stream().map(QuestaoDTO::of).toList();
    }

    /** Faltava o GET do item: a questão tinha PUT e DELETE, mas não dava para lê-la. */
    @GetMapping("/api/questoes/{id}")
    public QuestaoDTO detalharQuestao(@PathVariable Long id) {
        return questoes.findById(id).map(QuestaoDTO::of)
                .orElseThrow(() -> ApiException.notFound("Questão não encontrada."));
    }

    public record NovaQuestaoRequest(String enunciado, String disciplina,
                                     Questao.Tipo tipo, Questao.Dificuldade dificuldade) {}

    @PostMapping("/api/questoes")
    public ResponseEntity<QuestaoDTO> criarQuestao(@RequestBody NovaQuestaoRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado operador) {
        if (request.enunciado() == null || request.enunciado().isBlank()) {
            throw ApiException.validation("Dados inválidos.", Map.of("enunciado", "Informe o enunciado."));
        }
        // O banco de questões alimenta as avaliações, então segue a mesma regra
        // delas: o professor só cria questão da matéria que leciona. Sem isto,
        // bastava criar a questão de Química aqui para contornar o escopo.
        escopoDocente.exigirMateria(request.disciplina(), operador);
        Questao questao = new Questao();
        questao.setEnunciado(request.enunciado().trim());
        questao.setDisciplina(request.disciplina());
        if (request.tipo() != null) questao.setTipo(request.tipo());
        if (request.dificuldade() != null) questao.setDificuldade(request.dificuldade());
        QuestaoDTO dto = QuestaoDTO.of(questoes.save(questao));
        return ResponseEntity.created(uriDoItem(dto.id())).body(dto);
    }

    @PutMapping("/api/questoes/{id}")
    public QuestaoDTO atualizarQuestao(@PathVariable Long id, @RequestBody NovaQuestaoRequest request,
                                       @AuthenticationPrincipal UsuarioAutenticado operador) {
        Questao q = questoes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Questão não encontrada."));
        // Editar questão alheia para a própria matéria — ou a própria para uma
        // matéria alheia — tem que passar pelas duas pontas.
        escopoDocente.exigirMateria(q.getDisciplina(), operador);
        if (request.disciplina() != null) escopoDocente.exigirMateria(request.disciplina(), operador);
        if (request.enunciado() != null && !request.enunciado().isBlank()) q.setEnunciado(request.enunciado().trim());
        if (request.disciplina() != null) q.setDisciplina(request.disciplina());
        if (request.tipo() != null) q.setTipo(request.tipo());
        if (request.dificuldade() != null) q.setDificuldade(request.dificuldade());
        return QuestaoDTO.of(questoes.save(q));
    }

    @DeleteMapping("/api/questoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirQuestao(@PathVariable Long id,
                               @AuthenticationPrincipal UsuarioAutenticado operador) {
        Questao q = questoes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Questão não encontrada."));
        escopoDocente.exigirMateria(q.getDisciplina(), operador);
        questoes.delete(q);
    }
}
