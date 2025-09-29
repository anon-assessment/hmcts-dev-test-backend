package uk.gov.hmcts.reform.dev.controllers;

import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/testData")
    public ResponseEntity<?> testData() {
        try {
            daoService.loadTestData();
            return ok().build();
        } catch (Exception e) {
            // Handle double press
            if(e instanceof ConstraintViolationException){
                return ok().build();
            }
            return internalServerError().build();
        }
    }

    @PostMapping("/clearTestData")
    public ResponseEntity<?> clearTestData() {
        try{
            daoService.clearTestData();
            return ok().build();
        } catch (Exception e) {
            if(e instanceof ConstraintViolationException){
                return ok().build();
            }
            return internalServerError().build();
        }
    }


}
