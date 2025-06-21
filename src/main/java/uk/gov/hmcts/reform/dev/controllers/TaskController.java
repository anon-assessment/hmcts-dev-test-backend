package uk.gov.hmcts.reform.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.dto.TaskDto;
import uk.gov.hmcts.reform.dev.services.DAOService;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Routes for CRUD operations on the Task data
 */
@RestController
public class TaskController {

    private final DAOService daoService;
    private final PagedResourcesAssembler<TaskDto> assembler;

    public TaskController(@Autowired DAOService daoService,
                          @Autowired PagedResourcesAssembler<TaskDto> assembler) {
        this.daoService = daoService;
        this.assembler = assembler;
    }

    /**
     * Get individual task by ID
     *
     * @param id ID to fetch task by
     * @return HTTP OK containing found task or HTTP Not Found if no task by id
     */
    @GetMapping("/task/{id}")
    public ResponseEntity<?> getTask(@PathVariable UUID id) {
        Optional<TaskDto> task = daoService.getTask(id);
        if (task.isPresent()) {
            return ok(task.get());
        }else{
            return notFound().build();
        }
    }

    /**
     * Get tasks attached to a specific case
     *
     * @param id ID of the case to find tasks from
     * @param pageable Pageable parameters (pageNumber, pageSize and sort) for traversing page set.
     * @return PagedModel containing info about page and any results in _embedded
     */
    @GetMapping("/task/forCase/{id}")
    public ResponseEntity<?> getTasksForCase(@PathVariable UUID id, Pageable pageable) {
        return ok(assembler.toModel(
            daoService.getTasksForParent(id, pageable))
        );
    }

    /**
     * Creates a task from JSON specification
     *
     * @param task Task JSON to create from, id will be ignored
     * @return HTTP OK with the created task object
     */
    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody TaskDto task) {
        return ok(daoService.saveTask(task));
    }

    /**
     * Delete task by ID
     *
     * @param id ID for task to delete
     * @return HTTP OK after deletion
     */
    @DeleteMapping("/task")
    public ResponseEntity<?> deleteTask(@RequestParam UUID id) {
        daoService.deleteTask(id);
        return ok().build();
    }

    /**
     * Update task with JSON, will be identified by task ID
     *
     * @param task Task object to update, must contain ID
     * @return HTTP OK with update task object
     */
    @PutMapping("/task")
    public ResponseEntity<?> updateTask(@RequestBody TaskDto task) {
        return ok(daoService.saveTask(task));
    }

    /**
     * Update task status by ID
     *
     * @param id ID of task to update
     * @param status New status for task
     * @return HTTP OK with new task or HTTP Not Found if ID not in database
     */
    @PostMapping("/task/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable UUID id, @RequestBody String status) {
        try{
            return ok(daoService.updateTaskStatus(id, status));
        }catch (IllegalArgumentException e) {
            return notFound().build();
        }
    }

}
