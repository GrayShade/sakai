package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import org.sakaiproject.core.persistence.grades.model.CourseGrade;

public interface CourseGradeRepository extends CrudRepository<CourseGrade, Long> {

    CourseGrade findOneByGradebook_Id(Long gradebookId);
}
