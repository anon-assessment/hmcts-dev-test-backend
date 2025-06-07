package uk.gov.hmcts.reform.dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest()
public class CaseTest {

    CaseRepository caseRepository;

    public CaseTest(@Autowired CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @Test
    public void caseLifecycleTest(){
        Case c = new Case("number", "title",
                          "description", "status", LocalDateTime.now());

        assertDoesNotThrow(() -> caseRepository.save(c), "Could not save case");

        assertNotNull(c.getId(), "Case id not automatically provided");

        c.setCaseNumber("newNumber");
        try{
            caseRepository.save(c);
        }catch (Exception ignored){
            fail("Case could not be saved over");
        }

        Optional<Case> b = caseRepository.findById(c.getId());

        assertTrue(b.isPresent(), "Could not fetch case by id after save and update");

        assertEquals(c, b.get(), "Fetched differing case after save and update");

        assertDoesNotThrow(() -> caseRepository.deleteById(c.getId()), "Could not delete case by id");

        b = caseRepository.findById(c.getId());

        assertFalse(b.isPresent(), "Case found after deletion");

    }

    @Test
    public void caseSearchTest() {
        for(int i=0; i<10; i++){
            Case c = new Case("same-"+i, "title",
                              "description", "status", LocalDateTime.now());
            caseRepository.save(c);
        }
        // To test searching functionality
        Case different = new Case("different", "title",
                                  "description", "status", LocalDateTime.now());
        caseRepository.save(different);

        Page<Case> page = caseRepository.searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(
            null, null, "same", Pageable.unpaged()
        );

        assertEquals(10, page.getTotalElements(), "Search included invalid result");

        page = caseRepository.searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(
            null, null, "",  Pageable.unpaged()
        );

        assertEquals(11, page.getTotalElements(), "Blank search did not return all elements");

        page = caseRepository.searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(
            null, null, null, Pageable.unpaged()
        );

        assertEquals(0, page.getTotalElements(), "Null search returned elements");

        page = caseRepository.searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(
            null, "title", null, Pageable.unpaged()
        );

        assertEquals(11, page.getTotalElements(), "Broad search did not return all elements (title)");

    }

}
