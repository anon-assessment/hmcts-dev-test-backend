package uk.gov.hmcts.reform.dev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.reform.dev.dto.CaseDto;
import uk.gov.hmcts.reform.dev.dto.TaskDto;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DataUpdateSmokeTest {

    @Value("${TEST_URL:http://localhost:4000}")
    private String testUrl;
    ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
    }

    /**
     * Tests saving a Case, then updating the status
     *
     * @throws JsonProcessingException If case cannot be deserialized
     */
    @Test
    public void loadAndUpdateCase() throws JsonProcessingException {

        CaseDto caseDto = new CaseDto();

        caseDto.setCaseNumber("CaseNumber/Example");
        caseDto.setTitle("Title");
        caseDto.setDescription("Description");
        caseDto.setStatus("Ongoing");

        Response response = given()
            .body(objectMapper.writeValueAsString(caseDto))
            .contentType(ContentType.JSON)
            .when()
            .post("/case")
            .then()
            .extract().response();

        assertEquals(200, response.getStatusCode(), "Could not request save");

        CaseDto returned = objectMapper.readValue(response.getBody().asString(), CaseDto.class);

        assertEquals(returned.getCaseNumber(), caseDto.getCaseNumber(), "Case returned does not match");

        response = given()
            .contentType(ContentType.JSON)
            .when()
            .post("/case/"+returned.getId()+"/status?value=Resolved")
            .then()
            .extract().response();

        returned = objectMapper.readValue(response.getBody().asString(), CaseDto.class);

        assertEquals("Resolved", returned.getStatus(), "Status returned does not match after update");
    }

    /**
     * Tests saving a Task, then updating the status
     *
     * @throws JsonProcessingException If task or parent case cannot be deserialized
     */
    @Test
    public void loadAndUpdateTask() throws JsonProcessingException {

        CaseDto caseDto = new CaseDto();

        caseDto.setCaseNumber("CaseNumber/Example");
        caseDto.setTitle("Title");
        caseDto.setDescription("Description");
        caseDto.setStatus("Ongoing");

        Response response = given()
            .body(objectMapper.writeValueAsString(caseDto))
            .contentType(ContentType.JSON)
            .when()
            .post("/case")
            .then()
            .extract().response();

        assertEquals(200, response.getStatusCode(), "Could not save parent case: "+response.getBody().asString());

        caseDto = objectMapper.readValue(response.getBody().asString(), CaseDto.class);

        TaskDto taskDto = new TaskDto();

        taskDto.setTitle("Title");
        taskDto.setDescription("Description");
        taskDto.setStatus("Blocked");
        taskDto.setParentCase(caseDto.getId());
        taskDto.setDueDate(LocalDateTime.of(2026, 1, 1, 12, 0));

        response = given()
            .body(objectMapper.writeValueAsString(taskDto))
            .contentType(ContentType.JSON)
            .when()
            .post("/task")
            .then()
            .extract().response();

        assertEquals(200, response.getStatusCode(), "Could not save task");

        TaskDto returned = objectMapper.readValue(response.getBody().asString(), TaskDto.class);

        assertEquals(returned.getTitle(), taskDto.getTitle(), "Task returned does not match");

        response = given()
            .contentType(ContentType.JSON)
            .when()
            .post("/task/"+returned.getId()+"/status?value=Complete")
            .then()
            .extract().response();

        assertEquals(200, response.getStatusCode(), "Could not update task: "+response.getBody().asString());

        returned = objectMapper.readValue(response.getBody().asString(), TaskDto.class);

        assertEquals("Complete", returned.getStatus(), "Status returned does not match after update");
    }



}
