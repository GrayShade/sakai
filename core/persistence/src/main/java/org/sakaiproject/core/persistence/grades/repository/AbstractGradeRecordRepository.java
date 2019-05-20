package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

import org.sakaiproject.core.persistence.grades.model.AbstractGradeRecord;
import org.sakaiproject.core.persistence.grades.model.Gradebook;

import java.util.List;

@NoRepositoryBean
public interface AbstractGradeRecordRepository<T extends AbstractGradeRecord> extends CrudRepository<T, Long> {

    long deleteByGradableObject_Gradebook_Id(Long gradebookId);
}
