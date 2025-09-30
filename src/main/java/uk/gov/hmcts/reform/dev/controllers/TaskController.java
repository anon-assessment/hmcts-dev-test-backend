package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.dto.TaskDto;
import uk.gov.hmcts.reform.dev.services.DAOService;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

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
     * Get individual Task by ID
     *
     * @param id ID to fetch Task by
     * @return HTTP OK containing found Task or HTTP Not Found if no Task by id
     */
    @Operation(summary = "Get a Task by ID", description = "Gets an individual Task by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task matching given ID"),
        @ApiResponse(responseCode = "404", description = "Task with given ID not found")
    })
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
    @Operation(
        summary = "Finds Tasks by their parent Case",
        description = "Returns pageable of Tasks associated with specified parent ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pageable containing any matching Tasks")
    })
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
    @Operation(summary = "Creates a Task", description = "Creates a Task from a given body")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task with internal ID post-save"),
        @ApiResponse(responseCode = "400", description = "Either no parent case provided or no such parent case")
    })
    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody TaskDto task) {
        try{
            return ok(daoService.saveTask(task));
        }catch (NoSuchElementException e){
            return badRequest().body("Could not save task, no case with ID "+task.getParentCase());
        }catch (IllegalArgumentException e){
            return badRequest().body("Could not save task, no parent case provided");
        }
    }

    /**
     * Delete task by ID
     *
     * @param id ID for task to delete
     * @return HTTP OK after deletion
     */
    @Operation(summary = "Delete Task by ID", description = "Deletes a Task matching ID provided")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Either Task with that ID never existed or was deleted")
    })
    @DeleteMapping("/task/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable UUID id) {
        daoService.deleteTask(id);
        return ok().build();
    }

    /**
     * Endpoint to update individual task property by id
     * <br>
     *
     * @param id UUID of task to update
     * @param value New value for specified task property
     * @param property Name of property to update
     * @return HTTP Ok with updated task DTO, HTTP Bad Request if task doesn't exist with id or could not update
     */
    @Operation(
        summary = "Endpoint to update individual task property by ID",
        description = "Updates a Task by ID's property with the specified value"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task with property updated"),
        @ApiResponse(responseCode = "400", description = "Could not update specified Task property or Task doesn't exist")
    })
    @PostMapping(value = "/task/{id}/{property}", produces = "application/json")
    public ResponseEntity<?> updateProperty(@PathVariable UUID id, @PathVariable String property,
                                            @RequestParam String value) {
        try {
            return ok(daoService.updateTaskProperty(id, value, property));
        }catch (IllegalArgumentException e){
            return badRequest().body("Could not update: "+e.getMessage());
        }
    }

}
