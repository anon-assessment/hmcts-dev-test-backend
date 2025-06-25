package uk.gov.hmcts.reform.dev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.dev.dto.CaseDto;
import uk.gov.hmcts.reform.dev.dto.TaskDto;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RelationshipTest {

    @Autowired
    private transient MockMvc mockMvc;

    ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    /**
     * Tests a correct basic Case/Task relationship via MockMvc requests
     */
    @DisplayName("Tests a correct basic Case/Task relationship via MockMvc requests")
    @Test
    public void correctRelationships() {
        CaseDto caseDto = new CaseDto();

        caseDto.setCaseNumber("");
        caseDto.setTitle("Test Case");
        caseDto.setDescription("This is a test Case");
        caseDto.setStatus("Ongoing");

        try {
            MvcResult result = mockMvc.perform(
                    post("/case")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(caseDto)))
                .andExpect(status().isOk()).andReturn();

            caseDto = objectMapper.readValue(result.getResponse().getContentAsString(), CaseDto.class);
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize case: ", e);
        }catch (Exception e) {
            fail("Could not post case: ", e);
        }

        TaskDto taskDto = new TaskDto();
        taskDto.setTitle("Test Task");
        taskDto.setDescription("This is a test Task");
        taskDto.setStatus("Blocked");
        taskDto.setDueDate(LocalDateTime.now().plusYears(1));
        taskDto.setParentCase(caseDto.getId());

        try {
            MvcResult result = mockMvc.perform(
                    post("/task")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(taskDto)))
                .andExpect(status().isOk()).andReturn();

            taskDto = objectMapper.readValue(result.getResponse().getContentAsString(), TaskDto.class);
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize task: ", e);
        }catch (Exception e) {
            fail("Could not post task: ", e);
        }

        try {
            MvcResult result = mockMvc.perform(
                    get("/case/" + caseDto.getId()))
                .andExpect(status().isOk()).andReturn();

            caseDto = objectMapper.readValue(result.getResponse().getContentAsString(), CaseDto.class);
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize case with child task: ", e);
        }catch (Exception e) {
            fail("Could not get case with child task: ", e);
        }

        assertEquals(1, caseDto.getTasks().size(), "Case was not updated with child task");
        assertEquals(caseDto.getTasks().getFirst(), taskDto.getId(), "Case was updated with incorrect id");
    }

    /**
     * Tests various invalid Case/Task relationship via MockMvc requests
     */
    @DisplayName("Tests various invalid Case/Task relationship via MockMvc requests")
    @Test
    public void invalidRelationships(){
        CaseDto caseDto = new CaseDto();
        caseDto.setCaseNumber("");
        caseDto.setTitle("Test Case");
        caseDto.setDescription("This is a test Case");
        caseDto.setStatus("Ongoing");

        try{
            MvcResult result = mockMvc.perform(
                    post("/case")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(caseDto)))
                .andExpect(status().isOk()).andReturn();

            caseDto = objectMapper.readValue(result.getResponse().getContentAsString(), CaseDto.class);
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize case: ", e);
        }catch (Exception e) {
            fail("Could not post case: ", e);
        }

        TaskDto taskDto = new TaskDto();
        taskDto.setTitle("Test Task");
        taskDto.setDescription("This is a test Task");
        taskDto.setStatus("Blocked");
        taskDto.setDueDate(LocalDateTime.now().plusYears(1));
        // Random UUID, not present in the database
        taskDto.setParentCase(UUID.randomUUID());

        try {
            mockMvc.perform(
                    post("/task")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(taskDto)))
                .andExpect(status().isBadRequest()).andReturn();
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize task: ", e);
        }catch (Exception e) {
            fail("Allowed to post task with invalid parent ID: ", e);
        }
    }


}
