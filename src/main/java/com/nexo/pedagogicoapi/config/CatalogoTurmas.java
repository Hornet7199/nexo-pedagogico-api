package com.nexo.pedagogicoapi.config;

import com.nexo.pedagogicoapi.domain.Turma;
import com.nexo.pedagogicoapi.repository.TurmaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turmas da escola: anos finais do fundamental (6º-9º) e todo o ensino médio
 * (1º-3º), cada um com 5 turmas (A-E). Não existe tela de cadastro de turmas,
 * então este catálogo garante que elas existam em qualquer ambiente — sem
 * ele, o seletor de "ano do ensino básico" do cadastro de aluno ficaria vazio
 * fora do banco de exemplo (DataSeeder só roda com banco vazio).
 */
@Configuration
public class CatalogoTurmas {

    private static final List<String> ANOS_FUNDAMENTAL = List.of("6º Ano", "7º Ano", "8º Ano", "9º Ano");
    private static final List<String> ANOS_MEDIO = List.of("1º Ano EM", "2º Ano EM", "3º Ano EM");
    private static final List<String> LETRAS = List.of("A", "B", "C", "D", "E");

    @Bean
    @Order(3) // depois do DataSeeder — evita duplicar as turmas da história de exemplo
    CommandLineRunner semearTurmas(TurmaRepository turmas, JdbcTemplate jdbc) {
        return args -> {
            renomearTurmasAntigas(jdbc);

            Set<String> existentes = turmas.findAll().stream()
                    .map(Turma::getNome).collect(Collectors.toCollection(LinkedHashSet::new));

            ANOS_FUNDAMENTAL.forEach(ano -> criarFaltantes(turmas, existentes, ano, "Manhã"));
            ANOS_MEDIO.forEach(ano -> criarFaltantes(turmas, existentes, ano, "Tarde"));
        };
    }

    private void criarFaltantes(TurmaRepository turmas, Set<String> existentes, String ano, String turno) {
        for (String letra : LETRAS) {
            String nome = ano + " " + letra;
            if (existentes.contains(nome)) continue;
            Turma t = new Turma();
            t.setNome(nome);
            t.setAnoLetivo(2026);
            t.setTurno(turno);
            turmas.save(t);
        }
    }

    /** Bancos de antes desta mudança tinham "1º/2º Ano EM" sem letra de turma. */
    private void renomearTurmasAntigas(JdbcTemplate jdbc) {
        jdbc.update("update turmas set nome = '1º Ano EM A' where nome = '1º Ano EM'");
        jdbc.update("update turmas set nome = '2º Ano EM A' where nome = '2º Ano EM'");
    }
}
