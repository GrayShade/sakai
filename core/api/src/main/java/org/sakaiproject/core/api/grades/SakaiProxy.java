package org.sakaiproject.core.api.grades;

import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SakaiProxy {

    String getCurrentUserId();

    boolean getBooleanConfig(String key, boolean defaultValue);

    boolean isUserAbleToEditAssessments(String gradebookUid);

    boolean isUserAbleToGradeItemForStudent(String gradebookUid, Long itemId, String studentId) throws IllegalArgumentException;

    boolean isUserAbleToViewItemForStudent(String gradebookUid, Long itemId, String studentId) throws IllegalArgumentException;

	String getGradeViewFunctionForUserForStudentForItem(String gradebookUid, Long itemId, String studentId);

	public boolean isUserAbleToViewOwnGrades(String gradebookUid);

    boolean isUserAbleToGrade(String siteId);

    boolean isUserAbleToGrade(String siteId, String userId);

    boolean isUserAbleToGradeAll(String siteId, String userId);

	boolean isUserAbleToViewStudentNumbers(String gradebookUid);

	boolean isUserAbleToGradeAll(String gradebookUid);

    public boolean isCurrentUserFromGroup(String gradebookUid, String studentOrGroupId);

    /**
     * Get the list of students for the given gradebook
     *
     * @param uid the gradebook uid
     * @return a list of uuids for the students
     */
    public List<String> getStudentsForGradebook(String gradebookUid);

    public Map findMatchingEnrollmentsForItem(String gradebookUid, Long categoryId, int gbCategoryType, String optionalSearchString, String optionalSectionUid);

    public Map findMatchingEnrollmentsForItemForUser(String userUid, String gradebookUid, Long categoryId, int gbCategoryType, String optionalSearchString, String optionalSectionUid);

	public Map<EnrollmentRecord, String> findMatchingEnrollmentsForViewableCourseGrade(String gradebookUid, int gbCategoryType, String optionalSearchString, String optionalSectionUid);

    public Set<String> getAllStudentUids(String gradebookUid);

    public List<CourseSection> getViewableSections(String gradebookUid);

	public void postEvent(String event, String ref);

    public boolean isSuperUser();

	public boolean isUpdateSameScore();
}
