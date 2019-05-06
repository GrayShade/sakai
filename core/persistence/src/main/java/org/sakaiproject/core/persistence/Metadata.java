package org.sakaiproject.core.persistence;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.Embeddable;
import java.time.Instant;

@Data
@Embeddable
public class Metadata {

    @Type(type = "org.sakaiproject.springframework.orm.hibernate.type.InstantType")
    private Instant created;
    @Type(type = "org.sakaiproject.springframework.orm.hibernate.type.InstantType")
    private Instant modified;
    private String creator;
    private String modifier;
}
