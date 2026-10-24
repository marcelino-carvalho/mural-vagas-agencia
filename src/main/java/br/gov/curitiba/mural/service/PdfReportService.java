package br.gov.curitiba.mural.service;

import br.gov.curitiba.mural.domain.Vaga;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Serviço responsável por compor e renderizar o relatório em PDF com as vagas disponíveis.
 * Agrupa as vagas por categorias pré-definidas (Convencionais, Ensino Superior, PCD)
 * e formata o layout visual para impressão ou download.
 */
@Service
public class PdfReportService {

    /**
     * Gera o documento PDF contendo o relatório tabular de vagas agrupadas por categoria.
     *
     * @param vagas Lista de vagas a serem incluídas no relatório
     * @return Array de bytes do arquivo PDF gerado
     */
    public byte[] generatePdfReport(List<Vaga> vagas) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // Definições de fontes tipográficas
            Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fonteCategoria = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font fonteCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font fonteDados = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Cabeçalho Principal do Relatório
            Paragraph titulo = new Paragraph("RELATÓRIO DE VAGAS DISPONÍVEIS", fonteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(10);
            document.add(titulo);

            // Configuração das categorias de exibição
            String[] categorias = {"CONVENCIONAL", "SUPERIOR", "PCD"};
            String[] nomesExibicao = {"VAGAS CONVENCIONAIS", "ENSINO SUPERIOR", "VAGAS PCD"};
            Color[] cores = {
                    new Color(0, 86, 179),  // Azul institucional
                    new Color(211, 84, 0),  // Laranja
                    new Color(22, 160, 133) // Verde
            };

            for (int i = 0; i < categorias.length; i++) {
                String catAtual = categorias[i];
                String nomeExibicao = nomesExibicao[i];
                Color corAtual = cores[i];

                List<Vaga> vagasDaCategoria = vagas.stream()
                        .filter(v -> v.getCategoria() != null && v.getCategoria().equalsIgnoreCase(catAtual))
                        .toList();

                if (vagasDaCategoria.isEmpty()) {
                    continue;
                }

                // Tabela da categoria: Cód (10%), Cargo (35%), Salário (20%), Detalhes/Benefícios (35%)
                PdfPTable table = new PdfPTable(new float[]{1f, 3.5f, 2f, 3.5f});
                table.setWidthPercentage(100);
                table.setSpacingBefore(10);
                table.setSpacingAfter(10);

                // Cabeçalho da Categoria com barra colorida
                PdfPCell cellTitulo = new PdfPCell(new Phrase(nomeExibicao, fonteCategoria));
                cellTitulo.setColspan(4);
                cellTitulo.setBackgroundColor(corAtual);
                cellTitulo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellTitulo.setPadding(6);
                table.addCell(cellTitulo);

                // Colunas de dados
                addCellHeader(table, "CÓD", fonteCabecalho);
                addCellHeader(table, "CARGO / FUNÇÃO", fonteCabecalho);
                addCellHeader(table, "SALÁRIO", fonteCabecalho);
                addCellHeader(table, "EXP / BENEFÍCIOS", fonteCabecalho);

                // Linhas das vagas
                for (Vaga vaga : vagasDaCategoria) {
                    table.addCell(new Phrase(vaga.getCodigo() != null ? vaga.getCodigo() : "-", fonteDados));
                    table.addCell(new Phrase(vaga.getTitulo() != null ? vaga.getTitulo() : "-", fonteDados));
                    table.addCell(new Phrase("R$ " + (vaga.getSalario() != null ? vaga.getSalario() : "-"), fonteDados));

                    String exp = (vaga.getExperiencia() != null && vaga.getExperiencia().equalsIgnoreCase("Sim")) ? "[COM EXP] " : "";
                    String ben = vaga.getBeneficios() != null ? vaga.getBeneficios() : "";
                    table.addCell(new Phrase(exp + ben, fonteDados));
                }

                document.add(table);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório em PDF das vagas", e);
        }
    }

    /**
     * Cria e estiliza uma célula de cabeçalho da tabela com fundo cinza claro.
     */
    private void addCellHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
