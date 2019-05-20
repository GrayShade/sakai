package org.sakaiproject.core.services.grades;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.core.api.grades.SakaiProxy;
import org.sakaiproject.core.api.grades.GradingPermissionService;
import org.sakaiproject.core.utils.grades.GradingConstants;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.section.api.SectionAwareness;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.section.api.coursemanagement.ParticipationRecord;
import org.sakaiproject.section.api.facade.Role;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

@Slf4j
public class SakaiProxyImpl implements SakaiProxy {

    public static final String PERMISSION_GRADE_ALL = "gradebook.gradeAll";
    public static final String PERMISSION_GRADE_SECTION = "gradebook.gradeSection";
    public static final String PERMISSION_EDIT_ASSIGNMENTS = "gradebook.editAssignments";
    public static final String PERMISSION_VIEW_OWN_GRADES = "gradebook.viewOwnGrades";
    public static final String PERMISSION_VIEW_STUDENT_NUMBERS = "gradebook.viewStudentNumbers";

    /**
	 * Property in sakai.properties used to allow this service to update scores in the db every time the update method is called. By
	 * default, scores are only updated if the score is different than what is currently in the db.
	 */
	public static final String UPDATE_SAME_SCORE_PROP = "gradebook.externalAssessments.updateSameScore";
	public static final boolean UPDATE_SAME_SCORE_PROP_DEFAULT = false;

    @Resource private EventTrackingService eventTrackingService;
    @Resource private GradingPermissionService gradingPermissionService;
    @Resource private SectionAwareness sectionAwareness;
    @Resource private SecurityService securityService;
    @Resource private ServerConfigurationService serverConfigurationService;
    @Resource private SessionManager sessionManager;
    @Resource private SiteService siteService;
    @Resource private ToolManager toolManager;
    @Resource private UserDirectoryService userDirectoryService;

    public String getCurrentUserId() {

        Session session = sessionManager.getCurrentSession();
        String userId = session.getUserId();
        log.debug("current user id is {}", userId);
        return userId;
    }

    public boolean getBooleanConfig(String key, boolean defaultValue) {
        return serverConfigurationService.getBoolean(key, defaultValue);
    }

	public boolean isUserAbleToEditAssessments(String gradebookUid) {
		return securityService.unlock(PERMISSION_EDIT_ASSIGNMENTS, siteService.siteReference(gradebookUid));
	}

	public boolean isUserAbleToGradeItemForStudent(String gradebookUid, Long itemId, String studentUid) throws IllegalArgumentException {	
		return isUserAbleToGradeOrViewItemForStudent(gradebookUid, itemId, studentUid, GradingConstants.gradePermission);
	}
	
	public boolean isUserAbleToViewItemForStudent(String gradebookUid, Long itemId, String studentUid) throws IllegalArgumentException {
		return isUserAbleToGradeOrViewItemForStudent(gradebookUid, itemId, studentUid, GradingConstants.viewPermission);
	}

    public String getGradeViewFunctionForUserForStudentForItem(String gradebookUid, Long itemId, String studentId) {

		if (itemId == null || studentId == null || gradebookUid == null) {
			throw new IllegalArgumentException("Null parameter(s) in SakaiProxyImpl.getGradeViewFunctionForUserForStudentForItem");
		}
		
		if (isUserAbleToGradeAll(gradebookUid)) {
			return GradingConstants.gradePermission;
		}
		
		String userId = getCurrentUserId();
		
		List<CourseSection> viewableSections = getViewableSections(gradebookUid);
        List<String> sectionIds = viewableSections.stream().map(CourseSection::getUuid).collect(Collectors.toList());
		
		if (isUserHasGraderPermissions(gradebookUid, userId)) {

			// get the map of authorized item (assignment) ids to grade/view function
			Map<Long, String> itemIdFunctionMap = gradingPermissionService.getAvailableItemsForStudent(gradebookUid, userId, studentId, viewableSections);
			
			if (itemIdFunctionMap == null || itemIdFunctionMap.isEmpty()) {
				return null;  // not authorized to grade/view any items for this student
			}
			
			String functionValueForItem = itemIdFunctionMap.get(itemId);
			String view = GradingConstants.viewPermission;
			String grade = GradingConstants.gradePermission;
			
			if (functionValueForItem != null) {
				if (functionValueForItem.equalsIgnoreCase(grade))
					return GradingConstants.gradePermission;
				
				if (functionValueForItem.equalsIgnoreCase(view))
					return GradingConstants.viewPermission;
			}
	
			return null;
			
		} else {
			// use OOTB permissions based upon TA section membership
			for (String sectionUuid : sectionIds) {
				if (isUserTAinSection(sectionUuid, Optional.empty()) && sectionAwareness.isSectionMemberInRole(sectionUuid, studentId, Role.STUDENT)) {
					return GradingConstants.gradePermission;
				}
			}
	
			return null;
		}
	}

	public boolean isUserAbleToViewOwnGrades(String gradebookUid) {
		return securityService.unlock(PERMISSION_VIEW_OWN_GRADES, siteService.siteReference(gradebookUid));
	}

    private boolean isUserAbleToGradeOrViewItemForStudent(String gradebookUid, Long itemId, String studentUid, String function) throws IllegalArgumentException {

		if (itemId == null || studentUid == null || function == null) {
			throw new IllegalArgumentException("Null parameter(s) in AuthzSectionsServiceImpl.isUserAbleToGradeItemForStudent");
		}
		
		if (isUserAbleToGradeAll(gradebookUid)) {
			return true;
		}
		
		String userUid = getCurrentUserId();
		
		List<CourseSection> viewableSections = getViewableSections(gradebookUid);
		List<String> sectionIds = new ArrayList<>();
		if (viewableSections != null && !viewableSections.isEmpty()) {
			for (CourseSection section : viewableSections) {
				sectionIds.add(section.getUuid());
			}
		}
		
		if (isUserHasGraderPermissions(gradebookUid)) {

			// get the map of authorized item (assignment) ids to grade/view function
			Map itemIdFunctionMap = gradingPermissionService.getAvailableItemsForStudent(gradebookUid, userUid, studentUid, viewableSections);
			
			if (itemIdFunctionMap == null || itemIdFunctionMap.isEmpty()) {
				return false;  // not authorized to grade/view any items for this student
			}
			
			String functionValueForItem = (String)itemIdFunctionMap.get(itemId);
			String view = GradingConstants.viewPermission;
			String grade = GradingConstants.gradePermission;
			
			if (functionValueForItem != null) {
				if (function.equalsIgnoreCase(grade) && functionValueForItem.equalsIgnoreCase(grade)) {
					return true;
                }
				
				if (function.equalsIgnoreCase(view) && (functionValueForItem.equalsIgnoreCase(grade) || functionValueForItem.equalsIgnoreCase(view))) {
					return true;
                }
			}
	
			return false;
		} else {
			// use OOTB permissions based upon TA section membership
			for (String sectionId : sectionIds) {
				if (isUserTAinSection(sectionId, Optional.empty()) && sectionAwareness.isSectionMemberInRole(sectionId, studentUid, Role.STUDENT)) {
					return true;
				}
			}
	
			return false;
		}
	}

	public boolean isUserHasGraderPermissions(String gradebookUid) {
	    return isUserHasGraderPermissions(gradebookUid, getCurrentUserId());
	}

    public boolean isUserHasGraderPermissions(String gradebookUid, String userId) {

		String userUid = getCurrentUserId();
		List permissions = gradingPermissionService.getGraderPermissionsForUser(gradebookUid, userUid);
		return permissions != null && permissions.size() > 0;
	}

    public List<CourseSection> getViewableSections(String gradebookUid) {

		List<CourseSection> viewableSections = new ArrayList<>();
		
		List<CourseSection> allSections = sectionAwareness.getSections(gradebookUid);

		if (allSections == null || allSections.isEmpty()) {
			return viewableSections;
		}
		
		if (isUserAbleToGradeAll(gradebookUid)) {
			return allSections;
		}

		Map<String, CourseSection> sectionIdCourseSectionMap
            = allSections.stream().collect(Collectors.toMap(CourseSection::getUuid, Function.identity()));

		String userUid = getCurrentUserId();

		if (isUserHasGraderPermissions(gradebookUid)) {	

			List viewableSectionIds =  gradingPermissionService.getViewableGroupsForUser(gradebookUid, userUid, new ArrayList(sectionIdCourseSectionMap.keySet()));
			if (viewableSectionIds != null && !viewableSectionIds.isEmpty()) {
				for (Iterator idIter = viewableSectionIds.iterator(); idIter.hasNext();) {
					String sectionUuid = (String) idIter.next();
					CourseSection viewableSection = sectionIdCourseSectionMap.get(sectionUuid);
					if (viewableSection != null)
						viewableSections.add(viewableSection);
				}
			}
		} else {
			// return all sections that the current user is a TA for
			for (Map.Entry<String, CourseSection> entry : sectionIdCourseSectionMap.entrySet()) {
	            String sectionUuid = entry.getKey();
				if (isUserTAinSection(sectionUuid, Optional.empty())) {
					CourseSection viewableSection = sectionIdCourseSectionMap.get(sectionUuid);
					if (viewableSection != null)
						viewableSections.add(viewableSection);
				}
			}
		}

    	//Collections.sort(viewableSections);

		return viewableSections;
	}

    private boolean isUserTAinSection(String sectionUid, Optional<String> userId) { 

        if (userId.isPresent()) {
            return sectionAwareness.isSectionMemberInRole(sectionUid, userId.get(), Role.TA);
        } else {
            return sectionAwareness.isSectionMemberInRole(sectionUid, getCurrentUserId(), Role.TA);
        }
    }

	public boolean isUserAbleToGradeAll(String gradebookUid) {
		return isUserAbleToGradeAll(gradebookUid, getCurrentUserId());
	}

    /*
	public boolean isUserAbleToGradeAll(String gradebookUid, String userUid) {
		return sectionAwareness.isSiteMemberInRole(gradebookUid, userUid, Role.INSTRUCTOR);
	}
    */

    public boolean isUserAbleToGrade(String gradebookUid) {
        return isUserAbleToGrade(gradebookUid, getCurrentUserId());
    }

    public boolean isUserAbleToGrade(String gradebookUid, String userId) {

	    String ref = siteService.siteReference(gradebookUid);

	    try {
	        User user = userDirectoryService.getUser(userId);
	        return securityService.unlock(user, PERMISSION_GRADE_ALL, ref) || securityService.unlock(user, PERMISSION_GRADE_SECTION, ref);
	    } catch (UserNotDefinedException unde) {
	        log.warn("User not found for userUid: {}", userId);
	        return false;
	    }
	}

	public boolean isUserAbleToGradeAll(String gradebookUid, String userId) {

	    String ref = siteService.siteReference(gradebookUid);

	    try {
	        User user = userDirectoryService.getUser(userId);
	        return securityService.unlock(user, PERMISSION_GRADE_ALL, ref);
	    } catch (UserNotDefinedException unde) {
	        log.warn("User not found for userUid: {}", userId);
	        return false;
	    }
	}

	public boolean isUserAbleToViewStudentNumbers(String gradebookUid) {
		return hasPermission(gradebookUid, PERMISSION_VIEW_STUDENT_NUMBERS, Optional.empty());
	}

    private boolean hasPermission(String gradebookUid, String permission, Optional<String> userId) {

        String ref = siteService.siteReference(gradebookUid);

	    try {
	        User user = userDirectoryService.getUser(userId.orElse(getCurrentUserId()));
	        return securityService.unlock(user, permission, ref);
	    } catch (UserNotDefinedException unde) {
	        log.warn("User not found for userUid: {}", userId);
	        return false;
	    }
    }

    public boolean isCurrentUserFromGroup(String gradebookUid, String studentOrGroupId) {

		boolean isFromGroup = false;
		try {
			Site s = siteService.getSite(gradebookUid);
			Group g = s.getGroup(studentOrGroupId);
			isFromGroup = (g != null) && (g.getMember(getCurrentUserId()) != null);
		} catch (Exception e) {
			// Id not found
			log.error("Error in isCurrentUserFromGroup: ", e);
		}
		return isFromGroup;
	}

    /**
     * Get the list of students for the given gradebook
     *
     * @param gradebook the gradebook for the site
     * @return a list of uuids for the students
     */
    public List<String> getStudentsForGradebook(String gradebookUid) {

        return sectionAwareness.getSiteMembersInRole(gradebookUid, Role.STUDENT).stream()
            .map(EnrollmentRecord::getUser)
            .map(org.sakaiproject.section.api.coursemanagement.User::getUserUid)
            .collect(Collectors.toList());
    }

    public Map findMatchingEnrollmentsForItem(String gradebookUid, Long categoryId, int gbCategoryType, String optionalSearchString, String optionalSectionUid) {

	    String userUid = getCurrentUserId();
		return findMatchingEnrollmentsForItemOrCourseGrade(userUid, gradebookUid, categoryId, gbCategoryType, optionalSearchString, optionalSectionUid, false);
	}

	public Map findMatchingEnrollmentsForItemForUser(String userUid, String gradebookUid, Long categoryId, int gbCategoryType, String optionalSearchString, String optionalSectionUid) {
        return findMatchingEnrollmentsForItemOrCourseGrade(userUid, gradebookUid, categoryId, gbCategoryType, optionalSearchString, optionalSectionUid, false);
	}

	public Map<EnrollmentRecord, String> findMatchingEnrollmentsForViewableCourseGrade(String gradebookUid, int gbCategoryType, String optionalSearchString, String optionalSectionUid) {

	    String userUid = getCurrentUserId();
		return findMatchingEnrollmentsForItemOrCourseGrade(userUid, gradebookUid, null, gbCategoryType, optionalSearchString, optionalSectionUid, true);
	}

    /**
     * @param userUid
     * @param gradebookUid
     * @param categoryId
     * @param optionalSearchString
     * @param optionalSectionUid
     * @param itemIsCourseGrade
     * @return Map of EnrollmentRecord --> View or Grade 
     */
    private Map findMatchingEnrollmentsForItemOrCourseGrade(String userUid, String gradebookUid, Long categoryId, int gbCategoryType, String optionalSearchString, String optionalSectionUid, boolean itemIsCourseGrade) {
        Map enrollmentMap = new HashMap();
        List filteredEnrollments = new ArrayList();
        
        if (optionalSearchString != null)
            filteredEnrollments = sectionAwareness.findSiteMembersInRole(gradebookUid, Role.STUDENT, optionalSearchString);
        else
            filteredEnrollments = sectionAwareness.getSiteMembersInRole(gradebookUid, Role.STUDENT);
        
        if (filteredEnrollments.isEmpty()) 
            return enrollmentMap;
        
        // get all the students in the filtered section, if appropriate
        Map studentsInSectionMap = new HashMap();
        if (optionalSectionUid !=  null) {
            List sectionMembers = sectionAwareness.getSectionMembersInRole(optionalSectionUid, Role.STUDENT);
            if (!sectionMembers.isEmpty()) {
                for(Iterator memberIter = sectionMembers.iterator(); memberIter.hasNext();) {
                    EnrollmentRecord member = (EnrollmentRecord) memberIter.next();
                    studentsInSectionMap.put(member.getUser().getUserUid(), member);
                }
            }
        }
        
        Map studentIdEnrRecMap = new HashMap();
        for (Iterator enrIter = filteredEnrollments.iterator(); enrIter.hasNext();) {
            EnrollmentRecord enr = (EnrollmentRecord) enrIter.next();
            String studentId = enr.getUser().getUserUid();
            if (optionalSectionUid != null) {
                if (studentsInSectionMap.containsKey(studentId)) {
                    studentIdEnrRecMap.put(studentId, enr);
                }
            } else {
                studentIdEnrRecMap.put(enr.getUser().getUserUid(), enr);
            }
        }
            
        if (isUserAbleToGradeAll(gradebookUid, userUid)) {
            List enrollments = new ArrayList(studentIdEnrRecMap.values());
            
            for (Iterator enrIter = enrollments.iterator(); enrIter.hasNext();) {
                EnrollmentRecord enr = (EnrollmentRecord) enrIter.next();
                enrollmentMap.put(enr, GradingConstants.gradePermission);
            }

        } else {
            Map sectionIdCourseSectionMap = new HashMap();
            List allSections = sectionAwareness.getSections(gradebookUid);
            for (Iterator sectionIter = allSections.iterator(); sectionIter.hasNext();) {
                CourseSection section = (CourseSection) sectionIter.next();
                sectionIdCourseSectionMap.put(section.getUuid(), section);
            }

            if (isUserHasGraderPermissions(gradebookUid, userUid)) {
                // user has special grader permissions that override default perms
                
                List myStudentIds = new ArrayList(studentIdEnrRecMap.keySet());
                
                List selSections = new ArrayList();
                if (optionalSectionUid == null) {  
                    // pass all sections
                    selSections = new ArrayList(sectionIdCourseSectionMap.values());
                } else {
                    // only pass the selected section
                    CourseSection section = (CourseSection) sectionIdCourseSectionMap.get(optionalSectionUid);
                    if (section != null)
                        selSections.add(section);
                }
                
                Map viewableEnrollees = new HashMap();
                if (itemIsCourseGrade) {
                    viewableEnrollees = gradingPermissionService.getCourseGradePermission(gradebookUid, userUid, myStudentIds, selSections);
                } else {
                    viewableEnrollees = gradingPermissionService.getStudentsForItem(gradebookUid, userUid, myStudentIds, gbCategoryType, categoryId, selSections);
                }
                
                if (!viewableEnrollees.isEmpty()) {
                    for (Iterator<Map.Entry<String, EnrollmentRecord>> enrIter = viewableEnrollees.entrySet().iterator(); enrIter.hasNext();) {
                        Map.Entry<String, EnrollmentRecord> entry = enrIter.next();
                        String studentId = entry.getKey();
                        EnrollmentRecord enrRec = (EnrollmentRecord)studentIdEnrRecMap.get(studentId);
                        if (enrRec != null) {
                            enrollmentMap.put(enrRec, (String)viewableEnrollees.get(studentId));
                        }
                    }
                }

            } else { 
                // use default section-based permissions
                enrollmentMap = getEnrollmentMapUsingDefaultPermissions(userUid, studentIdEnrRecMap, sectionIdCourseSectionMap, optionalSectionUid);
            }
        }

        return enrollmentMap;
    }

    /**
	 * 
	 * @param userUid
	 * @param studentIdEnrRecMap
	 * @param sectionIdCourseSectionMap
	 * @param optionalSectionUid
	 * @return Map of EnrollmentRecord to function view/grade using the default permissions (based on TA section membership)
	 */
	private Map getEnrollmentMapUsingDefaultPermissions(String userUid, Map studentIdEnrRecMap, Map sectionIdCourseSectionMap, String optionalSectionUid) {
		// Determine the current user's section memberships
		Map enrollmentMap = new HashMap();
		List availableSections = new ArrayList();
		if (optionalSectionUid != null && isUserTAinSection(optionalSectionUid, Optional.of(userUid))) {
			if (sectionIdCourseSectionMap.containsKey(optionalSectionUid))
				availableSections.add(optionalSectionUid);
		} else {
			for (Iterator iter = sectionIdCourseSectionMap.keySet().iterator(); iter.hasNext(); ) {
				String sectionUuid = (String)iter.next();
				if (isUserTAinSection(sectionUuid, Optional.of(userUid))) {
					availableSections.add(sectionUuid);
				}
			}
		}
		
		// Determine which enrollees are in these sections
		Map uniqueEnrollees = new HashMap();
		for (Iterator iter = availableSections.iterator(); iter.hasNext(); ) {
			String sectionUuid = (String)iter.next();
			List sectionEnrollments = sectionAwareness.getSectionMembersInRole(sectionUuid, Role.STUDENT);
			for (Iterator eIter = sectionEnrollments.iterator(); eIter.hasNext(); ) {
				EnrollmentRecord enr = (EnrollmentRecord)eIter.next();
				uniqueEnrollees.put(enr.getUser().getUserUid(), enr);
			}
		}
		
		// Filter out based upon the original filtered students
		for (Iterator iter = studentIdEnrRecMap.keySet().iterator(); iter.hasNext(); ) {
			String enrId = (String)iter.next();
			if (uniqueEnrollees.containsKey(enrId)) {
				enrollmentMap.put(studentIdEnrRecMap.get(enrId), GradingConstants.gradePermission);
			}
		}
		
		return enrollmentMap;
	}

    public Set<String> getAllStudentUids(String gradebookUid) {

		final List<EnrollmentRecord> enrollments = sectionAwareness.getSiteMembersInRole(gradebookUid, Role.STUDENT);
        return enrollments.stream().map(e -> e.getUser().getUserUid()).collect(Collectors.toSet());
	}

	public void postEvent(String event, String ref) {
		eventTrackingService.post(eventTrackingService.newEvent(event, ref, true));
    }

    public boolean isSuperUser() {
        return securityService.isSuperUser();
    }

    /**
	 * Determines whether to update a grade record when there have been no changes. This is useful when we need to update only
	 * gb_grade_record_t's 'DATE_RECORDED' field for instance. Generally uses the sakai.property
	 * 'gradebook.externalAssessments.updateSameScore', but a site property by the same name can override it. That is to say, the site
	 * property is checked first, and if it is not present, the sakai.property is used.
	 */
	public boolean isUpdateSameScore() {

		String siteProperty = null;
		try {
			String siteId = toolManager.getCurrentPlacement().getContext();
			Site site = siteService.getSite(siteId);
			siteProperty = site.getProperties().getProperty(UPDATE_SAME_SCORE_PROP);
		} catch (Exception e) {
			// Can't access site property. Leave it set to null
		}

		// Site property override not set. Use setting in sakai.properties
		if (siteProperty == null) {
			return serverConfigurationService.getBoolean(UPDATE_SAME_SCORE_PROP, UPDATE_SAME_SCORE_PROP_DEFAULT);
		}

		return Boolean.TRUE.toString().equals(siteProperty);
	}
}
