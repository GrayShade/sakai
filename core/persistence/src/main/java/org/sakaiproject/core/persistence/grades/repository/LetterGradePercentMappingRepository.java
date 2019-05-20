package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.sakaiproject.core.persistence.grades.model.LetterGradePercentMapping;

public interface LetterGradePercentMappingRepository extends CrudRepository<LetterGradePercentMapping, Long> {

    List<LetterGradePercentMapping> findByMappingType(int mappingType);
    LetterGradePercentMapping findByGradebookIdAndMappingType(Long gradebookId, int mappingType);
}
