package com.nexo.pedagogicoapi.config;

import com.nexo.pedagogicoapi.domain.ConteudoMateria;
import com.nexo.pedagogicoapi.domain.Materia;
import com.nexo.pedagogicoapi.repository.ConteudoMateriaRepository;
import com.nexo.pedagogicoapi.repository.MateriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Map;

/**
 * Conteúdo de estudo das 10 matérias — 5 tópicos cada, nos mesmos nomes que o
 * catálogo do aluno já lista em materias.data.ts, para a tela não mudar de
 * assunto ao ganhar o texto.
 *
 * Cada tópico é escrito para ser lido de uma sentada: um resumo de uma frase, o
 * corpo em parágrafos curtos e um exemplo concreto no fim. É material de
 * referência semeado, não conteúdo criado por usuário — não há endpoint de
 * escrita em MateriasController —, então o seed reescreve o conjunto quando ele
 * muda, em vez de só preencher matéria vazia. Sem isso, uma matéria semeada numa
 * versão anterior ficaria presa no texto antigo para sempre.
 */
@Configuration
public class ConteudoMateriaSeed {

    /** Um tópico de estudo. `texto` usa linha em branco para separar parágrafos. */
    private record Topico(String titulo, int minutos, String resumo, String texto, String exemplo) {}

    @Bean
    @Order(4) // depois do CatalogoMaterias (garante que as matérias já existem)
    CommandLineRunner semearConteudoDasMaterias(MateriaRepository materias, ConteudoMateriaRepository conteudos) {
        return args -> {
            Map<String, List<Topico>> porMateria = catalogo();

            for (Materia materia : materias.findAllByOrderByNome()) {
                List<Topico> topicos = porMateria.get(materia.getNome());
                if (topicos == null) continue;

                List<ConteudoMateria> atuais = conteudos.findByMateriaIdOrderByOrdemAsc(materia.getId());
                if (jaEstaEmDia(atuais, topicos)) continue;

                conteudos.deleteAll(atuais);
                int ordem = 1;
                for (Topico t : topicos) {
                    ConteudoMateria c = new ConteudoMateria();
                    c.setMateria(materia);
                    c.setTitulo(t.titulo());
                    c.setResumo(t.resumo());
                    c.setTexto(t.texto());
                    c.setExemplo(t.exemplo());
                    c.setMinutos(t.minutos());
                    c.setOrdem(ordem++);
                    conteudos.save(c);
                }
            }
        };
    }

    /** Compara pelos títulos: se mudou a lista, a matéria é reescrita inteira. */
    private boolean jaEstaEmDia(List<ConteudoMateria> atuais, List<Topico> topicos) {
        if (atuais.size() != topicos.size()) return false;
        for (int i = 0; i < atuais.size(); i++) {
            if (!atuais.get(i).getTitulo().equals(topicos.get(i).titulo())) return false;
            // Conteúdo semeado antes dos campos novos existirem fica sem resumo.
            if (atuais.get(i).getResumo() == null || atuais.get(i).getResumo().isBlank()) return false;
        }
        return true;
    }

    private Map<String, List<Topico>> catalogo() {
        return Map.of(
                "Biologia", biologia(),
                "Matemática", matematica(),
                "Inglês", ingles(),
                "Português", portugues(),
                "História", historia(),
                "Geografia", geografia(),
                "Física", fisica(),
                "Química", quimica(),
                "Artes", artes(),
                "Educação Física", educacaoFisica()
        );
    }

    // ── Biologia ─────────────────────────────────────────────────────────────

    private List<Topico> biologia() {
        return List.of(
                new Topico("Células e tecidos", 3,
                        "Toda vida começa na célula — e células parecidas se juntam para formar tecidos.",
                        """
                        A célula é a menor parte viva do seu corpo. Ela nasce, se alimenta, trabalha e morre. \
                        Tudo o que você é começa aí.

                        Dentro dela, cada peça tem um cargo. O núcleo guarda o DNA, que é o manual de \
                        instruções. A mitocôndria queima açúcar e libera energia — é a usina. A membrana \
                        decide o que entra e o que sai, como um porteiro.

                        Células iguais trabalhando juntas formam um tecido. Tecidos formam órgãos, órgãos \
                        formam sistemas, e os sistemas formam você. É sempre o pequeno montando o grande.""",
                        "Pense num prédio: a célula é o tijolo, o tecido é a parede, o órgão é o cômodo e o "
                                + "sistema é o andar inteiro. Ninguém constrói um andar sem tijolo."),

                new Topico("Corpo humano", 4,
                        "Seus órgãos não trabalham sozinhos: eles se organizam em sistemas que se ajudam.",
                        """
                        O corpo humano é um time. Cada sistema tem uma função, mas nenhum ganha o jogo sozinho.

                        O sistema digestório quebra a comida em pedaços pequenos o bastante para entrar no \
                        sangue. O respiratório traz oxigênio e leva embora o gás carbônico. O circulatório \
                        é a entrega: o coração bombeia e o sangue distribui comida e oxigênio para todas as \
                        células. O nervoso comanda tudo, e é rápido — pensa em milésimos de segundo.

                        Repare como eles dependem uns dos outros: sem o digestório não há nutriente, sem o \
                        respiratório não há oxigênio, e sem o circulatório os dois primeiros não entregam \
                        nada a lugar nenhum.""",
                        "Quando você corre, três sistemas reagem juntos: o respiratório acelera (você "
                                + "ofega), o circulatório acelera (o coração dispara) e o nervoso coordena os "
                                + "dois. Por isso o cansaço vem do corpo inteiro, não só da perna."),

                new Topico("Ecossistemas", 3,
                        "Um ecossistema é o conjunto dos seres vivos de um lugar mais o ambiente em que vivem.",
                        """
                        Ecossistema é tudo que existe num lugar: os seres vivos e também o que não é vivo — \
                        água, luz, solo, temperatura.

                        A energia entra sempre pelo sol. As plantas capturam essa energia e viram alimento \
                        (são os produtores). Quem come planta é consumidor, e quem come o consumidor também. \
                        No fim da fila estão os decompositores — fungos e bactérias — que devolvem os \
                        nutrientes para o solo e recomeçam o ciclo.

                        Essa fila é a cadeia alimentar. Quando um elo some, os outros sentem: é por isso que \
                        a extinção de uma única espécie pode desequilibrar um ecossistema inteiro.""",
                        "Capim → gafanhoto → sapo → cobra → gavião. Se o sapo desaparece, o gafanhoto se "
                                + "multiplica e come o capim todo, e a cobra fica sem comida. Um elo derruba a fila."),

                new Topico("Genética básica", 4,
                        "Genética explica por que você parece com sua família — e por que não é igual a ninguém.",
                        """
                        O DNA é a receita do seu corpo, escrita com quatro letras químicas: A, T, C e G. Um \
                        trecho dessa receita que dá uma instrução específica é um gene.

                        Você recebe dois genes para cada característica: um da mãe, um do pai. Quando eles \
                        são diferentes, um costuma se impor — é o dominante — e o outro fica escondido, o \
                        recessivo. O escondido não some: ele continua ali e pode aparecer nos seus filhos.

                        Por isso duas pessoas de olhos castanhos podem ter um filho de olhos claros. O gene \
                        claro estava guardado nos dois, esperando encontrar um par.""",
                        "Use letras: B (castanho, dominante) e b (claro, recessivo). Pai Bb e mãe Bb são "
                                + "castanhos, mas há 1 chance em 4 de o filho ser bb — e nascer de olhos claros."),

                new Topico("Reinos dos seres vivos", 3,
                        "Cinco grandes grupos organizam toda a diversidade da vida na Terra.",
                        """
                        Para estudar milhões de espécies, os cientistas as separam em reinos, agrupando quem \
                        se parece no funcionamento.

                        Monera são seres de uma célula só e sem núcleo definido — as bactérias. Protista \
                        também são simples, mas já têm núcleo, como as amebas. Fungi são os fungos, que não \
                        fazem fotossíntese e absorvem alimento pronto. Plantae faz o próprio alimento com \
                        luz. Animalia se move e come outros seres.

                        A pergunta que separa quase tudo é simples: como esse ser consegue energia? Fazendo \
                        a própria comida, absorvendo ou comendo.""",
                        "Cogumelo parece planta, mas é fungo — não tem clorofila e não faz fotossíntese. "
                                + "Ele absorve o alimento já pronto da matéria em decomposição.")
        );
    }

    // ── Matemática ───────────────────────────────────────────────────────────

    private List<Topico> matematica() {
        return List.of(
                new Topico("Funções", 3,
                        "Função é uma máquina: entra um número, sai outro, sempre pela mesma regra.",
                        """
                        Uma função recebe uma entrada (x), aplica uma regra e devolve uma saída (y). A regra \
                        nunca muda — a mesma entrada dá sempre a mesma saída.

                        A mais comum é a do 1º grau: f(x) = ax + b. O 'a' é a inclinação, e ele conta a \
                        história do gráfico: se for positivo, a reta sobe (função crescente); se for \
                        negativo, desce. O 'b' é onde a reta corta o eixo y, ou seja, o valor de partida \
                        quando x = 0.

                        A raiz é o x que zera a função. Para achá-la, resolva ax + b = 0, que dá x = -b/a.""",
                        "Um táxi cobra R$ 5 fixos mais R$ 2 por km: f(x) = 2x + 5. O 5 é a bandeirada (o "
                                + "'b', valor de partida) e o 2 é o preço por km (o 'a', a inclinação). Para 10 km: "
                                + "f(10) = 2·10 + 5 = R$ 25."),

                new Topico("Geometria plana e espacial", 4,
                        "Plana mede o que cabe numa folha; espacial mede o que ocupa lugar no espaço.",
                        """
                        Na geometria plana, tudo tem duas dimensões e a pergunta é área. Retângulo é base × \
                        altura. Triângulo é a metade disso: (base × altura) / 2. Círculo é πr².

                        Na espacial entra a terceira dimensão e a pergunta vira volume. E há um atalho que \
                        resolve muita coisa: em prismas e cilindros, volume = área da base × altura. Ou seja, \
                        você calcula a área plana da base e depois "empilha" essa base pela altura.

                        Cone e pirâmide são exatamente um terço do prisma de mesma base e mesma altura.""",
                        "Uma lata de refrigerante é um cilindro de raio 3 cm e altura 10 cm. Área da base = "
                                + "π·3² ≈ 28,3 cm². Volume = 28,3 × 10 ≈ 283 cm³ — ou seja, uns 283 mL."),

                new Topico("Trigonometria", 4,
                        "Três razões ligam os ângulos aos lados de um triângulo retângulo.",
                        """
                        Num triângulo retângulo, a hipotenusa é o lado oposto ao ângulo de 90° e o maior de \
                        todos. Os outros dois são os catetos.

                        Escolhido um ângulo, o cateto que está na frente dele é o oposto e o que está \
                        encostado é o adjacente. Daí saem as três razões: seno = oposto / hipotenusa, \
                        cosseno = adjacente / hipotenusa e tangente = oposto / adjacente.

                        E vale sempre o Teorema de Pitágoras: a² = b² + c², onde 'a' é a hipotenusa. É ele \
                        que permite achar o terceiro lado quando você conhece dois.""",
                        "Macete para lembrar a ordem: SOH-CAH-TOA. Seno = Oposto/Hipotenusa, Cosseno = "
                                + "Adjacente/Hipotenusa, Tangente = Oposto/Adjacente."),

                new Topico("Estatística", 3,
                        "Média, mediana e moda resumem um monte de números em um só — mas contam histórias diferentes.",
                        """
                        A média soma tudo e divide pela quantidade. A mediana é o valor do meio quando você \
                        põe os números em ordem. A moda é o que mais se repete.

                        Elas não são intercambiáveis. A média é sensível a extremos: um único valor muito \
                        alto puxa a média para cima e engana. A mediana ignora esse exagero, porque só olha \
                        para a posição do meio.

                        Por isso, quando os dados têm valores muito fora do padrão, a mediana costuma \
                        descrever melhor a realidade do grupo.""",
                        "Salários de 5 pessoas: 2, 2, 3, 3 e 40 mil. A média dá 10 mil — e ninguém ganha "
                                + "isso. A mediana dá 3 mil, que retrata o grupo de verdade."),

                new Topico("Probabilidade", 3,
                        "Probabilidade é contar quantos resultados te servem entre todos os possíveis.",
                        """
                        A conta é uma divisão: casos favoráveis dividido por casos possíveis. O resultado \
                        fica sempre entre 0 (impossível) e 1 (certeza), e pode virar porcentagem \
                        multiplicando por 100.

                        Para dois eventos independentes acontecerem juntos, multiplique as probabilidades. \
                        Para um OU outro acontecer, em eventos que não podem ocorrer ao mesmo tempo, some.

                        O erro mais comum é achar que o passado influencia o próximo sorteio. A moeda não \
                        tem memória: depois de cinco caras seguidas, a chance da próxima ainda é 1/2.""",
                        "Num dado de 6 faces, tirar par: casos favoráveis são 2, 4 e 6 (três), casos "
                                + "possíveis são seis. P = 3/6 = 0,5 = 50%.")
        );
    }

    // ── Inglês ───────────────────────────────────────────────────────────────

    private List<Topico> ingles() {
        return List.of(
                new Topico("Verb tenses", 4,
                        "Três tempos resolvem a maior parte das conversas: presente, passado e futuro.",
                        """
                        O Present Simple fala de rotina e de fatos: "I study every day". Lembre do -s na \
                        terceira pessoa: he studies, she works, it rains.

                        O Past Simple fala do que acabou. Verbos regulares ganham -ed (worked, played); os \
                        irregulares você precisa memorizar, e são justamente os mais usados (go/went, \
                        have/had, see/saw).

                        O futuro tem dois jeitos com sentidos diferentes: "will" para decisão na hora \
                        ("I'll help you") e "going to" para plano que já existia ("I'm going to travel").""",
                        "Compare: \"I'll answer the phone\" (decidi agora, ele está tocando) e \"I'm going "
                                + "to answer the emails\" (já era o meu plano). O tempo verbal muda a intenção."),

                new Topico("Vocabulary building", 3,
                        "Aprender palavra solta é lento; aprender em bloco e por pedaço é rápido.",
                        """
                        Palavras andam acompanhadas. Em vez de decorar "make", guarde os blocos prontos: \
                        make a decision, make a mistake, make friends. Você aprende a palavra e o uso dela \
                        de uma vez.

                        Outro atalho é reconhecer pedaços. Prefixos mudam o sentido: un- e in- negam \
                        (happy → unhappy). Sufixos mudam a classe: -tion vira substantivo (inform → \
                        information), -ly vira advérbio (quick → quickly).

                        Com esses pedaços você deduz o significado de palavras que nunca viu — e é assim \
                        que o vocabulário cresce sozinho.""",
                        "Você não conhece \"unpredictable\"? Quebre: un (não) + predict (prever) + able "
                                + "(capaz de). Ou seja: que não dá para prever, imprevisível."),

                new Topico("Reading comprehension", 3,
                        "Você não precisa entender toda palavra para entender o texto.",
                        """
                        Comece pelo mapa: título, subtítulos e imagens já dizem do que se trata. Isso é \
                        skimming — passar o olho para captar a ideia geral.

                        Quando quiser um dado específico (uma data, um nome, um número), faça scanning: \
                        varra o texto procurando só aquilo, sem ler o resto.

                        Diante de uma palavra desconhecida, não pare. Leia a frase inteira e a seguinte — o \
                        contexto quase sempre entrega o sentido. Parar em cada palavra nova é o que faz a \
                        leitura travar.""",
                        "\"The drought lasted months and the crops died from lack of water.\" Não sabe "
                                + "\"drought\"? O resto da frase entrega: falta de água. É seca."),

                new Topico("Conversation", 3,
                        "Conversar é manter a bola rolando, não falar sem errar.",
                        """
                        Toda conversa tem uma estrutura previsível: abertura, assunto e encerramento. \
                        Dominar as frases de abertura já resolve o começo, que costuma ser a parte mais \
                        travada.

                        Tenha à mão frases de socorro: "Could you repeat that, please?", "How do you say \
                        ... in English?", "I'm not sure I understand." Elas mantêm a conversa viva quando \
                        você se perde — e usá-las é sinal de fluência, não de fraqueza.

                        Devolva sempre a pergunta. "And you?" faz o outro falar e te dá tempo para pensar.""",
                        "Travou no meio da frase? Ganhe tempo como um nativo: \"Well...\", \"Let me "
                                + "think...\", \"You know...\". Silêncio incomoda; essas palavras não."),

                new Topico("Writing essays", 4,
                        "Um bom texto em inglês tem começo, meio e fim — e avisa o leitor onde ele está.",
                        """
                        A estrutura clássica é de cinco parágrafos: introdução, três de desenvolvimento e \
                        conclusão. A introdução termina com a thesis statement, a frase que anuncia a sua \
                        posição.

                        Cada parágrafo do meio defende um argumento só, e começa pela topic sentence — a \
                        frase que avisa do que aquele parágrafo trata. O resto do parágrafo sustenta essa \
                        frase.

                        Os conectivos são as placas de trânsito do texto: however (mas), therefore \
                        (portanto), for example (por exemplo), in addition (além disso). Sem eles, o leitor \
                        se perde mesmo com boas ideias.""",
                        "Thesis statement fraca: \"I will talk about social media.\" Forte: \"Social media "
                                + "harms teenagers' sleep and should be limited at night.\" A segunda diz o que "
                                + "você vai defender.")
        );
    }

    // ── Português ────────────────────────────────────────────────────────────

    private List<Topico> portugues() {
        return List.of(
                new Topico("Interpretação de texto", 3,
                        "Interpretar é responder com o que está no texto — não com o que você acha.",
                        """
                        Todo texto tem uma ideia central e ideias que a sustentam. Achar a central é o \
                        primeiro passo: normalmente ela aparece no começo ou no fim de cada parágrafo.

                        Separe três coisas que costumam se misturar. O que o texto diz com todas as letras \
                        é explícito. O que ele deixa entender sem dizer é implícito. E o que você conclui \
                        sozinho, sem apoio no texto, é opinião sua — e essa não vale na prova.

                        A regra de ouro: se você não consegue apontar o trecho que sustenta a resposta, ela \
                        provavelmente está errada.""",
                        "\"João olhou a conta, checou a carteira e pediu só um copo d'água.\" O texto não "
                                + "diz que ele está sem dinheiro — mas dá para concluir. Isso é implícito, e vale."),

                new Topico("Gramática", 4,
                        "Sujeito e predicado: descobrir quem faz o quê resolve a maior parte das dúvidas.",
                        """
                        O sujeito é quem pratica ou sofre a ação; o predicado é o que se diz dele. Para \
                        achar o sujeito, pergunte ao verbo: quem? o quê?

                        Daí sai a concordância, que é onde mais se erra: o verbo acompanha o sujeito. Se o \
                        sujeito é plural, o verbo é plural. A pegadinha clássica é uma expressão longa entre \
                        o sujeito e o verbo, que faz você concordar com a palavra errada.

                        Vale sempre isolar o núcleo do sujeito — a palavra principal — e ignorar o resto na \
                        hora de decidir o verbo.""",
                        "\"A caixa de bombons estava aberta.\" O núcleo é \"caixa\" (singular), não "
                                + "\"bombons\". Por isso é \"estava\", e não \"estavam\"."),

                new Topico("Ortografia", 3,
                        "A maioria dos erros de escrita se concentra em poucos pares de palavras.",
                        """
                        Mas indica oposição (queria ir, mas choveu). Mais indica quantidade (quero mais \
                        arroz). Trocar um pelo outro é o erro mais comum da língua.

                        Há indica tempo passado ou o verbo haver (há dois anos, há pessoas esperando). A \
                        indica distância ou tempo futuro (daqui a dois anos).

                        Mal é o oposto de bem; mau é o oposto de bom. Esse é fácil de testar: troque na \
                        frase pelo oposto e veja qual faz sentido.""",
                        "Teste do oposto: \"Ele dormiu mal\" → troque por \"bem\": funciona, então é mal. "
                                + "\"Ele é um mau aluno\" → troque por \"bom\": funciona, então é mau."),

                new Topico("Produção de redação", 4,
                        "Uma redação nota alta tem tese clara, argumentos com repertório e proposta concreta.",
                        """
                        A introdução apresenta o tema e termina com a sua tese — o que você vai defender. \
                        Sem tese, o texto vira um amontoado de informação.

                        Cada parágrafo de desenvolvimento sustenta um argumento com repertório: um dado, um \
                        fato histórico, uma lei, uma obra. Argumento sem repertório é achismo; repertório \
                        sem argumento é enfeite.

                        A conclusão retoma a tese e apresenta a proposta de intervenção. Ela precisa dizer \
                        quem faz, o que faz, como faz e para quê — proposta vaga custa pontos.""",
                        "Proposta fraca: \"O governo deve investir em educação.\" Forte: \"O Ministério da "
                                + "Educação (quem) deve criar oficinas de leitura (o quê) por meio de parcerias "
                                + "com bibliotecas públicas (como), a fim de ampliar o acesso ao livro (para quê).\""),

                new Topico("Literatura", 4,
                        "Cada escola literária é uma reação à anterior — e isso é o fio da história.",
                        """
                        As escolas não surgem soltas: cada uma responde à que veio antes, geralmente \
                        negando o que ela valorizava.

                        O Romantismo idealizou o amor, a mulher e a pátria, com muita emoção. O Realismo \
                        veio como resposta direta: trocou a idealização pela crítica social e pela análise \
                        fria do comportamento humano. O Modernismo rompeu com a forma engessada e trouxe a \
                        linguagem do dia a dia, o verso livre e o Brasil real.

                        Se você guarda a reação, guarda o movimento — é mais fácil que decorar datas.""",
                        "Compare a mulher em cada escola: no Romantismo, a Iracema idealizada de Alencar; "
                                + "no Realismo, a Capitu ambígua e humana de Machado de Assis. A virada está aí.")
        );
    }

    // ── História ─────────────────────────────────────────────────────────────

    private List<Topico> historia() {
        return List.of(
                new Topico("História Antiga", 3,
                        "A escrita e a agricultura criaram as primeiras cidades — e com elas o Estado.",
                        """
                        Enquanto o ser humano caçava e coletava, vivia em grupos pequenos e andando. Ao \
                        aprender a plantar, ele parou num lugar — e tudo mudou.

                        Comida estocada gerou excedente. Excedente gerou gente que não precisava plantar: \
                        sacerdotes, soldados, artesãos. Para controlar o estoque, inventaram a escrita. Para \
                        organizar o povo, inventaram leis e governo.

                        Egito e Mesopotâmia nasceram assim, à beira de grandes rios. Grécia e Roma vieram \
                        depois e nos deixaram a política, a filosofia e o direito.""",
                        "O Código de Hamurabi (Babilônia, ~1750 a.C.) é a lei escrita mais famosa da "
                                + "Antiguidade. Sua lógica — \"olho por olho\" — mostra o Estado assumindo a "
                                + "punição, que antes era vingança particular."),

                new Topico("Idade Média", 4,
                        "Sem um Estado forte, terra virou poder — e nasceu o feudalismo.",
                        """
                        Com a queda do Império Romano do Ocidente (476), o comércio encolheu, as cidades \
                        esvaziaram e a segurança sumiu. As pessoas se refugiaram no campo.

                        Nasceu o feudalismo: o senhor feudal cedia terra e proteção, e o servo pagava com \
                        trabalho e parte da colheita. Não era escravidão — o servo não era vendido —, mas \
                        também não era liberdade: ele estava preso à terra.

                        A Igreja era a única instituição presente em toda a Europa, e por isso concentrava \
                        também poder político e econômico, não só religioso.""",
                        "A sociedade se explicava em três ordens: quem reza (clero), quem luta (nobreza) e "
                                + "quem trabalha (servos). Cada um nascia na sua e ali ficava."),

                new Topico("Brasil Colônia", 4,
                        "Por três séculos, o Brasil existiu para dar lucro a Portugal.",
                        """
                        A lógica da colonização foi o pacto colonial: a colônia só podia comerciar com a \
                        metrópole, vendendo matéria-prima barata e comprando manufatura cara.

                        A economia girou em ciclos. O pau-brasil veio primeiro, por escambo com os \
                        indígenas. Depois a cana-de-açúcar organizou o Nordeste em grandes engenhos. No \
                        século XVIII, o ouro deslocou o eixo para Minas Gerais — e a capital do país para o \
                        Rio de Janeiro.

                        Todos esses ciclos foram sustentados pelo trabalho escravizado, primeiro indígena e \
                        depois africano. É o fio que atravessa os três séculos.""",
                        "A Inconfidência Mineira (1789) nasce do peso do ouro: a derrama cobrava o "
                                + "imposto atrasado de uma vez. Quando a cobrança aperta, a revolta aparece."),

                new Topico("Revolução Industrial", 4,
                        "A máquina mudou o que se produzia, como se produzia e onde as pessoas viviam.",
                        """
                        Começou na Inglaterra do século XVIII, que tinha o que era preciso: capital do \
                        comércio, carvão, ferro e gente disponível para trabalhar.

                        A máquina a vapor substituiu a força humana e animal. A produção saiu da oficina \
                        artesanal, onde um mestre fazia a peça inteira, e foi para a fábrica, onde cada um \
                        faz uma etapa. Muito mais rápido — e muito mais repetitivo.

                        As consequências sociais foram duras: jornadas de 14 horas, trabalho infantil, \
                        cidades inchadas e sem saneamento. Foi como reação a isso que nasceram os \
                        sindicatos e as leis trabalhistas.""",
                        "O ludismo — operários quebrando máquinas no início do século XIX — não era "
                                + "birra: a máquina realmente tomava o emprego de quem fazia o trabalho à mão."),

                new Topico("Guerras Mundiais", 4,
                        "Duas guerras em trinta anos redesenharam o mapa e a política do mundo.",
                        """
                        A Primeira (1914-1918) foi o estouro de uma panela de pressão: disputa por \
                        colônias, corrida armamentista e alianças que se puxavam. O atentado em Sarajevo foi \
                        o estopim, não a causa.

                        O Tratado de Versalhes puniu duramente a Alemanha derrotada — humilhação e crise \
                        que abriram caminho para o nazismo. A Segunda (1939-1945) nasce em boa parte do fim \
                        malfeito da Primeira.

                        Do saldo da Segunda vieram a ONU, a Declaração Universal dos Direitos Humanos e a \
                        divisão do mundo entre EUA e URSS, que virou a Guerra Fria.""",
                        "Guarde a ligação: a paz mal resolvida de 1919 é a semente de 1939. Por isso, em "
                                + "1945, os vencedores optaram por reconstruir a Alemanha em vez de puni-la.")
        );
    }

    // ── Geografia ────────────────────────────────────────────────────────────

    private List<Topico> geografia() {
        return List.of(
                new Topico("Cartografia", 3,
                        "Todo mapa mente um pouco — a graça é saber em quê.",
                        """
                        A Terra é quase uma esfera, e o mapa é plano. Não existe jeito de achatar uma \
                        esfera sem distorcer alguma coisa: ou a área, ou a forma, ou a distância.

                        A projeção de Mercator preserva os ângulos, o que é ótimo para navegar, mas incha \
                        as regiões perto dos polos. A de Peters preserva as áreas e deixa os continentes com \
                        formato esticado.

                        A escala diz quanto o mapa encolheu a realidade. Em 1:100.000, cada centímetro no \
                        papel vale 100.000 cm — ou seja, 1 km no chão.""",
                        "Na Mercator, a Groenlândia parece do tamanho da África. Na realidade, a África é "
                                + "cerca de 14 vezes maior. O mapa não errou: ele escolheu o que preservar."),

                new Topico("Relevo e clima", 4,
                        "Relevo é a forma do terreno; clima é o comportamento do tempo ao longo dos anos.",
                        """
                        O relevo se forma numa queda de braço. As forças internas (tectonismo, vulcanismo) \
                        constroem, levantando montanhas. As externas (chuva, vento, rios) desgastam, \
                        desmanchando o que foi levantado.

                        Não confunda tempo com clima. Tempo é como está hoje; clima é o padrão de muitos \
                        anos. Um dia frio no verão não desmente o clima tropical.

                        Três fatores explicam quase todo clima: latitude (perto da linha do Equador é mais \
                        quente), altitude (quanto mais alto, mais frio) e proximidade do mar, que suaviza os \
                        extremos.""",
                        "Quito, no Equador, fica na linha do Equador e mesmo assim é fria o ano todo: está "
                                + "a 2.850 m de altitude. A altitude venceu a latitude."),

                new Topico("Geopolítica", 3,
                        "Geopolítica é a disputa por poder no espaço: território, recursos e influência.",
                        """
                        Depois de 1945, o mundo se dividiu em dois blocos: EUA e capitalismo de um lado, \
                        URSS e socialismo do outro. Foi a Guerra Fria — disputa sem confronto direto entre \
                        os dois, mas com guerras em terceiros países.

                        Com o fim da URSS em 1991, os EUA ficaram sozinhos no topo. Mas a ascensão da \
                        China, da Índia e de outros centros tornou o mundo multipolar: o poder passou a ter \
                        vários endereços.

                        Hoje a disputa é menos por território e mais por tecnologia, energia e rotas de \
                        comércio.""",
                        "Os chips de computador viraram questão de Estado. Quem os fabrica tem poder — por "
                                + "isso Taiwan, uma ilha pequena, está no centro da tensão global."),

                new Topico("Urbanização", 3,
                        "O mundo saiu do campo e foi para a cidade em poucas décadas — rápido demais.",
                        """
                        Urbanização é o crescimento da população urbana em relação à rural. Na Europa foi \
                        gradual, acompanhando a industrialização. No Brasil, foi acelerada: em 1940, 31% da \
                        população vivia em cidades; hoje passa de 85%.

                        Quando a cidade cresce mais rápido do que a infraestrutura, aparecem os problemas \
                        que conhecemos: moradia precária, trânsito, falta de saneamento e ocupação de áreas \
                        de risco.

                        Metrópoles que se juntam a cidades vizinhas formam regiões metropolitanas, onde \
                        milhões cruzam fronteiras municipais todo dia para trabalhar.""",
                        "O movimento pendular: a pessoa mora em Guarulhos e trabalha em São Paulo. Ela usa "
                                + "a estrutura de duas cidades, mas só paga imposto em uma."),

                new Topico("Meio ambiente", 4,
                        "Os problemas ambientais mais graves são globais — não respeitam fronteira.",
                        """
                        O efeito estufa é natural e necessário: sem ele, a Terra seria congelada. O problema \
                        é a intensificação, causada pela queima de combustíveis fósseis e pelo \
                        desmatamento, que aumenta a temperatura média do planeta.

                        O desmatamento cobra duas vezes: libera o carbono estocado nas árvores e elimina a \
                        floresta que absorveria o carbono futuro.

                        Desenvolvimento sustentável é a ideia de atender às necessidades de hoje sem \
                        comprometer as das próximas gerações. Não é parar de produzir — é produzir sem \
                        destruir a base.""",
                        "A Amazônia funciona como um rio voador: a floresta evapora água que vira chuva no "
                                + "Sudeste. Desmatar lá seca a torneira aqui.")
        );
    }

    // ── Física ───────────────────────────────────────────────────────────────

    private List<Topico> fisica() {
        return List.of(
                new Topico("Cinemática", 3,
                        "Cinemática descreve o movimento sem perguntar o que o causou.",
                        """
                        Velocidade é a variação da posição pelo tempo: v = Δs / Δt. Aceleração é a variação \
                        da velocidade pelo tempo: a = Δv / Δt.

                        No movimento uniforme (MU), a velocidade é constante e a posição avança em ritmo \
                        fixo: s = s₀ + v·t. No uniformemente variado (MUV), a aceleração é constante e \
                        entra o termo do tempo ao quadrado: s = s₀ + v₀t + at²/2.

                        Cuidado com um detalhe: velocidade média não é a média das velocidades. É a \
                        distância total dividida pelo tempo total.""",
                        "Queda livre é MUV com a ≈ 10 m/s². Depois de 3 segundos caindo, a velocidade é "
                                + "v = 10 × 3 = 30 m/s — mais de 100 km/h."),

                new Topico("Leis de Newton", 4,
                        "Três leis explicam por que as coisas se movem, param ou empurram de volta.",
                        """
                        A primeira é a inércia: um corpo mantém o que está fazendo — parado ou em movimento \
                        retilíneo uniforme — até que uma força mude isso.

                        A segunda dá a conta: F = m·a. A força é o que produz aceleração, e quanto maior a \
                        massa, mais força é preciso para o mesmo efeito.

                        A terceira é a ação e reação: toda força vem em par, mesma intensidade e sentidos \
                        opostos. O detalhe que confunde é que as duas forças agem em corpos diferentes — \
                        por isso não se anulam.""",
                        "Você empurra a parede e ela te empurra de volta com a mesma força. Se as duas "
                                + "agissem em você, nada faria sentido — mas uma age na parede e a outra, em você."),

                new Topico("Energia e trabalho", 3,
                        "Energia não some nem aparece do nada: ela só muda de forma.",
                        """
                        Trabalho é força aplicada ao longo de um deslocamento: T = F·d. Se não há \
                        deslocamento, não há trabalho — segurar um peso parado, por mais cansativo, é \
                        trabalho zero na física.

                        A energia cinética é a do movimento (Ec = mv²/2) e a potencial gravitacional é a da \
                        altura (Ep = mgh). Repare que a cinética depende do quadrado da velocidade: dobrar a \
                        velocidade quadruplica a energia.

                        E vale a conservação: em um sistema sem atrito, a soma das energias permanece a \
                        mesma. O que se perde de altura, ganha-se de velocidade.""",
                        "Por isso a velocidade é tão perigosa no trânsito: a 80 km/h um carro tem quatro "
                                + "vezes a energia que tem a 40 km/h — não o dobro."),

                new Topico("Eletricidade", 4,
                        "Tensão empurra, corrente flui e resistência atrapalha — a Lei de Ohm liga as três.",
                        """
                        Corrente (I) é o fluxo de elétrons, medido em ampères. Tensão (U) é a força que os \
                        empurra, medida em volts. Resistência (R) é a dificuldade que o material oferece, \
                        medida em ohms.

                        A Lei de Ohm amarra tudo: U = R·I. Sabendo duas grandezas, você acha a terceira.

                        Em série, os componentes dividem a mesma corrente e a falha de um interrompe todos. \
                        Em paralelo, cada um recebe a tensão total e funciona de forma independente — é por \
                        isso que a sua casa é ligada em paralelo.""",
                        "Analogia da água: a tensão é a pressão, a corrente é a vazão e a resistência é o "
                                + "diâmetro do cano. Cano fino (muita resistência) reduz a vazão."),

                new Topico("Óptica", 3,
                        "A luz anda em linha reta — até encontrar algo que a faça mudar de rumo.",
                        """
                        Na reflexão, a luz bate e volta, e o ângulo de saída é igual ao de entrada. É o que \
                        acontece no espelho.

                        Na refração, a luz muda de meio (do ar para a água, por exemplo) e muda de \
                        velocidade — por isso entorta. É por essa razão que um canudo dentro do copo parece \
                        quebrado.

                        Lentes usam a refração de propósito. A convergente junta os raios num ponto e \
                        corrige a hipermetropia; a divergente espalha os raios e corrige a miopia.""",
                        "O arco-íris é refração em ação: cada cor da luz branca entorta um pouco "
                                + "diferente ao atravessar a gota de água, e elas se separam no céu.")
        );
    }

    // ── Química ──────────────────────────────────────────────────────────────

    private List<Topico> quimica() {
        return List.of(
                new Topico("Tabela periódica", 3,
                        "A tabela não é lista para decorar: a posição de cada elemento já conta o que ele faz.",
                        """
                        Os elementos estão em ordem de número atômico — a quantidade de prótons. Isso é o \
                        que define o elemento: mudou o número de prótons, mudou de elemento.

                        As colunas (grupos) reúnem quem tem o mesmo número de elétrons na última camada, e \
                        é por isso que reagem de modo parecido. As linhas (períodos) indicam quantas camadas \
                        o átomo tem.

                        Sabendo a coluna, você já prevê o comportamento: o grupo 1 reage violentamente com \
                        água, o 17 forma sais e o 18 (gases nobres) quase não reage, porque já está \
                        completo.""",
                        "Sódio e potássio estão na mesma coluna e ambos explodem em contato com a água. "
                                + "Mesma coluna, mesmo comportamento — a posição entregou a informação."),

                new Topico("Ligações químicas", 4,
                        "Átomos se ligam por um motivo só: ficar estáveis com a última camada completa.",
                        """
                        Quase todo átomo busca oito elétrons na camada externa — é a regra do octeto. Para \
                        isso, ele doa, recebe ou compartilha elétrons.

                        Na ligação iônica, um doa e o outro recebe: os dois ficam com carga elétrica e se \
                        atraem. Costuma ocorrer entre metal e não metal, e forma sólidos duros que conduzem \
                        eletricidade quando dissolvidos.

                        Na covalente, dois não metais compartilham o par de elétrons, porque nenhum quer \
                        doar. Já a metálica é um caso à parte: os elétrons ficam livres circulando entre os \
                        átomos — daí os metais conduzirem tão bem.""",
                        "No sal de cozinha (NaCl), o sódio doa um elétron e o cloro recebe. Na água (H₂O), "
                                + "os átomos compartilham. Doar/receber é iônica; dividir é covalente."),

                new Topico("Reações químicas", 3,
                        "Numa reação, os átomos se reorganizam — mas nenhum é criado ou destruído.",
                        """
                        Reagentes entram, produtos saem, e a quantidade de cada tipo de átomo tem de ser a \
                        mesma dos dois lados. É a Lei de Lavoisier: na natureza, nada se cria, nada se \
                        perde, tudo se transforma.

                        Balancear é ajustar os coeficientes até essa contagem fechar. Note que você mexe nos \
                        coeficientes, nunca nos índices — mudar o índice muda a substância.

                        Alguns sinais denunciam que houve reação: mudança de cor, liberação de gás, formação \
                        de sólido ou variação de temperatura.""",
                        "H₂ + O₂ → H₂O não fecha: há 2 oxigênios à esquerda e 1 à direita. Balanceado: "
                                + "2H₂ + O₂ → 2H₂O. Agora são 4 H e 2 O dos dois lados."),

                new Topico("Estequiometria", 4,
                        "É a receita de bolo da química: proporção entre o que entra e o que sai.",
                        """
                        O mol é a unidade de contagem do químico, como a dúzia é a do feirante. Um mol \
                        equivale a 6,02 × 10²³ partículas — o número de Avogadro.

                        A massa molar diz quantos gramas há em um mol daquela substância, e é o que liga o \
                        mundo dos átomos (invisível) ao da balança (mensurável).

                        Os coeficientes da equação balanceada dão a proporção. A partir deles você monta uma \
                        regra de três e descobre quanto de produto sai de uma dada quantidade de reagente.""",
                        "Em 2H₂ + O₂ → 2H₂O a proporção é 2:1:2. Com 4 mols de H₂ você precisa de 2 mols "
                                + "de O₂ e obtém 4 mols de água. É regra de três, como na cozinha."),

                new Topico("Química orgânica", 4,
                        "É a química do carbono — o átomo que monta cadeias e forma quase tudo que é vivo.",
                        """
                        O carbono tem quatro ligações disponíveis e consegue se ligar a outros carbonos. \
                        Por isso forma cadeias longas, ramificadas e em anel: é a base de milhões de \
                        compostos.

                        O nome do composto é montado por peças. O prefixo diz o número de carbonos (met- 1, \
                        et- 2, prop- 3, but- 4), o meio diz o tipo de ligação (-an- simples, -en- dupla) e o \
                        sufixo diz a função (-o hidrocarboneto, -ol álcool).

                        A função química é o que determina o comportamento: todo álcool tem o grupo OH e se \
                        parece no jeito de reagir.""",
                        "Decifre \"etanol\": et- (2 carbonos) + -an- (ligações simples) + -ol (álcool). É "
                                + "o álcool comum, C₂H₅OH. O nome descreve a molécula inteira.")
        );
    }

    // ── Artes ────────────────────────────────────────────────────────────────

    private List<Topico> artes() {
        return List.of(
                new Topico("História da arte", 4,
                        "Cada período responde a uma pergunta diferente sobre o que a arte deve fazer.",
                        """
                        Na Idade Média, a arte servia à fé: figuras planas, douradas, sem preocupação com \
                        realismo — o que importava era o significado religioso.

                        O Renascimento virou a chave para o ser humano e para a observação da natureza. \
                        Chegaram a perspectiva, a proporção e o estudo da anatomia. A arte quis parecer real.

                        No século XIX, a fotografia tomou esse papel: se a máquina copia melhor, para que \
                        pintar cópias? Daí vieram o Impressionismo, que pinta a impressão da luz, e depois a \
                        arte moderna, que abandona de vez a obrigação de representar.""",
                        "Compare a Mona Lisa (Da Vinci) com uma paisagem de Monet. A primeira quer "
                                + "precisão; a segunda quer a sensação da luz num instante. A pergunta mudou."),

                new Topico("Desenho e pintura", 3,
                        "Desenhar bem é aprender a enxergar — não é dom de nascença.",
                        """
                        Comece pelas formas básicas. Qualquer objeto complexo cabe dentro de círculos, \
                        quadrados e triângulos; construa a estrutura antes de detalhar.

                        O que dá volume é a luz. Identifique de onde ela vem e distribua os tons: a área \
                        iluminada, a meia-sombra, a sombra própria (no objeto) e a sombra projetada (no \
                        chão). Sem sombra, tudo fica chapado.

                        Nas cores, as primárias são vermelho, azul e amarelo. Misturadas duas a duas, dão as \
                        secundárias. Cores opostas no círculo cromático são complementares e criam contraste \
                        forte quando ficam lado a lado.""",
                        "Para desenhar uma xícara, comece por um cilindro e uma elipse. Depois escolha a "
                                + "direção da luz e escureça o lado oposto. A forma aparece sozinha."),

                new Topico("Música", 3,
                        "Som vira música quando é organizado em altura, duração e intensidade.",
                        """
                        A melodia é a sequência de notas que você assobia. A harmonia são as notas tocadas \
                        juntas, formando acordes, que dão a cor emocional. O ritmo é a organização no tempo \
                        — é o que faz o pé bater.

                        As sete notas (dó, ré, mi, fá, sol, lá, si) se repetem em oitavas, mais graves ou \
                        mais agudas.

                        Um detalhe que explica muita coisa: acordes maiores soam alegres e menores soam \
                        melancólicos. A diferença entre os dois é meio tom em uma única nota.""",
                        "A mesma canção em acorde maior soa festiva e em menor soa triste. Trocar uma nota "
                                + "muda a emoção inteira da música."),

                new Topico("Teatro", 3,
                        "Teatro é a arte do presente: acontece uma vez, na frente de quem está ali.",
                        """
                        Três elementos bastam para haver teatro: alguém que atua, alguém que assiste e uma \
                        história. Cenário e figurino ajudam, mas não são obrigatórios.

                        A tragédia grega tratava de personagens nobres derrubados pelo próprio destino ou por \
                        uma falha de caráter. A comédia usava o riso para criticar costumes e poderosos.

                        No século XX, Brecht virou a lógica: em vez de emocionar o público até ele esquecer \
                        que está no teatro, quis que ele lembrasse disso o tempo todo — para pensar \
                        criticamente em vez de só sentir.""",
                        "Quando um ator olha para a plateia e fala diretamente com ela, está quebrando a "
                                + "quarta parede. Brecht fazia isso de propósito, para tirar o público do transe."),

                new Topico("Arte digital", 3,
                        "A ferramenta mudou, mas os fundamentos continuam os mesmos.",
                        """
                        Existem dois modos de guardar uma imagem. O raster (bitmap) é feito de pixels: ótimo \
                        para foto e pintura, mas perde qualidade ao ampliar. O vetorial é feito de fórmulas \
                        matemáticas: pode crescer infinitamente sem borrar, e é por isso que logos são \
                        vetoriais.

                        As cores também mudam de sistema conforme o destino. Na tela é RGB (luz somada: \
                        vermelho, verde e azul). No papel é CMYK (tinta subtraída: ciano, magenta, amarelo e \
                        preto). Um arquivo RGB impresso sai com cor diferente.

                        As camadas são a grande vantagem do digital: cada elemento fica separado e pode ser \
                        editado sem estragar o resto.""",
                        "Amplie muito um JPG e ele vira um borrão quadriculado; amplie um SVG e ele "
                                + "continua nítido. Por isso a logo de uma marca é sempre feita em vetor.")
        );
    }

    // ── Educação Física ──────────────────────────────────────────────────────

    private List<Topico> educacaoFisica() {
        return List.of(
                new Topico("Esportes coletivos", 3,
                        "Muda a bola e a quadra, mas a lógica dos jogos coletivos é a mesma.",
                        """
                        Todo esporte coletivo se resume a dois momentos: quando seu time tem a posse \
                        (ataque) e quando não tem (defesa). Saber em qual você está define o que fazer.

                        No ataque, o objetivo é criar espaço — abrir a defesa adversária com movimentação e \
                        passe. Na defesa, é fechar espaço, reduzindo as opções de quem tem a bola.

                        As marcações seguem essa lógica: individual (cada um cuida de um adversário) ou por \
                        zona (cada um cuida de uma área). A zona cansa menos e protege melhor o miolo; a \
                        individual pressiona mais.""",
                        "No futebol e no basquete o princípio é idêntico: sem a bola, você corre para "
                                + "abrir espaço. Quem só corre atrás da bola atrapalha o próprio time."),

                new Topico("Ginástica", 3,
                        "Antes de força, ginástica pede consciência do próprio corpo.",
                        """
                        As capacidades trabalhadas são equilíbrio, flexibilidade, força e coordenação. Elas \
                        se sustentam: sem força de tronco não há equilíbrio, e sem flexibilidade a amplitude \
                        do movimento não aparece.

                        O centro de tudo é o core — abdômen, lombar e quadril. É ele que estabiliza o corpo \
                        em qualquer movimento, inclusive fora da ginástica.

                        Aquecer antes não é formalidade: eleva a temperatura do músculo, aumenta a \
                        elasticidade e reduz de fato o risco de lesão. Alongue depois, com o músculo já \
                        aquecido.""",
                        "Faça a prancha (plank): parado, apoiado nos antebraços e nas pontas dos pés, "
                                + "corpo reto. Não parece exercício, mas 30 segundos já mostram onde está seu core."),

                new Topico("Saúde e nutrição", 4,
                        "Comer bem é combinar os grupos de alimentos — não cortar tudo o que é gostoso.",
                        """
                        São três os macronutrientes. Carboidratos são o combustível principal, o que o corpo \
                        queima primeiro. Proteínas constroem e reparam músculo e tecido. Gorduras armazenam \
                        energia e são necessárias para absorver certas vitaminas.

                        Vitaminas e minerais entram em quantidade pequena, mas sem eles várias reações do \
                        corpo simplesmente não acontecem.

                        Hidratação é parte da nutrição, e costuma ser esquecida: o corpo é cerca de 60% de \
                        água, e você perde líquido só de respirar. Sede já é sinal de desidratação começando.""",
                        "Antes de um jogo, prefira carboidrato (massa, pão, fruta) — é a energia de uso "
                                + "rápido. Proteína depois ajuda a recuperar o músculo trabalhado."),

                new Topico("Atletismo", 3,
                        "É o esporte mais antigo que existe: correr, saltar e arremessar.",
                        """
                        As provas se dividem em três famílias. As corridas vão da velocidade pura (100 m) \
                        até o fundo (maratona, 42 km). Os saltos incluem distância, altura, triplo e com \
                        vara. Os lançamentos incluem peso, disco, dardo e martelo.

                        A grande diferença entre velocidade e fundo é o tipo de energia. Provas curtas usam \
                        o sistema anaeróbio, sem oxigênio, potente e de curta duração. Provas longas usam o \
                        aeróbio, com oxigênio, que sustenta o esforço por muito tempo.

                        Por isso o treino é diferente: velocista treina explosão; fundista treina \
                        resistência.""",
                        "Um velocista de 100 m chega a correr quase sem respirar — a prova acaba antes de "
                                + "o oxigênio virar decisivo. Já o maratonista regula o ritmo pela respiração."),

                new Topico("Jogos cooperativos", 3,
                        "Jogos em que só se vence junto — ou não se vence.",
                        """
                        No jogo competitivo, o ganho de um é a perda do outro. No cooperativo, o objetivo é \
                        comum: ou o grupo inteiro alcança, ou ninguém alcança.

                        Isso muda o comportamento. Como não há adversário, ninguém é excluído por jogar mal, \
                        e a atenção vai para comunicação, estratégia e confiança. Quem tem mais dificuldade \
                        recebe ajuda em vez de ser deixado de lado.

                        Não é o oposto de competir: é outro treino. A competição desenvolve superação \
                        individual; a cooperação desenvolve a habilidade de resolver problema em grupo — que \
                        é o que a vida mais cobra.""",
                        "No \"nó humano\", o grupo de mãos dadas se embaraça e precisa se desfazer sem "
                                + "soltar as mãos. Só sai quem conversa e combina — força sozinha não resolve.")
        );
    }
}
