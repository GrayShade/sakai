package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;

import org.sakaiproject.core.persistence.grades.model.GradableObject;
import org.sakaiproject.core.persistence.grades.model.GradingEvent;

import java.util.Date;
import java.util.List;

public interface GradingEventRepository extends CrudRepository<GradingEvent, Long> {

    List<GradingEvent> findByStudentIdAndGradableObject_Id(String studentId, Long assignmentId);
    List<GradingEvent> findByDateGradedGreaterThanEqualAndGradableObject_IdIn(Date since, List<Long> assignmentIds);
    long deleteByGradableObject_Gradebook_Id(Long gradebookId);
    long deleteByGradableObject(GradableObject go);
}
