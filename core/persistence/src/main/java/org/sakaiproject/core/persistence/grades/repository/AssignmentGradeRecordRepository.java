package org.sakaiproject.core.persistence.grades.repository;

import org.sakaiproject.core.persistence.grades.model.AssignmentGradeRecord;
import org.sakaiproject.core.persistence.grades.model.GradableObject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AssignmentGradeRecordRepository extends AbstractGradeRecordRepository<AssignmentGradeRecord> {

    AssignmentGradeRecord findOneByStudentIdAndGradableObject_Id(String studentId, Long gradableObjectId);
    List<AssignmentGradeRecord> findByGradableObject_RemovedIsFalseAndGradableObject_IdOrderByPointsEarned(Long gradableObjectId);
    List<AssignmentGradeRecord> findByGradableObject_Gradebook_IdAndGradableObject_RemovedIsFalseAndStudentIdIn(Long gradebookId, Collection<String> studentUids);
    List<AssignmentGradeRecord> findByGradableObject_RemovedIsFalseAndGradableObject_IdInAndStudentIdIn(List<Long> gradableObjectIds, List<String> studentUids);
    List<AssignmentGradeRecord> findByGradableObject_Id(Long gradableObjectId);
    long deleteByGradableObject(GradableObject go);
    List<AssignmentGradeRecord> findByGradableObjectAndStudentIdIn(GradableObject go, Set<String> studentIds);
}
