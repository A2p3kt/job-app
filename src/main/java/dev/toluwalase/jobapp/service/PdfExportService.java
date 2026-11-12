package dev.toluwalase.jobapp.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import dev.toluwalase.jobapp.domain.ApplicationKit;

/**
 * Service generating high-quality, ATS-friendly PDF documents for customized CVs
 * and Cover Letters using OpenPDF.
 */
@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    // Color Palette
    private static final Color COLOR_PRIMARY = new Color(15, 23, 42);     // Slate 900
    private static final Color COLOR_ACCENT = new Color(67, 56, 202);     // Indigo 700
    private static final Color COLOR_BODY = new Color(51, 65, 85);         // Slate 700
    private static final Color COLOR_MUTED = new Color(100, 116, 139);     // Slate 500
    private static final Color COLOR_LINE = new Color(226, 232, 240);      // Slate 200

    // Font Styles
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_MUTED);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_ACCENT);
    private static final Font FONT_SUBSECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_PRIMARY);
    private static final Font FONT_ITALIC_DATE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, COLOR_MUTED);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_BODY);
    private static final Font FONT_BULLET = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COLOR_BODY);

    // Cover Letter Fonts
    private static final Font FONT_CL_DATE = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_MUTED);
    private static final Font FONT_CL_RECIPIENT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_PRIMARY);
    private static final Font FONT_CL_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, COLOR_BODY);
    private static final Font FONT_CL_SIGN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY);

    /**
     * Converts the tailored CV content of an ApplicationKit into a professional PDF byte stream.
     *
     * @param kit ApplicationKit holding candidate CV data
     * @return Generated PDF document as byte array
     */
    public byte[] generateCvPdf(ApplicationKit kit) {
        String content = (kit != null && kit.getTailoredCvContent() != null)
                ? kit.getTailoredCvContent()
                : "# Candidate Curriculum Vitae\nNo content available.";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            String[] lines = content.split("\r?\n");
            for (String rawLine : lines) {
                String line = rawLine.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("# ")) {
                    // Document Main Title (Candidate Name)
                    String titleText = line.substring(2).trim();
                    Paragraph p = new Paragraph(titleText, FONT_TITLE);
                    p.setSpacingBefore(4);
                    p.setSpacingAfter(4);
                    document.add(p);
                } else if (line.startsWith("## ")) {
                    // Major Section Header
                    String sectionText = line.substring(3).trim();
                    Paragraph p = new Paragraph(sectionText, FONT_SECTION);
                    p.setSpacingBefore(12);
                    p.setSpacingAfter(3);
                    document.add(p);

                    LineSeparator lineSep = new LineSeparator(1f, 100, COLOR_LINE, Element.ALIGN_CENTER, -2);
                    document.add(lineSep);
                } else if (line.startsWith("### ")) {
                    // Subsection / Role Title
                    String subText = line.substring(4).trim();
                    Paragraph p = new Paragraph(subText, FONT_SUBSECTION);
                    p.setSpacingBefore(8);
                    p.setSpacingAfter(2);
                    document.add(p);
                } else if (line.startsWith("- ") || line.startsWith("* ")) {
                    // Bulleted Item
                    String bulletText = line.substring(2).trim();
                    Paragraph p = new Paragraph();
                    p.setIndentationLeft(14);
                    p.setSpacingBefore(1.5f);
                    p.setSpacingAfter(1.5f);
                    p.add(new Chunk("\u2022  ", FONT_SECTION));
                    p.add(new Chunk(bulletText, FONT_BULLET));
                    document.add(p);
                } else if (line.startsWith("> ")) {
                    // Callout / Target role line
                    String quoteText = line.substring(2).trim().replaceAll("\\*\\*", "");
                    Paragraph p = new Paragraph(quoteText, FONT_SUBTITLE);
                    p.setSpacingBefore(2);
                    p.setSpacingAfter(6);
                    document.add(p);
                } else if (line.startsWith("*") && line.endsWith("*") && line.length() > 2) {
                    // Dates / Subtitle line
                    String italicText = line.substring(1, line.length() - 1).trim();
                    Paragraph p = new Paragraph(italicText, FONT_ITALIC_DATE);
                    p.setSpacingBefore(1);
                    p.setSpacingAfter(3);
                    document.add(p);
                } else {
                    // Regular Body Paragraph
                    String cleanedLine = line.replaceAll("\\*\\*", "");
                    Paragraph p = new Paragraph(cleanedLine, FONT_BODY);
                    p.setLeading(13f);
                    p.setSpacingBefore(2);
                    p.setSpacingAfter(4);
                    document.add(p);
                }
            }

            document.close();
            log.info("Successfully generated CV PDF ({} bytes) for ApplicationKit ID: {}",
                    out.size(), kit != null ? kit.getId() : "null");
            return out.toByteArray();

        } catch (DocumentException ex) {
            log.error("Failed to render CV PDF: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Could not generate CV PDF: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during CV PDF export: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to export CV PDF", ex);
        }
    }

    /**
     * Converts the tailored Cover Letter content of an ApplicationKit into a professional PDF byte stream.
     *
     * @param kit ApplicationKit holding candidate Cover Letter data
     * @return Generated PDF document as byte array
     */
    public byte[] generateCoverLetterPdf(ApplicationKit kit) {
        String content = (kit != null && kit.getTailoredCoverLetterContent() != null)
                ? kit.getTailoredCoverLetterContent()
                : "Cover Letter\nNo content available.";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 44, 44, 44, 44);
            PdfWriter.getInstance(document, out);
            document.open();

            String[] paragraphs = content.split("(\r?\n){2,}");
            boolean isFirst = true;

            for (String rawPara : paragraphs) {
                String para = rawPara.trim();
                if (para.isEmpty()) {
                    continue;
                }

                if (isFirst) {
                    // Typically date
                    Paragraph p = new Paragraph(para, FONT_CL_DATE);
                    p.setSpacingAfter(12);
                    document.add(p);
                    isFirst = false;
                    continue;
                }

                if (para.startsWith("Hiring Team") || para.startsWith("To:") || para.contains("Dear Hiring Team") || para.startsWith("Dear ")) {
                    Paragraph p = new Paragraph(para, FONT_CL_RECIPIENT);
                    p.setLeading(15f);
                    p.setSpacingAfter(10);
                    document.add(p);
                } else if (para.startsWith("Sincerely") || para.startsWith("Best regards") || para.startsWith("Warm regards")) {
                    Paragraph p = new Paragraph(para, FONT_CL_SIGN);
                    p.setLeading(16f);
                    p.setSpacingBefore(14);
                    p.setSpacingAfter(6);
                    document.add(p);
                } else {
                    Paragraph p = new Paragraph(para, FONT_CL_BODY);
                    p.setLeading(16f);
                    p.setSpacingBefore(4);
                    p.setSpacingAfter(8);
                    document.add(p);
                }
            }

            document.close();
            log.info("Successfully generated Cover Letter PDF ({} bytes) for ApplicationKit ID: {}",
                    out.size(), kit != null ? kit.getId() : "null");
            return out.toByteArray();

        } catch (DocumentException ex) {
            log.error("Failed to render Cover Letter PDF: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Could not generate Cover Letter PDF: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during Cover Letter PDF export: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to export Cover Letter PDF", ex);
        }
    }
}

