package com.example.examplefeature;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    TaskService(TaskRepository taskRepository) {

        this.taskRepository = taskRepository;
    }

    @Transactional
    public void createTask(String description, @Nullable LocalDate dueDate) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }
        var task = new Task(description, Instant.now());
        task.setDueDate(dueDate);
        taskRepository.saveAndFlush(task);
    }

    @Transactional(readOnly = true)
    public List<Task> list(Pageable pageable) {

        return taskRepository.findAllBy(pageable).toList();
    }

    @Transactional(readOnly = true)
    public List<Task> findAll() {
        // 使用 TaskRepository 的 findAll 方法，并传入排序参数
        // 按 creationDate 降序排列
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "creationDate"));
    }

}
