package uk.gov.hmcts.reform.dev.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.dto.TaskDto;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.services.DAOService;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class TaskController {

    TaskRepository taskRepository;
    private final DAOService daoService;

    public TaskController(@Autowired TaskRepository taskRepository, @Autowired DAOService daoService) {
        this.taskRepository = taskRepository;
        this.daoService = daoService;
    }

    @GetMapping("/task/{id}")
    public ResponseEntity<?> getTask(@PathVariable UUID id) {
        Optional<TaskDto> task = daoService.getTask(id);
        if (task.isPresent()) {
            return ok(task.get());
        }else{
            return notFound().build();
        }
    }

    @GetMapping("/task/forCase/{id}")
    public ResponseEntity<?> getTaskForCase(@PathVariable UUID id,
                                            @RequestParam Integer pageNumber,
                                            @RequestParam Integer pageSize) {
        return ok(daoService.getTasksForParent(id, PageRequest.of(pageNumber, pageSize, Sort.by("title"))));
    }

    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody TaskDto task) throws JsonProcessingException {
        return ok(daoService.saveTask(task));
    }

    @DeleteMapping("/task")
    public ResponseEntity<?> deleteTask(@RequestParam UUID id) {
        daoService.deleteTask(id);
        return ok().build();
    }

    @PutMapping("/task")
    public ResponseEntity<?> updateTask(@RequestBody TaskDto task) {
        return ok(daoService.saveTask(task));
    }

    @PostMapping("/task/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable UUID id, @RequestBody String status) {
        try{
            return ok(daoService.updateTaskStatus(id, status));
        }catch (IllegalArgumentException e) {
            return notFound().build();
        }
    }

}
