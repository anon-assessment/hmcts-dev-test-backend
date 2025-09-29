package uk.gov.hmcts.reform.dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;
import uk.gov.hmcts.reform.dev.services.DAOService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for Task entity
 */
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TaskTest {

    TaskRepository taskRepository;
    CaseRepository caseRepository;

    public TaskTest(@Autowired TaskRepository taskRepository, @Autowired CaseRepository caseRepository) {
        this.taskRepository = taskRepository;
        this.caseRepository = caseRepository;
    }

    @Test
    public void taskLifecycleTest(){
        Case c = new Case("rootCase", "title",
                          "description", "status", LocalDateTime.now());

        c = caseRepository.save(c);

        Task t = new Task("title",
                          "description", "status", LocalDateTime.now(), c);

        t = taskRepository.save(t);
        c.addTask(t);

        caseRepository.save(c);

        assertNotNull(t.getId(), "Task id not automatically provided");

        try{
            taskRepository.save(t);
        }catch (Exception ignored){
            fail("Task could not be saved over");
        }

        Optional<Task> b = taskRepository.findById(t.getId());

        assertTrue(b.isPresent(), "Could not fetch task by id after save and update");

        assertEquals(t, b.get(), "Fetched differing task after save and update");

        taskRepository.deleteById(t.getId());

        b = taskRepository.findById(t.getId());

        assertFalse(b.isPresent(), "Task found after deletion");

    }

    @Test
    public void taskCaseLifecycleTest(){

        Case c = new Case("rootCase", "title",
                          "description", "status", LocalDateTime.now());

        c = caseRepository.save(c);

        Task t = new Task("title",
                          "description", "status", LocalDateTime.now(), c);

        t = taskRepository.save(t);

        c.addTask(t);

        caseRepository.save(c);

        UUID taskId = t.getId();

        caseRepository.delete(c);

        Optional<Task> b = taskRepository.findById(taskId);

        assertFalse(b.isPresent(), "Task found after deletion of parent case");
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
