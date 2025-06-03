package uk.gov.hmcts.reform.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

@RestController
public class TaskController {

    TaskRepository taskRepository;

    public TaskController(@Autowired TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/task/{id}")
    public ResponseEntity<?> getTask(@PathVariable UUID id) {
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            return ok(task.get());
        }else{
            return notFound().build();
        }
    }

    @GetMapping("/task/forCase/{id}")
    public ResponseEntity<?> getTaskForCase(@PathVariable UUID id) {
        return ok(taskRepository.findAllByParentCaseId(id));
    }

    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        taskRepository.save(task);
        return ok(task);
    }

    @PutMapping("/task")
    public ResponseEntity<?> updateTask(@RequestBody Task task) {
        taskRepository.save(task);
        return ok(task);
    }

    @PostMapping("/task/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable UUID id, @RequestBody String status) {
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            task.get().setStatus(status);
            taskRepository.save(task.get());
            return ok(task);
        }else {
            return notFound().build();
        }
    }

}
