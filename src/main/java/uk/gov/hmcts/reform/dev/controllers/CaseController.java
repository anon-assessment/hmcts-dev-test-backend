package uk.gov.hmcts.reform.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.dto.CaseDto;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.services.DAOService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.*;

/**
 * Routes for CRUD operations on the Case data
 */
@RestController()
public class CaseController {

    CaseRepository caseRepository;

    private final DAOService daoService;

    private final PagedResourcesAssembler<CaseDto> assembler;

    /**
     * Controller constructor, autowires components for operations
     */
    public CaseController(@Autowired CaseRepository caseRepository, @Autowired DAOService daoService,
                          @Autowired PagedResourcesAssembler<CaseDto> assembler) {
        this.caseRepository = caseRepository;
        this.daoService = daoService;
        this.assembler = assembler;
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
     * @return HTTP Ok with new case DTO or HTTP Bad Request if case invalid
     */
    @PostMapping(value = "/case", produces = "application/json")
    public ResponseEntity<?> createCase(@RequestBody CaseDto caseDetails) {
        if(caseDetails.getCreatedDate() == null){
            caseDetails.setCreatedDate(LocalDateTime.now());
        }

        try {
            return ok(daoService.saveCase(caseDetails));
        }catch(IllegalArgumentException e){
            return badRequest().body(e.getMessage());
        }
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
    @DeleteMapping(value = "/case/{id}", produces = "application/json")
    public ResponseEntity<?> deleteCase(@PathVariable UUID id) {
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
     * Endpoint to update individual case property by id
     * <br>
     *
     * @param id UUID of case to update
     * @param value New value for specified case property
     * @param property Name of property to update
     * @return HTTP Ok with updated case DTO, HTTP Bad Request if case doesn't exist with id or could not update
     */
    @PostMapping(value = "/case/{id}/{property}", produces = "application/json")
    public ResponseEntity<?> updateProperty(@PathVariable UUID id, @PathVariable String property,
                                            @RequestParam String value) {
        try {
            return ok(daoService.updateCaseProperty(id, value, property));
        }catch (IllegalArgumentException e){
            return new ResponseEntity<>("Could not update: "+e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * Initial implementation of paginated searching to find a case, sorted by title
     * <br>
     * TODO: Must be switched to a strictly defined format, see Spring HATEOAS/HAL docs
     *
     * @param searchString The string to search by, if it is a UUID it will automatically be used, otherwise
     *                     results where the title/number contain the value (non-case sensitive).
     * @param pageable Pageable parameters (pageNumber, pageSize and sort) for traversing page set.
     * @return PagedModel containing info about page and any results in _embedded
     */
    @PostMapping(value = "/case/search")
    public ResponseEntity<?> searchCase(@RequestParam String searchString,
                                        Pageable pageable) {

        return ok(assembler.toModel(
            daoService.searchCases(searchString, pageable)
        ));
    }

}
