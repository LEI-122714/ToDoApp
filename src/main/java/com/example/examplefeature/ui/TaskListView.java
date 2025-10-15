package com.example.examplefeature.ui;

import com.example.examplefeature.EmailService;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.ButtonVariant;

import com.example.base.ui.component.ViewToolbar;
import com.example.examplefeature.QRCodeGenerator;
import com.example.examplefeature.PdfExporter; // 导入 PdfExporter
import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.vaadin.flow.component.UI; // 导入 UI
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon; // 导入 VaadinIcon
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Base64; // 导入 Base64
import java.util.Optional;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;


import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("")
@PageTitle("Task List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Task List")
class TaskListView extends Main {

    private final TaskService taskService;
    private final PdfExporter pdfExporter; // 注入依赖

    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Grid<Task> taskGrid;
    final Button exportPdfBtn; // 声明导出按钮

    // 构造函数中同时注入 TaskService 和 PdfExporter
    TaskListView(TaskService taskService, PdfExporter pdfExporter) {
        this.taskService = taskService;
        this.pdfExporter = pdfExporter;

        description = new TextField();
        description.setPlaceholder("What do you want to do?");
        description.setAriaLabel("Task description");
        description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("20em");

        dueDate = new DatePicker();
        dueDate.setPlaceholder("Due date");
        dueDate.setAriaLabel("Due date");

        createBtn = new Button("Create", event -> createTask());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // 新增：PDF 导出按钮，点击时调用 exportPdf 方法
        exportPdfBtn = new Button("Export PDF", VaadinIcon.FILE_TEXT.create(), event -> exportPdf());
        exportPdfBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(getLocale())
                .withZone(ZoneId.systemDefault());
        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

        taskGrid = new Grid<>();
        taskGrid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        taskGrid.addColumn(Task::getDescription).setHeader("Description");
        taskGrid.addColumn(task -> Optional.ofNullable(task.getDueDate()).map(dateFormatter::format).orElse("Never"))
                .setHeader("Due Date");
        taskGrid.addColumn(task -> dateTimeFormatter.format(task.getCreationDate())).setHeader("Creation Date");
        taskGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        // 将新的导出按钮添加到工具栏
        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn, exportPdfBtn)));
        add(taskGrid);


        taskGrid.addComponentColumn(task -> {
            Button qrButton = new Button("QR Code", e -> {
                // Construir conteúdo robusto do QR
                StringBuilder sb = new StringBuilder();
                sb.append("Task ID: ").append(task.getId() == null ? "N/A" : task.getId()).append("\n");
                sb.append("Description: ").append(task.getDescription() == null ? "" : task.getDescription()).append("\n");
                if (task.getDueDate() != null) {
                    sb.append("Due Date: ").append(task.getDueDate().toString()).append("\n");
                }
                sb.append("Created: ").append(task.getCreationDate() == null ? "N/A" : task.getCreationDate().toString());

                String qrText = sb.toString();

                // DEBUG: mostra o texto numa notificação curta (útil para confirmar que não está vazio)
                Notification note = Notification.show(qrText, 3000, Notification.Position.BOTTOM_END);
                note.addThemeVariants(NotificationVariant.LUMO_CONTRAST);

                // Gerar QR e mostrar num dialog
                String qrBase64 = QRCodeGenerator.generateQRCodeImage(qrText, 300, 300);

                Image qrImage = new Image(qrBase64, "QR Code");
                qrImage.setWidth("260px");
                qrImage.setHeight("260px");

                VerticalLayout content = new VerticalLayout();
                content.setPadding(false);
                content.setSpacing(false);
                content.add(qrImage);

                // Opcional: mostra também o texto em texto pré-formatado (útil para cópia)
                Pre pre = new Pre(qrText);
                pre.getStyle().set("white-space", "pre-wrap");
                content.add(pre);

                Dialog dialog = new Dialog(content);
                dialog.setWidth("320px");
                dialog.setHeight("420px");
                dialog.setCloseOnOutsideClick(true);
                dialog.open();
            });
            qrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return qrButton;
        }).setHeader("QR Code");
        taskGrid.addComponentColumn(task -> {
            Button emailButton = new Button("Enviar Email", e -> {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Enviar tarefa por email");

                // Apenas o campo de destinatário
                EmailField emailField = new EmailField("Destinatário");
                emailField.setPlaceholder("exemplo@email.com");
                emailField.setWidthFull();

                // Botão de envio
                Button sendButton = new Button("Enviar", sendEvent -> {
                    if (emailField.getValue() == null || emailField.getValue().isBlank()) {
                        Notification.show("Introduza um email de destino válido.", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    // Corpo do e-mail
                    String body = """
                    Task details:
                    -----------------------
                    Description: %s
                    Due Date: %s
                    Creation Date: %s
                    """.formatted(
                            task.getDescription(),
                            task.getDueDate() != null ? task.getDueDate() : "Sem data",
                            task.getCreationDate()
                    );

                    try {
                        EmailService.sendEmail(
                                emailField.getValue(),
                                "Tarefa Partilhada: " + task.getDescription(),
                                body
                        );

                        Notification.show("Email enviado com sucesso!", 4000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        dialog.close();
                    } catch (Exception ex) {
                        Notification.show("Erro ao enviar email: " + ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                // Layout do diálogo
                VerticalLayout layout = new VerticalLayout(emailField, sendButton);
                layout.setPadding(false);
                dialog.add(layout);
                dialog.open();
            });
            emailButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return emailButton;
        }).setHeader("Email");
    }

    private void createTask() {
        taskService.createTask(description.getValue(), dueDate.getValue());
        taskGrid.getDataProvider().refreshAll();
        description.clear();
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);


    }

    // 新增：使用 Data URI 触发 PDF 下载的方法
    private void exportPdf() {
        try {
            // 1. 在服务器端生成 PDF 字节数组
            byte[] pdfBytes = pdfExporter.exportAllTasksToPdf();

            // 2. 将字节数组转换为 Base64 字符串
            String base64Content = Base64.getEncoder().encodeToString(pdfBytes);

            String mimeType = "application/pdf";
            String fileName = "tasks-report.pdf";

            // 3. 执行 JavaScript 触发下载 (最简洁的非 StreamResource 方案)
            // 该 JS 会在浏览器中创建临时 a 标签，设置 Data URI，模拟点击并移除 a 标签
            UI.getCurrent().getPage().executeJs("""
                const link = document.createElement('a');
                link.href = 'data:' + $0 + ';base64,' + $1;
                link.download = $2;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            """, mimeType, base64Content, fileName); // $0, $1, $2 是参数占位符

        } catch (IOException e) {
            // 导出失败时显示错误通知
            Notification.show("PDF 导出失败: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }

}
