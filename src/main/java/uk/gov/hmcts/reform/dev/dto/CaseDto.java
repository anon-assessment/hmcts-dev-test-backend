package uk.gov.hmcts.reform.dev.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CaseDto implements Serializable {

    private UUID id;

    private String caseNumber;
    private String title;
    private String description;
    private String status;

    // Using ISO-8601 both on serialization and deserialization
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdDate;

    private List<UUID> tasks = new ArrayList<>();

}
