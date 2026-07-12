package com.hrms.service;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class DocxPreviewService {

    public String convertToHtml(String filePath) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"font-family: Georgia, serif; line-height: 1.6; color: #1f2933;\">");

        try (InputStream is = new FileInputStream(filePath); XWPFDocument document = new XWPFDocument(is)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String alignment = paragraph.getAlignment() == ParagraphAlignment.CENTER ? "center" : "left";
                html.append("<p style=\"margin: 0 0 10px 0; text-align: ").append(alignment).append(";\">");

                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    if (text == null) continue;
                    text = escapeHtml(text);
                    if (run.isBold()) text = "<strong>" + text + "</strong>";
                    if (run.isItalic()) text = "<em>" + text + "</em>";
                    html.append(text);
                }
                html.append("</p>");
            }

            for (XWPFTable table : document.getTables()) {
                html.append("<table style=\"border-collapse: collapse; width: 100%; margin-bottom: 12px;\">");
                for (XWPFTableRow row : table.getRows()) {
                    html.append("<tr>");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        html.append("<td style=\"border: 1px solid #e4e7ec; padding: 6px; font-size: 13px;\">")
                                .append(escapeHtml(cell.getText()))
                                .append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
            }
        } catch (IOException e) {
            html.append("<p>Unable to render this document.</p>");
        }

        html.append("</div>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
