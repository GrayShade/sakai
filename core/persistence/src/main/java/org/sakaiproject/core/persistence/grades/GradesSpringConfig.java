package org.sakaiproject.core.persistence.grades;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.sakaiproject.core.persistence.grades.model.Category;
import org.sakaiproject.core.persistence.grades.model.Comment;

import org.sakaiproject.springframework.orm.hibernate.impl.AdditionalHibernateMappingsImpl;

@Configuration
public class GradesSpringConfig {

    @Bean
    public AdditionalHibernateMappingsImpl gradesHibernateMappings() {

        AdditionalHibernateMappingsImpl mappings = new AdditionalHibernateMappingsImpl();

        Class[] annotatedClasses = new Class[] {
            Category.class,
            Comment.class
        };

        mappings.setAnnotatedClasses(annotatedClasses);

        return mappings;
    }
}
