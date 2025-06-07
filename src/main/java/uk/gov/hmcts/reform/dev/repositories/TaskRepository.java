package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.dev.models.Task;

import java.util.UUID;

public interface TaskRepository extends CrudRepository<Task, UUID> {

    Page<Task> findAllByParentCaseId(UUID id, Pageable pageable);

    Page<Task> searchByIdOrTitleContainingIgnoreCase(UUID id, String title, Pageable pageable);

}
