package org.sakaiproject.core.persistence.grades.model;

import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString
public class Grade {
    private Long id;
    private int version;
    private String studentId;
    private String graderId;
//    private GradableObject gradableObject;
    private Instant dateRecorded;
}
