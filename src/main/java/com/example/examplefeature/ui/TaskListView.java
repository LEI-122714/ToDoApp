package com.example.examplefeature.ui;

import com.example.base.ui.component.ViewToolbar;
import com.example.examplefeature.QRCodeGenerator;
import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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

    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Grid<Task> taskGrid;

    TaskListView(TaskService taskService) {
        this.taskService = taskService;

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

        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn)));
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
    }

    private void createTask() {
        taskService.createTask(description.getValue(), dueDate.getValue());
        taskGrid.getDataProvider().refreshAll();
        description.clear();
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

}
