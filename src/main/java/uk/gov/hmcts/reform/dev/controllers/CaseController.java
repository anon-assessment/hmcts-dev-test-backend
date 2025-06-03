package uk.gov.hmcts.reform.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

@RestController()
public class CaseController {

    CaseRepository caseRepository;

    /**
     * Event listener for app startup generating example cases for frontend testing
     * <br>
     * TODO: Remove/adapt as unit testing only data
     *
     * @param event Unused startup event
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        for(int i = 0; i < 25; i++){
            Case c = new Case("ABC12345-"+i, "Case Title",
                              "Case Description", "Case Status", LocalDateTime.now());
            for(int j = 0; j < new Random().nextInt(5); j++){
                c.addTask(new Task("Task Title-"+i+"-"+j, "Task Description", "Task Status",
                                             LocalDateTime.now(), c));
            }
            caseRepository.save(c);
        }
    }

    /**
     * Controller constructor, autowires case repository for managing cases
     *
     * @param caseRepository Autowired CrudRepository for cases
     */
    public CaseController(@Autowired CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    /**
     * Endpoint to return sample case
     *
     * @return HTTP Ok with example case data
     */
    @GetMapping(value = "/get-example-case", produces = "application/json")
    public ResponseEntity<Case> getExampleCase() {
        return ok(new Case(UUID.fromString("88f3823a-6927-41e8-9f39-a8f93a825630"), "ABC12345", "Case Title",
                           "Case Description", "Case Status", LocalDateTime.now(), List.of()
        ));
    }

    /**
     * Endpoint to create a case in the database
     *
     * @param caseDetails DTO of the case to be created (should not have id)
     * @return HTTP Ok with new case DTO
     */
    @PostMapping(value = "/case", produces = "application/json")
    public ResponseEntity<Case> createCase(@RequestBody Case caseDetails) {
        caseRepository.save(caseDetails);
        return ok(caseDetails);
    }

    /**
     * Endpoint to get a single case
     *
     * @param id UUID of case to fetch
     * @return HTTP Ok with case requested, else HTTP Not Found if case doesn't exist
     */
    @GetMapping(value = "/case/{id}", produces = "application/json")
    public ResponseEntity<Case> getCase(@PathVariable UUID id) {
        Optional<Case> optionalCase = caseRepository.findById(id);
        return optionalCase.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
    }

    /**
     * Endpoint to delete case by id
     *
     * @param id UUID of the case to be deleted
     * @return HTTP Ok with boolean true (case existed, now deleted) or false (case did not exist)
     */
    @DeleteMapping(value = "/case", produces = "application/json")
    public ResponseEntity<Boolean> deleteCase(@RequestParam UUID id) {
        if (caseRepository.existsById(id)) {
            caseRepository.deleteById(id);
        }else{
            return ok(false);
        }
        return ok(true);
    }

    /**
     * Endpoint to create a list of cases
     *
     * @param cases List of cases to create
     * @return HTTP Ok with list of created cases (now with internal ids)
     */
    @PostMapping(value = "/case/list", produces = "application/json")
    public ResponseEntity<List<Case>> getCaseList(@RequestBody List<Case> cases) {
        caseRepository.saveAll(cases);
        return ok(cases);
    }

    /**
     * Endpoint to fetch all cases
     *
     * @return HTTP Ok List of cases that currently exist in the database
     */
    @GetMapping(value = "/case/list", produces = "application/json")
    public ResponseEntity<Iterable<Case>> getAllCases() {
        return ok(caseRepository.findAll());
    }

    /**
     * Endpoint to update individual case status by id
     * <br>
     * <i>Note: potential need for validation to avoid null status?</i>
     *
     * @param id UUID of case to update
     * @param status New status for specified case
     * @return HTTP Ok with updated case DTO, HTTP Not Found if case doesn't exist with id
     */
    @PostMapping(value = "/case/{id}/status", produces = "application/json")
    public ResponseEntity<Case> updateCaseStatus(@PathVariable UUID id, @RequestParam String status) {
        Optional<Case> caseOptional = caseRepository.findById(id);
        if (caseOptional.isPresent()) {
            caseOptional.get().setStatus(status);
            caseRepository.save(caseOptional.get());
            return ok(caseOptional.get());
        }else{
            return notFound().build();
        }
    }


    /**
     * Initial implementation of paginated searching to find a case, sorted by title
     * <br>
     * TODO: Must be switched to a strictly defined format, see Spring HATEOAS/HAL docs
     *
     * @param searchString The string to search by, if it is a UUID it will automatically be used, otherwise
     *                     results where the title/number contain the value (non-case sensitive).
     * @param pageNumber Number of the current page TODO: swap to HATEOAS/HAL
     * @param pageSize Size of pages being returned TODO: swap to HATEOAS/HAL
     * @return Page containing information about result set and the page content requested or bad request
     */
    @PostMapping(value = "/case/search")
    public ResponseEntity<?> searchCase(@RequestParam String searchString,
                                                 @RequestParam Integer pageNumber,
                                                 @RequestParam Integer pageSize) {
        if(pageSize < 1){
            return badRequest().body("Page size must be at least 1");
        }else if(pageNumber < 0){
            return badRequest().body("Page number must be greater than 0");
        }
        UUID id = null;
        try {
            id = UUID.fromString(searchString);
        }catch(IllegalArgumentException ignored){
        }

        return ok(
            caseRepository.searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(
                id,
                searchString,
                searchString,
                PageRequest.of(pageNumber, pageSize, Sort.by("title"))
            )
        );
    }

}
