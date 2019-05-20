package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.sakaiproject.core.persistence.grades.model.Comment;
import org.sakaiproject.core.persistence.grades.model.GradableObject;

public interface CommentRepository extends CrudRepository<Comment, Long> {

    Comment findOneByStudentIdAndGradableObject_Gradebook_UidAndGradableObject_IdAndGradableObject_RemovedIsFalse(
        String studentId, String gradebookUid, Long assignmentId);
    List<Comment> findByGradableObjectAndStudentIdIn(GradableObject gradable, List<String> studentIds);
    long deleteByGradableObject(GradableObject go);
}
