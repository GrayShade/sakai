package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import org.sakaiproject.core.persistence.grades.model.Gradebook;

public interface GradebookRepository extends CrudRepository<Gradebook, Long> {

    Gradebook findByUid(String uid);
    boolean existsByUid(String uid);
}
