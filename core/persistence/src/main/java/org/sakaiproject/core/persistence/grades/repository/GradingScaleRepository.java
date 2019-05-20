package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

import org.sakaiproject.core.persistence.grades.model.GradingScale;

public interface GradingScaleRepository extends CrudRepository<GradingScale, Long> {

    //List<GradingScale> findByUnavailable(boolean unavailable);
    List<GradingScale> findByUidNotInAndUnavailableIsFalse(Set<String> uidsToSet);
    List<GradingScale> findByUidIn(Set<String> uidsToSet);
    List<GradingScale> findByUnavailableIsFalse();
}
