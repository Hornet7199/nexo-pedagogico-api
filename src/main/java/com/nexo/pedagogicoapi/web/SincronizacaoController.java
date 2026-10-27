package com.nexo.pedagogicoapi.web;

import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import com.nexo.pedagogicoapi.service.SincronizacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Canal pelo qual o usuario-api avisa o pedagógico de quem foi cadastrado ou mudou. Não é
 * usado pelo front. Hoje a chamada vem com o token do próprio operador (o diretor que
 * cadastrou, ou o usuário que mudou o perfil), então a autorização é a de uma pessoa.
 */
@RestController
@RequestMapping("/api/sync")
public class SincronizacaoController {

    private final SincronizacaoService sincronizacao;

    public SincronizacaoController(SincronizacaoService sincronizacao) {
        this.sincronizacao = sincronizacao;
    }

    public record AlunoSync(Long usuarioId,
                            @NotBlank(message = "Informe o nome.") @Size(max = 150) String nome,
                            Long turmaId,
                            @Size(max = 255) String foto) {}

    public record ProfessorSync(Long usuarioId,
                                @NotBlank(message = "Informe o nome.") @Size(max = 150) String nome,
                                @Size(max = 255) String foto,
                                List<Long> materiaIds) {}

    public record PerfilSync(@Size(max = 150) String nome, @Size(max = 255) String foto) {}

    @PutMapping("/alunos/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aluno(@PathVariable Long id, @Valid @RequestBody AlunoSync corpo,
                      @AuthenticationPrincipal UsuarioAutenticado operador) {
        sincronizacao.sincronizarAluno(id, corpo.usuarioId(), corpo.nome(), corpo.turmaId(), corpo.foto(),
                operador.nome());
    }

    @PutMapping("/professores/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void professor(@PathVariable Long id, @Valid @RequestBody ProfessorSync corpo,
                           @AuthenticationPrincipal UsuarioAutenticado operador) {
        sincronizacao.sincronizarProfessor(id, corpo.usuarioId(), corpo.nome(), corpo.foto(),
                corpo.materiaIds(), operador.nome());
    }

    /**
     * Nome e foto de uma conta. Quem pode: o próprio dono (é ele quem edita o perfil) ou a
     * gestão. Sem esta checagem qualquer usuário logado trocaria o nome de outro.
     */
    @PatchMapping("/usuarios/{usuarioId}/perfil")
    @PreAuthorize("hasAnyRole('DIRETOR','ADMIN') or #usuarioId == authentication.principal.id()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void perfil(@PathVariable Long usuarioId, @Valid @RequestBody PerfilSync corpo) {
        sincronizacao.sincronizarPerfil(usuarioId, corpo.nome(), corpo.foto());
    }
}
