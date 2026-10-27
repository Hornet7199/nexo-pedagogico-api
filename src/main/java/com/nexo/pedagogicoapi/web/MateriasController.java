package com.nexo.pedagogicoapi.web;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.domain.ConteudoMateria;
import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.domain.SegmentoEnsino;
import com.nexo.pedagogicoapi.repository.AlunoRepository;
import com.nexo.pedagogicoapi.repository.ConteudoMateriaRepository;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Transacional porque a etapa do aluno passa pela turma dele, que é LAZY: com
 * open-in-view desligado (application.yml), ler a turma fora de transação estoura
 * LazyInitializationException.
 */
@RestController
@RequestMapping("/api/materias")
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class MateriasController {

    private final MateriaRepository materias;
    private final ConteudoMateriaRepository conteudos;
    private final AlunoRepository alunos;

    public MateriasController(MateriaRepository materias, ConteudoMateriaRepository conteudos,
                              AlunoRepository alunos) {
        this.materias = materias;
        this.conteudos = conteudos;
        this.alunos = alunos;
    }

    public record MateriaDTO(Long id, String nome, String segmento) {
        static MateriaDTO of(Materia m) {
            return new MateriaDTO(m.getId(), m.getNome(), m.getSegmento().name());
        }
    }

    /**
     * Etapa do aluno logado, a partir da turma dele. Aluno sem turma não cursa
     * nada ainda — cai no fundamental, a etapa mais restrita.
     */
    private SegmentoEnsino segmentoDoAluno(UsuarioAutenticado operador) {
        Aluno aluno = alunos.findByUsuarioId(operador.id())
                .orElseThrow(() -> ApiException.forbidden("Aluno sem cadastro vinculado."));
        return aluno.getTurma() == null
                ? SegmentoEnsino.FUNDAMENTAL
                : aluno.getTurma().getSegmento();
    }

    public record ConteudoMateriaDTO(Long id, String titulo, String resumo, String texto,
                                     String exemplo, int minutos, int ordem) {
        static ConteudoMateriaDTO of(ConteudoMateria c) {
            // minutos é nulo em conteúdo gravado antes do campo existir (ver
            // ConteudoMateria); 0 esconde a etiqueta de tempo em vez de estourar.
            return new ConteudoMateriaDTO(c.getId(), c.getTitulo(), c.getResumo(), c.getTexto(),
                    c.getExemplo(), c.getMinutos() == null ? 0 : c.getMinutos(), c.getOrdem());
        }
    }

    /**
     * Catálogo de matérias.
     *
     * <p>Para quem administra (diretor, administrador, professor) é o catálogo inteiro
     * — é o que alimenta a seleção do cadastro de professor. Para o ALUNO a lista
     * já vem recortada pela etapa dele: o aluno do médio não cursa Ciências e o do
     * fundamental não cursa Física, então não faz sentido nem exibi-las. O recorte
     * é feito aqui, no servidor, e não por filtro de tela — filtro de tela é
     * conveniência, não regra de acesso.
     */
    @GetMapping
    public List<MateriaDTO> listar(@AuthenticationPrincipal UsuarioAutenticado operador) {
        var todas = materias.findAllByOrderByNome();
        if (!"ALUNO".equals(operador.role())) {
            return todas.stream().map(MateriaDTO::of).toList();
        }
        SegmentoEnsino etapa = segmentoDoAluno(operador);
        return todas.stream()
                .filter(m -> m.getSegmento().atende(etapa))
                .map(MateriaDTO::of)
                .toList();
    }

    /**
     * Conteúdo/documentos da matéria, para a tela do aluno. Matérias sem
     * conteúdo cadastrado devolvem lista vazia — o client trata esse caso.
     *
     * <p>A checagem de etapa se repete aqui de propósito: esconder a matéria da
     * listagem não impede ninguém de pedir /api/materias/7/conteudos na mão. Pelo
     * mesmo motivo o despublicado é filtrado aqui, e não na tela: conteúdo tirado
     * do ar pelo administrador não pode voltar por uma requisição feita na mão.
     */
    @GetMapping("/{id}/conteudos")
    public List<ConteudoMateriaDTO> conteudos(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioAutenticado operador) {
        Materia materia = materias.findById(id)
                .orElseThrow(() -> ApiException.notFound("Matéria não encontrada."));

        if ("ALUNO".equals(operador.role()) && !materia.getSegmento().atende(segmentoDoAluno(operador))) {
            throw ApiException.forbidden("Esta matéria não faz parte da sua etapa de ensino.");
        }
        return conteudos.publicadosDaMateria(id).stream().map(ConteudoMateriaDTO::of).toList();
    }
}
