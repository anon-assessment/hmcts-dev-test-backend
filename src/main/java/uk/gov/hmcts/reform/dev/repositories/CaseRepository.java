package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.dev.models.Case;

import java.util.Optional;
import java.util.UUID;

/**
 * Automatically generates all necessary underlying requests to CRUD operate
 * upon Case (tasks) table.
 */
public interface CaseRepository extends CrudRepository<Case, UUID> {

    Page<Case> searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(UUID id, String title,
                                                                                     String caseNumber,
                                                                                     Pageable pageable);

    Optional<Case> findFirstByCaseNumber(String caseNumber);


}
