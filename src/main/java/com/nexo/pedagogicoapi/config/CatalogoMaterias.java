package com.nexo.pedagogicoapi.config;

import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.domain.SegmentoEnsino;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Matérias da escola. Diferente do DataSeeder (dados de exemplo, só em banco
 * vazio), este é dado de referência: precisa existir em qualquer ambiente,
 * senão o cadastro de professor não teria o que oferecer.
 */
@Configuration
public class CatalogoMaterias {

    /**
     * Currículo por etapa, no recorte usual da escola brasileira: Ciências é do
     * fundamental; Biologia, Física e Química entram no médio no lugar dela; o
     * restante atravessa as duas etapas.
     */
    static final Map<String, SegmentoEnsino> MATERIAS = new LinkedHashMap<>();
    static {
        MATERIAS.put("Artes", SegmentoEnsino.AMBOS);
        MATERIAS.put("Biologia", SegmentoEnsino.MEDIO);
        MATERIAS.put("Ciências", SegmentoEnsino.FUNDAMENTAL);
        MATERIAS.put("Educação Física", SegmentoEnsino.AMBOS);
        MATERIAS.put("Física", SegmentoEnsino.MEDIO);
        MATERIAS.put("Geografia", SegmentoEnsino.AMBOS);
        MATERIAS.put("História", SegmentoEnsino.AMBOS);
        MATERIAS.put("Inglês", SegmentoEnsino.AMBOS);
        MATERIAS.put("Matemática", SegmentoEnsino.AMBOS);
        MATERIAS.put("Português", SegmentoEnsino.AMBOS);
        MATERIAS.put("Química", SegmentoEnsino.MEDIO);
    }

    @Bean
    @Order(1) // antes do DataSeeder
    CommandLineRunner semearMaterias(MateriaRepository materias) {
        return args -> {
            Map<String, Materia> existentes = materias.findAll().stream()
                    .collect(Collectors.toMap(Materia::getNome, m -> m, (a, b) -> a));

            MATERIAS.forEach((nome, segmento) -> {
                Materia materia = existentes.get(nome);
                if (materia == null) {
                    materias.save(new Materia(nome, segmento));
                } else if (materia.getSegmento() != segmento) {
                    // Banco criado antes da coluna existir tem tudo como AMBOS; o
                    // catálogo é a referência e corrige no arranque.
                    materia.setSegmento(segmento);
                    materias.save(materia);
                }
            });
        };
    }
}
