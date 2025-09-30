package uk.gov.hmcts.reform.dev.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.dev.dto.CaseDto;
import uk.gov.hmcts.reform.dev.dto.TaskDto;
import uk.gov.hmcts.reform.dev.models.Case;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.repositories.CaseRepository;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DAOService {

    CaseRepository caseRepository;
    TaskRepository taskRepository;

    ClassPathResource exampleCases;
    ClassPathResource exampleTasks;

    ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    /**
     * Autowired constructor
     *
     * @param caseRepository CaseRepository for CRUD operations
     * @param taskRepository TaskRepository for CRUD operations
     * @param exampleCases Example case objects for demonstration/testing (classpath resource)
     * @param exampleTasks Example task objects for demonstration/testing (classpath resource)
     */
    public DAOService(
        @Autowired CaseRepository caseRepository,
        @Autowired TaskRepository taskRepository,
        @Value("example-cases.json") ClassPathResource exampleCases,
        @Value("example-tasks.json") ClassPathResource exampleTasks) {
        this.exampleCases = exampleCases;
        this.exampleTasks = exampleTasks;
        this.caseRepository = caseRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Fetch the example cases from JSON files in classpath
     *
     * @return List of CaseDto (data transfer objects) from file
     * @throws IOException If JSON parsing fails
     */
    private List<CaseDto> getExampleCases() throws IOException {
        CollectionType typeReference =
            TypeFactory.defaultInstance().constructCollectionType(List.class, CaseDto.class);
        return objectMapper.readValue(exampleCases.getURL(), typeReference);
    }

    /**
     * Fetch the example tasks from JSON files in classpath
     *
     * @return Map of String (Case Number) to List of TaskDto (data transfer objects) from file
     * @throws IOException If JSON parsing fails
     */
    private Map<String, List<TaskDto>> getExampleTasks() throws IOException {
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        MapType typeReference =
            TypeFactory.defaultInstance().constructMapType(Map.class, typeFactory.constructType(String.class),
                                                           typeFactory.constructCollectionType(List.class, TaskDto.class));
        return objectMapper.readValue(exampleTasks.getURL(), typeReference);
    }

    /**
     * Loads test data from example files into the current DataBase instance
     * @throws ConstraintViolationException If the CaseNumber unique constraint is violated (duplicated case number)
     */
    public void loadTestData() throws ConstraintViolationException {
        List<CaseDto> exampleCases = List.of();
        saveCases(exampleCases);

        try {
            exampleCases = getExampleCases();
            saveCases(exampleCases);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, List<TaskDto>> exampleTasks;

        try{
            exampleTasks = getExampleTasks();

            for(String key : exampleTasks.keySet()){
                CaseDto c = getCaseByNumber(key).orElseThrow();

                for(TaskDto task : exampleTasks.get(key)){
                    task.setParentCase(c.getId());
                    saveTask(task);
                }

            }
        } catch (IOException | NoSuchElementException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all items in the DB matching the cases from the example case file, does not remove any added cases
     */
    public void clearTestData() {
        try{
            List<CaseDto> exampleCases = getExampleCases();
            this.caseRepository.deleteAllByCaseNumberIn(exampleCases.stream().map(CaseDto::getCaseNumber).collect(Collectors.toSet()));
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a CaseDto to an internal Case object
     *
     * @param caseDto CaseDto to convert
     * @return Case converted from input
     */
    private Case convertCaseDto(CaseDto caseDto) {
        return new Case(
            caseDto.getCaseNumber(), caseDto.getTitle(), caseDto.getDescription(),
            caseDto.getStatus(), caseDto.getCreatedDate()
        );
    }

    /**
     * Converts a TaskDto to an internal Task object, fetching it's parent case in the process
     *
     * @param taskDto TaskDto to convert
     * @return Task converted from input
     * @throws IllegalArgumentException If TaskDto provided has no parentCase property
     * @throws NoSuchElementException If DB does not contain a Case matching parentCase property
     */
    private Task convertTaskDto(TaskDto taskDto) throws IllegalArgumentException, NoSuchElementException {
        if(taskDto.getParentCase() == null){
            throw new IllegalArgumentException("Parent case is null");
        }
        Optional<Case> caseOptional = caseRepository.findById(taskDto.getParentCase());
        if(caseOptional.isEmpty()){
            throw new NoSuchElementException("Parent case not found");
        }

        return new Task(
            taskDto.getTitle(), taskDto.getDescription(), taskDto.getStatus(),
            taskDto.getDueDate(), caseOptional.get()
        );
    }

    /**
     * Converts an internal Case object to a CaseDto for external use
     *
     * @param c Case object to convert to data transfer equivalent
     * @return CaseDto converted from input
     */
    private CaseDto convertCase(Case c){
        return new CaseDto(
            c.getId(), c.getCaseNumber(), c.getTitle(),
            c.getDescription(), c.getStatus(), c.getCreatedDate(),
            c.getTasks().stream().map(Task::getId).toList()
        );
    }

    /**
     * Converts an internal Task object to a TaskDto for external use
     *
     * @param task Task object to convert to data transfer equivalent
     * @return TaskDto converted from input
     */
    private TaskDto convertTask(Task task){
        return new TaskDto(
            task.getId(), task.getTitle(), task.getDescription(),
            task.getStatus(), task.getDueDate(), task.getParentCase().getId()
        );
    }

    /**
     * Save an externally sourced case
     *
     * @param caseDto CaseDto to convert to a Case and save
     * @return CaseDto post-save transaction
     * @throws IllegalArgumentException If either the CaseDto contains tasks or the caseNumber already exists in the DB
     */
    public CaseDto saveCase(CaseDto caseDto) throws IllegalArgumentException, DataIntegrityViolationException{
        if(!caseDto.getTasks().isEmpty()) {
            throw new IllegalArgumentException("New case contains tasks");
        }
        return convertCase(caseRepository.save(convertCaseDto(caseDto)));
    }

    /**
     * Save an externally sourced task
     *
     * @param taskDto TaskDto to convert to a Task and save
     * @return TaskDto post-save transaction
     * @throws NoSuchElementException If DB does not contain a Case matching parentCase property
     * @throws IllegalArgumentException If the TaskDto provided has no parentCase property
     */
    public TaskDto saveTask(TaskDto taskDto) throws NoSuchElementException, IllegalArgumentException {
        Task output = taskRepository.save(convertTaskDto(taskDto));
        Case c = caseRepository.findById(taskDto.getParentCase()).orElseThrow();
        c.addTask(output);
        caseRepository.save(c);
        return convertTask(output);
    }

    /**
     * Deletes a case and therefore it's tasks by Case ID
     *
     * @param id UUID of the case to delete, silently succeeds if case does not exist
     */
    public void deleteCase(UUID id) {
        caseRepository.deleteById(id);
    }

    /**
     * Deletes a task (but not it's parent case) by Task ID
     *
     * @param id UUID of the task to delete, silently succeeds if task does not exist
     */
    public void deleteTask(UUID id) {
        taskRepository.deleteById(id);
    }

    /**
     * Save a collection of CaseDto objects
     *
     * @param caseDtos Collection of CaseDto objects to convert and save
     * @return Collection of CaseDto objects post-save transaction
     * @throws IllegalArgumentException If either a CaseDto contains tasks or the caseNumber already exists in DB
     *
     * @apiNote This function will interrupt midway having already saved the previous CaseDto objects
     *          without declaring which it did or didn't save,
     *          TODO: Implement improved version to provide transaction success/fail info
     */
    public Collection<CaseDto> saveCases(Collection<CaseDto> caseDtos) throws IllegalArgumentException,
        DataIntegrityViolationException {
        return caseDtos.stream().map(this::saveCase).toList();
    }

    /**
     * Save a collection of TaskDto objects
     *
     * @param taskDtos  Collection of TaskDto objects to convert and save
     * @return Collection of TaskDto objects post-save transaction
     * @throws NoSuchElementException If DB does not contain a Case matching parentCase property for a TaskDto
     * @throws IllegalArgumentException If a TaskDto provided has no parentCase property
     *
     * @apiNote This function will interrupt midway having already saved the previous TaskDto objects
     *      *          without declaring which it did or didn't save,
     *      *          TODO: Implement improved version to provide transaction success/fail info
     */
    public Collection<TaskDto> saveTasks(Collection<TaskDto> taskDtos) throws IllegalArgumentException, NoSuchElementException {
        return taskDtos.stream().map(this::saveTask).toList();
    }

    /**
     * Search for a case by a String matching its ID, Title or CaseNumber (case-insensitive, ironically)
     *
     * @param searchString String to search by, will be coerced into UUID if possible
     * @param pageable Pageable object for continuity
     * @return A Page containing CaseDto objects matching the searchString in some capacity
     *
     * @apiNote This function does not allow for partial matches of UUID due to the repo interface
     *          limitations,
     *          TODO: reimplement to allow partial ID search (niche use case but necessary)
     */
    public Page<CaseDto> searchCases(String searchString, Pageable pageable) {
        UUID id = null;
        try {
            id = UUID.fromString(searchString);
        }catch(IllegalArgumentException ignored){
        }

        return caseRepository.searchByIdOrTitleContainingIgnoreCaseOrCaseNumberContainingIgnoreCase(
            id, searchString, searchString, pageable
        ).map(this::convertCase);
    }

    /**
     * Get a case by ID, returning an empty Optional if not found
     *
     * @param id UUID of the Case to fetch
     * @return Optional containing CaseDto if match found in DB
     */
    public Optional<CaseDto> getCase(UUID id) {
        return caseRepository.findById(id).map(this::convertCase);
    }

    /**
     * Get a task by ID returning an empty Optional if not found
     *
     * @param id UUID of the Task to fetch
     * @return Optional containing TaskDto if match found in DB
     */
    public Optional<TaskDto> getTask(UUID id) {
        return taskRepository.findById(id).map(this::convertTask);
    }

    /**
     * Parametrically update a case property
     *
     * @param id UUID of the case to update
     * @param value New value (will be coerced as ISO date if property is a date)
     * @param property Name of the property to update (status, description, title, caseNumber or createdDate)
     * @return Updated CaseDto
     * @throws IllegalArgumentException If date unparseable or Case not found with ID
     */
    public CaseDto updateCaseProperty(UUID id, String value, String property) throws IllegalArgumentException {
        Optional<Case> caseOptional = caseRepository.findById(id);
        if (caseOptional.isPresent()) {
            try {
                switch (property) {
                    case "status" -> caseOptional.get().setStatus(value);
                    case "description" -> caseOptional.get().setDescription(value);
                    case "title" -> caseOptional.get().setTitle(value);
                    case "caseNumber" -> caseOptional.get().setCaseNumber(value);
                    case "createdDate" -> {
                        LocalDateTime localDate = LocalDateTime.parse(value);
                        caseOptional.get().setCreatedDate(localDate);
                    }
                    default -> throw new IllegalArgumentException("Cannot find modifiable property '"+property+"'");
                }
            }catch(DateTimeParseException e){
                throw new IllegalArgumentException("Could not parse date '" + value + "'");
            }
            caseRepository.save(caseOptional.get());
            return convertCase(caseOptional.get());
        }
        throw new IllegalArgumentException("Case not found '"+id+"'");
    }

    /**
     * Parametrically update a task property
     *
     * @param id UUID of the task to update
     * @param value New value (will be coerced as ISO date if property is a date)
     * @param property Name of the property to update (status, description, title or dueDate)
     * @return Updated TaskDto
     * @throws IllegalArgumentException If date unparseable or Task not found with ID
     */
    public TaskDto updateTaskProperty(UUID id, String value, String property) throws IllegalArgumentException {
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            try {
                switch (property) {
                    case "status" -> taskOptional.get().setStatus(value);
                    case "description" -> taskOptional.get().setDescription(value);
                    case "title" -> taskOptional.get().setTitle(value);
                    case "dueDate" -> {
                        LocalDateTime localDate = LocalDateTime.parse(value);
                        taskOptional.get().setDueDate(localDate);
                    }
                    case "parentCase" -> {
                        try{
                            UUID caseId = UUID.fromString(value);
                            Optional<Case> caseOptional = caseRepository.findById(caseId);
                            if (caseOptional.isPresent()) {
                                caseOptional.get().addTask(taskOptional.get());
                                caseRepository.save(caseOptional.get());
                            }else{
                                throw new IllegalArgumentException("Case not found '"+value+"'");
                            }
                        }catch(IllegalArgumentException e){
                            throw new IllegalArgumentException("Case with ID "+value+" not found, ID may be invalid");
                        }
                    }
                    default -> throw new IllegalArgumentException("Cannot find modifiable property '"+property+"'");
                }
            }catch(DateTimeParseException e){
                throw new IllegalArgumentException("Could not parse date '" + value + "'");
            }
            taskRepository.save(taskOptional.get());
            return convertTask(taskOptional.get());
        }
        throw new IllegalArgumentException("Case not found '"+id+"'");
    }

    /**
     * Get page of Tasks by the parent case ID
     *
     * @param id UUID of the parent case
     * @param pageable Pageable object for continuity
     * @return Page containing any matching TaskDto objects
     */
    public Page<TaskDto> getTasksForParent(UUID id, Pageable pageable) {
        return taskRepository.findAllByParentCaseId(id, pageable).map(this::convertTask);
    }

    /**
     * Get a Case by its caseNumber
     *
     * @param caseNumber Case Number to find by
     * @return Optional containing CaseDto if match found
     */
    public Optional<CaseDto> getCaseByNumber(String caseNumber) {
        return caseRepository.findFirstByCaseNumber(caseNumber).map(this::convertCase);
    }

}
