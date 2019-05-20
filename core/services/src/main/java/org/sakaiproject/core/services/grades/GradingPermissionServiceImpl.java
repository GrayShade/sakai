/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.core.services.grades;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.section.api.SectionAwareness;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.section.api.coursemanagement.ParticipationRecord;
import org.sakaiproject.section.api.facade.Role;
import org.sakaiproject.core.api.grades.GradingPermissionService;
import org.sakaiproject.core.api.grades.GradingPersistenceManager;
import org.sakaiproject.core.api.grades.GradingService;
import org.sakaiproject.core.api.grades.GraderPermission;
import org.sakaiproject.core.api.grades.PermissionDefinition;
import org.sakaiproject.core.api.grades.SakaiProxy;
import org.sakaiproject.core.persistence.grades.model.Category;
import org.sakaiproject.core.persistence.grades.model.Gradebook;
import org.sakaiproject.core.persistence.grades.model.GradebookAssignment;
import org.sakaiproject.core.persistence.grades.model.Permission;
import org.sakaiproject.core.utils.grades.GradingConstants;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class GradingPermissionServiceImpl implements GradingPermissionService {

    @Resource
    private SectionAwareness sectionAwareness;

    @Resource
    private GradingPersistenceManager persistence;

    @Resource
    private GradingService gradingService;

    @Resource
    private SakaiProxy sakaiProxy;
    
    public List<Long> getCategoriesForUser(Long gradebookId, String userId, List<Long> categoryIdList) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getCategoriesForUser");
        }
        
        List<Permission> anyCategoryPermission = persistence.getPermissionsForUserAnyCategory(gradebookId, userId);
        if (anyCategoryPermission != null && anyCategoryPermission.size() > 0 ) {
            return categoryIdList;
        } else {
            List<Long> returnCatIds = new ArrayList<>();
            for (Permission perm : persistence.getPermissionsForUserForCategory(gradebookId, userId, categoryIdList)) {
                if (perm != null && !returnCatIds.contains(perm.getCategoryId())) {
                    returnCatIds.add(perm.getCategoryId());
                }
            }
            
            return returnCatIds;
        }
    }
    
    public List<Long> getCategoriesForUserForStudentView(Long gradebookId, String userId, String studentId,
            List<Long> categoriesIds, List<String> sectionIds) throws IllegalArgumentException {

        if (gradebookId == null || userId == null || studentId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getCategoriesForUser");
        }
        
        List<Long> returnCategoryList = new ArrayList<>();
        //Map categoryMap = new HashMap();  // to keep the elements unique
        if (categoriesIds == null || categoriesIds.isEmpty()) {
            return returnCategoryList;
        }
        
        List<Permission> graderPermissions = persistence.getPermissionsForUser(gradebookId, userId);
        if (graderPermissions == null || graderPermissions.isEmpty()) {
            return returnCategoryList;
        }
        
        List<String> studentSections = new ArrayList<String>();
        
        if (sectionIds != null) {
            for (String sectionId : sectionIds) {
                if (sectionId != null && sectionAwareness.isSectionMemberInRole(sectionId, studentId, Role.STUDENT)) {
                    studentSections.add(sectionId);
                }
            }
        }

        for (Permission perm : graderPermissions) {
            String sectionId = perm.getGroupId();
            if (studentSections.contains(sectionId) || sectionId == null) {
                Long catId = perm.getCategoryId();
                if (catId == null) {
                    return returnCategoryList;
                } else{
                    returnCategoryList.add(catId);
                }
            }
        }
        
        return returnCategoryList;
    }
    
    public boolean getPermissionForUserForAllAssignment(Long gradebookId, String userId) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getPermissionForUserForAllAssignment");
        }
        
        List<Permission> anyCategoryPermission = persistence.getPermissionsForUserAnyCategory(gradebookId, userId);

        if (anyCategoryPermission != null && anyCategoryPermission.size() > 0 ) {
            return true;
        }

        return false;
    }
    
    public boolean getPermissionForUserForAllAssignmentForStudent(Long gradebookId, String userId, String studentId, List<String> sectionIds) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getPermissionForUserForAllAssignment");
        }
        
        List<Permission> graderPermissions = persistence.getPermissionsForUser(gradebookId, userId);
        if (graderPermissions == null || graderPermissions.isEmpty()) {
            return false;
        }
        
        for (Permission perm : graderPermissions) {
            String sectionId = perm.getGroupId();
            if (sectionId == null || (sectionIds.contains(sectionId) && sectionAwareness.isSectionMemberInRole(sectionId, studentId, Role.STUDENT))) {
                if (perm.getCategoryId() == null) {
                    return true;
                }
            }
        }

        return false;
    }

    public Map<String, String> getStudentsForItem(Long gradebookId, String userId, List<String> studentIds, int cateType, Long categoryId, List<CourseSection> courseSections)
    throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getStudentsForItem");
        }
        if (cateType != GradingConstants.CATEGORY_TYPE_ONLY_CATEGORY && cateType != GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY && cateType != GradingConstants.CATEGORY_TYPE_NO_CATEGORY) {
            throw new IllegalArgumentException("Invalid category type in GradebookPermissionServiceImpl.getStudentsForItem");
        }

        if (studentIds != null) {
            Map<String, List<String>> sectionIdStudentIdsMap = getSectionIdStudentIdsMap(courseSections, studentIds);
            if (cateType == GradingConstants.CATEGORY_TYPE_NO_CATEGORY) {
                List<Permission> perms = persistence.getPermissionsForUserAnyGroup(gradebookId, userId);

                Map<String, String> studentMap = new HashMap<>();
                if (perms != null && perms.size() > 0) {
                    boolean view = false;
                    boolean grade = false;
                    for (Permission perm : perms) {
                        if (perm != null && perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                            grade = true;
                            break;
                        }
                        if (perm != null && perm.getFunction().equalsIgnoreCase(GradingConstants.viewPermission)) {
                            view = true;
                        }
                    }
                    for (String studentId : studentIds) {
                        if (grade == true) {
                            studentMap.put(studentId, GradingConstants.gradePermission);
                        } else if (view == true) {
                            studentMap.put(studentId, GradingConstants.viewPermission);
                        }
                    }
                }

                perms = persistence.getPermissionsForUser(gradebookId, userId);

                if (perms != null) {
                    Map<String, String> studentMapForGroups = filterPermissionForGrader(perms, studentIds, sectionIdStudentIdsMap);
                    for (Map.Entry<String, String> entry : studentMapForGroups.entrySet()) {
                        String key = entry.getKey();
                        if ((studentMap.containsKey(key) && ((String)studentMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                || !studentMap.containsKey(key))
                            studentMap.put(key, studentMapForGroups.get(key));
                    }
                }

                return studentMap;
            } else {
                List<Long> cateList = new ArrayList<>();
                cateList.add(categoryId);
                List<Permission> perms = persistence.getPermissionsForUserAnyGroupForCategory(gradebookId, userId, cateList);

                Map<String, String> studentMap = new HashMap<>();
                if (perms != null && perms.size() > 0) {
                    boolean view = false;
                    boolean grade = false;
                    for (Permission perm : perms) {
                        if (perm != null && perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                            grade = true;
                            break;
                        }
                        if (perm != null && perm.getFunction().equalsIgnoreCase(GradingConstants.viewPermission)) {
                            view = true;
                        }
                    }
                    for (String studentId : studentIds) {
                        if (grade == true) {
                            studentMap.put(studentId, GradingConstants.gradePermission);
                        } else if(view == true) {
                            studentMap.put(studentId, GradingConstants.viewPermission);
                        }
                    }
                }
                perms = persistence.getPermissionsForUserAnyGroupAnyCategory(gradebookId, userId);

                if (perms != null) {

                    Map<String, String> studentMapForGroups = filterPermissionForGraderForAllStudent(perms, studentIds);
                    for (Entry<String, String> entry : studentMapForGroups.entrySet()) {
                        String key = entry.getKey();
                        if ((studentMap.containsKey(key) && ((String)studentMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                || !studentMap.containsKey(key)) {
                            studentMap.put(key, entry.getValue());
                        }
                    }
                }
                
                if (courseSections != null && !courseSections.isEmpty()) {
                    final List<String> groupIds
                        = courseSections.stream().filter(s -> s != null).map(CourseSection::getUuid).collect(Collectors.toList());

                    perms = persistence.getPermissionsForUserForGoupsAnyCategory(gradebookId, userId, groupIds);
                    if (perms != null) {
                        Map<String, String> studentMapForGroups = filterPermissionForGrader(perms, studentIds, sectionIdStudentIdsMap);
                        for (Entry<String, String> entry : studentMapForGroups.entrySet()) {
                            String key = entry.getKey();
                            if ((studentMap.containsKey(key) && ((String)studentMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                    || !studentMap.containsKey(key)) {
                                studentMap.put(key, entry.getValue());
                            }
                        }
                    }
                }

                perms = persistence.getPermissionsForUserForCategory(gradebookId, userId, cateList);
                if (perms != null) {
                    Map<String, String> studentMapForGroups = filterPermissionForGrader(perms, studentIds, sectionIdStudentIdsMap);
                    for (Entry<String, String> entry : studentMapForGroups.entrySet()) {
                        String key = entry.getKey();
                        if ((studentMap.containsKey(key) && ((String)studentMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                || !studentMap.containsKey(key)) {
                            studentMap.put(key, entry.getValue());
                        }
                    }
                }

                return studentMap;
            }
        }
        return null;
    }
    
    public Map<String, String> getStudentsForItem(String gradebookUid, String userId, List<String> studentIds, int cateType, Long categoryId, List<CourseSection> courseSections)
    throws IllegalArgumentException {

        if (gradebookUid == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getStudentsForItem");
        }
    
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        return getStudentsForItem(gradebookId, userId, studentIds, cateType, categoryId, courseSections);
    }

    public List<String> getViewableGroupsForUser(Long gradebookId, String userId, List<String> groupIds) {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getViewableSectionsForUser");
        }
        
        if (groupIds == null || groupIds.size() == 0) {
            return null;
        }
        
        List<Permission> anyGroupPermission = persistence.getPermissionsForUserAnyGroup(gradebookId, userId);
        if (anyGroupPermission != null && anyGroupPermission.size() > 0 ) {
            return groupIds;
        } else {
            List<Permission> permList = persistence.getPermissionsForUserForGroup(gradebookId, userId, groupIds);
            
            List<String> filteredGroups = new ArrayList<>();
            for (String groupId : groupIds) {
                if (groupId != null) {
                    for (Permission perm : permList) {
                        if (perm != null && perm.getGroupId().equals(groupId)) {
                            filteredGroups.add(groupId);
                            break;
                        }
                    }
                }
            }
            return filteredGroups;
        }
    }
    
    public List getViewableGroupsForUser(String gradebookUid, String userId, List groupIds) {

        if (gradebookUid == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getViewableSectionsForUser");
        }
    
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        
        return getViewableGroupsForUser(gradebookId, userId, groupIds);
    }
    
    public List<Permission> getGraderPermissionsForUser(Long gradebookId, String userId) {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getPermissionsForUser");
        }
        
        return persistence.getPermissionsForUser(gradebookId, userId);
    }
    
    public List<Permission> getGraderPermissionsForUser(String gradebookUid, String userId) {

        if (gradebookUid == null || userId == null) {
            throw new IllegalArgumentException("Null gradebookUid or userId passed to getGraderPermissionsForUser");
        }
        
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        
        return persistence.getPermissionsForUser(gradebookId, userId);
    }
    
    private Map<String, String> filterPermissionForGrader(List<Permission> perms, List<String> studentIds, Map<String, List<String>> sectionIdStudentIdsMap) {

        if (perms != null) {
            Map<String, String> permMap = new HashMap<>();
            for (Permission perm : perms) {
                if (perm != null) {
                    if (permMap.containsKey(perm.getGroupId()) && ((String)permMap.get(perm.getGroupId())).equalsIgnoreCase(GradingConstants.viewPermission)) {
                        if (perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission))
                            permMap.put(perm.getGroupId(), GradingConstants.gradePermission);
                    } else if (!permMap.containsKey(perm.getGroupId())) {
                        permMap.put(perm.getGroupId(), perm.getFunction());
                    }
                }
            }
            Map<String, String> studentMap = new HashMap<>();

            if (perms != null) {
                for (String studentId : studentIds) {
                    if (sectionIdStudentIdsMap != null) {
                        for (Map.Entry<String, List<String>> entry : sectionIdStudentIdsMap.entrySet()) {
                            String grpId = entry.getKey();
                            List<String> sectionMembers = entry.getValue();

                            if (sectionMembers != null && sectionMembers.contains(studentId) && permMap.containsKey(grpId)) {
                                if (studentMap.containsKey(studentId) && ((String)studentMap.get(studentId)).equalsIgnoreCase(GradingConstants.viewPermission)) {
                                    if (((String)permMap.get(grpId)).equalsIgnoreCase(GradingConstants.gradePermission)) {
                                        studentMap.put(studentId, GradingConstants.gradePermission);
                                    }
                                } else if (!studentMap.containsKey(studentId)) {
                                    studentMap.put(studentId, permMap.get(grpId));
                                }
                            }
                        }
                    }
                }
            }
            return studentMap;
        } else {
            return new HashMap<String, String>();
        }
    }

    private Map<String, String> filterPermissionForGraderForAllStudent(List<Permission> perms, List<String> studentIds) {

        if (perms != null) {
            Boolean grade = false;
            Boolean view = false;
            for (Permission perm : perms) {
                if (perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                    grade = true;
                    break;
                } else if (perm.getFunction().equalsIgnoreCase(GradingConstants.viewPermission)) {
                    view = true;
                }
            }

            Map<String, String> studentMap = new HashMap<>();

            if (grade || view) {
                for (String studentId : studentIds) {
                    if (grade) {
                        studentMap.put(studentId, GradingConstants.gradePermission);
                    } else if (view) {
                        studentMap.put(studentId, GradingConstants.viewPermission);
                    }
                }
            }
            return studentMap;
        } else {
            return new HashMap<String, String>();
        }
    }

    private Map filterPermissionForGraderForAllAssignments(List<Permission> perms, List<GradebookAssignment> assignmentList) {

        if (perms != null) {
            Boolean grade = false;
            Boolean view = false;
            for (Permission perm : perms) {
                if (perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                    grade = true;
                    break;
                } else if (perm.getFunction().equalsIgnoreCase(GradingConstants.viewPermission)) {
                    view = true;
                }
            }

            Map<Long, String> assignMap = new HashMap<>();

            if (grade || view) {
                for (GradebookAssignment assign : assignmentList) {
                    if (grade && assign != null) {
                        assignMap.put(assign.getId(), GradingConstants.gradePermission);
                    } else if(view && assign != null) {
                        assignMap.put(assign.getId(), GradingConstants.viewPermission);
                    }
                }
            }
            return assignMap;
        } else {
            return new HashMap();
        }
    }

    //private Map<Long, String> getAvailableItemsForStudent(Gradebook gradebook, String userId, String studentId, Map<String, CourseSection> sectionIdCourseSectionMap, Map catIdCategoryMap, List assignments, List<Permission> permsForUserAnyGroup, List allPermsForUser, List permsForAnyGroupForCategories, List permsForUserAnyGroupAnyCategory, List permsForGroupsAnyCategory, List permsForUserForCategories, Map sectionIdStudentIdsMap) throws IllegalArgumentException {
    private Map<Long, String> getAvailableItemsForStudent(Gradebook gradebook, String userId, String studentId,
                    Map<Long, Category> catIdCategoryMap, List<GradebookAssignment> assignments,
                    List<Permission> permsForUserAnyGroup, List allPermsForUser,
                    List<Permission> permsForAnyGroupForCategories, List<Permission> permsForUserAnyGroupAnyCategory,
                    List<Permission> permsForGroupsAnyCategory, List<Permission> permsForUserForCategories,
                    Map<String, List<String>> sectionIdStudentIdsMap) throws IllegalArgumentException {

        if (gradebook == null || userId == null || studentId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getAvailableItemsForStudent");
        }
        
        List<Category> cateList = new ArrayList(catIdCategoryMap.values());
        
        if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_NO_CATEGORY) {
            Map<Long, String> assignMap = new HashMap<>();
            if (permsForUserAnyGroup != null && permsForUserAnyGroup.size() > 0) {
                boolean view = false;
                boolean grade = false;
                for (Permission perm : permsForUserAnyGroup) {
                    if (perm != null && perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                        grade = true;
                        break;
                    }
                    if (perm != null && perm.getFunction().equalsIgnoreCase(GradingConstants.viewPermission)) {
                        view = true;
                    }
                }
                for (GradebookAssignment as : assignments) {
                    if (grade == true && as != null) {
                        assignMap.put(as.getId(), GradingConstants.gradePermission);
                    } else if (view == true && as != null) {
                        assignMap.put(as.getId(), GradingConstants.viewPermission);
                    }
                }
            }

            if (allPermsForUser != null) {
                Map<Long, String> assignsMapForGroups = filterPermissionForGrader(allPermsForUser, studentId, assignments, sectionIdStudentIdsMap);
                for (Map.Entry<Long, String> entry : assignsMapForGroups.entrySet()) {
                    Long key = entry.getKey();
                    if ((assignMap.containsKey(key) && ((String)assignMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                            || !assignMap.containsKey(key)) {
                        assignMap.put(key, entry.getValue());
                    }
                }
            }
            return assignMap;
        } else {
            Map assignMap = new HashMap();
            if (permsForAnyGroupForCategories != null && permsForAnyGroupForCategories.size() > 0) {
                for (Permission perm : permsForAnyGroupForCategories) {
                    if (perm != null) {
                        if (perm.getCategoryId() != null) {
                            for (Category cate : cateList) {
                                if (cate != null && cate.getId().equals(perm.getCategoryId())) {
                                    List<GradebookAssignment> assignmentList = cate.getAssignmentList();
                                    if (assignmentList != null) {
                                        for (GradebookAssignment as : assignmentList) {
                                            if (as != null) {
                                                Long assignId = as.getId();
                                                if (as.getCategory() != null) {
                                                    if (assignMap.containsKey(assignId) && ((String)assignMap.get(assignId)).equalsIgnoreCase(GradingConstants.viewPermission)) {
                                                        if (perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                                                            assignMap.put(assignId, GradingConstants.gradePermission);
                                                        }
                                                    } else if(!assignMap.containsKey(assignId)) {
                                                        assignMap.put(assignId, perm.getFunction());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }               
            }

            if (permsForUserAnyGroupAnyCategory != null) {
                Map<Long, String> assignMapForGroups = filterPermissionForGraderForAllAssignments(permsForUserAnyGroupAnyCategory, assignments);
                for (Entry<Long, String> entry : assignMapForGroups.entrySet()) {
                    Long key = entry.getKey();
                    if ((assignMap.containsKey(key) && ((String)assignMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                            || !assignMap.containsKey(key)) {
                        assignMap.put(key, entry.getValue());
                    }
                }
            }
            
            if (permsForGroupsAnyCategory != null) {
                Map<Long, String> assignMapForGroups = filterPermissionForGrader(permsForGroupsAnyCategory, studentId, assignments, sectionIdStudentIdsMap);
                for(Entry<Long, String> entry : assignMapForGroups.entrySet()) {
                    Long key = entry.getKey();
                    if ((assignMap.containsKey(key) && ((String)assignMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                            || !assignMap.containsKey(key)) {
                        assignMap.put(key, entry.getValue());
                    }
                }
            }

            if (permsForUserForCategories != null) {
                Map<Long, String> assignMapForGroups = filterPermissionForGraderForCategory(permsForUserForCategories, studentId, cateList, sectionIdStudentIdsMap);
                if (assignMapForGroups != null) {
                    for(Entry<Long, String> entry : assignMapForGroups.entrySet()) {
                        Long key = entry.getKey();
                        if ((assignMap.containsKey(key) && ((String)assignMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                || !assignMap.containsKey(key)) {
                            assignMap.put(key, entry.getValue());
                        }
                    }
                }
            }

            return assignMap;
        }
    }
    
    public Map<Long, String> getAvailableItemsForStudent(Long gradebookId, String userId, String studentId, Collection<CourseSection> courseSections) throws IllegalArgumentException {

        if (gradebookId == null || userId == null || studentId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getAvailableItemsForStudent");
        }

        List<Category> categories = persistence.getCategoriesWithAssignments(gradebookId);
        Map<Long, Category> catIdCategoryMap = new HashMap<>();
        if (!categories.isEmpty()) {
            for (Category cat : categories) {
                if (cat != null) {
                    catIdCategoryMap.put(cat.getId(), cat);
                }
            }
        }
        Map<String, CourseSection> sectionIdCourseSectionMap = new HashMap<>();
        if (!courseSections.isEmpty()) {
            for (CourseSection section : courseSections) {
                if (section != null) {
                    sectionIdCourseSectionMap.put(section.getUuid(), section);
                }
            }
        }
        List<String> studentIds = new ArrayList<>();
        studentIds.add(studentId);
        Map<String, List<String>> sectionIdStudentIdsMap = getSectionIdStudentIdsMap(courseSections, studentIds);
        
        Gradebook gradebook = persistence.getGradebook(gradebookId);
        List<GradebookAssignment> assignments = persistence.getAssignments(gradebookId);
        List<Long> categoryIds = new ArrayList(catIdCategoryMap.keySet());
        List<String> groupIds = new ArrayList(sectionIdCourseSectionMap.keySet());
        
        // Retrieve all the different permission info needed here so not called repeatedly for each student
        List<Permission> permsForUserAnyGroup = persistence.getPermissionsForUserAnyGroup(gradebookId, userId);
        List<Permission> allPermsForUser = persistence.getPermissionsForUser(gradebookId, userId);
        List<Permission> permsForAnyGroupForCategories = persistence.getPermissionsForUserAnyGroupForCategory(gradebookId, userId, categoryIds);
        List<Permission> permsForUserAnyGroupAnyCategory = persistence.getPermissionsForUserAnyGroupAnyCategory(gradebookId, userId);
        List<Permission> permsForGroupsAnyCategory = persistence.getPermissionsForUserForGoupsAnyCategory(gradebookId, userId, groupIds);
        List<Permission> permsForUserForCategories = persistence.getPermissionsForUserForCategory(gradebookId, userId, categoryIds);
        
        //return getAvailableItemsForStudent(gradebook, userId, studentId, sectionIdCourseSectionMap, catIdCategoryMap, assignments, permsForUserAnyGroup, allPermsForUser, permsForAnyGroupForCategories, permsForUserAnyGroupAnyCategory, permsForGroupsAnyCategory, permsForUserForCategories, sectionIdStudentIdsMap);
        return getAvailableItemsForStudent(gradebook, userId, studentId, catIdCategoryMap, assignments, permsForUserAnyGroup, allPermsForUser, permsForAnyGroupForCategories, permsForUserAnyGroupAnyCategory, permsForGroupsAnyCategory, permsForUserForCategories, sectionIdStudentIdsMap);
    }
    
    public Map<Long, String> getAvailableItemsForStudent(String gradebookUid, String userId, String studentId, Collection<CourseSection> courseSections) throws IllegalArgumentException {

        if (gradebookUid == null || userId == null || studentId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getAvailableItemsForStudent");
        }
        
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        
        return getAvailableItemsForStudent(gradebookId, userId, studentId, courseSections);
    }

    private Map<Long, String> filterPermissionForGrader(List<Permission> perms, String studentId, List<GradebookAssignment> assignmentList, Map<String, List<String>> sectionIdStudentIdsMap) {

        if (perms != null) {
            Map<String, String> permMap = new HashMap<>();
            for (Permission perm : perms) {
                if (perm != null) {
                    if (permMap.containsKey(perm.getGroupId()) && ((String)permMap.get(perm.getGroupId())).equalsIgnoreCase(GradingConstants.viewPermission)) {
                        if (perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                            permMap.put(perm.getGroupId(), GradingConstants.gradePermission);
                        }
                    } else if (!permMap.containsKey(perm.getGroupId())) {
                        permMap.put(perm.getGroupId(), perm.getFunction());
                    }
                }
            }
            Map<Long, String> assignmentMap = new HashMap<>();

            if (perms != null && sectionIdStudentIdsMap != null) {
                for (GradebookAssignment ga : assignmentList) {
                    Long assignId = ga.getId();
                    for (Map.Entry<String, List<String>> entry : sectionIdStudentIdsMap.entrySet()) {
                        String grpId = entry.getKey();
                        List<String> sectionMembers = sectionIdStudentIdsMap.get(grpId);
                        
                        if (sectionMembers != null && sectionMembers.contains(studentId) && permMap.containsKey(grpId)) {
                            if (assignmentMap.containsKey(assignId) && ((String)assignmentMap.get(assignId)).equalsIgnoreCase(GradingConstants.viewPermission)) {
                                if (((String)permMap.get(grpId)).equalsIgnoreCase(GradingConstants.gradePermission)) {
                                    assignmentMap.put(assignId, GradingConstants.gradePermission);
                                }
                            } else if(!assignmentMap.containsKey(assignId)) {
                                assignmentMap.put(assignId, permMap.get(grpId));
                            }
                        }
                    }
                }
            }
            return assignmentMap;
        } else {
            return new HashMap<>();
        }
    }

    private  Map<Long, String> filterPermissionForGraderForCategory(List<Permission> perms, String studentId, List<Category> categoryList, Map<String, List<String>> sectionIdStudentIdsMap) {

        if (perms != null) {
            Map<Long, String> assignmentMap = new HashMap<>();
            
            for (Permission perm : perms) {
                if (perm != null && perm.getCategoryId() != null) {
                    for (Category cate : categoryList) {
                        if (cate != null && cate.getId().equals(perm.getCategoryId())) {
                            List<GradebookAssignment> assignmentList = cate.getAssignmentList();
                            if (assignmentList != null) {
                                for (GradebookAssignment as : assignmentList) {
                                    if (as != null && sectionIdStudentIdsMap != null) {
                                        Long assignId = as.getId();
                                        for (Map.Entry<String, List<String>> entry : sectionIdStudentIdsMap.entrySet()) {
                                            String grpId = entry.getKey();
                                            List sectionMembers = (List) sectionIdStudentIdsMap.get(grpId);

                                            if (sectionMembers != null && sectionMembers.contains(studentId) && as.getCategory() != null) {
                                                if (assignmentMap.containsKey(assignId) && grpId.equals(perm.getGroupId()) && ((String)assignmentMap.get(assignId)).equalsIgnoreCase(GradingConstants.viewPermission)) {
                                                    if (perm.getFunction().equalsIgnoreCase(GradingConstants.gradePermission)) {
                                                        assignmentMap.put(assignId, GradingConstants.gradePermission);
                                                    }
                                                } else if (!assignmentMap.containsKey(assignId) && grpId.equals(perm.getGroupId())) {
                                                    assignmentMap.put(assignId, perm.getFunction());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
            return assignmentMap;
        } else {
            return new HashMap();
        }
    }

    public Map<String, Map<Long, String>> getAvailableItemsForStudents(Long gradebookId, String userId, List<String> studentIds, Collection<CourseSection> courseSections) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getAvailableItemsForStudents");
        }
        
        Map<Long, Category> catIdCategoryMap = new HashMap<>();
        List<Category> categories = persistence.getCategoriesWithAssignments(gradebookId);
        if (categories != null && !categories.isEmpty()) {
            for (Category cat : categories) {
                if (cat != null) {
                    catIdCategoryMap.put(cat.getId(), cat);
                }
            }
        }
        Map<String, CourseSection> sectionIdCourseSectionMap = new HashMap<>();
        if (!courseSections.isEmpty()) {
            for (Iterator sectionIter = courseSections.iterator(); sectionIter.hasNext();) {
                CourseSection section = (CourseSection) sectionIter.next();
                if (section != null) {
                    sectionIdCourseSectionMap.put(section.getUuid(), section);
                }
            }
        }
        
        Map<String, List<String>> sectionIdStudentIdsMap = getSectionIdStudentIdsMap(courseSections, studentIds);
        
        Gradebook gradebook = persistence.getGradebook(gradebookId);
        List<GradebookAssignment> assignments = persistence.getAssignments(gradebookId);
        List<Long> categoryIds = new ArrayList(catIdCategoryMap.keySet());
        List<String> groupIds = new ArrayList(sectionIdCourseSectionMap.keySet());
        
        // Retrieve all the different permission info needed here so not called repeatedly for each student
        List permsForUserAnyGroup = persistence.getPermissionsForUserAnyGroup(gradebookId, userId);
        List allPermsForUser = persistence.getPermissionsForUser(gradebookId, userId);
        List permsForAnyGroupForCategories = persistence.getPermissionsForUserAnyGroupForCategory(gradebookId, userId, categoryIds);
        List permsForUserAnyGroupAnyCategory = persistence.getPermissionsForUserAnyGroupAnyCategory(gradebookId, userId);
        List permsForGroupsAnyCategory = persistence.getPermissionsForUserForGoupsAnyCategory(gradebookId, userId, groupIds);
        List permsForUserForCategories = persistence.getPermissionsForUserForCategory(gradebookId, userId, categoryIds);
        
        if (studentIds != null) {
            Map<String, Map<Long, String>> studentsMap = new HashMap<>();
            for (String studentId : studentIds) {
                if (studentId != null) {
                    Map<Long, String> assignMap = getAvailableItemsForStudent(gradebook, userId, studentId, catIdCategoryMap, assignments, permsForUserAnyGroup, allPermsForUser, permsForAnyGroupForCategories, permsForUserAnyGroupAnyCategory, permsForGroupsAnyCategory, permsForUserForCategories, sectionIdStudentIdsMap);
                    studentsMap.put(studentId, assignMap);
                }
            }
            return studentsMap;
        }

        return new HashMap<>();
    }
    
    public Map<String, Map<Long, String>> getAvailableItemsForStudents(String gradebookUid, String userId, List<String> studentIds, Collection<CourseSection> courseSections) throws IllegalArgumentException {

        if (gradebookUid == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getAvailableItemsForStudents");
        }
        
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        return getAvailableItemsForStudents(gradebookId, userId, studentIds, courseSections);
    }

    public Map<String, String> getCourseGradePermission(Long gradebookId, String userId, List<String> studentIds, List<CourseSection> courseSections) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getCourseGradePermission");
        }

        if (studentIds != null) {
            Map<String, String> studentsMap = new HashMap<>();
            Map<String, List<String>> sectionIdStudentIdsMap = getSectionIdStudentIdsMap(courseSections, studentIds);

            List perms = persistence.getPermissionsForUserAnyGroupAnyCategory(gradebookId, userId);
            if (perms != null) {
                Map<String, String> studentMapForGroups = filterPermissionForGraderForAllStudent(perms, studentIds);
                for (Map.Entry<String, String> entry : studentMapForGroups.entrySet()) {
                    String key = entry.getKey();
                    if ((studentsMap.containsKey(key) && ((String)studentsMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                            || !studentsMap.containsKey(key)) {
                        studentsMap.put(key, studentMapForGroups.get(key));
                    }
                }
            }

            List<String> groupIds = new ArrayList<>();
            if (courseSections != null) {
                for (CourseSection grp : courseSections) {
                    if (grp != null) {
                        groupIds.add(grp.getUuid());
                    }
                }
                
                perms = persistence.getPermissionsForUserForGoupsAnyCategory(gradebookId, userId, groupIds);
                if (perms != null) {
                    Map<String, String> studentMapForGroups = filterPermissionForGrader(perms, studentIds, sectionIdStudentIdsMap);
                    for (Entry<String, String> entry : studentMapForGroups.entrySet()) {
                        String key = entry.getKey();
                        if ((studentsMap.containsKey(key) && studentsMap.get(key).equalsIgnoreCase(GradingConstants.viewPermission))
                                || !studentsMap.containsKey(key)) {
                            studentsMap.put(key, entry.getValue());
                        }
                    }
                }
                
                Gradebook gradebook = persistence.getGradebook(gradebookId);
                if (gradebook != null && (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_ONLY_CATEGORY || 
                        gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY)) {
                    List<Category> cateList = persistence.getCategories(gradebookId);
                    
                    perms = persistence.getPermissionsForUserForGroup(gradebookId, userId, groupIds);
                    if (perms != null) {
                        Map<String, String> studentMapForGroups = filterForAllCategoryStudents(perms, studentIds, cateList, sectionIdStudentIdsMap);
                        for (Entry<String, String> entry : studentMapForGroups.entrySet()) {
                            String key = entry.getKey();
                            if ((studentsMap.containsKey(key) && ((String)studentsMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                    || !studentsMap.containsKey(key)) {
                                studentsMap.put(key, entry.getValue());
                            }
                        }
                    }
                    
                    List<Long> cateIdList = new ArrayList<>();
                    for (Category cate : cateList) {
                        if (cate != null)
                            cateIdList.add(cate.getId());
                    }
                    perms = persistence.getPermissionsForUserAnyGroupForCategory(gradebookId, userId, cateIdList);
                    if (perms != null && perms.size() > 0) {
                        Map<String, String> studentMapForGroups = filterForAllCategoryStudentsAnyGroup(perms, courseSections, studentIds, cateList);
                        for (Entry<String, String> entry : studentMapForGroups.entrySet()) {
                            String key = entry.getKey();
                            if ((studentsMap.containsKey(key) && ((String)studentsMap.get(key)).equalsIgnoreCase(GradingConstants.viewPermission))
                                    || !studentsMap.containsKey(key)) {
                                studentsMap.put(key, entry.getValue());
                            }
                        }
                    }
                }
            }

            return studentsMap;
        }
        return new HashMap<>();
    }
    
    public Map<String, String> getCourseGradePermission(String gradebookUid, String userId, List<String> studentIds, List<CourseSection> courseSections) throws IllegalArgumentException {

        if (gradebookUid == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getCourseGradePermission");
        }
    
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        return getCourseGradePermission(gradebookId, userId, studentIds, courseSections);
    }
    
    private Map<String, String> filterForAllCategoryStudents(List<Permission> perms, List<String> studentIds, List<Category> cateList, Map<String, List<String>> sectionIdStudentIdsMap) {

        if (perms != null && sectionIdStudentIdsMap != null && studentIds != null && cateList != null) {
            List<Long> cateIdList = new ArrayList<>();
            for (Category cate : cateList) {
                if (cate != null) {
                    cateIdList.add(cate.getId());
                }
            }

            Map<String, Map<Long, String>> studentCateMap = new HashMap<>();
            for (String studentId : studentIds) {
                studentCateMap.put(studentId, new HashMap<>());
                if (studentId != null) {
                    for (Map.Entry<String, List<String>> entry : sectionIdStudentIdsMap.entrySet()) {
                        String grpId = entry.getKey();
                        
                        if (grpId != null) {               
                            List<String> grpMembers = sectionIdStudentIdsMap.get(grpId);
                            if (grpMembers != null && !grpMembers.isEmpty() && grpMembers.contains(studentId)) {
                                for (Permission perm : perms) {
                                    if (perm != null && perm.getGroupId().equals(grpId) && perm.getCategoryId() != null && cateIdList.contains(perm.getCategoryId())) {
                                        Map<Long, String> cateMap = studentCateMap.get(studentId);
                                        if (cateMap.get(perm.getCategoryId()) == null || cateMap.get(perm.getCategoryId()).equals(GradingConstants.viewPermission)) {
                                            cateMap.put(perm.getCategoryId(), perm.getFunction());
                                        }
                                        studentCateMap.put(studentId, cateMap);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Map<String, String> studentPermissionMap = new HashMap<>();
            for (Entry<String, Map<Long, String>> perEntry : studentCateMap.entrySet()) {
                String studentId = perEntry.getKey();
                Map<Long, String> cateMap = perEntry.getValue();
                if (cateMap != null) {
                    for (Long existCateId : cateIdList) {
                        if (existCateId != null) {
                            boolean hasPermissionForCate = false;
                            String permission = null;
                            for (Entry<Long, String> entry : cateMap.entrySet()) {
                                Long cateId = entry.getKey();
                                if (cateId.equals(existCateId)) {
                                    hasPermissionForCate = true;
                                    permission = entry.getValue();
                                    break;
                                }
                            }
                            if (hasPermissionForCate && permission != null) {
                                if (studentPermissionMap.get(studentId) == null || studentPermissionMap.get(studentId).equals(GradingConstants.gradePermission)) {
                                    studentPermissionMap.put(studentId, permission);
                                }
                            } else if (!hasPermissionForCate) {
                                if (studentPermissionMap.get(studentId) != null) {
                                    studentPermissionMap.remove(studentId);
                                }
                            }
                        }
                    }
                }
            }
            return studentPermissionMap;
        }
        return new HashMap<String, String>();
    }

    private Map<String, String> filterForAllCategoryStudentsAnyGroup(List<Permission> perms, List courseSections, List<String> studentIds, List<Category> cateList) {

        if (perms != null && courseSections != null && studentIds != null && cateList != null) {   
            Map<Long, String> cateMap = new HashMap<>();
            for (Category cate : cateList) {
                if (cate != null) {
                    boolean permissionExistForCate = false;
                    for (Permission perm : perms) {
                        if (perm != null && perm.getCategoryId().equals(cate.getId())) {
                            if ((cateMap.get(cate.getId()) == null || cateMap.get(cate.getId()).equals(GradingConstants.viewPermission))) {
                                cateMap.put(cate.getId(), perm.getFunction());
                            }
                            permissionExistForCate = true;
                        }
                    }
                    if (!permissionExistForCate) {
                        return new HashMap<String, String>();
                    }
                }
            }
            
            final boolean view = cateMap.values().contains(GradingConstants.viewPermission);

            Map<String, String> studentMap = new HashMap<>();
            for (String studentId : studentIds) {
                if (view) {
                    studentMap.put(studentId, GradingConstants.viewPermission);
                } else {
                    studentMap.put(studentId, GradingConstants.gradePermission);
                }
            }
            
            return studentMap;
        }
        return new HashMap<String, String>();
    }
    
    public List<String> getViewableStudentsForUser(Long gradebookId, String userId, List<String> studentIds, List<CourseSection> sections) {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getAvailableItemsForStudent");
        }
        
        List<String> viewableStudents = new ArrayList();
        
        if (studentIds == null || studentIds.isEmpty()) {
            return viewableStudents;
        }
        
        
        List<Permission> permsForAnyGroup = persistence.getPermissionsForUserAnyGroup(gradebookId, userId);
        if (!permsForAnyGroup.isEmpty()) {
            return studentIds;
        }
        
        Map<String, List<String>> sectionIdStudentIdsMap = getSectionIdStudentIdsMap(sections, studentIds);
        
        if (sectionIdStudentIdsMap.isEmpty()) {
            return null;
        }
        
        // use a map to make sure the student ids are unique
        Map<String, Object> studentMap = new HashMap<>();
        
        // Next, check for permissions for specific sections
        List<String> groupIds = new ArrayList(sectionIdStudentIdsMap.keySet());
        List<Permission> permsForGroupsAnyCategory = persistence.getPermissionsForUserForGroup(gradebookId, userId, groupIds);
        
        if (permsForGroupsAnyCategory.isEmpty()) {
            return viewableStudents;
        }
        
        for (Iterator permsIter = permsForGroupsAnyCategory.iterator(); permsIter.hasNext();) {
            Permission perm = (Permission) permsIter.next();
            String groupId = perm.getGroupId();
            if (groupId != null) {
                List sectionStudentIds = (ArrayList)sectionIdStudentIdsMap.get(groupId);
                if (sectionStudentIds != null && !sectionStudentIds.isEmpty()) {
                    for (Iterator studentIter = sectionStudentIds.iterator(); studentIter.hasNext();) {
                        String studentId = (String) studentIter.next();
                        studentMap.put(studentId, null);
                    }
                }
            }
        }
        
        return new ArrayList(studentMap.keySet());
    }
    
    public List<String> getViewableStudentsForUser(String gradebookUid, String userId, List<String> studentIds, List<CourseSection> sections) {

        if (gradebookUid == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in GradebookPermissionServiceImpl.getViewableStudentsForUser");
        }
        
        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
        
        return getViewableStudentsForUser(gradebookId, userId, studentIds, sections);
    }

    /**
     * Get a list of permissions defined for the given user based on section and role or all sections if allowed. 
     * This method checks realms permissions for role/section and is independent of the 
     * gb_permissions_t permissions.
     *
     * note: If user has the grade privilege, they are given the GraderPermission.VIEW_COURSE_GRADE permission to match
     * GB classic functionality. This needs to be reviewed.
     *
     * @param userId
     * @param siteId
     * @param role user Role
     * @return list of {@link org.sakaiproject.service.gradebook.shared.PermissionDefinition PermissionDefinitions} or empty list if none
     */
    public List<PermissionDefinition> getRealmsPermissionsForUser(String userId,String siteId, Role role) {

        List<PermissionDefinition> permissions = new ArrayList<PermissionDefinition>();

        if (sakaiProxy.isUserAbleToGrade(siteId, userId)) {
            //FIXME:giving them view course grade (this needs to be reviewed!!), 
            //it appears in GB classic, User can view course grades if they have the ability to grade in realms
            PermissionDefinition permDef = new PermissionDefinition();
            permDef.setFunction(GraderPermission.VIEW_COURSE_GRADE.toString());
            permDef.setUserId(userId);
            permissions.add(permDef);

            if (sakaiProxy.isUserAbleToGradeAll(siteId, userId)) {
                permDef = new PermissionDefinition();
                permDef.setFunction(GraderPermission.GRADE.toString());
                permDef.setUserId(userId);
                permissions.add(permDef);
            } else {
                //get list of sections belonging to user and set a PermissionDefinition for each one
                //Didn't find a method that returned gradeable sections for a TA, only for the logged in user.
                //grabbing list of sections for the site, if User is a member of the section and has privilege to
                //grade their sections, they are given the grade permission. Seems straight forward??
                List<CourseSection> sections = this.sectionAwareness.getSections(siteId);

                for (CourseSection section: sections){
                    if (this.sectionAwareness.isSectionMemberInRole(section.getUuid(), userId,role)) {
                        //realms have no categories defined for grading, just perms and group id
                        permDef = new PermissionDefinition();
                        permDef.setFunction(GraderPermission.GRADE.toString());
                        permDef.setUserId(userId);
                        permDef.setGroupReference(section.getUuid());
                        permissions.add(permDef);
                    }
                }
            }
        }

        return permissions;
    }

    private Map<String, List<String>> getSectionIdStudentIdsMap(Collection<CourseSection> courseSections, Collection<String> studentIds) {

        Map<String, List<String>> sectionIdStudentIdsMap = new HashMap<>();
        if (courseSections != null) {
            for (CourseSection section : courseSections) {
                if (section != null) {
                    String sectionId = section.getUuid();
                    List<? extends ParticipationRecord> members = this.sectionAwareness.getSectionMembersInRole(sectionId, Role.STUDENT);
                    List<String> sectionMembersFiltered = new ArrayList<>();
                    if (!members.isEmpty()) {
                        for (ParticipationRecord enr : members) {
                            String studentId = enr.getUser().getUserUid();
                            if (studentIds.contains(studentId))
                                sectionMembersFiltered.add(studentId);
                        }
                    }
                    sectionIdStudentIdsMap.put(sectionId, sectionMembersFiltered);
                }
            }
        }
        return sectionIdStudentIdsMap;
    }
    
    @Override
    public List<PermissionDefinition> getPermissionsForUser(String gradebookUid, String userId) {

        Long gradebookId = persistence.getGradebook(gradebookUid).getId();
             
        List<Permission> permissions = persistence.getPermissionsForUser(gradebookId, userId);
        List<PermissionDefinition> rval = new ArrayList<>();
             
        for (Permission permission: permissions) {
            rval.add(toPermissionDefinition(permission));
        }
             
        return rval;
    }
    
    @Override
    public void updatePermissionsForUser(final String gradebookUid, final String userId, List<PermissionDefinition> permissionDefinitions) {

        Long gradebookId = persistence.getGradebook(gradebookUid).getId();

        if (permissionDefinitions.isEmpty()) {
            PermissionDefinition noPermDef = new PermissionDefinition();
            noPermDef.setFunction(GraderPermission.NONE.toString());
            noPermDef.setUserId(userId);
            permissionDefinitions.add(noPermDef);
        }

        //get the current list of permissions
        final List<Permission> currentPermissions = persistence.getPermissionsForUser(gradebookId, userId);
        
        //convert PermissionDefinition to Permission
        final List<Permission> newPermissions = new ArrayList<>();
        for (PermissionDefinition def: permissionDefinitions) {
            if (!StringUtils.equalsIgnoreCase(def.getFunction(), GraderPermission.GRADE.toString())
                    && !StringUtils.equalsIgnoreCase(def.getFunction(), GraderPermission.VIEW.toString())
                    && !StringUtils.equalsIgnoreCase(def.getFunction(), GraderPermission.VIEW_COURSE_GRADE.toString())
                    && !StringUtils.equalsIgnoreCase(def.getFunction(), GraderPermission.NONE.toString())) {
                throw new IllegalArgumentException("Invalid function for permission definition: " + def.getFunction());
            }
            
            Permission permission = new Permission();
            permission.setCategoryId(def.getCategoryId());
            permission.setGradebookId(gradebookId);
            permission.setGroupId(def.getGroupReference());
            permission.setFunction(def.getFunction());
            permission.setUserId(userId);
            
            newPermissions.add(permission);
        }

        //Note: rather than iterate both lists and figure out the differences and add/update/delete as applicable,
        //it is far simpler to just remove the existing permissions and add new ones in one transaction
        persistence.replacePermissions(currentPermissions, newPermissions);
    }

    public void clearPermissionsForUser(final String gradebookUid, final String userId) {

        Long gradebookId = persistence.getGradebook(gradebookUid).getId();

        // remove all current permissions for user
        persistence.deletePermissionsForUser(gradebookId, userId);
    }

    /**
     * Maps a Permission to a PermissionDefinition
     * Note that the persistent groupId is actually the group reference
     * @param permission
     * @return a {@link PermissionDefinition}
     */
    private PermissionDefinition toPermissionDefinition(Permission permission) {

         PermissionDefinition rval = new PermissionDefinition();
         if (permission != null) {
             rval.setId(permission.getId());
             rval.setUserId(permission.getUserId());
             rval.setCategoryId(permission.getCategoryId());
             rval.setFunction(permission.getFunction());
             rval.setGroupReference(permission.getGroupId()); 
         }
         return rval;
    }
}
