package uk.gov.hmcts.reform.dev.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Fundamental Case entity, represents all required data to perform as a DTO
 * <p>
 * Potential improvements:
 *  - Making status a lookup to another table, allowing an extendable choice from a list, some basic statuses with the
 *  option to add additional
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
    // Renamed table to cases to avoid name conflict in H2
    name = "cases",
    // Enforced unique case number
    uniqueConstraints = {
        @UniqueConstraint(name = "UniqueCaseNumber", columnNames = {"caseNumber"})
    }
)
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id")
public class Case {

    // Preferable for a distributed/parallel system, consistent format e.g. in url params
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public Case(String caseNumber, String title, String description, String status, LocalDateTime createdDate){
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdDate = createdDate;
    }

    // Required metadata
    private String caseNumber;
    private String title;
    private String description;
    private String status;

    // Using ISO-8601 both on serialization and deserialization
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdDate;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonIdentityReference(alwaysAsId=true)
    private List<Task> tasks = new ArrayList<>();

    public void addTask(Task task){
        this.tasks.add(task);
    }


}
