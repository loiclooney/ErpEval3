package itu.mg.erp.service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import itu.mg.erp.util.ReflectionUtil;

@Service
public class PDFService {
    private static final Logger logger = Logger.getLogger(PDFService.class.getName());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final DateTimeFormatter LDT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter LD_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter INPUT_LDT_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
    private static final DateTimeFormatter INPUT_LD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Génère le PDF.
     * 
     * @param data        l'objet ou Map source
     * @param listColumns map des clés de listes vers leur liste de colonnes métier
     *                    à afficher (null = auto)
     */
    @SuppressWarnings("unchecked")
    public byte[] generate(Object data, Map<String, List<String>> listColumns)
            throws DocumentException, IllegalArgumentException, IllegalAccessException {

        Document document = new Document();
        document.setPageSize(PageSize.A4);
        document.setMargins(40, 40, 60, 50);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        Map<String, Object> map = data instanceof Map
                ? (Map<String, Object>) data
                : ReflectionUtil.toMap(data);

        // Ajout de l'en-tête
        addHeader(document, map);

        // Informations de l'employé
        addEmployeeInfo(document, map);

        // Période de paie
        addPayrollPeriod(document, map);

        // Gains (earnings)
        if (map.containsKey("earnings") && map.get("earnings") instanceof List) {
            List<Map<String, Object>> earnings = (List<Map<String, Object>>) map.get("earnings");
            if (!earnings.isEmpty()) {
                addEarningsTable(document, earnings);
            }
        }

        // Déductions
        if (map.containsKey("deductions") && map.get("deductions") instanceof List) {
            List<Map<String, Object>> deductions = (List<Map<String, Object>>) map.get("deductions");
            if (!deductions.isEmpty()) {
                addDeductionsTable(document, deductions);
            }
        }

        // Résumé financier
        addFinancialSummary(document, map);

        // Pied de page
        addFooter(document, map);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, Map<String, Object> data) throws DocumentException {
        // Logo et titre de l'entreprise
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
        Font companyFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);

        Paragraph title = new Paragraph("BULLETIN DE PAIE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        if (data.get("company") != null) {
            Paragraph company = new Paragraph(data.get("company").toString(), companyFont);
            company.setAlignment(Element.ALIGN_CENTER);
            company.setSpacingAfter(20);
            document.add(company);
        }

        // Ligne de séparation
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.GRAY);
        document.add(new Chunk(line));
        document.add(Chunk.NEWLINE);
    }

    private void addEmployeeInfo(Document document, Map<String, Object> data) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        // Titre section
        Paragraph sectionTitle = new Paragraph("INFORMATIONS EMPLOYÉ", headerFont);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Tableau des informations employé
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        // Configuration des colonnes
        float[] columnWidths = { 25f, 25f, 25f, 25f };
        infoTable.setWidths(columnWidths);

        // Ajout des cellules
        addInfoCell(infoTable, "Nom", data.get("employee_name"), contentFont);
        addInfoCell(infoTable, "Matricule", data.get("employee"), contentFont);
        addInfoCell(infoTable, "Statut", data.get("status"), contentFont);
        addInfoCell(infoTable, "Devise", data.get("currency"), contentFont);

        document.add(infoTable);
    }

    private void addPayrollPeriod(Document document, Map<String, Object> data) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        Paragraph sectionTitle = new Paragraph("PÉRIODE DE PAIE", headerFont);
        sectionTitle.setSpacingBefore(5);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable periodTable = new PdfPTable(3);
        periodTable.setWidthPercentage(100);
        periodTable.setSpacingAfter(15);

        addInfoCell(periodTable, "Date début", data.get("start_date"), contentFont);
        addInfoCell(periodTable, "Date fin", data.get("end_date"), contentFont);
        addInfoCell(periodTable, "Fréquence", data.get("payroll_frequency"), contentFont);

        document.add(periodTable);
    }

    private void addEarningsTable(Document document, List<Map<String, Object>> earnings) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

        Paragraph sectionTitle = new Paragraph("GAINS ET RÉMUNÉRATIONS", headerFont);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        float[] columnWidths = { 40f, 15f, 20f, 25f };
        table.setWidths(columnWidths);

        // En-têtes de tableau
        addTableHeader(table, "Composant", tableHeaderFont, BaseColor.DARK_GRAY);
        addTableHeader(table, "Code", tableHeaderFont, BaseColor.DARK_GRAY);
        addTableHeader(table, "Montant", tableHeaderFont, BaseColor.DARK_GRAY);
        addTableHeader(table, "Cumul année", tableHeaderFont, BaseColor.DARK_GRAY);

        // Données
        boolean isEven = false;
        double totalEarnings = 0;

        for (Map<String, Object> earning : earnings) {
            BaseColor bgColor = isEven ? new BaseColor(245, 245, 245) : BaseColor.WHITE;

            addTableCell(table, formatValue(earning.get("salary_component")), contentFont, bgColor);
            addTableCell(table, formatValue(earning.get("abbr")), contentFont, bgColor);

            String amount = formatCurrency(earning.get("amount"));
            addTableCell(table, amount, contentFont, bgColor, Element.ALIGN_RIGHT);

            addTableCell(table, formatCurrency(earning.get("year_to_date")), contentFont, bgColor, Element.ALIGN_RIGHT);

            // Calculer le total
            if (earning.get("amount") != null) {
                try {
                    totalEarnings += Double.parseDouble(earning.get("amount").toString());
                } catch (NumberFormatException ignored) {
                }
            }

            isEven = !isEven;
        }

        // Ligne de total
        addTotalRow(table, "TOTAL GAINS", totalEarnings, tableHeaderFont);

        document.add(table);
    }

    private void addDeductionsTable(Document document, List<Map<String, Object>> deductions) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

        Paragraph sectionTitle = new Paragraph("DÉDUCTIONS ET RETENUES", headerFont);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        float[] columnWidths = { 40f, 15f, 20f, 25f };
        table.setWidths(columnWidths);

        // En-têtes
        addTableHeader(table, "Composant", tableHeaderFont, new BaseColor(150, 50, 50));
        addTableHeader(table, "Code", tableHeaderFont, new BaseColor(150, 50, 50));
        addTableHeader(table, "Montant", tableHeaderFont, new BaseColor(150, 50, 50));
        addTableHeader(table, "Cumul année", tableHeaderFont, new BaseColor(150, 50, 50));

        // Données
        boolean isEven = false;
        double totalDeductions = 0;

        for (Map<String, Object> deduction : deductions) {
            BaseColor bgColor = isEven ? new BaseColor(250, 240, 240) : BaseColor.WHITE;

            addTableCell(table, formatValue(deduction.get("salary_component")), contentFont, bgColor);
            addTableCell(table, formatValue(deduction.get("abbr")), contentFont, bgColor);

            String amount = formatCurrency(deduction.get("amount"));
            addTableCell(table, amount, contentFont, bgColor, Element.ALIGN_RIGHT);

            addTableCell(table, formatCurrency(deduction.get("year_to_date")), contentFont, bgColor,
                    Element.ALIGN_RIGHT);

            // Calculer le total
            if (deduction.get("amount") != null) {
                try {
                    totalDeductions += Double.parseDouble(deduction.get("amount").toString());
                } catch (NumberFormatException ignored) {
                }
            }

            isEven = !isEven;
        }

        // Ligne de total
        addTotalRow(table, "TOTAL DÉDUCTIONS", totalDeductions, tableHeaderFont);

        document.add(table);
    }

    private void addFinancialSummary(Document document, Map<String, Object> data) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
        Font summaryFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font netPayFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);

        Paragraph sectionTitle = new Paragraph("RÉSUMÉ FINANCIER", headerFont);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.setSpacingAfter(15);

        float[] summaryWidths = { 60f, 40f };
        summaryTable.setWidths(summaryWidths);

        // Salaire brut
        addSummaryRow(summaryTable, "Salaire brut", data.get("gross_pay"), summaryFont, BaseColor.LIGHT_GRAY);

        // Total déductions
        addSummaryRow(summaryTable, "Total déductions", data.get("total_deduction"), summaryFont, BaseColor.LIGHT_GRAY);

        // Salaire net (mis en évidence)
        PdfPCell netLabel = new PdfPCell(new Phrase("SALAIRE NET", netPayFont));
        netLabel.setBorder(Rectangle.BOX);
        netLabel.setBorderWidth(2);
        netLabel.setBackgroundColor(new BaseColor(220, 220, 220));
        netLabel.setPadding(8);
        summaryTable.addCell(netLabel);

        PdfPCell netAmount = new PdfPCell(new Phrase(formatCurrency(data.get("net_pay")), netPayFont));
        netAmount.setBorder(Rectangle.BOX);
        netAmount.setBorderWidth(2);
        netAmount.setBackgroundColor(new BaseColor(220, 220, 220));
        netAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netAmount.setPadding(8);
        summaryTable.addCell(netAmount);

        document.add(summaryTable);

        // Montant en lettres
        if (data.get("total_in_words") != null) {
            Font wordsFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.DARK_GRAY);
            Paragraph words = new Paragraph("Montant en lettres: " + data.get("total_in_words"), wordsFont);
            words.setAlignment(Element.ALIGN_CENTER);
            words.setSpacingAfter(20);
            document.add(words);
        }
    }

    private void addFooter(Document document, Map<String, Object> data) throws DocumentException {
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

        // Ligne de séparation
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);
        document.add(new Chunk(line));

        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(10);

        if (data.get("posting_date") != null) {
            footer.add(new Chunk("Document généré le: " + data.get("posting_date"), footerFont));
        }

        if (data.get("name") != null) {
            footer.add(new Chunk(" | Référence: " + data.get("name"), footerFont));
        }

        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    // Méthodes utilitaires
    private void addInfoCell(PdfPTable table, String label, Object value, Font font) {
        PdfPCell labelCell = new PdfPCell(
                new Phrase(label + ":", new Font(font.getFamily(), font.getSize(), Font.BOLD)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(formatValue(value), font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        addTableCell(table, text, font, bgColor, Element.ALIGN_LEFT);
    }

    private void addTableCell(PdfPTable table, String text, Font font, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, double total, Font font) {
        PdfPCell totalLabelCell = new PdfPCell(new Phrase(label, font));
        totalLabelCell.setColspan(3);
        totalLabelCell.setBackgroundColor(BaseColor.DARK_GRAY);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(8);
        table.addCell(totalLabelCell);

        PdfPCell totalAmountCell = new PdfPCell(new Phrase(formatCurrency(total), font));
        totalAmountCell.setBackgroundColor(BaseColor.DARK_GRAY);
        totalAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalAmountCell.setPadding(8);
        table.addCell(totalAmountCell);
    }

    private void addSummaryRow(PdfPTable table, String label, Object value, Font font, BaseColor bgColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBackgroundColor(bgColor);
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(value), font));
        valueCell.setBackgroundColor(bgColor);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private String formatValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private String formatCurrency(Object value) {
        if (value == null)
            return "0,00";

        try {
            double amount = Double.parseDouble(value.toString());
            return String.format("%,.2f", amount);
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String generateUniqueFileName(Map<String, Object> data) {
        String doctype = (String) data.getOrDefault("doctype", "Document");
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return doctype + "-" + currentDate + ".pdf";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> processListData(Object listData) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (listData instanceof List<?>) {
            List<?> rawList = (List<?>) listData;
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?>) {
                    Map<?, ?> item = (Map<?, ?>) obj;
                    Map<String, Object> processedItem = new HashMap<>();

                    // Copie des champs essentiels avec validation
                    String[] fields = { "salary_component", "abbr", "amount", "year_to_date" };
                    for (String field : fields) {
                        Object value = item.get(field);
                        processedItem.put(field, value != null ? value : "");
                    }
                    result.add(processedItem);
                }
            }
        }

        return result;
    }

    private String generateFileName(Map<String, Object> data) {
        StringBuilder fileName = new StringBuilder("Bulletin_Paie");

        if (data.get("employee_name") != null) {
            fileName.append("_").append(data.get("employee_name").toString().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        if (data.get("start_date") != null) {
            fileName.append("_").append(data.get("start_date").toString().replaceAll("[^0-9]", ""));
        }

        fileName.append(".pdf");
        return fileName.toString();
    }

    private Map<String, Object> extractEssentialData(Map<String, Object> data) {
        Map<String, Object> essentialData = new HashMap<>();

        // Données de base
        String[] basicFields = {
                "name", "employee", "employee_name", "company", "posting_date",
                "status", "currency", "payroll_frequency", "start_date", "end_date",
                "gross_pay", "total_deduction", "net_pay", "total_in_words"
        };

        for (String field : basicFields) {
            essentialData.put(field, data.get(field));
        }

        // Traitement des gains
        essentialData.put("earnings", processListData(data.get("earnings")));

        // Traitement des déductions
        essentialData.put("deductions", processListData(data.get("deductions")));

        return essentialData;
    }
}
