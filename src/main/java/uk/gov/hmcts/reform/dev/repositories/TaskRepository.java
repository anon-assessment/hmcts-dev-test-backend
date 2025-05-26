package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.dev.models.Task;

import java.util.UUID;

public interface TaskRepository extends CrudRepository<Task, UUID> {
}
