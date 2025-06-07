package uk.gov.hmcts.reform.dev.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.dto.CaseDto;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.services.DAOService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

@RestController()
public class CaseController {

    CaseRepository caseRepository;

    private final DAOService daoService;

    /**
     * Controller constructor, autowires case repository for managing cases
     *
     * @param caseRepository Autowired CrudRepository for cases
     */
    public CaseController(@Autowired CaseRepository caseRepository, @Autowired DAOService daoService) {
        this.caseRepository = caseRepository;
        this.daoService = daoService;
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
    public ResponseEntity<?> createCase(@RequestBody CaseDto caseDetails) {
        if(caseDetails.getCreatedDate() == null){
            caseDetails.setCreatedDate(LocalDateTime.now());
        }

        return ok(daoService.saveCase(caseDetails));
    }

    /**
     * Endpoint to get a single case
     *
     * @param id UUID of case to fetch
     * @return HTTP Ok with case requested, else HTTP Not Found if case doesn't exist
     */
    @GetMapping(value = "/case/{id}", produces = "application/json")
    public ResponseEntity<?> getCase(@PathVariable UUID id) {
        Optional<CaseDto> optionalCase = daoService.getCase(id);
        return optionalCase.map(ResponseEntity::ok).orElseGet(() -> notFound().build());
    }

    /**
     * Endpoint to delete case by id
     *
     * @param id UUID of the case to be deleted
     * @return HTTP Ok with boolean true (case existed, now deleted) or false (case did not exist)
     */
    @DeleteMapping(value = "/case", produces = "application/json")
    public ResponseEntity<?> deleteCase(@RequestParam UUID id) {
        daoService.deleteCase(id);
        return ok().build();
    }

    /**
     * Endpoint to create a list of cases
     *
     * @param cases List of cases to create
     * @return HTTP Ok with list of created cases (now with internal ids)
     */
    @PostMapping(value = "/case/list", produces = "application/json")
    public ResponseEntity<?> getCaseList(@RequestBody List<CaseDto> cases) {
        return ok(daoService.saveCases(cases));
    }

    /**
     * Endpoint to fetch all cases
     *
     * @return HTTP Ok List of cases that currently exist in the database
     * @deprecated Removing due to no reasonable, scalable use for such functionality
     */
    @GetMapping(value = "/case/list", produces = "application/json")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> getAllCases() {
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
    public ResponseEntity<?> updateCaseStatus(@PathVariable UUID id, @RequestParam String status) {
        try {
            return ok(daoService.updateCaseStatus(id, status));
        }catch (IllegalArgumentException e){
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
                                                 @RequestParam Integer pageSize) throws JsonProcessingException {
        if(pageSize < 1){
            return badRequest().body("Page size must be at least 1");
        }else if(pageNumber < 0){
            return badRequest().body("Page number must be greater than 0");
        }
        return ok(
            daoService.searchCases(searchString, PageRequest.of(pageNumber, pageSize, Sort.by("title")))
        );
    }

}
