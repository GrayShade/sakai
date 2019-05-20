package org.sakaiproject.core.persistence.grades.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import org.sakaiproject.core.persistence.grades.model.GradebookAssignment;

public interface GradebookAssignmentRepository extends CrudRepository<GradebookAssignment, Long> {

    List<GradebookAssignment> findByGradebook_IdAndRemovedIsFalse(Long gradebookId);
    GradebookAssignment findOneByIdAndGradebook_UidAndRemovedIsFalse(Long assignmentId, String gradebookUid);
    GradebookAssignment findOneByNameAndGradebook_UidAndRemovedIsFalse(String assignmentName, String gradebookUid);
    List<GradebookAssignment> findByGradebook_Uid(String gradebookUid);
    GradebookAssignment findOneByGradebook_UidAndExternalId(String gradebookUid, String externalId);
    List<GradebookAssignment> findByCategory_IdAndRemovedIsFalse(Long categoryId);
    List<GradebookAssignment> findByGradebook_IdAndRemovedIsFalseAndNotCountedIsFalse(Long gradebookId);
    long countByIdAndRemovedIsFalse(Long id);
    long countByExternalIdAndGradebook_Uid(String externalId, String gradebookUid);
    boolean existsByIdAndRemovedIsFalse(Long id);
    List<GradebookAssignment> findByGradebook_IdAndRemovedIsFalseAndNotCountedIsFalseAndUngradedIsFalse(Long gradebookId);
}
