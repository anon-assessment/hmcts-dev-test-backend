package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.dev.models.Case;

import java.util.UUID;

public interface CaseRepository extends CrudRepository<Case, UUID> {

}
