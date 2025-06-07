package uk.gov.hmcts.reform.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.services.DAOService;

import static org.springframework.http.ResponseEntity.*;

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
            System.err.println(e.getMessage());
            return internalServerError().build();
        }
    }

}
