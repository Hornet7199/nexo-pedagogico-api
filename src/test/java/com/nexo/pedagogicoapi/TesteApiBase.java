package com.nexo.pedagogicoapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sobe a aplicação com H2 em memória e o seed de exemplo (perfil "teste"). O login não
 * existe aqui — é do usuario-api —, então {@link #bearer(String)} emite o token direto,
 * para as contas do seed (diretor, professor, aluno, admin).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("teste")
public abstract class TesteApiBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Header Authorization de uma conta do seed. */
    protected String bearer(String login) {
        return "Bearer " + TokenDeTeste.gerar(login);
    }
}
