package com.nexo.pedagogicoapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catálogo de conteúdo no painel do administrador. Contas e desafios não são mais daqui. */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:teste-admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class CatalogoAdminTest extends TesteApiBase {

    private JsonNode json(String rota, String bearer) throws Exception {
        return json.readTree(mvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("conteúdo despublicado some da tela do aluno e volta ao ser republicado")
    void publicarEDespublicarConteudo() throws Exception {
        String admin = bearer("admin");
        String aluno = bearer("aluno");

        long materiaId = materiaComConteudo(admin);
        JsonNode conteudos = json("/api/admin/catalogo/materias/" + materiaId + "/conteudos", admin);
        long conteudoId = conteudos.get(0).get("id").asLong();

        int antes = json("/api/materias/" + materiaId + "/conteudos", aluno).size();

        mvc.perform(patch("/api/admin/catalogo/conteudos/" + conteudoId + "/publicado")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":false}")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        assertThat(json("/api/materias/" + materiaId + "/conteudos", aluno).size())
                .as("o despublicado sai da lista do aluno").isEqualTo(antes - 1);

        // Mas continua no catálogo de quem administra — não foi apagado.
        assertThat(json("/api/admin/catalogo/materias/" + materiaId + "/conteudos", admin).size())
                .isEqualTo(conteudos.size());

        mvc.perform(patch("/api/admin/catalogo/conteudos/" + conteudoId + "/publicado")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":true}")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        assertThat(json("/api/materias/" + materiaId + "/conteudos", aluno).size()).isEqualTo(antes);
    }

    @Test
    @DisplayName("reordenar exige a lista completa da matéria e aplica a ordem enviada")
    void reordenarConteudos() throws Exception {
        String admin = bearer("admin");
        long materiaId = materiaComConteudo(admin);

        JsonNode atuais = json("/api/admin/catalogo/materias/" + materiaId + "/conteudos", admin);
        List<Long> ids = new ArrayList<>();
        atuais.forEach(c -> ids.add(c.get("id").asLong()));
        assertThat(ids.size()).as("a matéria de teste precisa de ao menos 2 conteúdos").isGreaterThan(1);

        // Lista parcial é recusada: deixaria os ausentes com a ordem antiga.
        mvc.perform(patch("/api/admin/catalogo/materias/" + materiaId + "/ordem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudoIds\":[" + ids.get(0) + "]}")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest());

        // Invertida, a ordem devolvida é a enviada.
        List<Long> invertida = new ArrayList<>(ids);
        java.util.Collections.reverse(invertida);
        JsonNode depois = json.readTree(mvc.perform(
                        patch("/api/admin/catalogo/materias/" + materiaId + "/ordem")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"conteudoIds\":" + invertida + "}")
                                .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        List<Long> resultado = new ArrayList<>();
        depois.forEach(c -> resultado.add(c.get("id").asLong()));
        assertThat(resultado).isEqualTo(invertida);
    }

    @Test
    @DisplayName("o painel conta o catálogo e separa o que está fora do ar")
    void dashboard() throws Exception {
        JsonNode d = json("/api/admin/dashboard", bearer("admin"));

        assertThat(d.get("turmas").asLong()).isGreaterThan(0);
        assertThat(d.get("materias").asLong()).isGreaterThan(0);
        assertThat(d.get("conteudos").asLong()).isGreaterThan(0);
        assertThat(d.has("conteudosDespublicados")).isTrue();
        // contas e desafios saíram: são do usuario-api e da gamificação
        for (String campo : new String[]{"contas", "alunos", "professores", "desafios"}) {
            assertThat(d.has(campo)).as("o dashboard não pode mais trazer " + campo).isFalse();
        }
    }

    private long materiaComConteudo(String admin) throws Exception {
        for (JsonNode m : json("/api/admin/catalogo", admin)) {
            if (m.get("conteudos").asInt() > 1) return m.get("id").asLong();
        }
        throw new AssertionError("o seed precisa de uma matéria com ao menos 2 conteúdos");
    }
}
