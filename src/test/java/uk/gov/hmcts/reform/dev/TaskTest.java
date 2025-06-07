package uk.gov.hmcts.reform.dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class TaskTest {

    TaskRepository taskRepository;
    CaseRepository caseRepository;

    public TaskTest(@Autowired TaskRepository taskRepository, @Autowired CaseRepository caseRepository) {
        this.taskRepository = taskRepository;
        this.caseRepository = caseRepository;
    }

    @Test
    public void taskLifecycleTest(){
        Task t = new Task("title",
                          "description", "status", LocalDateTime.now(), null);

        assertDoesNotThrow(() -> taskRepository.save(t), "Could not save task");

        assertNotNull(t.getId(), "Task id not automatically provided");

        try{
            taskRepository.save(t);
        }catch (Exception ignored){
            fail("Task could not be saved over");
        }

        Optional<Task> b = taskRepository.findById(t.getId());

        assertTrue(b.isPresent(), "Could not fetch task by id after save and update");

        assertEquals(t, b.get(), "Fetched differing task after save and update");

        assertDoesNotThrow(() -> taskRepository.deleteById(t.getId()), "Could not delete task by id");

        b = taskRepository.findById(t.getId());

        assertFalse(b.isPresent(), "Task found after deletion");

    }

    @Test
    public void taskSearchTest() {
        Case c = new Case("rootCase", "title",
                          "description", "status", LocalDateTime.now());
        caseRepository.save(c);
        for(int i=0; i<10; i++){
            Task t = new Task("title-"+i,
                              "description", "status", LocalDateTime.now(), c);
            taskRepository.save(t);
        }
        // To test searching functionality
        Task different = new Task("different", "description", "status", LocalDateTime.now(), c);
        taskRepository.save(different);

        Page<Task> page = taskRepository.searchByIdOrTitleContainingIgnoreCase(
            null, "title", Pageable.unpaged()
        );

        assertEquals(10, page.getTotalElements(), "Search included invalid result");

        page = taskRepository.searchByIdOrTitleContainingIgnoreCase(
            null, "",  Pageable.unpaged()
        );

        assertEquals(11, page.getTotalElements(), "Blank search did not return all elements");

        page = taskRepository.searchByIdOrTitleContainingIgnoreCase(
            null, null, Pageable.unpaged()
        );

        assertEquals(0, page.getTotalElements(), "Null search returned elements");

    }

}
