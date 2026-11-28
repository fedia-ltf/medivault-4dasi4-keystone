package tn.esprit.medivault.dossier.services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.dossier.entities.Consultation;
import tn.esprit.medivault.dossier.entities.Dossier;
import tn.esprit.medivault.dossier.exceptions.DossierNotFoundException;
import tn.esprit.medivault.dossier.repositories.ConsultationRepository;
import tn.esprit.medivault.dossier.repositories.DossierRepository;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationPdfService {

    private final DossierRepository dossierRepository;
    private final ConsultationRepository consultationRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color HEADER_BG  = new Color(0x0B, 0x25, 0x45);
    private static final Color HEADER_TXT = Color.WHITE;
    private static final Color ROW_ALT    = new Color(0xF0, 0xF7, 0xFF);
    private static final Color BORDER     = new Color(0xCC, 0xDD, 0xEE);

    @Transactional(readOnly = true)
    public byte[] genererPdf(String patientId, String medecinId, LocalDate du, LocalDate au) {

        Dossier dossier = dossierRepository.findByPatientId(patientId)
                .orElseThrow(() -> new DossierNotFoundException(
                        "Aucun dossier trouvé pour le patient : " + patientId));

        List<Consultation> consultations = consultationRepository
                .findFiltered(dossier.getId(), medecinId, du, au,
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "date")))
                .getContent();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 40, 40, 60, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ---- Titre ----
            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, HEADER_BG);
            Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(0x44, 0x44, 0x55));
            Font labelFont  = new Font(Font.HELVETICA, 9,  Font.BOLD,   HEADER_BG);
            Font valueFont  = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.DARK_GRAY);
            Font smallFont  = new Font(Font.HELVETICA, 8,  Font.ITALIC, Color.GRAY);
            Font colFont    = new Font(Font.HELVETICA, 8,  Font.BOLD,   HEADER_TXT);
            Font cellFont   = new Font(Font.HELVETICA, 8,  Font.NORMAL, Color.DARK_GRAY);

            Paragraph titre = new Paragraph("Dossier médical — Consultations", titleFont);
            titre.setAlignment(Element.ALIGN_CENTER);
            doc.add(titre);

            Paragraph generee = new Paragraph("Généré le " + LocalDate.now().format(D_FMT), subtitleFont);
            generee.setAlignment(Element.ALIGN_CENTER);
            generee.setSpacingAfter(16);
            doc.add(generee);

            // ---- Infos patient ----
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(16);
            infoTable.setWidths(new float[]{1, 2});

            addInfoRow(infoTable, "Patient ID", patientId, labelFont, valueFont);
            addInfoRow(infoTable, "Groupe sanguin",
                    dossier.getGroupeSanguin() != null ? dossier.getGroupeSanguin().name() : "—", labelFont, valueFont);
            addInfoRow(infoTable, "Allergies",
                    dossier.getAllergies() == null || dossier.getAllergies().isEmpty()
                            ? "Aucune" : String.join(", ", dossier.getAllergies()),
                    labelFont, valueFont);
            addInfoRow(infoTable, "Antécédents",
                    dossier.getAntecedents() != null && !dossier.getAntecedents().isBlank()
                            ? dossier.getAntecedents() : "—",
                    labelFont, valueFont);
            doc.add(infoTable);

            // ---- Filtres actifs ----
            if (medecinId != null || du != null || au != null) {
                StringBuilder filtres = new StringBuilder("Filtres actifs : ");
                if (medecinId != null) filtres.append("Médecin=").append(medecinId).append("  ");
                if (du != null) filtres.append("Depuis=").append(du.format(D_FMT)).append("  ");
                if (au != null) filtres.append("Au=").append(au.format(D_FMT));
                Paragraph pFiltres = new Paragraph(filtres.toString(), smallFont);
                pFiltres.setSpacingAfter(8);
                doc.add(pFiltres);
            }

            // ---- Tableau des consultations ----
            if (consultations.isEmpty()) {
                Paragraph vide = new Paragraph("Aucune consultation trouvée pour ce dossier.", valueFont);
                vide.setSpacingBefore(10);
                doc.add(vide);
            } else {
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.6f, 2.0f, 2.4f, 2.0f, 2.6f, 2.4f});
                table.setHeaderRows(1);

                String[] headers = {"Date", "Médecin", "Motif", "Diagnostic", "Observations", "Analyses"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, colFont));
                    cell.setBackgroundColor(HEADER_BG);
                    cell.setPadding(5);
                    cell.setBorderColor(BORDER);
                    table.addCell(cell);
                }

                boolean alt = false;
                for (Consultation c : consultations) {
                    Color bg = alt ? ROW_ALT : Color.WHITE;
                    alt = !alt;

                    addCell(table, c.getDate() != null ? c.getDate().format(DT_FMT) : "—", cellFont, bg);
                    addCell(table, c.getMedecinId(), cellFont, bg);
                    addCell(table, c.getMotif(), cellFont, bg);
                    addCell(table, nvl(c.getDiagnostic()), cellFont, bg);
                    addCell(table, truncate(nvl(c.getObservations()), 300), cellFont, bg);
                    String analyses = c.getAnalysesDemandees() == null || c.getAnalysesDemandees().isEmpty()
                            ? "—" : String.join(", ", c.getAnalysesDemandees());
                    addCell(table, analyses, cellFont, bg);
                }
                doc.add(table);

                Paragraph nb = new Paragraph(consultations.size() + " consultation(s) listée(s).", smallFont);
                nb.setSpacingBefore(6);
                doc.add(nb);
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF : " + e.getMessage(), e);
        }
    }

    // ---- Helpers ----

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setPadding(4);
        l.setBorderColor(BORDER);
        l.setBackgroundColor(new Color(0xF4, 0xF8, 0xFF));
        table.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(value, valueFont));
        v.setPadding(4);
        v.setBorderColor(BORDER);
        table.addCell(v);
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(bg);
        cell.setNoWrap(false);
        table.addCell(cell);
    }

    private String nvl(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
