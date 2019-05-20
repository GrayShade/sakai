package org.sakaiproject.core.persistence.grades.repository;

import org.sakaiproject.core.persistence.grades.model.CourseGradeRecord;
import org.sakaiproject.core.persistence.grades.model.Gradebook;

import java.util.List;

public interface CourseGradeRecordRepository extends AbstractGradeRecordRepository<CourseGradeRecord> {

    CourseGradeRecord findOneByStudentIdAndGradableObject_Gradebook(String studentId, Gradebook gradebook);
    long countByGradableObject_Gradebook_IdAndEnteredGradeNotNullAndStudentIdIn(Long gradebookId, List<String> studentIds);
    List<CourseGradeRecord> findByGradableObject_GradebookAndEnteredGradeNotNull(Gradebook gradebook);
    List<CourseGradeRecord> findByGradableObject_Id(Long id);
}
