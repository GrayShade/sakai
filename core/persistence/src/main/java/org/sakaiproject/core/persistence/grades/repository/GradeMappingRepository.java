package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import org.sakaiproject.core.persistence.grades.model.GradeMapping;

import java.util.Set;

public interface GradeMappingRepository extends CrudRepository<GradeMapping, Long> {

    Set<GradeMapping> findByGradebook_Id(Long gradebookId);
    Set<GradeMapping> findByGradebook_Uid(String gradebookUid);
    long deleteByGradebook_Id(Long gradebookId);
}
