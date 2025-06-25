package uk.gov.hmcts.reform.dev.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class DAOService {

    CaseRepository caseRepository;
    TaskRepository taskRepository;

    ClassPathResource exampleCases;
    ClassPathResource exampleTasks;

    ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

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

    private List<CaseDto> getExampleCases() throws IOException {
        CollectionType typeReference =
            TypeFactory.defaultInstance().constructCollectionType(List.class, CaseDto.class);
        return objectMapper.readValue(exampleCases.getURL(), typeReference);
    }

    private Map<String, List<TaskDto>> getExampleTasks() throws IOException {
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        MapType typeReference =
            TypeFactory.defaultInstance().constructMapType(Map.class, typeFactory.constructType(String.class),
                                                           typeFactory.constructCollectionType(List.class, TaskDto.class));
        return objectMapper.readValue(exampleTasks.getURL(), typeReference);
    }

    public void loadTestData(){
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

    private Case convertCaseDto(CaseDto caseDto) {
        return new Case(
            caseDto.getCaseNumber(), caseDto.getTitle(), caseDto.getDescription(),
            caseDto.getStatus(), caseDto.getCreatedDate()
        );
    }

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

    private CaseDto convertCase(Case c){
        return new CaseDto(
            c.getId(), c.getCaseNumber(), c.getTitle(),
            c.getDescription(), c.getStatus(), c.getCreatedDate(),
            c.getTasks().stream().map(Task::getId).toList()
        );
    }

    private TaskDto convertTask(Task task){
        return new TaskDto(
            task.getId(), task.getTitle(), task.getDescription(),
            task.getStatus(), task.getDueDate(), task.getParentCase().getId()
        );
    }

    public CaseDto saveCase(CaseDto caseDto) throws IllegalArgumentException{
        if(!caseDto.getTasks().isEmpty()) {
            throw new IllegalArgumentException("New case contains tasks");
        }
        try{
            return convertCase(caseRepository.save(convertCaseDto(caseDto)));
        }catch (DataIntegrityViolationException e){
            throw new IllegalArgumentException("Constraint violation", e);
        }
    }

    public TaskDto saveTask(TaskDto taskDto) throws NoSuchElementException, IllegalArgumentException {
        Task output = taskRepository.save(convertTaskDto(taskDto));
        Case c = caseRepository.findById(taskDto.getParentCase()).orElseThrow();
        c.addTask(output);
        caseRepository.save(c);
        return convertTask(output);
    }

    public void deleteCase(UUID id) {
        caseRepository.deleteById(id);
    }

    public void deleteTask(UUID id) {
        taskRepository.deleteById(id);
    }

    public Collection<CaseDto> saveCases(Collection<CaseDto> caseDtos) {
        return caseDtos.stream().map(this::saveCase).toList();
    }

    public Collection<TaskDto> saveTasks(Collection<TaskDto> taskDtos) throws IllegalStateException{
        return taskDtos.stream().map(this::saveTask).toList();
    }

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

    public Optional<CaseDto> getCase(UUID id) {
        return caseRepository.findById(id).map(this::convertCase);
    }

    public Optional<TaskDto> getTask(UUID id) {
        return taskRepository.findById(id).map(this::convertTask);
    }

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

    public Page<TaskDto> getTasksForParent(UUID id, Pageable pageable) {
        return taskRepository.findAllByParentCaseId(id, pageable).map(this::convertTask);
    }

    public Optional<CaseDto> getCaseByNumber(String caseNumber) {
        return caseRepository.findFirstByCaseNumber(caseNumber).map(this::convertCase);
    }

}
