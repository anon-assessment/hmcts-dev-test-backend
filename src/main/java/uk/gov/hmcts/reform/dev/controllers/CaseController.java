package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final DAOService daoService;

    private final PagedResourcesAssembler<CaseDto> assembler;

    /**
     * Controller constructor, autowires components for operations
     */
    public CaseController(@Autowired DAOService daoService,
                          @Autowired PagedResourcesAssembler<CaseDto> assembler) {
        this.daoService = daoService;
        this.assembler = assembler;
    }

    /**
     * Endpoint to return sample case
     *
     * @return HTTP Ok with example case data
     */
    @Operation(summary = "Gets the default example Case", description = "Gets a default example case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Example Case object")
    })
    @GetMapping(value = "/get-example-case", produces = "application/json")
    public ResponseEntity<CaseDto> getExampleCase() {
        return ok(new CaseDto(UUID.fromString("88f3823a-6927-41e8-9f39-a8f93a825630"), "ABC12345", "Case Title",
                           "Case Description", "Case Status", LocalDateTime.now(), List.of()
        ));
    }

    /**
     * Endpoint to create a case in the database
     *
     * @param caseDetails DTO of the case to be created (should not have id)
     * @return HTTP Ok with new case DTO or HTTP Bad Request if case invalid
     */
    @Operation(
        summary = "Endpoint to create a case in the database",
        description = "Creates a Case from body and returns it after save operation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns Case with internal ID set from save"),
        @ApiResponse(
            responseCode = "400",
            description = "Error in either the body of the case or a duplicate case number"
        )
    })
    @PostMapping(value = "/case", produces = "application/json")
    public ResponseEntity<?> createCase(@RequestBody CaseDto caseDetails) {
        if(caseDetails.getCreatedDate() == null){
            caseDetails.setCreatedDate(LocalDateTime.now());
        }

        try {
            return ok(daoService.saveCase(caseDetails));
        }catch(IllegalArgumentException | DataIntegrityViolationException e){
            return badRequest().body(e.getMessage());
        }
    }

    /**
     * Endpoint to get a single case by ID
     *
     * @param id UUID of case to fetch
     * @return HTTP Ok with case requested, else HTTP Not Found if case doesn't exist
     */
    @Operation(summary = "Endpoint to get a single case by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The found Case object matching given ID"),
        @ApiResponse(responseCode = "404", description = "Case with ID not found")
    })
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
    @Operation(summary = "Endpoint to delete a case by id", description = "Deletes a case with the given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Either the case was deleted or never existed")
    })
    @DeleteMapping(value = "/case/{id}", produces = "application/json")
    public ResponseEntity<?> deleteCase(@PathVariable UUID id) {
        daoService.deleteCase(id);
        return ok().build();
    }

    /**
     * Endpoint to create a list of cases
     *
     * @param cases List of cases to create
     * @return HTTP Ok with list of created cases (now with internal ids) or Bad Request
     */
    @Operation(summary = "Endpoint to create multiple cases from a list", description = "Saves all cases and returns")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cases successfully saved"),
        @ApiResponse(responseCode = "400", description = "Issue with saving cases, aborted")
    })
    @PostMapping(value = "/case/list", produces = "application/json")
    public ResponseEntity<?> postCaseList(@RequestBody List<CaseDto> cases) {
        try {
            return ok(daoService.saveCases(cases));
        }catch(IllegalArgumentException | DataIntegrityViolationException e){
            return badRequest().body(e.getMessage());
        }
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
    @Operation(
        summary = "Endpoint to update individual case property by ID",
        description = "Updates a specified case property for an ID, a property name and a new value, optionally parsed"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case with property updated"),
        @ApiResponse(responseCode = "400", description = "Either case not found or could not update")
    })
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
    @Operation(
        summary = "Search Cases by ID/title/case number",
        description =
            "Searches non-case sensitive by title and number unless the value is a valid UUID, in which case also by that"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pageable object with page of results")
    })
    @PostMapping(value = "/case/search")
    public ResponseEntity<?> searchCase(@RequestParam String searchString,
                                        Pageable pageable) {
        return ok(assembler.toModel(
            daoService.searchCases(searchString, pageable)
        ));
    }

}
