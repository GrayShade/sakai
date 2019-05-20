package org.sakaiproject.core.api.grades;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;

import org.sakaiproject.core.api.grades.AssessmentNotFoundException;
import org.sakaiproject.core.api.grades.CommentDefinition;
import org.sakaiproject.core.api.grades.ConflictingAssignmentNameException;
import org.sakaiproject.core.api.grades.ConflictingCategoryNameException;
import org.sakaiproject.core.api.grades.GradebookNotFoundException;

import org.sakaiproject.core.api.grades.StaleObjectModificationException;
import org.sakaiproject.core.persistence.grades.model.GradebookAssignment;
import org.sakaiproject.core.persistence.grades.model.AssignmentGradeRecord;
import org.sakaiproject.core.persistence.grades.model.Category;
import org.sakaiproject.core.persistence.grades.model.Comment;
import org.sakaiproject.core.persistence.grades.model.CourseGrade;
import org.sakaiproject.core.persistence.grades.model.GradeMapping;
import org.sakaiproject.core.persistence.grades.model.Gradebook;
import org.sakaiproject.core.persistence.grades.model.LetterGradePercentMapping;
import org.sakaiproject.core.persistence.grades.model.Permission;

public interface GradingPersistenceManager {

    public Gradebook getGradebook(Long id) throws GradebookNotFoundException;

    public Gradebook getGradebook(String uid) throws GradebookNotFoundException;

    public boolean isGradebookDefined(String gradebookUid);

    public List<GradebookAssignment> getAssignments(Long gradebookId);

    public CourseGrade getCourseGrade(Long gradebookId);

    public String getGradebookUid(Long id);

    public String getPropertyValue(String name);

	public void setPropertyValue(String name, String value);

	@Deprecated
	public GradebookAssignment getAssignmentWithoutStats(String gradebookUid, String assignmentName);

	public GradebookAssignment getAssignmentWithoutStats(String gradebookUid, Long assignmentId);

    public AssignmentGradeRecord getAssignmentGradeRecord(GradebookAssignment assignment, String studentUid);

    public Long createAssignment(Long gradebookId, String name, Double points, Date dueDate, Boolean isNotCounted,
           Boolean isReleased, Boolean isExtraCredit, Integer sortOrder) throws ConflictingAssignmentNameException, StaleObjectModificationException;

    public Long createAssignmentForCategory(Long gradebookId, Long categoryId, String name, Double points, Date dueDate, Boolean isNotCounted, 
           Boolean isReleased, Boolean isExtraCredit, Integer categorizedSortOrder)
    throws ConflictingAssignmentNameException, StaleObjectModificationException, IllegalArgumentException;

    public void updateGradebook(Gradebook gradebook) throws StaleObjectModificationException;

    public boolean isExplicitlyEnteredCourseGradeRecords(Long gradebookId);

	public void postEvent(String event, String objectReference);

    public Long createCategory(Long gradebookId, String name, Double weight, Integer drop_lowest,
                               Integer dropHighest, Integer keepHighest, Boolean is_extra_credit);

    public Long createCategory(Long gradebookId, String name, Double weight, Integer drop_lowest,
                               Integer dropHighest, Integer keepHighest, Boolean is_extra_credit,
                               Integer categoryOrder) throws ConflictingCategoryNameException, StaleObjectModificationException;

    public List<Category> getCategories(Long gradebookId) throws HibernateException;

    public List<Category> getCategoriesWithAssignments(Long gradebookId);

    public List<GradebookAssignment> getAssignmentsForCategory(Long categoryId) throws HibernateException;

    public Category getCategory(Long categoryId) throws HibernateException;

    public void updateCategory(Category category) throws ConflictingCategoryNameException, StaleObjectModificationException;

    public void removeCategory(Long categoryId) throws StaleObjectModificationException;

    public LetterGradePercentMapping getDefaultLetterGradePercentMapping();

    public void createOrUpdateDefaultLetterGradePercentMapping(Map<String, Double> gradeMap);

    public void createDefaultLetterGradePercentMapping(Map<String, Double> gradeMap);

    public LetterGradePercentMapping getLetterGradePercentMapping(Gradebook gradebook);

    public void saveOrUpdateLetterGradePercentMapping(Map<String, Double> gradeMap, Gradebook gradebook);

    public Long createUngradedAssignment(Long gradebookId, String name, Date dueDate, Boolean isNotCounted,
                Boolean isReleased) throws ConflictingAssignmentNameException, StaleObjectModificationException;

    public Long createUngradedAssignmentForCategory(Long gradebookId, Long categoryId, String name, Date dueDate, Boolean isNotCounted,
                                                    Boolean isReleased) throws ConflictingAssignmentNameException, StaleObjectModificationException, IllegalArgumentException;

    public Long addPermission(Long gradebookId, String userId, String function, Long categoryId,
                              String groupId) throws IllegalArgumentException;

    @Deprecated
    public List<Permission> getPermissionsForGB(Long gradebookId) throws IllegalArgumentException;

    @Deprecated
    public void updatePermission(Collection perms);

    @Deprecated
    public void updatePermission(Permission perm) throws IllegalArgumentException;

    @Deprecated
    public void deletePermission(Permission perm) throws IllegalArgumentException;

    public void deletePermissionsForUser(Long gradebookId, String userId);

    public void replacePermissions(List<Permission> currentPermissions, List<Permission> newPermissions);

    public List<Permission> getPermissionsForUser(Long gradebookId, String userId) throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserForCategory(Long gradebookId, String userId, List<Long> categoryIds)
        throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserAnyCategory(Long gradebookId, String userId) throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserAnyGroup(Long gradebookId, String userId)
        throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserAnyGroupForCategory(Long gradebookId, String userId, List<Long> categoryIds)
        throws IllegalArgumentException;

    public List<Permission> getPermissionsForGBForCategoryIds(Long gradebookId, List<Long> categoryIds) throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserAnyGroupAnyCategory(Long gradebookId, String userId) throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserForGoupsAnyCategory(Long gradebookId, String userId, List<String> groupIds)
        throws IllegalArgumentException;

    public List<Permission> getPermissionsForUserForGroup(Long gradebookId, String userId, List<String> groupIds)
        throws IllegalArgumentException;

    public boolean isAssignmentDefined(Long gradableObjectId);

    /**
     *
     * @param gradableObjectId
     * @return the GradebookAssignment object with the given id
     */
    public GradebookAssignment getAssignment(Long gradableObjectId);

    public List<Comment> getComments(GradebookAssignment assignment, List<String> studentIds);

	//public Map<String, List<String>> getVisibleExternalAssignments(String gradebookUid, Collection<String> studentIds)
	//		throws GradebookNotFoundException;

	public CommentDefinition getAssignmentScoreComment(String gradebookUid, Long assignmentId, String studentUid) throws GradebookNotFoundException, AssessmentNotFoundException;

	public void setAssignmentScoreComment(String gradebookUid, Long assignmentId, String studentUid, String commentText) throws GradebookNotFoundException, AssessmentNotFoundException;

	public boolean getIsAssignmentExcused(String gradebookUid, Long assignmentId, String studentUid) throws GradebookNotFoundException, AssessmentNotFoundException;

	public void updateGradeMapping(Long gradeMappingId, Map<String, Double> gradeMap);

	public Comment getInternalComment(String gradebookUid, Long assignmentId, String studentId);
}
