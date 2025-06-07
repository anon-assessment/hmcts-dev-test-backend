package uk.gov.hmcts.reform.dev.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Task DTO completing requirements set out at for the requirements laid out (<a href="https://github.com/hmcts/dts-developer-challenge?tab=readme-ov-file">https://github.com/hmcts/dts-developer-challenge?tab=readme-ov-file</a>)
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "tasks"
)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public Task(String title, String description, String status, LocalDateTime dueDate, Case parentCase){
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.parentCase = parentCase;
    }

    private String title;
    private String description;
    private String status;


    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.DETACH)
    private Case parentCase;

}
