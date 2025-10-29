package com.example.examplefeature;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExporter {

    private final TaskService taskService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PdfExporter(TaskService taskService) {
        this.taskService = taskService;
    }

    public byte[] exportAllTasksToPdf() throws IOException {

        List<Task> todos = taskService.findAll();

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font FONT_BOLD = new PDType1Font(FontName.HELVETICA_BOLD);
            PDType1Font FONT_NORMAL = new PDType1Font(FontName.HELVETICA);

            final int MARGIN = 50;
            final int LINE_HEIGHT = 18;
            int yPosition = 750;

            PDPageContentStream contentStream = null;

            try {
                contentStream = new PDPageContentStream(document, page);

                contentStream.setFont(FONT_BOLD, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("Relatório Completo de Tarefas");
                contentStream.endText();

                yPosition -= 40;
                contentStream.setFont(FONT_NORMAL, 10);

                for (Task task : todos) {

                    if (yPosition < MARGIN) {

                        if (contentStream != null) {
                            contentStream.close();
                        }

                        page = new PDPage();
                        document.addPage(page);

                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(FONT_NORMAL, 10);
                        yPosition = 750;
                    }

                    String dueDateStr = (task.getDueDate() != null)
                            ? "Data Limite: " + task.getDueDate().format(DATE_FORMATTER)
                            : "Sem Data Limite";

                    String line = String.format("ID %s: %s | %s",
                            task.getId() != null ? task.getId().toString() : "?",
                            task.getDescription(),
                            dueDateStr);

                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN, yPosition);
                    contentStream.showText(line);
                    contentStream.endText();

                    yPosition -= LINE_HEIGHT;
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }
}