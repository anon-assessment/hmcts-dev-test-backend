package uk.gov.hmcts.reform.dev.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class CaseController {

    CaseRepository caseRepository;

    public CaseController(@Autowired CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @GetMapping(value = "/get-example-case", produces = "application/json")
    public ResponseEntity<Case> getExampleCase() {
        return ok(new Case(UUID.fromString("88f3823a-6927-41e8-9f39-a8f93a825630"), "ABC12345", "Case Title",
                           "Case Description", "Case Status", LocalDateTime.now()
        ));
    }

    @PostMapping(value = "/case", produces = "application/json")
    public ResponseEntity<Case> createCase(@RequestBody Case caseDetails) {
        caseRepository.save(caseDetails);
        return ok(caseDetails);
    }

    @PutMapping(value = "/case")
    public ResponseEntity<Case> updateCase(@RequestBody Case caseDetails) {
        caseRepository.save(caseDetails);
        return ok(caseDetails);
    }

    @GetMapping(value = "/case")
    public ResponseEntity<Case> getCase(@RequestParam UUID id) {
        return ok(caseRepository.findById(id).orElseThrow());
    }

    @DeleteMapping(value = "/case")
    public ResponseEntity<Boolean> deleteCase(@RequestParam UUID id) {
        if (caseRepository.existsById(id)) {
            caseRepository.deleteById(id);
        }else{
            return ok(false);
        }
        return ok(true);
    }

    @PostMapping("/case/list")
    public ResponseEntity<List<Case>> getCaseList(@RequestBody List<Case> cases) {
        caseRepository.saveAll(cases);
        return ok(cases);
    }

}
