package uk.gov.hmcts.reform.dev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.dev.dto.CaseDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConstraintTest {

    @Autowired
    private transient MockMvc mockMvc;

    ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    /**
     * Test constraint that two Cases cannot have the same case number
     */
    @DisplayName("Test constraint that two Cases cannot have the same case number")
    @Test
    public void caseNumberUniqueness() {
        CaseDto caseDto = new CaseDto();

        caseDto.setCaseNumber("Example");
        caseDto.setTitle("Test Case");
        caseDto.setDescription("This is a test Case");
        caseDto.setStatus("Ongoing");

        try {
            mockMvc.perform(
                    post("/case")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(caseDto)))
                .andExpect(status().isOk()).andReturn();
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize case: ", e);
        }catch (Exception e) {
            fail("Could not post case: ", e);
        }

        try {
            mockMvc.perform(
                    post("/case")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(caseDto)))
                .andExpect(status().isBadRequest()).andReturn();
        }catch (JsonProcessingException e){
            fail("Could not serialize/deserialize case:", e);
        }catch (Exception e) {
            fail("Allowed to save case with duplicate case number:", e);
        }

    }

}
