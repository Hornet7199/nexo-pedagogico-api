package com.nexo.pedagogicoapi.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nexo.pedagogicoapi.api.ApiException;
import com.nexo.pedagogicoapi.domain.Aluno;
import com.nexo.pedagogicoapi.domain.Turma;
import com.nexo.pedagogicoapi.repository.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agregações institucionais (KPIs, séries por turma) e exportação real PDF/Excel —
 * substitui os números fixos de relatorios-diretor.ts e os botões sem handler.
 */
@Service
public class RelatorioService {

    public record SerieTurma(String turma, double media, double frequencia, int alunos) {}

    public record Desempenho(double taxaAprovacao, double mediaGeral, double frequenciaMedia,
                             double engajamentoMedio, long totalAlunos, List<SerieTurma> turmas) {}

    private final AlunoRepository alunos;
    private final TurmaRepository turmas;
    private final AgregadosAcademicos agregados;
    private final NotaRepository notas;

    public RelatorioService(AlunoRepository alunos, TurmaRepository turmas, AgregadosAcademicos agregados,
                            NotaRepository notas) {
        this.alunos = alunos;
        this.turmas = turmas;
        this.agregados = agregados;
        this.notas = notas;
    }

    /** Períodos disponíveis para o filtro da tela (só os que têm nota lançada). */
    @Transactional(readOnly = true)
    public List<String> periodosDisponiveis() {
        return notas.listarPeriodos();
    }

    /**
     * KPIs institucionais e série por turma em 4 queries fixas (antes: {@code 4N + T + 4},
     * com N = alunos — o que fazia esta tela levar segundos em produção).
     */
    @Transactional(readOnly = true)
    public Desempenho desempenho(String periodo) {
        var indices = agregados.carregar(periodo);
        var todosAlunos = alunos.findAllComTurma();
        long total = todosAlunos.size();

        // Um único passo pelos alunos alimenta os quatro KPIs. A versão anterior
        // materializava um List<Double> com uma média boxed por aluno e depois o
        // percorria três vezes (média, aprovados, engajamento).
        double somaMedias = 0;
        int comNota = 0;
        int aprovados = 0;
        long somaEngajamento = 0;
        for (Aluno a : todosAlunos) {
            Double m = indices.media(a.getId());
            if (m != null) {
                somaMedias += m;
                comNota++;
                if (m >= 6.0) aprovados++;
            }
            somaEngajamento += a.getEngajamento();
        }

        double mediaGeral = arred(comNota == 0 ? 0 : somaMedias / comNota);
        double taxaAprovacao = comNota == 0 ? 0 : arred(aprovados * 100.0 / comNota);
        double engajamentoMedio = arred(total == 0 ? 0 : (double) somaEngajamento / total);

        var geral = indices.totalGeral();
        double frequenciaMedia = geral.total() == 0 ? 0
                : arred((geral.total() - geral.faltas()) * 100.0 / geral.total());

        Map<Long, List<Aluno>> porTurma = todosAlunos.stream()
                .filter(a -> a.getTurma() != null)
                .collect(Collectors.groupingBy(a -> a.getTurma().getId()));

        List<SerieTurma> series = new ArrayList<>(porTurma.size());
        for (Turma turma : turmas.findAll()) {
            var alunosTurma = porTurma.getOrDefault(turma.getId(), List.of());
            if (alunosTurma.isEmpty()) continue;
            double somaTurma = 0;
            int comNotaTurma = 0;
            double somaPresenca = 0;
            for (Aluno a : alunosTurma) {
                Double m = indices.media(a.getId());
                if (m != null) {
                    somaTurma += m;
                    comNotaTurma++;
                }
                somaPresenca += indices.percentualPresenca(a.getId());
            }
            double media = arred(comNotaTurma == 0 ? 0 : somaTurma / comNotaTurma);
            double freq = arred(somaPresenca / alunosTurma.size());
            series.add(new SerieTurma(turma.getNome(), media, freq, alunosTurma.size()));
        }

        return new Desempenho(taxaAprovacao, mediaGeral, frequenciaMedia, engajamentoMedio, total, series);
    }

    @Transactional(readOnly = true)
    public byte[] exportar(String formato, String periodo) {
        Desempenho dados = desempenho(periodo);
        return switch (formato == null ? "" : formato.toLowerCase()) {
            case "pdf" -> exportarPdf(dados);
            case "xlsx" -> exportarXlsx(dados);
            default -> throw ApiException.badRequest("Formato de exportação inválido. Use pdf ou xlsx.");
        };
    }

    private byte[] exportarPdf(Desempenho dados) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph cabecalho = new Paragraph("Nexo — Relatório de Desempenho Institucional", titulo);
            cabecalho.setAlignment(Element.ALIGN_CENTER);
            doc.add(cabecalho);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(String.format(
                    "Taxa de aprovação: %.1f%%   |   Média geral: %.1f   |   Frequência média: %.1f%%   |   Engajamento médio: %.1f",
                    dados.taxaAprovacao(), dados.mediaGeral(), dados.frequenciaMedia(), dados.engajamentoMedio())));
            doc.add(new Paragraph(" "));

            PdfPTable tabela = new PdfPTable(4);
            tabela.setWidthPercentage(100);
            for (String h : List.of("Turma", "Alunos", "Média", "Frequência (%)")) {
                tabela.addCell(new Paragraph(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            }
            for (SerieTurma s : dados.turmas()) {
                tabela.addCell(s.turma());
                tabela.addCell(String.valueOf(s.alunos()));
                tabela.addCell(String.format("%.1f", s.media()));
                tabela.addCell(String.format("%.1f", s.frequencia()));
            }
            doc.add(tabela);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF", e);
        }
    }

    private byte[] exportarXlsx(Desempenho dados) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet kpis = wb.createSheet("KPIs");
            String[][] linhasKpi = {
                    {"Taxa de aprovação (%)", String.valueOf(dados.taxaAprovacao())},
                    {"Média geral", String.valueOf(dados.mediaGeral())},
                    {"Frequência média (%)", String.valueOf(dados.frequenciaMedia())},
                    {"Engajamento médio", String.valueOf(dados.engajamentoMedio())},
                    {"Total de alunos", String.valueOf(dados.totalAlunos())},
            };
            for (int i = 0; i < linhasKpi.length; i++) {
                Row row = kpis.createRow(i);
                row.createCell(0).setCellValue(linhasKpi[i][0]);
                row.createCell(1).setCellValue(Double.parseDouble(linhasKpi[i][1]));
            }

            Sheet porTurma = wb.createSheet("Por turma");
            Row header = porTurma.createRow(0);
            String[] colunas = {"Turma", "Alunos", "Média", "Frequência (%)"};
            for (int i = 0; i < colunas.length; i++) header.createCell(i).setCellValue(colunas[i]);
            int linha = 1;
            for (SerieTurma s : dados.turmas()) {
                Row row = porTurma.createRow(linha++);
                row.createCell(0).setCellValue(s.turma());
                row.createCell(1).setCellValue(s.alunos());
                row.createCell(2).setCellValue(s.media());
                row.createCell(3).setCellValue(s.frequencia());
            }

            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar Excel", e);
        }
    }

    private static double arred(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
