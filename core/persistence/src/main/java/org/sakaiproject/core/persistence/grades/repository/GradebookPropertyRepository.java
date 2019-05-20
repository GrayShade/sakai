package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.sakaiproject.core.persistence.grades.model.GradebookProperty;

public interface GradebookPropertyRepository extends CrudRepository<GradebookProperty, Long> {

    List<GradebookProperty> findByName(String name);
}
