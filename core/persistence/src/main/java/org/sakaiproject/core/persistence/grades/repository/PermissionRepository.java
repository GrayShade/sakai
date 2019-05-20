package org.sakaiproject.core.persistence.grades.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.sakaiproject.core.persistence.grades.model.Permission;

public interface PermissionRepository extends CrudRepository<Permission, Long> {

    List<Permission> findByGradebookId(Long gradebookId);
    List<Permission> findByGradebookIdAndUserId(Long gradebookId, String userId);
    List<Permission> findByGradebookIdAndCategoryIdIn(Long gradebookId, List<Long> categoryIds);
    List<Permission> findByGradebookIdAndUserIdAndCategoryIdIn(Long gradebookId, String userId, List<Long> categoryIds);
    List<Permission> findByGradebookIdAndUserIdAndCategoryIdIsNullAndFunctionIn(Long gradebookId, String userId, List<String> permissions);
    List<Permission> findByGradebookIdAndUserIdAndCategoryIdInAndGroupIdIsNull(Long gradebookId, String userId, List<Long> categoryIds);
    List<Permission> findByGradebookIdAndUserIdAndGroupIdIsNullAndFunctionIn(Long gradebookId, String userId, List<String> permissions);
    List<Permission> findByGradebookIdAndUserIdAndCategoryIdIsNullAndGroupIdIsNull(Long gradebookId, String userId);
    List<Permission> findByGradebookIdAndUserIdAndCategoryIdIsNullAndGroupIdIn(Long gradebookId, String userId, List<String> groupIds);
    List<Permission> findByGradebookIdAndUserIdAndGroupIdIn(Long gradebookId, String userId, List<String> groupIds);
    void deleteByGradebookIdAndUserId(Long gradebookId, String userId);
}
