package itu.mg.erp.service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import itu.mg.erp.util.ReflectionUtil;

@Service
public class PDFService {
    private static final Logger logger = Logger.getLogger(PDFService.class.getName());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final DateTimeFormatter LDT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter LD_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter INPUT_LDT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
    private static final DateTimeFormatter INPUT_LD_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Génère le PDF.
     * @param data l'objet ou Map source
     * @param listColumns map des clés de listes vers leur liste de colonnes métier à afficher (null = auto)
     */
    @SuppressWarnings("unchecked")
    public byte[] generate(Object data, Map<String, List<String>> listColumns)
            throws DocumentException, IllegalArgumentException, IllegalAccessException {

        Document document = new Document();
        document.setPageSize(PageSize.A4.rotate());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Map<String, Object> map = data instanceof Map
            ? (Map<String, Object>) data
            : ReflectionUtil.toMap(data);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (skipValue(value)) continue;
            document.add(Chunk.NEWLINE);

            String label = capitalize(key.replace("_", " "));

            if (value instanceof List && !((List<?>) value).isEmpty()) {
                List<String> cols = listColumns != null ? listColumns.get(key) : null;
                List<Map<String, Object>> rows = (List<Map<String, Object>>) value;
                addListTable(document, label, rows, cols);

            } else if (value instanceof Map) {
                addTableFromMap(document, label, (Map<?, ?>) value);

            } else {
                addParagraph(document, label, formatValue(value));
            }
        }

        document.close();
        return baos.toByteArray();
    }
    @SuppressWarnings("unchecked")
    public byte[] generateSalarySlip(Object data, Map<String, List<String>> listColumns)
            throws DocumentException, IllegalArgumentException, IllegalAccessException {

        Document document = new Document();
        document.setPageSize(PageSize.A4.rotate());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Map<String, Object> map = data instanceof Map
            ? (Map<String, Object>) data
            : ReflectionUtil.toMap(data);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (skipValue(value)) continue;
            document.add(Chunk.NEWLINE);

            String label = capitalize(key.replace("_", " "));

            if (value instanceof List && !((List<?>) value).isEmpty()) {
                List<String> cols = listColumns != null ? listColumns.get(key) : null;
                List<Map<String, Object>> rows = (List<Map<String, Object>>) value;
                addListTable(document, label, rows, cols);

            } else if (value instanceof Map) {
                addTableFromMap(document, label, (Map<?, ?>) value);

            } else {
                addParagraph(document, label, formatValue(value));
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private boolean skipValue(Object value) {
        if (value == null) return true;
        if (value instanceof List && ((List<?>) value).isEmpty()) return true;
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) return true;
        if (value instanceof String && ((String) value).isEmpty()) return true;
        if (value instanceof Number && ((Number) value).doubleValue() == 0) return true;
        if (value instanceof Boolean && !((Boolean) value)) return true;
        return false;
    }

    private void addParagraph(Document doc, String label, String text) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(5);
        p.setSpacingAfter(5);
        p.add(new Chunk(label + ": ", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        p.add(new Chunk(text, FontFactory.getFont(FontFactory.HELVETICA)));
        doc.add(p);
    }

    /**
     * Génère un PdfPTable à partir d'une List de Map, avec colonnes optionnelles.
     * @param doc document PDF
     * @param title titre de la section
     * @param rows liste des Map (lignes)
     * @param columns liste ordonnée de clés à afficher (null = auto-détection)
     */
    private void addListTable(Document doc, String title,
                              List<Map<String, Object>> rows,
                              List<String> columns)
            throws DocumentException {
        // Titre
        Paragraph titre = new Paragraph(title,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        titre.setSpacingBefore(10);
        titre.setSpacingAfter(5);
        doc.add(titre);

        // Détermine colonnes effectives
        Set<String> effectiveCols = new LinkedHashSet<>();
        if (columns != null && !columns.isEmpty()) {
            effectiveCols.addAll(columns);
        } else {
            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, Object> e : row.entrySet()) {
                    if (!skipValue(e.getValue())) {
                        effectiveCols.add(e.getKey());
                    }
                }
            }
        }
        if (effectiveCols.isEmpty()) {
            addParagraph(doc, title, "Aucune donnée à afficher");
            return;
        }

        PdfPTable table = new PdfPTable(effectiveCols.size());
        table.setWidthPercentage(100);
        table.setSplitRows(true);
        com.itextpdf.text.Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);

        // En-têtes
        for (String keyCol : effectiveCols) {
            String labelCol = capitalize(keyCol.replace("_", " "));
            PdfPCell h = new PdfPCell(new Phrase(labelCol, small));
            h.setHorizontalAlignment(Element.ALIGN_CENTER);
            h.setPadding(2);
            table.addCell(h);
        }

        // Lignes
        for (Map<String, Object> row : rows) {
            for (String keyCol : effectiveCols) {
                Object raw = row.get(keyCol);
                String text = raw == null ? "" : raw.toString();
                PdfPCell c = new PdfPCell(new Phrase(text, small));
                c.setPadding(2);
                table.addCell(c);
            }
        }
        doc.add(table);
    }

    private void addTableFromMap(Document doc, String title, Map<?, ?> map)
            throws DocumentException {
        // Remplacez ici par votre implémentation existante ou stylée
        Paragraph titlePara = new Paragraph(
            title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        titlePara.setSpacingBefore(10);
        titlePara.setSpacingAfter(5);
        doc.add(titlePara);

        PdfPTable table = new PdfPTable(new float[]{3, 7});
        table.setWidthPercentage(80);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        PdfPCell k = new PdfPCell(new Phrase("Key",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        k.setBackgroundColor(BaseColor.LIGHT_GRAY);
        k.setBorderColor(BaseColor.GRAY);
        k.setPadding(4f);
        table.addCell(k);

        PdfPCell v = new PdfPCell(new Phrase("Value",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        v.setBackgroundColor(BaseColor.LIGHT_GRAY);
        v.setBorderColor(BaseColor.GRAY);
        v.setPadding(4f);
        table.addCell(v);

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = e.getKey().toString().replace("_"," ");
            key = key.substring(0,1).toUpperCase() + key.substring(1);
            String val = formatValue(e.getValue());

            PdfPCell kc = new PdfPCell(new Phrase(key, cellFont));
            kc.setBorderColor(BaseColor.LIGHT_GRAY);
            kc.setPadding(3f);
            table.addCell(kc);

            PdfPCell vc = new PdfPCell(new Phrase(val, cellFont));
            vc.setBorderColor(BaseColor.LIGHT_GRAY);
            vc.setPadding(3f);
            table.addCell(vc);
        }

        doc.add(table);
    }

    private String formatValue(Object value) {
        try {
            if (value instanceof Date) return DATE_TIME_FORMAT.format((Date) value);
            if (value instanceof LocalDateTime) return ((LocalDateTime) value).format(LDT_FORMATTER);
            if (value instanceof LocalDate) return ((LocalDate) value).format(LD_FORMATTER);
            if (value instanceof String) {
                String s = (String) value;
                try { return LocalDateTime.parse(s, INPUT_LDT_FORMATTER).format(LDT_FORMATTER); } catch (Exception e) {}
                try { return LocalDate.parse(s, INPUT_LD_FORMATTER).format(LD_FORMATTER); } catch (Exception e) {}
                return s;
            }
        } catch (Exception ex) {
            logger.warning("Failed to format value: " + value);
        }
        return value.toString();
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static String generateUniqueFileName(Map<String, Object> data) {
        String doctype = (String) data.getOrDefault("doctype", "Document");
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return doctype + "-" + currentDate + ".pdf";
    }
}
