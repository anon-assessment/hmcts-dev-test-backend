package uk.gov.hmcts.reform.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.dev.dto.CaseDto;
import uk.gov.hmcts.reform.dev.dto.TaskDto;
import uk.gov.hmcts.reform.dev.models.Case;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class StandardSmokeTest {
    protected static final String CONTENT_TYPE_VALUE = "application/json";

    @Value("${TEST_URL:http://localhost:4000}")
    private String testUrl;
    ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private final ClassPathResource exampleCases;
    private final ClassPathResource exampleTasks;

    public StandardSmokeTest(@Value("example-cases.json") ClassPathResource exampleCases,
        @Value("example-tasks.json") ClassPathResource exampleTasks) {
        this.exampleCases = exampleCases;
        this.exampleTasks = exampleTasks;
    }

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
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

    /**
     * Root test
     */
    @Test
    void smokeTest() {
        Response response = given()
            .contentType(ContentType.JSON)
            .when()
            .get()
            .then()
            .extract().response();

        assertEquals(200, response.statusCode());
        assertTrue(response.asString().startsWith("Welcome"));
    }

    /**
     * Test using example-cases.json and example-tasks.json, loading all items and
     * posting them to the controller for saving.
     */
    @Test
    void basicExampleLoad(){
        List<CaseDto> exampleCases = List.of();
        try {
            exampleCases = getExampleCases();

            for(CaseDto c : exampleCases){
                Response response = given()
                    .body(
                        objectMapper.writeValueAsString(c)
                    )
                    .contentType(ContentType.JSON)
                    .when()
                    .post("/case")
                    .then()
                    .extract().response();

                if(response.statusCode() != 200){
                    fail("Could not save example case: "+ response.getBody().asString());
                }

                Case out = objectMapper.readValue(response.getBody().asString(), Case.class);
                c.setId(out.getId());
            }
        } catch (IOException e) {
            fail("Could not load example cases", e);
        }

        Map<String, List<TaskDto>> exampleTasks;

        try{
            exampleTasks = getExampleTasks();

            for(String key : exampleTasks.keySet()){
                CaseDto c = exampleCases.stream()
                    .filter((other) -> Objects.equals(other.getCaseNumber(), key)).findFirst().orElseThrow();

                for(TaskDto task : exampleTasks.get(key)){
                    task.setParentCase(c.getId());

                    Response response = given()
                        .body(
                            objectMapper.writeValueAsString(task)
                        )
                        .contentType(ContentType.JSON)
                        .when()
                        .post("/task")
                        .then()
                        .extract().response();

                    if(response.statusCode() != 200){
                        fail("Could not save example task: "+ response.getBody().asString());
                    }
                }

            }
        } catch (IOException e) {
            fail("Could not load example tasks", e);
        } catch (NoSuchFieldError e) {
            fail("Example tasks do not match example cases");
        }
    }

    @Test
    void exampleLoadSearch(){

        basicExampleLoad();

        Response response = given()
            .contentType(ContentType.JSON)
            .when()
            .post("/case/search?searchString=&pageNumber=0&pageSize=10")
            .then()
            .extract().response();

        System.out.println(response.body().asString());
    }

}
