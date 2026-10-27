package com.nexo.pedagogicoapi.service;

import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.domain.Professor;
import com.nexo.pedagogicoapi.repository.ProfessorRepository;
import com.nexo.pedagogicoapi.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O que um docente pode tocar, por matéria.
 *
 * <p>O escopo por turma já existia (ver {@code exigirLeciona} nos controllers), mas
 * a matéria vinha como texto livre do cliente e ninguém conferia: o professor de
 * História lançava nota de Matemática na própria turma e criava avaliação de
 * Química. Turma certa, matéria alheia — e o registro ficava no boletim do aluno.
 *
 * <p>DIRETOR passa livre: ele responde pela escola inteira e é quem corrige
 * lançamento errado. ADMIN não chega aqui — nota e avaliação estão fora do
 * alcance dela por autorização de rota.
 */
@Service
public class EscopoDocente {

    private final ProfessorRepository professores;

    public EscopoDocente(ProfessorRepository professores) {
        this.professores = professores;
    }

    /**
     * Recusa a operação se {@code disciplina} não for uma das matérias do docente.
     *
     * <p>Comparação sem acento e sem caixa porque o nome chega digitado do cliente
     * ("matematica", "MATEMÁTICA") e recusar por causa de um acento seria ruído, não
     * segurança. Disciplina em branco também é recusada: antes ela virava "Geral" e
     * abria uma gaveta fora de qualquer matéria.
     */
    @Transactional(readOnly = true)
    public void exigirMateria(String disciplina, UsuarioAutenticado operador) {
        if (!"PROFESSOR".equals(operador.role())) return;

        if (disciplina == null || disciplina.isBlank()) {
            throw ApiException.validation("Informe a matéria.",
                    Map.of("disciplina", "Escolha uma das matérias que você leciona."));
        }

        Set<String> minhas = materiasDe(operador);
        if (!minhas.contains(normalizar(disciplina))) {
            throw ApiException.forbidden("Você leciona apenas: " + String.join(", ", nomesDe(operador)) + ".");
        }
    }

    /** Nomes das matérias do docente, como estão no catálogo (para mensagem e filtro). */
    @Transactional(readOnly = true)
    public Set<String> nomesDe(UsuarioAutenticado operador) {
        return professores.findByUsuarioId(operador.id())
                .map(Professor::getMaterias)
                .orElseGet(Set::of)
                .stream()
                .map(Materia::getNome)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /** Matérias de um docente já carregado — evita reconsultar por usuarioId. */
    public Set<String> nomesDePorProfessor(Professor professor) {
        return professor.getMaterias().stream()
                .map(Materia::getNome)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /** Mesma normalização usada aqui, para quem precisa comparar nome de matéria. */
    public static String normalizarPublico(String texto) {
        return texto == null ? "" : normalizar(texto);
    }

    private Set<String> materiasDe(UsuarioAutenticado operador) {
        return nomesDe(operador).stream().map(EscopoDocente::normalizar).collect(Collectors.toSet());
    }

    /** Minúsculo e sem acento — "Matemática", "matematica" e "MATEMATICA" viram o mesmo. */
    private static String normalizar(String texto) {
        String semAcento = java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
