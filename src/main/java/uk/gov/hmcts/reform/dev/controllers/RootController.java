package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.services.DAOService;

import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class RootController {

    DAOService daoService;

    public RootController(@Autowired DAOService daoService) {
        this.daoService = daoService;
    }

    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to test-backend");
    }

    @Operation(summary = "Loads test data into the database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Either test data already in DB or was loaded successfully"),
        @ApiResponse(responseCode = "500", description = "Unknown error occurred")
    })
    @PostMapping("/testData")
    public ResponseEntity<?> testData() {
        try {
            daoService.loadTestData();
            return ok().build();
        } catch (Exception e) {
            // Handle double press
            if(e instanceof ConstraintViolationException || e instanceof DataIntegrityViolationException){
                return ok().build();
            }
            return internalServerError().build();
        }
    }

    @Operation(summary = "Clears test data from the database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Either test data wasn't in DB or was cleared successfully"),
        @ApiResponse(responseCode = "500", description = "Unknown error occurred")
    })
    @PostMapping("/clearTestData")
    public ResponseEntity<?> clearTestData() {
        try{
            daoService.clearTestData();
            return ok().build();
        } catch (Exception e) {
            if(e instanceof ConstraintViolationException || e instanceof DataIntegrityViolationException){
                return ok().build();
            }

            return internalServerError().build();
        }
    }


}
