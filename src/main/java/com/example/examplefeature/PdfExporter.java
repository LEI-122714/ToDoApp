package com.example.examplefeature;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
// 注意：PDFBox 3.x 版本的字体路径已更改
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 标记为 Spring 服务，以便可以被注入
@Service
public class PdfExporter {

    private final TaskService taskService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 依赖注入 TaskService
    public PdfExporter(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 生成包含所有任务列表的 PDF 字节数组。
     * @return PDF 文件的字节数组。
     * @throws IOException 如果 PDFBox 写入过程中发生错误。
     */
    public byte[] exportAllTasksToPdf() throws IOException {

        List<Task> todos = taskService.findAll();

        // 1. 外部 try-with-resources 只管理 PDDocument 和 ByteArrayOutputStream
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 定义常量和初始页
            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font FONT_BOLD = new PDType1Font(FontName.HELVETICA_BOLD);
            PDType1Font FONT_NORMAL = new PDType1Font(FontName.HELVETICA);

            final int MARGIN = 50;
            final int LINE_HEIGHT = 18;
            int yPosition = 750;

            // 2. 在循环外部声明并初始化 contentStream (手动管理关闭)
            PDPageContentStream contentStream = null;

            try {
                // 3. 第一次创建内容流
                contentStream = new PDPageContentStream(document, page);

                // 写入标题（仅需一次）
                contentStream.setFont(FONT_BOLD, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("Relatório Completo de Tarefas");
                contentStream.endText();

                yPosition -= 40;
                contentStream.setFont(FONT_NORMAL, 10);

                // 4. 循环写入任务列表
                for (Task task : todos) {

                    // 检查是否需要换页
                    if (yPosition < MARGIN) {

                        // a) 关闭当前页面的内容流
                        if (contentStream != null) {
                            contentStream.close();
                        }

                        // b) 创建新页面
                        page = new PDPage();
                        document.addPage(page);

                        // c) 创建新的内容流 (重新赋值，但 contentStream 不再是 final 变量)
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(FONT_NORMAL, 10);
                        yPosition = 750; // 重置 y 坐标
                    }

                    // ... (写入任务内容的逻辑保持不变)
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
                // 5. 确保在退出 try 块时关闭最后一个 contentStream
                if (contentStream != null) {
                    contentStream.close();
                }
            }

            // 6. 保存文档到字节数组并返回
            document.save(baos);
            return baos.toByteArray();
        }
    }
}