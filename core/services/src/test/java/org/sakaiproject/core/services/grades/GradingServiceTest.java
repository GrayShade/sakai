package org.sakaiproject.core.services.grades;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.sakaiproject.core.api.grades.AssessmentNotFoundException;
import org.sakaiproject.core.api.grades.Assignment;
import org.sakaiproject.core.api.grades.CategoryDefinition;
import org.sakaiproject.core.api.grades.CategoryScoreData;
import org.sakaiproject.core.api.grades.GradebookNotFoundException;
import org.sakaiproject.core.api.grades.GradeDefinition;
import org.sakaiproject.core.api.grades.GradebookInformation;
import org.sakaiproject.core.api.grades.GradingService;
import org.sakaiproject.core.api.grades.SakaiProxy;
import org.sakaiproject.core.persistence.grades.model.CourseGrade;
import org.sakaiproject.core.persistence.grades.model.Gradebook;
import org.sakaiproject.core.persistence.grades.model.GradebookAssignment;
import org.sakaiproject.core.utils.grades.GradingConstants;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GradingServiceTestConfiguration.class})
public class GradingServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Resource
    private GradingService gradingService;

    @Resource
    private SakaiProxy sakaiProxy;

    @PersistenceContext
    private EntityManager entityManager;

    private String gbUid = "697D804B-63C8-4AE1-97FA-1E6CB3CA503E";
    private String instructorId = "cfdabe69-09c7-4776-834e-db5fc62dd5dd";
    private String studentId = "0138eca0-d260-4f80-8b00-101b02c591b8";
    private String gbName = "Gradebook 1";
    private String assignmentName = "Assignment 1";
    private double assignmentPoints = 3D;

    @Test
    public void checkAddExistsAndGetGradebook() {

        Long id = gradingService.addGradebook(this.gbUid, this.gbName);
        Assert.assertTrue(gradingService.isGradebookDefined(this.gbUid));
        try {
            Gradebook gradebook = gradingService.getGradebook(this.gbUid);
            Assert.assertTrue(gradebook.getName().equals(this.gbName));
            gradebook = gradingService.getGradebook(id);
            Assert.assertTrue(gradebook.getUid().equals(this.gbUid));
        } catch (GradebookNotFoundException gnfe) {
            Assert.fail();
        }
    }

    @Test
    public void checkGradebookNotFoundException() {

        try {
            Gradebook gradebook = gradingService.getGradebook("bogus");
            Assert.fail();
        } catch (GradebookNotFoundException gnfe) {
        }
    }

    @Test
    public void checkDeleteGradebook() {

        checkAddExistsAndGetGradebook();

        try {
            gradingService.deleteGradebook(gbUid);
        } catch (GradebookNotFoundException gnfe) {
            Assert.fail();
        }

        try {
            Gradebook gradebook = gradingService.getGradebook(this.gbUid);
            Assert.fail();
        } catch (GradebookNotFoundException gnfe) {
        }
    }

    private Assignment newAssignment() {

        Assignment assignment = new Assignment();
        assignment.setPoints(this.assignmentPoints);
        assignment.setName(this.assignmentName);
        return assignment;
    }

    @Test
    public void checkAddAndGetAssignment() {

        gradingService.addGradebook(this.gbUid, this.gbName);

        Assert.assertTrue(gradingService.isGradebookDefined(this.gbUid));

        when(sakaiProxy.isUserAbleToEditAssessments(this.gbUid)).thenReturn(true);
        Assignment assignment = this.newAssignment();
        Long assignmentId = gradingService.addAssignment(this.gbUid, assignment);
        GradebookAssignment gradebookAssignment = gradingService.getAssignment(assignmentId);

        Assert.assertTrue(gradebookAssignment.getName().equals(this.assignmentName));
        Assert.assertTrue(gradebookAssignment.getPointsPossible() == this.assignmentPoints);

        try {
            Assignment storedAssignment = gradingService.getAssignment(this.gbUid, assignmentId);
            Assert.assertTrue(storedAssignment.getName().equals(this.assignmentName));
            Assert.assertTrue(storedAssignment.getPoints() == this.assignmentPoints);
        } catch (AssessmentNotFoundException anfe) {
            Assert.fail();
        }
    }

    @Test
    public void checkGetAssignmentByNameOrId() {

        Long id = gradingService.addGradebook(this.gbUid, this.gbName);

        Assignment assignment = this.newAssignment();
        Long assignmentId = gradingService.addAssignment(this.gbUid, assignment);
        String assignmentIdAsString = Long.toString(assignmentId);
        try {
            Assert.assertTrue(gradingService.getAssignmentByNameOrId(this.gbUid, this.assignmentName) != null);
            Assert.assertTrue(gradingService.getAssignmentByNameOrId(this.gbUid, assignmentIdAsString) != null);
        } catch (AssessmentNotFoundException e) {
            Assert.fail();
        }
    }

    @Test
    public void checkGetAssignments() {

        gradingService.addGradebook(this.gbUid, this.gbName);

        when(sakaiProxy.isUserAbleToEditAssessments(this.gbUid)).thenReturn(true);

        double assignmentPoints = 3D;

        List<String> names = Arrays.asList("Assignment 1", "Assignment 2", "Assignment 3");

        names.forEach(n -> {

            Assignment assignment1 = new Assignment();
            assignment1.setPoints(assignmentPoints);
            assignment1.setName(n);
            gradingService.addAssignment(this.gbUid , assignment1);
        });

        List<Assignment> assignments = gradingService.getAssignments(this.gbUid);
        Assert.assertTrue(assignments.size() == names.size());
        assignments.forEach(a -> Assert.assertTrue(names.contains(a.getName())));
    }

    @Test
	public void checkSaveGradeAndCommentForStudent() {

        String grade = "56.4";
        String comment = "Awesome";

        gradingService.addGradebook(this.gbUid, this.gbName);

        doReturn(true).when(sakaiProxy).isUserAbleToEditAssessments(this.gbUid);
        Long assignmentId = gradingService.addAssignment(this.gbUid, this.newAssignment());

        try {
            doReturn(true).when(sakaiProxy).isUserAbleToGrade(this.gbUid);
            doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, assignmentId, this.studentId);
	        gradingService.saveGradeAndCommentForStudent(this.gbUid, assignmentId, this.studentId, grade, comment);
            /*
            doReturn(this.studentId).when(sakaiProxy).getCurrentUserId();
            GradeDefinition gradeDefinition = gradingService.getGradeDefinitionForStudentForItem(this.gbUid, assignmentId, this.studentId);
            Assert.assertTrue(gradeDefinition != null && gradeDefinition.getGrade().equals(grade)
                && gradeDefinition.getGradeComment().equals(comment));
            */
        } catch (Exception e) {
            //Assert.fail(e.getClass().getName());
            Assert.fail(e.getStackTrace()[0].getFileName() + " : " + Integer.toString(e.getStackTrace()[0].getLineNumber()));
            //Assert.fail(e.getMessage());
        }
    }

    @Test
	public void checkSaveGradesAndComments() {

        String grade = "56.4";
        String comment = "Awesome";

        gradingService.addGradebook(this.gbUid, this.gbName);
        doReturn(true).when(sakaiProxy).isUserAbleToEditAssessments(this.gbUid);
        Long assignmentId = gradingService.addAssignment(this.gbUid, this.newAssignment());

        Date dateRecorded = new Date();

        try {
            doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, assignmentId, this.studentId);
            GradeDefinition gd1 = new GradeDefinition(this.studentId, this.instructorId, dateRecorded, grade, comment, 0, true, false);
            gradingService.saveGradesAndComments(this.gbUid, assignmentId, Arrays.asList(gd1));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkIsGradeValid() {

        gradingService.addGradebook(this.gbUid, this.gbName);

        try {
            Gradebook gradebook = gradingService.getGradebook(this.gbUid);
            GradebookInformation gbInfo = gradingService.getGradebookInformation(this.gbUid);
            gbInfo.setGradeType(GradingConstants.GRADE_TYPE_POINTS);
	        gradingService.updateGradebookSettings(this.gbUid, gbInfo);
	        Assert.assertFalse(gradingService.isGradeValid(this.gbUid, "-3"));
	        Assert.assertTrue(gradingService.isGradeValid(this.gbUid, "0"));
	        Assert.assertTrue(gradingService.isGradeValid(this.gbUid, "15.45"));
	        Assert.assertFalse(gradingService.isGradeValid(this.gbUid, "15.445"));
	        Assert.assertTrue(gradingService.isGradeValid(this.gbUid, "45094"));

            gbInfo.setGradeType(GradingConstants.GRADE_TYPE_LETTER);
	        gradingService.updateGradebookSettings(this.gbUid, gbInfo);
	        Assert.assertTrue(gradingService.isGradeValid(this.gbUid, "99"));
	        Assert.assertFalse(gradingService.isGradeValid(this.gbUid, "-3"));
        } catch (Exception gnfe) {
            Assert.fail();
        }
    }

    private CategoryDefinition addCategory(GradebookInformation gbInfo) {

        CategoryDefinition cd = new CategoryDefinition();
        cd.setExtraCredit(false);
        cd.setWeight(Double.valueOf(0));
        cd.setAssignmentList(Collections.<Assignment>emptyList());
        cd.setDropHighest(0);
        cd.setDropLowest(0);
        cd.setKeepHighest(0);
        cd.setCategoryOrder(gbInfo.getCategories().size());
        gbInfo.getCategories().add(cd);
        return cd;
    }

    @Test
    public void checkCalculateCategoryScore() {

        gradingService.addGradebook(this.gbUid, this.gbName);

        Assignment item1 = new Assignment();
        item1.setPoints(20D);
        item1.setName("Item 1");
        item1.setCounted(true);
        Long item1Id = gradingService.addAssignment(this.gbUid, item1);
        item1.setId(item1Id);

        Assignment item2 = new Assignment();
        item2.setPoints(40D);
        item2.setName("Item 2");
        item2.setCounted(true);
        Long item2Id = gradingService.addAssignment(this.gbUid, item2);
        item2.setId(item2Id);

        Date dateRecorded = new Date();

        doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, item1Id, this.studentId);
        String grade1 = "20.3";
        GradeDefinition gd1 = new GradeDefinition(this.studentId, null, dateRecorded, grade1, "", 0, true, false);
        gradingService.saveGradesAndComments(this.gbUid, item1Id, Arrays.asList(gd1));

        doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, item2Id, this.studentId);
        String grade2 = "16.7";
        GradeDefinition gd2 = new GradeDefinition(this.studentId, null, dateRecorded, grade2, "", 0, true, false);
        gradingService.saveGradesAndComments(this.gbUid, item2Id, Arrays.asList(gd2));

        GradebookInformation gbInfo = gradingService.getGradebookInformation(this.gbUid);
        CategoryDefinition category = this.addCategory(gbInfo);
        category.setName("Oranges");
        category.setAssignmentList(Arrays.asList(item1, item2));

        gradingService.updateGradebookSettings(this.gbUid, gbInfo);

        category = gradingService.getCategoryDefinitions(this.gbUid).get(0);

        item1.setCategoryId(category.getId());
        gradingService.updateAssignment(this.gbUid, item1Id,  item1);

        item2.setCategoryId(category.getId());
        gradingService.updateAssignment(this.gbUid, item2Id,  item2);

        Gradebook gradebook = gradingService.getGradebook(this.gbUid);

	    Optional<CategoryScoreData> scoreData = gradingService.calculateCategoryScore(gradebook.getId(), this.studentId, category.getId(), true);

        Assert.assertTrue(scoreData.isPresent());

        // (totalEarned/numberofassignments/(totalavailablepoints/numberofassignments)) * 100
        Assert.assertEquals(Double.parseDouble("61.66666667"), scoreData.get().score, 0);
    }

    @Test
    public void checkGetCourseGradeForStudent() {

        gradingService.addGradebook(this.gbUid, this.gbName);

        Assignment item1 = new Assignment();
        item1.setPoints(20D);
        item1.setName("Item 1");
        item1.setCounted(true);
        Long item1Id = gradingService.addAssignment(this.gbUid, item1);
        item1.setId(item1Id);

        Assignment item2 = new Assignment();
        item2.setPoints(40D);
        item2.setName("Item 2");
        item2.setCounted(true);
        Long item2Id = gradingService.addAssignment(this.gbUid, item2);
        item2.setId(item2Id);

        Date dateRecorded = new Date();

        doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, item1Id, this.studentId);
        String grade1 = "20.3";
        GradeDefinition gd1 = new GradeDefinition(this.studentId, null, dateRecorded, grade1, "", 0, true, false);
        gradingService.saveGradesAndComments(this.gbUid, item1Id, Arrays.asList(gd1));

        doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, item2Id, this.studentId);
        String grade2 = "16.7";
        GradeDefinition gd2 = new GradeDefinition(this.studentId, null, dateRecorded, grade2, "", 0, true, false);
        gradingService.saveGradesAndComments(this.gbUid, item2Id, Arrays.asList(gd2));

        /*
        Map<String, Double> gradeMap = new HashMap<>();
        gradeMap.put("A", 85.0);
        gradeMap.put("B", 75.0);
        gradeMap.put("C", 55.0);
        gradeMap.put("D", 45.0);
        gradeMap.put("E", 25.0);

	    gradingService.getCourseGradeForStudents(this.gbUid, Collections.singletonList(this.studentId), gradeMap);
        */
	    CourseGrade grade = gradingService.getCourseGradeForStudent(this.gbUid, this.studentId);

        Assert.assertTrue(grade.getCalculatedGrade().equals("61.66666667")); 
    }

    @Test
    public void checkGetCourseGradeForStudents() {

        gradingService.addGradebook(this.gbUid, this.gbName);

        Assignment item1 = new Assignment();
        item1.setPoints(20D);
        item1.setName("Item 1");
        item1.setCounted(true);
        Long item1Id = gradingService.addAssignment(this.gbUid, item1);
        item1.setId(item1Id);

        Assignment item2 = new Assignment();
        item2.setPoints(40D);
        item2.setName("Item 2");
        item2.setCounted(true);
        Long item2Id = gradingService.addAssignment(this.gbUid, item2);
        item2.setId(item2Id);

        Date dateRecorded = new Date();

        doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, item1Id, this.studentId);
        String grade1 = "20.3";
        GradeDefinition gd1 = new GradeDefinition(this.studentId, null, dateRecorded, grade1, "", 0, true, false);
        gradingService.saveGradesAndComments(this.gbUid, item1Id, Arrays.asList(gd1));

        doReturn(true).when(sakaiProxy).isUserAbleToGradeItemForStudent(this.gbUid, item2Id, this.studentId);
        String grade2 = "16.7";
        GradeDefinition gd2 = new GradeDefinition(this.studentId, null, dateRecorded, grade2, "", 0, true, false);
        gradingService.saveGradesAndComments(this.gbUid, item2Id, Arrays.asList(gd2));

        Map<String, Double> gradeMap = new HashMap<>();
        gradeMap.put("A", 85.0);
        gradeMap.put("B", 75.0);
        gradeMap.put("C", 55.0);
        gradeMap.put("D", 45.0);
        gradeMap.put("E", 25.0);

	    Map<String, CourseGrade> grades = gradingService.getCourseGradeForStudents(this.gbUid, Collections.singletonList(this.studentId), gradeMap);

        Assert.assertTrue(grades.size() == 1); 
        Assert.assertTrue(grades.containsKey(this.studentId)); 
        Assert.assertTrue(grades.get(this.studentId).getCalculatedGrade().equals("61.66666667")); 
        Assert.assertTrue(grades.get(this.studentId).getMappedGrade().equals("C")); 
    }
}
