package org.sakaiproject.core.services.grades;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.sakaiproject.core.persistence.grades.repository.AbstractGradeRecordRepository;
import org.sakaiproject.core.persistence.grades.repository.AssignmentGradeRecordRepository;
import org.sakaiproject.core.persistence.grades.repository.CategoryRepository;
import org.sakaiproject.core.persistence.grades.repository.CommentRepository;
import org.sakaiproject.core.persistence.grades.repository.CourseGradeRepository;
import org.sakaiproject.core.persistence.grades.repository.CourseGradeRecordRepository;
import org.sakaiproject.core.persistence.grades.repository.GradableObjectRepository;
import org.sakaiproject.core.persistence.grades.repository.GradebookRepository;
import org.sakaiproject.core.persistence.grades.repository.GradebookAssignmentRepository;
import org.sakaiproject.core.persistence.grades.repository.GradebookPropertyRepository;
import org.sakaiproject.core.persistence.grades.repository.GradeMappingRepository;
import org.sakaiproject.core.persistence.grades.repository.GradingEventRepository;
import org.sakaiproject.core.persistence.grades.repository.GradingScaleRepository;
import org.sakaiproject.core.persistence.grades.repository.LetterGradePercentMappingRepository;
import org.sakaiproject.core.persistence.grades.model.AssignmentGradeRecord;
import org.sakaiproject.core.persistence.grades.model.Category;
import org.sakaiproject.core.persistence.grades.model.Comment;
import org.sakaiproject.core.persistence.grades.model.CourseGrade;
import org.sakaiproject.core.persistence.grades.model.CourseGradeRecord;
import org.sakaiproject.core.persistence.grades.model.GradableObject;
import org.sakaiproject.core.persistence.grades.model.Gradebook;
import org.sakaiproject.core.persistence.grades.model.GradebookAssignment;
import org.sakaiproject.core.persistence.grades.model.GradebookProperty;
import org.sakaiproject.core.persistence.grades.model.GradeMapping;
import org.sakaiproject.core.persistence.grades.model.GradingEvent;
import org.sakaiproject.core.persistence.grades.model.GradingScale;
import org.sakaiproject.core.persistence.grades.model.LetterGradeMapping;
import org.sakaiproject.core.persistence.grades.model.LetterGradePercentMapping;
import org.sakaiproject.core.persistence.grades.model.LetterGradePlusMinusMapping;
import org.sakaiproject.core.persistence.grades.model.PassNotPassMapping;
import org.sakaiproject.core.persistence.grades.model.GradePointsMapping;

import org.sakaiproject.core.api.grades.AssessmentNotFoundException;
import org.sakaiproject.core.api.grades.Assignment;
import org.sakaiproject.core.api.grades.AssignmentHasIllegalPointsException;
import org.sakaiproject.core.api.grades.CategoryDefinition;
import org.sakaiproject.core.api.grades.CategoryScoreData;
import org.sakaiproject.core.api.grades.CommentDefinition;
import org.sakaiproject.core.api.grades.ConflictingAssignmentNameException;
import org.sakaiproject.core.api.grades.ConflictingCategoryNameException;
import org.sakaiproject.core.api.grades.ConflictingExternalIdException;
import org.sakaiproject.core.api.grades.ExternalAssignmentProvider;
import org.sakaiproject.core.api.grades.ExternalAssignmentProviderCompat;
import org.sakaiproject.core.api.grades.GradebookInformation;
import org.sakaiproject.core.api.grades.GradebookHelper;
import org.sakaiproject.core.api.grades.GradebookNotFoundException;
import org.sakaiproject.core.api.grades.GradebookSecurityException;
import org.sakaiproject.core.api.grades.GradeDefinition;
import org.sakaiproject.core.api.grades.GradeMappingDefinition;
import org.sakaiproject.core.api.grades.GradingPermissionService;
import org.sakaiproject.core.api.grades.GradingPersistenceManager;
import org.sakaiproject.core.api.grades.InvalidCategoryException;
import org.sakaiproject.core.api.grades.InvalidGradeException;
import org.sakaiproject.core.api.grades.StaleObjectModificationException;
import org.sakaiproject.core.api.grades.GradingService;
import org.sakaiproject.core.api.grades.SakaiProxy;
import org.sakaiproject.core.api.grades.SortType;
import org.sakaiproject.core.api.grades.UnmappableCourseGradeOverrideException;
import org.sakaiproject.core.utils.grades.GradingConstants;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.core.utils.grades.GradingScaleDefinition;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.util.ResourceLoader;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GradingServiceImpl implements GradingService {

    public static final String PROP_COURSE_POINTS_DISPLAYED = "gradebook.coursepoints.displayed";
    public static final String PROP_COURSE_GRADE_DISPLAYED = "gradebook.coursegrade.displayed";
    public static final String PROP_ASSIGNMENTS_DISPLAYED = "gradebook.assignments.displayed";
    public static final String UID_OF_DEFAULT_GRADING_SCALE_PROPERTY = "uidOfDefaultGradingScale";

    @Resource private AssignmentGradeRecordRepository assignmentGradeRecordRepository;
    @Resource private CategoryRepository categoryRepository;
    @Resource private CommentRepository commentRepository;
    @Resource private CourseGradeRepository courseGradeRepository;
    @Resource private CourseGradeRecordRepository courseGradeRecordRepository;
    @Resource private GradableObjectRepository gradableObjectRepository;
    @Resource private GradebookRepository gradebookRepository;
    @Resource private GradebookAssignmentRepository gradebookAssignmentRepository;
    @Resource private GradeMappingRepository gradeMappingRepository;
    @Resource private GradebookPropertyRepository gradebookPropertyRepository;
    @Resource private GradingScaleRepository gradingScaleRepository;
    @Resource private GradingEventRepository gradingEventRepository;
    @Resource private LetterGradePercentMappingRepository letterGradePercentMappingRepository;

    @Resource private GradingPersistenceManager persistence;
    @Resource private GradingPermissionService permissions;

    @Resource private SakaiProxy sakaiProxy;

    private Map<String, String> propertiesMap = new HashMap<>();

	private ConcurrentHashMap<String, ExternalAssignmentProvider> externalProviders = new ConcurrentHashMap<String, ExternalAssignmentProvider>();

	// Mapping of providers to their getAllExternalAssignments(String gradebookUid) methods,
	// used to allow the method to be called on providers not declaring the Compat interface.
	// This is to allow the same code to be used on 2.9 and beyond, where the secondary interface
	// may be removed, without build profiles.
	private final ConcurrentHashMap<ExternalAssignmentProvider, Method> providerMethods = new ConcurrentHashMap<ExternalAssignmentProvider, Method>();

    public Long addGradebook(final String uid, final String name) {

        if (isGradebookDefined(uid)) {
            log.warn("You can not add a gradebook with uid={}. That gradebook already exists.", uid);
            throw new RuntimeException("You can not add a gradebook with uid=" + uid + ".  That gradebook already exists.");
        }

        log.debug("Adding gradebook uid={} by userUid={}", uid, sakaiProxy.getCurrentUserId());

        createDefaultLetterGradeMapping(getHardDefaultLetterMapping());
        
        List<GradingScale> gradingScales = gradingScaleRepository.findByUnavailableIsFalse();

        // The application won't be able to run without grade mapping
        // templates, so if for some reason none have been defined yet,
        // do that now.
        if (gradingScales.isEmpty()) {
            log.info("No Grading Scale defined yet. This is probably because you have upgraded or you are working with a new database. Default grading scales will be created. Any customized system-wide grade mappings you may have defined in previous versions will have to be reconfigured.");
            gradingScales = addDefaultGradingScales();
        }

        // Create and save the gradebook
        final Gradebook gradebook = new Gradebook();
        gradebook.setName(name);
        gradebook.setUid(uid);
        gradebookRepository.save(gradebook);

        // Create the course grade for the gradebook
        final CourseGrade cg = new CourseGrade();
        cg.setGradebook(gradebook);
        courseGradeRepository.save(cg);

        // According to the specification, Display GradebookAssignment Grades is
        // on by default, and Display course grade is off. But can be overridden via properties

        final Boolean propAssignmentsDisplayed = sakaiProxy.getBooleanConfig(PROP_ASSIGNMENTS_DISPLAYED,true);
        gradebook.setAssignmentsDisplayed(propAssignmentsDisplayed);

        final Boolean propCourseGradeDisplayed = sakaiProxy.getBooleanConfig(PROP_COURSE_GRADE_DISPLAYED,false);
        gradebook.setCourseGradeDisplayed(propCourseGradeDisplayed);

        final Boolean propCoursePointsDisplayed = sakaiProxy.getBooleanConfig(PROP_COURSE_POINTS_DISPLAYED,false);
        gradebook.setCoursePointsDisplayed(propCoursePointsDisplayed);

        final String defaultScaleUid = getPropertyValue(UID_OF_DEFAULT_GRADING_SCALE_PROPERTY);

        // Add and save grade mappings based on the templates.
        GradeMapping defaultGradeMapping = null;
        final Set<GradeMapping> gradeMappings = new HashSet<>();
        for (final GradingScale gradingScale : gradingScales) {
            final GradeMapping gradeMapping = new GradeMapping(gradingScale);
            gradeMapping.setGradebook(gradebook);
            gradeMappingRepository.save(gradeMapping);
            gradeMappings.add(gradeMapping);
            if (gradingScale.getUid().equals(defaultScaleUid)) {
                defaultGradeMapping = gradeMapping;
            }
        }

        // Check for null default.
        if (defaultGradeMapping == null) {
            defaultGradeMapping = gradeMappings.iterator().next();
            if (log.isWarnEnabled()) {
                log.warn("No default GradeMapping found for new Gradebook={}; will set default to {}",
                        gradebook.getUid(), defaultGradeMapping.getName());
            }
        }
        gradebook.setSelectedGradeMapping(defaultGradeMapping);

        // The Hibernate mapping as of Sakai 2.2 makes this next
        // call meaningless when it comes to persisting changes at
        // the end of the transaction. It is, however, needed for
        // the mappings to be seen while the transaction remains
        // uncommitted.
        gradebook.setGradeMappings(gradeMappings);

        gradebook.setGrade_type(GradingConstants.GRADE_TYPE_POINTS);
        gradebook.setCategory_type(GradingConstants.CATEGORY_TYPE_NO_CATEGORY);

        //SAK-29740 make backwards compatible
        gradebook.setCourseLetterGradeDisplayed(true);
        gradebook.setCourseAverageDisplayed(true);

        // SAK-33855 turn on stats for new gradebooks
        gradebook.setAssignmentStatsDisplayed(true);
        gradebook.setCourseGradeStatsDisplayed(true);

        // Update the gradebook with the new selected grade mapping
        gradebookRepository.save(gradebook);

        return gradebook.getId();
    }

    @Override
	public void deleteGradebook(final String uid) throws GradebookNotFoundException {

        log.debug("Deleting gradebook uid={} by userUid={}", uid, sakaiProxy.getCurrentUserId());

        final Long gradebookId = getGradebook(uid).getId();

        // Worse of both worlds code ahead. We've been quick-marched
        // into Hibernate 3 sessions, but we're also having to use classic query
        // parsing -- which keeps us from being able to use either Hibernate's new-style
        // bulk delete queries or Hibernate's old-style session.delete method.
        // Instead, we're stuck with going through the Spring template for each
        // deletion one at a time.
        //final HibernateTemplate hibTempl = getHibernateTemplate();

        long numberDeleted;

        // TODO: this should all be in transaction

        numberDeleted = gradingEventRepository.deleteByGradableObject_Gradebook_Id(gradebookId);
        log.debug("Deleted {} grading events", numberDeleted);

        numberDeleted = assignmentGradeRecordRepository.deleteByGradableObject_Gradebook_Id(gradebookId);
        log.debug("Deleted {} assignment grade records", numberDeleted);

        numberDeleted = courseGradeRecordRepository.deleteByGradableObject_Gradebook_Id(gradebookId);
        log.debug("Deleted {} course grade records", numberDeleted);

        numberDeleted = gradableObjectRepository.deleteByGradebook_Id(gradebookId);
        log.debug("Deleted {} gradable objects", numberDeleted);

		Gradebook gradebook = gradebookRepository.findOne(gradebookId);
        gradebook.setSelectedGradeMapping(null);

        numberDeleted = gradeMappingRepository.deleteByGradebook_Id(gradebookId);
        log.debug("Deleted {} grade mappings", numberDeleted);

        gradebookRepository.delete(gradebook);
	}

    private LetterGradePercentMapping getDefaultLetterGradePercentMapping() {

        List<LetterGradePercentMapping> mapping = letterGradePercentMappingRepository.findByMappingType(1);

        if (mapping.size() == 0) {
            log.info("Default letter grade mapping hasn't been created in DB in BaseHibernateManager.getDefaultLetterGradePercentMapping");
            return null;
        }

        if (mapping.size() > 1) {
            log.error("Duplicate default letter grade mapping was created in DB in BaseHibernateManager.getDefaultLetterGradePercentMapping");
            return null;
        }

        return mapping.get(0);
    }

    private boolean validateLetterGradeMapping(Map<String, Double> gradeMap) {

        for (String key : gradeMap.keySet()) {
            boolean validLetter = false;
            for (String element : GradingConstants.validLetterGrade) {
                if (key.equalsIgnoreCase(element)) {
                    validLetter = true;
                    break;
                }
            }
            if (!validLetter) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Double> getHardDefaultLetterMapping() {

        Map<String, Double> gradeMap = new HashMap<>();
        gradeMap.put("A+", Double.valueOf(100));
        gradeMap.put("A", Double.valueOf(95));
        gradeMap.put("A-", Double.valueOf(90));
        gradeMap.put("B+", Double.valueOf(87));
        gradeMap.put("B", Double.valueOf(83));
        gradeMap.put("B-", Double.valueOf(80));
        gradeMap.put("C+", Double.valueOf(77));
        gradeMap.put("C", Double.valueOf(73));
        gradeMap.put("C-", Double.valueOf(70));
        gradeMap.put("D+", Double.valueOf(67));
        gradeMap.put("D", Double.valueOf(63));
        gradeMap.put("D-", Double.valueOf(60));
        gradeMap.put("F", Double.valueOf(0.0));
        
        return gradeMap;
    }

    private void createDefaultLetterGradeMapping(Map<String, Double> gradeMap) {

        if (getDefaultLetterGradePercentMapping() == null) {
            Set<String> keySet = gradeMap.keySet();

            if (keySet.size() != GradingConstants.validLetterGrade.length) {
                throw new IllegalArgumentException("gradeMap doesn't have right size in BaseHibernateManager.createDefaultLetterGradePercentMapping");
            }

            if (!validateLetterGradeMapping(gradeMap)) {
                throw new IllegalArgumentException("gradeMap contains invalid letter in BaseHibernateManager.createDefaultLetterGradePercentMapping");
            }

            LetterGradePercentMapping lgpm = new LetterGradePercentMapping();
            letterGradePercentMappingRepository.save(lgpm);
            Map<String, Double> saveMap = new HashMap<>(gradeMap);
            if (lgpm != null) {
                lgpm.setGradeMap(new HashMap<>(gradeMap));
                lgpm.setMappingType(1);
                letterGradePercentMappingRepository.save(lgpm);
            }
        }
    }

    private List addDefaultGradingScales() {

        final List<GradingScale> gradingScales = new ArrayList<>();

        // Base the default set of templates on the old
        // statically defined GradeMapping classes.
        final GradeMapping[] oldGradeMappings = {
            new LetterGradeMapping(),
            new LetterGradePlusMinusMapping(),
            new PassNotPassMapping(),
            new GradePointsMapping()
        };

        for (final GradeMapping sampleMapping : oldGradeMappings) {
            sampleMapping.setDefaultValues();
            final GradingScale gradingScale = new GradingScale();
            String uid = sampleMapping.getClass().getName();
            uid = uid.substring(uid.lastIndexOf('.') + 1);
            gradingScale.setUid(uid);
            gradingScale.setUnavailable(false);
            gradingScale.setName(sampleMapping.getName());
            gradingScale.setGrades(new ArrayList<>(sampleMapping.getGrades()));
            gradingScale.setDefaultBottomPercents(new HashMap<>(sampleMapping.getGradeMap()));
            gradingScaleRepository.save(gradingScale);
            if (log.isInfoEnabled()) {
                log.info("Added Grade Mapping " + gradingScale.getUid());
            }
            gradingScales.add(gradingScale);
        }
        setPropertyValue(UID_OF_DEFAULT_GRADING_SCALE_PROPERTY, "LetterGradePlusMinusMapping");
        return gradingScales;
    }

    public boolean isGradebookDefined(String gradebookUid) {
        return persistence.isGradebookDefined(gradebookUid);
    }

    public Gradebook getGradebook(Long id) throws GradebookNotFoundException {

        Gradebook gb = persistence.getGradebook(id);

        if (gb == null) {
            throw new GradebookNotFoundException("Could not find gradebook id=" + id.toString());
        }

        return gb;
    }

    public Gradebook getGradebook(String uid) throws GradebookNotFoundException {

        Gradebook gb = persistence.getGradebook(uid);

        if (gb == null) {
            throw new GradebookNotFoundException("Could not find gradebook uid=" + uid);
        }

        return gb;
    }

    public String getPropertyValue(String name) {
        return persistence.getPropertyValue(name);
    }

    public void setPropertyValue(String name, String value) {
        persistence.setPropertyValue(name, value);
    }

    public Long addAssignment(final String gradebookUid, final Assignment assignmentDefinition) {

        if (!sakaiProxy.isUserAbleToEditAssessments(gradebookUid)) {
            log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to add an assignment", sakaiProxy.getCurrentUserId(), gradebookUid);
            throw new GradebookSecurityException();
        }

        // Ensure that points is > zero.
        final Double points = assignmentDefinition.getPoints();
        if ((points == null) || (points <= 0)) {
            throw new AssignmentHasIllegalPointsException("Points must be > 0");
        }

        final Gradebook gradebook = getGradebook(gradebookUid);

        // if attaching to category
        if (assignmentDefinition.getCategoryId() != null) {
            return createAssignmentForCategory(gradebook.getId(), assignmentDefinition.getCategoryId(), assignmentDefinition.getName(),
                    points, assignmentDefinition.getDueDate(), !assignmentDefinition.isCounted(), assignmentDefinition.isReleased(),
                    assignmentDefinition.isExtraCredit(), assignmentDefinition.getCategorizedSortOrder());
        }

        return createAssignment(gradebook.getId(), assignmentDefinition.getName(), points, assignmentDefinition.getDueDate(),
                !assignmentDefinition.isCounted(), assignmentDefinition.isReleased(), assignmentDefinition.isExtraCredit(), assignmentDefinition.getSortOrder());
    }

    public Long createAssignmentForCategory(Long gradebookId, Long categoryId, String name, Double points, Date dueDate, Boolean isNotCounted, 
           Boolean isReleased, Boolean isExtraCredit, Integer categorizedSortOrder)
        throws ConflictingAssignmentNameException, StaleObjectModificationException, IllegalArgumentException {

        if (gradebookId == null || categoryId == null) {
            throw new IllegalArgumentException("gradebookId or categoryId is null in BaseHibernateManager.createAssignmentForCategory");
        }

        return createNewAssignment(gradebookId, categoryId, name, points, dueDate, isNotCounted, isReleased, isExtraCredit, null, categorizedSortOrder);
    }

    public Long createAssignment(Long gradebookId, String name, Double points, Date dueDate, Boolean isNotCounted,
            Boolean isReleased, Boolean isExtraCredit, Integer sortOrder)
            throws ConflictingAssignmentNameException, StaleObjectModificationException {
        return createNewAssignment(gradebookId, null, name, points, dueDate, isNotCounted, isReleased, isExtraCredit, sortOrder, null);
    }

    private Long createNewAssignment(final Long gradebookId, final Long categoryId, final String name, final Double points, final Date dueDate, final Boolean isNotCounted,
            final Boolean isReleased, final Boolean isExtraCredit, final Integer sortOrder, final Integer categorizedSortOrder) 
                    throws ConflictingAssignmentNameException, StaleObjectModificationException
    {
        final GradebookAssignment asn = prepareNewAssignment(name, points, dueDate, isNotCounted, isReleased, isExtraCredit, sortOrder, categorizedSortOrder);

        return saveNewAssignment(gradebookId, categoryId, asn);
    }

    private Long saveNewAssignment(final Long gradebookId, final Long categoryId, final GradebookAssignment asn) throws ConflictingAssignmentNameException {

        loadAssignmentGradebookAndCategory(asn, gradebookId, categoryId);

        if (assignmentNameExists(asn.getName(), asn.getGradebook())) {
            throw new ConflictingAssignmentNameException("You cannot save multiple assignments in a gradebook with the same name");
        }

        return gradableObjectRepository.save(asn).getId();
    }

    private void loadAssignmentGradebookAndCategory(final GradebookAssignment asn, final Long gradebookId, final Long categoryId) {

        final Gradebook gb = gradebookRepository.findOne(gradebookId);
        asn.setGradebook(gb);
        if (categoryId != null) {
            final Category cat = categoryRepository.findOne(categoryId);
            asn.setCategory(cat);
        }
    }

    private GradebookAssignment prepareNewAssignment(final String name, final Double points, final Date dueDate, final Boolean isNotCounted, final Boolean isReleased, 
            final Boolean isExtraCredit, final Integer sortOrder, final Integer categorizedSortOrder) {

        final String validatedName = StringUtils.trimToNull(name);
        if (validatedName == null){
            throw new ConflictingAssignmentNameException("You cannot save an assignment without a name");
        }

        // name cannot contain these special chars as they are reserved for special columns in import/export
        GradebookHelper.validateGradeItemName(validatedName);

        final GradebookAssignment asn = new GradebookAssignment();
        asn.setName(validatedName);
        asn.setPointsPossible(points);
        asn.setDueDate(dueDate);
        asn.setUngraded(false);
        if (isNotCounted != null) {
            asn.setNotCounted(isNotCounted.booleanValue());
        }
        if (isExtraCredit != null) {
            asn.setExtraCredit(isExtraCredit.booleanValue());
        }
        if (isReleased != null) {
            asn.setReleased(isReleased.booleanValue());
        }
        if (sortOrder != null) {
            asn.setSortOrder(sortOrder);
        }
        if (categorizedSortOrder != null) {
            asn.setCategorizedSortOrder(categorizedSortOrder);
        }

        return asn;
    }

    /**
     *
     * @param name the assignment name (will not be trimmed)
     * @param gradebook the gradebook to check
     * @return true if an assignment with the given name already exists in this gradebook.
     */
    private boolean assignmentNameExists(String name, Gradebook gradebook) {

        return gradableObjectRepository.countByNameAndGradebook_UidAndRemoved(name, gradebook.getUid(), false) > 0L;
    }

    @Override
    public GradebookAssignment getAssignment(Long id) {
        return gradebookAssignmentRepository.findOne(id);
    }

    @Override
    public Assignment getAssignment(String gradebookUid, Long assignmentId) throws AssessmentNotFoundException {

        if (assignmentId == null || gradebookUid == null) {
            throw new IllegalArgumentException("null parameter passed to getAssignment. Values are assignmentId:"
                    + assignmentId + " gradebookUid:" + gradebookUid);
        }
        if (!isUserAbleToViewAssignments(gradebookUid)
                && !sakaiProxy.isUserAbleToViewOwnGrades(gradebookUid)) {
            log.warn("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to get assignment with id {}"
                        , sakaiProxy.getCurrentUserId(), gradebookUid, assignmentId);
            throw new GradebookSecurityException();
        }

        GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);

        if (assignment == null) {
            throw new AssessmentNotFoundException("No gradebook item exists with gradable object id = " + assignmentId);
        }

        return getAssignmentDefinition(assignment);
    }

    @Override
    @Deprecated
    public Assignment getAssignment(String gradebookUid, String assignmentName) throws AssessmentNotFoundException {

        if (assignmentName == null || gradebookUid == null) {
            throw new IllegalArgumentException("null parameter passed to getAssignment. Values are assignmentName:"
                    + assignmentName + " gradebookUid:" + gradebookUid);
        }
        if (!isUserAbleToViewAssignments(gradebookUid) && !sakaiProxy.isUserAbleToViewOwnGrades(gradebookUid)) {
            log.warn("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to get assignment {}", sakaiProxy.getCurrentUserId(), gradebookUid,
                    assignmentName);
            throw new GradebookSecurityException();
        }

        GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentName);

        if (assignment != null) {
            return getAssignmentDefinition(assignment);
        } else {
            return null;
        }
    }

    @Override
    public Assignment getAssignmentByNameOrId(String gradebookUid, String assignmentName) throws AssessmentNotFoundException {

        Assignment assignment = null;
        try {
            assignment = getAssignment(gradebookUid, assignmentName);
        } catch (AssessmentNotFoundException e) {
            // Don't fail on this exception
            log.debug("Assessment not found by name", e);
        }

        if (assignment == null) {
            // Try to get the assignment by id
            if (NumberUtils.isCreatable(assignmentName)) {
                Long assignmentId = NumberUtils.toLong(assignmentName, -1L);
                return getAssignment(gradebookUid, assignmentId);
            }
        }
        return assignment;
    }

    @Override
    public GradeDefinition getGradeDefinitionForStudentForItem(String gradebookUid, Long assignmentId, String studentOrGroupId) {

        if (gradebookUid == null || assignmentId == null || studentOrGroupId == null) {
            throw new IllegalArgumentException("Null paramter passed to getGradeDefinitionForStudentForItem");
        }

        // studentId can be a groupId (from Assignments)
        final boolean studentRequestingOwnScore = sakaiProxy.getCurrentUserId().equals(studentOrGroupId)
                || sakaiProxy.isCurrentUserFromGroup(gradebookUid, studentOrGroupId);

        final GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);

        if (assignment == null) {
            throw new AssessmentNotFoundException(
                    "There is no assignment with the assignmentId " + assignmentId + " in gradebook " + gradebookUid);
        }

        if (!studentRequestingOwnScore && !isUserAbleToViewItemForStudent(gradebookUid, assignment.getId(), studentOrGroupId)) {
            log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to retrieve grade for student {} for assignment {}",
                    sakaiProxy.getCurrentUserId(), gradebookUid, studentOrGroupId, assignmentId);
            throw new GradebookSecurityException("BALLS");
        }

        System.out.println("HERE");

        final Gradebook gradebook = assignment.getGradebook();

        final GradeDefinition gradeDef = new GradeDefinition();
        gradeDef.setStudentUid(studentOrGroupId);
        gradeDef.setGradeEntryType(gradebook.getGrade_type());
        gradeDef.setGradeReleased(assignment.isReleased());

        // If this is the student, then the global setting needs to be enabled and the assignment needs to have
        // been released. Return null score information if not released
        if (studentRequestingOwnScore && (!gradebook.isAssignmentsDisplayed() || !assignment.isReleased())) {
            gradeDef.setDateRecorded(null);
            gradeDef.setGrade(null);
            gradeDef.setGraderUid(null);
            gradeDef.setGradeComment(null);
            log.debug("Student {} in gradebook {} retrieving score for unreleased assignment {}", sakaiProxy.getCurrentUserId(), gradebookUid,
                    assignment.getName());

        } else {
            final AssignmentGradeRecord gradeRecord
                = persistence.getAssignmentGradeRecord(assignment, studentOrGroupId);
            final CommentDefinition gradeComment
                = persistence.getAssignmentScoreComment(gradebookUid, assignmentId, studentOrGroupId);
            final String commentText = gradeComment != null ? gradeComment.getCommentText() : null;
            log.debug("gradeRecord={}", gradeRecord);

            if (gradeRecord == null) {
                gradeDef.setDateRecorded(null);
                gradeDef.setGrade(null);
                gradeDef.setGraderUid(null);
                gradeDef.setGradeComment(commentText);
                gradeDef.setExcused(false);
            } else {
                gradeDef.setDateRecorded(gradeRecord.getDateRecorded());
                gradeDef.setGraderUid(gradeRecord.getGraderId());
                gradeDef.setGradeComment(commentText);

                gradeDef.setExcused(gradeRecord.getExcludedFromGrade());

                if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_LETTER) {
                    List<AssignmentGradeRecord> gradeList = new ArrayList<>();
                    gradeList.add(gradeRecord);
                    convertPointsToLetterGrade(gradebook, gradeList);
                    AssignmentGradeRecord gradeRec = gradeList.get(0);
                    if (gradeRec != null) {
                        gradeDef.setGrade(gradeRec.getLetterEarned());
                    }
                } else if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_PERCENTAGE) {
                    Double percent
                        = calculateEquivalentPercent(assignment.getPointsPossible(), gradeRecord.getPointsEarned());
                    if (percent != null) {
                        gradeDef.setGrade(percent.toString());
                    }
                } else {
                    if (gradeRecord.getPointsEarned() != null) {
                        gradeDef.setGrade(gradeRecord.getPointsEarned().toString());
                    }
                }
            }
        }

        log.debug("returning grade def for {}", studentOrGroupId);
        return gradeDef;
    }

    /**
     *
     * @param doublePointsPossible
     * @param doublePointsEarned
     * @return the % equivalent for the given points possible and points earned
     */
    private Double calculateEquivalentPercent(Double doublePointsPossible, Double doublePointsEarned) {

        if (doublePointsEarned == null || doublePointsPossible == null) {
            return null;
        }

        // scale to handle points stored as repeating decimals
        final BigDecimal pointsEarned = new BigDecimal(doublePointsEarned.toString());
        final BigDecimal pointsPossible = new BigDecimal(doublePointsPossible.toString());

        // Avoid dividing by zero
        if (pointsEarned.compareTo(BigDecimal.ZERO) == 0 || pointsPossible.compareTo(BigDecimal.ZERO) == 0) {
            return new Double(0);
        }

        final BigDecimal equivPercent = pointsEarned.divide(pointsPossible, GradingConstants.MATH_CONTEXT).multiply(new BigDecimal("100"));
        return Double.valueOf(equivPercent.doubleValue());

    }

    /**
     * Converts points to letter grade for the given AssignmentGradeRecords
     * @param gradebook
     * @param studentRecordsFromDB
     * @return
     */
    private List<AssignmentGradeRecord> convertPointsToLetterGrade(Gradebook gradebook, List<AssignmentGradeRecord> studentRecordsFromDB) {

        final List letterGradeList = new ArrayList();
        final LetterGradePercentMapping lgpm = persistence.getLetterGradePercentMapping(gradebook);
        for (AssignmentGradeRecord agr : studentRecordsFromDB) {
            if (agr != null) {
                Double pointsPossible = agr.getAssignment().getPointsPossible();
                agr.setDateRecorded(agr.getDateRecorded());
                agr.setGraderId(agr.getGraderId());
                if (pointsPossible == null || agr.getPointsEarned() == null) {
                    agr.setLetterEarned(null);
                    letterGradeList.add(agr);
                } else {
                    String letterGrade = lgpm.getGrade(calculateEquivalentPercent(pointsPossible, agr.getPointsEarned()));
                    agr.setLetterEarned(letterGrade);
                    letterGradeList.add(agr);
                }
            }
        }
        return letterGradeList;
    }

    /**
     * Converts points to percentage for the given AssignmentGradeRecords
     * @param gradebook
     * @param studentRecordsFromDB
     * @return
     */
    private List convertPointsToPercentage(Gradebook gradebook, List studentRecordsFromDB) {

    	final List percentageList = new ArrayList();
    	for (int i=0; i < studentRecordsFromDB.size(); i++) {

    		final AssignmentGradeRecord agr = (AssignmentGradeRecord) studentRecordsFromDB.get(i);
    		if (agr != null) {
    			final Double pointsPossible = agr.getAssignment().getPointsPossible();
    			if (pointsPossible == null || agr.getPointsEarned() == null) {
    				agr.setPercentEarned(null);
        			percentageList.add(agr);
    			} else {
        			agr.setDateRecorded(agr.getDateRecorded());
        			agr.setGraderId(agr.getGraderId());
        			agr.setPercentEarned(calculateEquivalentPercent(pointsPossible, agr.getPointsEarned()));
        			percentageList.add(agr);
    			}
    		}
    	}
    	return percentageList;
    }

    @Override
    public List<Assignment> getAssignments(String gradebookUid) throws GradebookNotFoundException {
        return getAssignments(gradebookUid, SortType.SORT_BY_NONE);
    }

    /**
     * Get a list of assignments in the gradebook attached to the given category. Note that each assignment only knows the category by name.
     *
     * <p>
     * Note also that this is different to {@link BaseHibernateManager#getAssignmentsForCategory(Long)} because this method returns the
     * shared GradebookAssignment object.
     *
     * @param gradebookUid
     * @param categoryName
     * @return
     */
    private List<Assignment> getAssignments(String gradebookUid, String categoryName) {

        List<Assignment> allAssignments = getAssignments(gradebookUid);
        return allAssignments.stream().filter(a -> StringUtils.equals(a.getCategoryName(), categoryName))
            .collect(Collectors.toList());
    }

    @Override
    public List<Assignment> getAssignments(String gradebookUid, SortType sortBy) throws GradebookNotFoundException {

        if (!isUserAbleToViewAssignments(gradebookUid)) {
            log.warn("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to get assignments list", sakaiProxy.getCurrentUserId(), gradebookUid);
            throw new GradebookSecurityException();
        }

        Long gradebookId = getGradebook(gradebookUid).getId();

        List<GradebookAssignment> internalAssignments = persistence.getAssignments(gradebookId);

        sortAssignments(internalAssignments, sortBy, true);

        return internalAssignments.stream().map(a -> getAssignmentDefinition(a)).collect(Collectors.toList());
    }

    /**
     * Get a list of assignments, sorted
     *
     * @param gradebookId
     * @param sortBy
     * @param ascending
     * @return
     *
     *      NOTE: When the UI changes, this needs to go back to private
     */
    public List<GradebookAssignment> getAssignments(Long gradebookId, SortType sortBy, boolean ascending) {

        List<GradebookAssignment> assignments = persistence.getAssignments(gradebookId);
        sortAssignments(assignments, sortBy, ascending);
        return assignments;
    }

    /**
     * Sort the list of (internal) assignments by the given criteria
     *
     * @param assignments
     * @param sortBy
     * @param ascending
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void sortAssignments(List<GradebookAssignment> assignments, SortType sortBy, boolean ascending) {

        // note, this is duplicated in the tool GradebookManagerHibernateImpl class
        Comparator comp;

        if (sortBy == null) {
            sortBy = SortType.SORT_BY_SORTING; // default
        }

        switch (sortBy) {

            case SORT_BY_NONE:
                return; // no sorting
            case SORT_BY_NAME:
                comp = GradableObject.nameComparator;
                break;
            case SORT_BY_DATE:
                comp = GradableObject.dateComparator;
                break;
            case SORT_BY_MEAN:
                comp = GradableObject.meanComparator;
                break;
            case SORT_BY_POINTS:
                comp = GradebookAssignment.pointsComparator;
                break;
            case SORT_BY_RELEASED:
                comp = GradebookAssignment.releasedComparator;
                break;
            case SORT_BY_COUNTED:
                comp = GradebookAssignment.countedComparator;
                break;
            case SORT_BY_EDITOR:
                comp = GradebookAssignment.gradeEditorComparator;
                break;
            case SORT_BY_SORTING:
                comp = GradableObject.sortingComparator;
                break;
            case SORT_BY_CATEGORY:
                comp = GradebookAssignment.categoryComparator;
                break;
            default:
                comp = GradableObject.defaultComparator;
        }

        Collections.sort(assignments, comp);
        if (!ascending) {
            Collections.reverse(assignments);
        }
        if (log.isDebugEnabled()) {
            log.debug("sortAssignments: ordering by " + sortBy + " (" + comp + "), ascending=" + ascending);
        }
    }

    public boolean isUserAbleToViewItemForStudent(String gradebookUid, Long assignmentId, String studentId) {
        return sakaiProxy.isUserAbleToViewItemForStudent(gradebookUid, assignmentId, studentId);
    }

    public String getGradeViewFunctionForUserForStudentForItem(String gradebookUid, Long assignmentId, String studentId) {
        return sakaiProxy.getGradeViewFunctionForUserForStudentForItem(gradebookUid, assignmentId, studentId);
    }

    private boolean isUserAbleToViewAssignments(String gradebookUid) {
        return sakaiProxy.isUserAbleToEditAssessments(gradebookUid) || sakaiProxy.isUserAbleToGrade(gradebookUid);
    }

    private Assignment getAssignmentDefinition(final GradebookAssignment internalAssignment) {

        Assignment assignmentDefinition = new Assignment();
        assignmentDefinition.setName(internalAssignment.getName());
        assignmentDefinition.setPoints(internalAssignment.getPointsPossible());
        assignmentDefinition.setDueDate(internalAssignment.getDueDate());
        assignmentDefinition.setCounted(internalAssignment.isCounted());
        assignmentDefinition.setExternallyMaintained(internalAssignment.isExternallyMaintained());
        assignmentDefinition.setExternalAppName(internalAssignment.getExternalAppName());
        assignmentDefinition.setExternalId(internalAssignment.getExternalId());
        assignmentDefinition.setExternalData(internalAssignment.getExternalData());
        assignmentDefinition.setReleased(internalAssignment.isReleased());
        assignmentDefinition.setId(internalAssignment.getId());
        assignmentDefinition.setExtraCredit(internalAssignment.isExtraCredit());
        if (internalAssignment.getCategory() != null) {
            assignmentDefinition.setCategoryName(internalAssignment.getCategory().getName());
            assignmentDefinition.setWeight(internalAssignment.getCategory().getWeight());
            assignmentDefinition.setCategoryExtraCredit(internalAssignment.getCategory().isExtraCredit());
            assignmentDefinition.setCategoryId(internalAssignment.getCategory().getId());
            assignmentDefinition.setCategoryOrder(internalAssignment.getCategory().getCategoryOrder());
        }
        assignmentDefinition.setUngraded(internalAssignment.getUngraded());
        assignmentDefinition.setSortOrder(internalAssignment.getSortOrder());
        assignmentDefinition.setCategorizedSortOrder(internalAssignment.getCategorizedSortOrder());

        return assignmentDefinition;
    }

    @Override
    public boolean isUserAbleToGradeItemForStudent(String gradebookUid, Long itemId, String studentUid) {
        return sakaiProxy.isUserAbleToGradeItemForStudent(gradebookUid, itemId, studentUid);
    }

    public CommentDefinition getAssignmentScoreComment(String gradebookUid, Long assignmentId, String studentUid)
        throws GradebookNotFoundException, AssessmentNotFoundException {

        if (gradebookUid == null || assignmentId == null || studentUid == null) {
            throw new IllegalArgumentException("null parameter passed to getAssignmentScoreComment. Values are gradebookUid:" + gradebookUid + " assignmentId:" + assignmentId + " studentUid:"+ studentUid);
        }
        GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);
        if (assignment == null) {
            throw new AssessmentNotFoundException("There is no assignmentId " + assignmentId + " for gradebookUid " + gradebookUid);
        }

        CommentDefinition commentDefinition = null;
        Comment comment = persistence.getInternalComment(gradebookUid, assignmentId, studentUid);
        if (comment != null) {
            commentDefinition = new CommentDefinition();
            commentDefinition.setAssignmentName(assignment.getName());
            commentDefinition.setCommentText(comment.getCommentText());
            commentDefinition.setDateRecorded(comment.getDateRecorded());
            commentDefinition.setGraderUid(comment.getGraderId());
            commentDefinition.setStudentUid(comment.getStudentId());
        }
        return commentDefinition;
    }

    public boolean getIsAssignmentExcused(String gradebookUid, Long assignmentId, String studentUid)
        throws GradebookNotFoundException, AssessmentNotFoundException {

        if (gradebookUid == null || assignmentId == null || studentUid == null){
            throw new IllegalArgumentException("null parameter passed to getAssignmentScoreComment. Values are gradebookUid:" + gradebookUid + " assignmentId:" + assignmentId + " studentUid:"+ studentUid);
        }

        GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);
        AssignmentGradeRecord agr = persistence.getAssignmentGradeRecord(assignment, studentUid);

        if (agr == null) {
            return false;
        } else {
            return BooleanUtils.toBoolean(agr.getExcludedFromGrade());
        }
    }

    public void setAssignmentScoreComment(String gradebookUid, Long assignmentId, String studentUid, String commentText)
        throws GradebookNotFoundException, AssessmentNotFoundException {

        Comment comment = persistence.getInternalComment(gradebookUid, assignmentId, studentUid);
        if (comment == null) {
            comment = new Comment(studentUid, commentText, persistence.getAssignmentWithoutStats(gradebookUid, assignmentId));
        } else {
            comment.setCommentText(commentText);
        }
        comment.setGraderId(sakaiProxy.getCurrentUserId());
        comment.setDateRecorded(new Date());
        commentRepository.save(comment);
    }

    @Override
    public boolean isAssignmentDefined(String gradebookUid, String assignmentName) {

        if (!this.isUserAbleToViewAssignments(gradebookUid)) {
            log.warn("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to check for assignment {}", sakaiProxy.getCurrentUserId(), gradebookUid,
                    assignmentName);
            throw new GradebookSecurityException();
        }
        return persistence.getAssignmentWithoutStats(gradebookUid, assignmentName) != null;
    }

    public boolean isAssignmentDefined(Long gradableObjectId) {
        return gradebookAssignmentRepository.existsByIdAndRemovedIsFalse(gradableObjectId);
    }

    private void updateGradebook(Gradebook gradebook) throws StaleObjectModificationException {

        /*
        // Get the gradebook and selected mapping from persistence
        final Gradebook gradebookFromPersistence = gradebookRepository.findOne(gradebook.getId());
        final GradeMapping mappingFromPersistence = gradebookFromPersistence.getSelectedGradeMapping();

        // If the mapping has changed, and there are explicitly entered
        // course grade records, disallow this update.
        if (!mappingFromPersistence.getId().equals(gradebook.getSelectedGradeMapping().getId())) {
            if(isExplicitlyEnteredCourseGradeRecords(gradebook.getId())) {
                throw new IllegalStateException("Selected grade mapping can not be changed, since explicit course grades exist.");
            }
        }

        // Evict the persisted objects from the session and update the gradebook
        // so the new grade mapping is used in the sort column update
        //session.evict(mappingFromPersistence);
        for (Object element : gradebookFromPersistence.getGradeMappings()) {
            session.evict(element);
        }
        session.evict(gradebookFromPersistence);
        try {
            session.update(gradebook);
            session.flush();
        } catch (final StaleObjectStateException e) {
            throw new StaleObjectModificationException(e);
        }
        */
        gradebookRepository.save(gradebook);
    }

    private Long createCategory(Long gradebookId, String name, Double weight, Integer drop_lowest,
                               Integer dropHighest, Integer keepHighest, Boolean is_extra_credit) {
        return createCategory(gradebookId, name, weight, drop_lowest, dropHighest, keepHighest, is_extra_credit, null);
    }

    private Long createCategory(Long gradebookId, String name, Double weight, Integer drop_lowest,
                               Integer dropHighest, Integer keepHighest, Boolean is_extra_credit,
                               Integer categoryOrder) throws ConflictingCategoryNameException, StaleObjectModificationException {

        final Gradebook gb = gradebookRepository.findOne(gradebookId);

        final long numNameConflicts = categoryRepository.countByNameAndGradebookAndRemovedIsFalse(name, gb);

        if (numNameConflicts > 0L) {
            throw new ConflictingCategoryNameException("You can not save multiple catetories in a gradebook with the same name");
        }
        if (weight > 1 || weight < 0) {
            throw new IllegalArgumentException("weight for category is greater than 1 or less than 0 in createCategory of BaseHibernateManager");
        }
        if(((drop_lowest!=null && drop_lowest > 0) || (dropHighest!=null && dropHighest > 0)) && (keepHighest!=null && keepHighest > 0)) {
            throw new IllegalArgumentException("a combination of positive values for keepHighest and either drop_lowest or dropHighest occurred in createCategory of BaseHibernateManager");
        }

        Category ca = new Category();
        ca.setGradebook(gb);
        ca.setName(name);
        ca.setWeight(weight);
        ca.setDropLowest(drop_lowest);
        ca.setDropHighest(dropHighest);
        ca.setKeepHighest(keepHighest);
        //ca.setItemValue(itemValue);
        ca.setRemoved(false);
        ca.setExtraCredit(is_extra_credit);
        ca.setCategoryOrder(categoryOrder);

        return categoryRepository.save(ca).getId();
    }


    @Override
    public Map<String,String> transferGradebook(GradebookInformation gradebookInformation,
        List<Assignment> assignments, String toGradebookUid, String fromContext) {

        final Map<String, String> transversalMap = new HashMap<>();

        final Gradebook gradebook = getGradebook(toGradebookUid);

        gradebook.setCategory_type(gradebookInformation.getCategoryType());
        gradebook.setGrade_type(gradebookInformation.getGradeType());

        updateGradebook(gradebook);

        // all categories that we need to end up with
        final List<CategoryDefinition> categories = gradebookInformation.getCategories();

        // filter out externally managed assignments. These are never imported.
        assignments.removeIf(a -> a.isExternallyMaintained());

        // this map holds the names of categories that have been created in the site to the category ids
        // and is updated as we go along
        // likewise for list of assignments
        final Map<String, Long> categoriesCreated = new HashMap<>();
        final List<String> assignmentsCreated = new ArrayList<>();

        if (!categories.isEmpty()) {

            // migrate the categories with assignments
            categories.forEach(c -> {

                assignments.forEach(a -> {
                    if (StringUtils.equals(c.getName(), a.getCategoryName())) {
                        if (!categoriesCreated.containsKey(c.getName())) {
                            // create category
                            Long categoryId = null;
                            try {
                                categoryId = createCategory(gradebook.getId(), c.getName(), c.getWeight(), c.getDropLowest(),
                                        c.getDropHighest(), c.getKeepHighest(), c.getExtraCredit(), c.getCategoryOrder());
                            } catch (ConflictingCategoryNameException e) {
                                // category already exists. Could be from a merge.
                                log.info("Category: {} already exists in target site. Skipping creation.", c.getName());
                            }

                            if (categoryId == null) {
                                // couldn't create so look up the id in the target site
                                final List<CategoryDefinition> existingCategories = getCategoryDefinitions(gradebook.getUid());
                                categoryId = existingCategories.stream().filter(e -> StringUtils.equals(e.getName(), c.getName()))
                                        .findFirst().get().getId();
                            }
                            // record that we have created this category
                            categoriesCreated.put(c.getName(), categoryId);
                        }

                        // create the assignment for the current category
                        try {
                            Long newId = createAssignmentForCategory(gradebook.getId(), categoriesCreated.get(c.getName()), a.getName(), a.getPoints(),
                                    a.getDueDate(), !a.isCounted(), a.isReleased(), a.isExtraCredit(), a.getCategorizedSortOrder());
                            transversalMap.put("gb/"+a.getId(),"gb/"+newId);
                        } catch (final ConflictingAssignmentNameException e) {
                            // assignment already exists. Could be from a merge.
                            log.info("GradebookAssignment: {} already exists in target site. Skipping creation.", a.getName());
                        } catch (final Exception ex) {
                            log.warn("GradebookAssignment: exception {} trying to create {} in target site. Skipping creation.", ex.getMessage(), a.getName());
                        }

                        // record that we have created this assignment
                        assignmentsCreated.add(a.getName());
                    }
                });
            });

            // create any remaining categories that have no assignments
            categories.removeIf(c -> categoriesCreated.containsKey(c.getName()));
            categories.forEach(c -> {
                try {
                    createCategory(gradebook.getId(), c.getName(), c.getWeight(), c.getDropLowest(), c.getDropHighest(), c.getKeepHighest(),
                            c.getExtraCredit(), c.getCategoryOrder());
                } catch (final ConflictingCategoryNameException e) {
                    // category already exists. Could be from a merge.
                    log.info("Category: {} already exists in target site. Skipping creation.", c.getName());
                }
            });
        }

        // create any remaining assignments that have no categories
        assignments.removeIf(a -> assignmentsCreated.contains(a.getName()));
        assignments.forEach(a -> {
            try {
                Long newId = createAssignment(gradebook.getId(), a.getName(), a.getPoints(), a.getDueDate(), !a.isCounted(), a.isReleased(), a.isExtraCredit(), a.getSortOrder());
                transversalMap.put("gb/"+a.getId(),"gb/"+newId);
            } catch (final ConflictingAssignmentNameException e) {
                // assignment already exists. Could be from a merge.
                log.info("GradebookAssignment: {} already exists in target site. Skipping creation.", a.getName());
            } catch (final Exception ex) {
                log.warn("GradebookAssignment: exception {} trying to create {} in target site. Skipping creation.", ex.getMessage(), a.getName());
            }
        });

        // Carry over the old gradebook's selected grading scheme if possible.
        final String fromGradingScaleUid = gradebookInformation.getSelectedGradingScaleUid();

        MERGE_GRADE_MAPPING: if (!StringUtils.isEmpty(fromGradingScaleUid)) {
            for (final GradeMapping gradeMapping : gradebook.getGradeMappings()) {
                if (gradeMapping.getGradingScale().getUid().equals(fromGradingScaleUid)) {
                    // We have a match. Now make sure that the grades are as expected.
                    final Map<String, Double> inputGradePercents = gradebookInformation.getSelectedGradingScaleBottomPercents();
                    final Set<String> gradeCodes = inputGradePercents.keySet();
                    if (gradeCodes.containsAll(gradeMapping.getGradeMap().keySet())) {
                        // Modify the existing grade-to-percentage map.
                        for (final String gradeCode : gradeCodes) {
                            gradeMapping.getGradeMap().put(gradeCode, inputGradePercents.get(gradeCode));
                        }
                        gradebook.setSelectedGradeMapping(gradeMapping);
                        updateGradebook(gradebook);
                        log.info("Merge to gradebook {} updated grade mapping", toGradebookUid);
                    } else {
                        log.info("Merge to gradebook {} skipped grade mapping change because the {} grade codes did not match",
                                toGradebookUid, fromGradingScaleUid);
                    }
                    break MERGE_GRADE_MAPPING;
                }
            }
            // Did not find a matching grading scale.
            log.info("Merge to gradebook {} skipped grade mapping change because grading scale {} is not defined", toGradebookUid,
                    fromGradingScaleUid);
        }
        return transversalMap;
    }

    @Override
    public List<CategoryDefinition> getCategoryDefinitions(String gradebookUid) {

        if (gradebookUid == null) {
            throw new IllegalArgumentException("Null gradebookUid passed to getCategoryDefinitions");
        }

        if (!isUserAbleToViewAssignments(gradebookUid)) {
            log.warn("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to retrieve all categories without permission", sakaiProxy.getCurrentUserId(),
                    gradebookUid);
            throw new GradebookSecurityException();
        }

        List<Category> gbCategories = categoryRepository.findByGradebook_Uid(gradebookUid);
        return gbCategories.stream().map(c -> getCategoryDefinition(c)).collect(Collectors.toList());
    }

    private CategoryDefinition getCategoryDefinition(Category category) {

        CategoryDefinition categoryDef = new CategoryDefinition();
        if (category != null) {
            categoryDef.setId(category.getId());
            categoryDef.setName(category.getName());
            categoryDef.setWeight(category.getWeight());
            categoryDef.setDropLowest(category.getDropLowest());
            categoryDef.setDropHighest(category.getDropHighest());
            categoryDef.setKeepHighest(category.getKeepHighest());
            categoryDef.setAssignmentList(getAssignments(category.getGradebook().getUid(), category.getName()));
            categoryDef.setDropKeepEnabled(category.isDropScores());
            categoryDef.setExtraCredit(category.isExtraCredit());
            categoryDef.setCategoryOrder(category.getCategoryOrder());
        }

        return categoryDef;
    }

    /**
     * Map a set of GradeMapping to a list of GradeMappingDefinition
     *
     * @param gradeMappings set of GradeMapping
     * @return list of GradeMappingDefinition
     */
    private List<GradeMappingDefinition> getGradebookGradeMappings(Set<GradeMapping> gradeMappings) {

        return gradeMappings.stream().map(m -> {
                return new GradeMappingDefinition(m.getId(), m.getName(),
                    GradeMappingDefinition.sortGradeMapping(m.getGradeMap()),
                    GradeMappingDefinition.sortGradeMapping(m.getDefaultBottomPercents()));
            }).collect(Collectors.toList());
    }

    @Override
    public GradebookInformation getGradebookInformation(String gradebookUid) {

        if (gradebookUid == null) {
            throw new IllegalArgumentException("null gradebookUid " + gradebookUid);
        }

        if (!sakaiProxy.isUserAbleToEditAssessments(gradebookUid) && !sakaiProxy.isUserAbleToGrade(gradebookUid)) {
            log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to access gb information", sakaiProxy.getCurrentUserId(), gradebookUid);
            throw new GradebookSecurityException();
        }

        final Gradebook gradebook = getGradebook(gradebookUid);
        if (gradebook == null) {
            throw new IllegalArgumentException("Their is no gradbook associated with this Id: " + gradebookUid);
        }

        final GradebookInformation rval = new GradebookInformation();

        // add in all available grademappings for this gradebook
        rval.setGradeMappings(getGradebookGradeMappings(gradebook.getGradeMappings()));

        // add in details about the selected one
        final GradeMapping selectedGradeMapping = gradebook.getSelectedGradeMapping();
        if (selectedGradeMapping != null) {

            rval.setSelectedGradingScaleUid(selectedGradeMapping.getGradingScale().getUid());
            rval.setSelectedGradeMappingId(Long.toString(selectedGradeMapping.getId()));

            // note that these are not the DEFAULT bottom percents but the configured ones per gradebook
            Map<String, Double> gradeMap = selectedGradeMapping.getGradeMap();
            gradeMap = GradeMappingDefinition.sortGradeMapping(gradeMap);
            rval.setSelectedGradingScaleBottomPercents(gradeMap);
            rval.setGradeScale(selectedGradeMapping.getGradingScale().getName());
        }

        rval.setGradeType(gradebook.getGrade_type());
        rval.setCategoryType(gradebook.getCategory_type());
        rval.setDisplayReleasedGradeItemsToStudents(gradebook.isAssignmentsDisplayed());

        // add in the category definitions
        rval.setCategories(getCategoryDefinitions(gradebookUid));

        // add in the course grade display settings
        rval.setCourseGradeDisplayed(gradebook.isCourseGradeDisplayed());
        rval.setCourseLetterGradeDisplayed(gradebook.isCourseLetterGradeDisplayed());
        rval.setCoursePointsDisplayed(gradebook.isCoursePointsDisplayed());
        rval.setCourseAverageDisplayed(gradebook.isCourseAverageDisplayed());

        // add in stats display settings
        rval.setAssignmentStatsDisplayed(gradebook.isAssignmentStatsDisplayed());
        rval.setCourseGradeStatsDisplayed(gradebook.isCourseGradeStatsDisplayed());

        return rval;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void removeAssignment(final Long assignmentId) {

        GradebookAssignment asn = gradebookAssignmentRepository.findOne(assignmentId);
        asn.setRemoved(true);
        gradebookAssignmentRepository.save(asn);
        log.info("GradebookAssignment {} has been removed.", asn.getName());
    }

    public List<Category> getCategories(Long gradebookId) {
        return categoryRepository.findByGradebook_IdAndRemovedIsFalse(gradebookId);
    }

    public void removeCategory(Long categoryId) {

        Category persistentCat = categoryRepository.findOne(categoryId);

        List<GradebookAssignment> assigns = getAssignmentsForCategory(categoryId);
        for (GradebookAssignment assignment : assigns) {
            assignment.setCategory(null);
            updateAssignment(assignment);
        }

        persistentCat.setRemoved(true);
        categoryRepository.save(persistentCat);
    }

    private List<GradebookAssignment> getAssignmentsForCategory(Long categoryId) {
        return gradebookAssignmentRepository.findByCategory_IdAndRemovedIsFalse(categoryId);
    }

    private void updateAssignment(GradebookAssignment assignment) throws ConflictingAssignmentNameException {

		// Ensure that we don't have the assignment in the session, since
		// we need to compare the existing one in the db to our edited assignment
        //final Session session = getSessionFactory().getCurrentSession();
		//session.evict(assignment);

		GradebookAssignment asnFromDb = gradebookAssignmentRepository.findOne(assignment.getId());

        long count = gradableObjectRepository.countByNameAndGradebookAndIdNotAndRemovedIsFalse(
            assignment.getName(), assignment.getGradebook(), assignment.getId());

		if (count > 0) {
			throw new ConflictingAssignmentNameException("You can not save multiple assignments in a gradebook with the same name");
		}

		//session.evict(asnFromDb);
		gradebookAssignmentRepository.save(assignment);
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void updateAssignment(final String gradebookUid, final Long assignmentId, final Assignment assignmentDefinition) {

        if (!sakaiProxy.isUserAbleToEditAssessments(gradebookUid)) {
            log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to change the definition of assignment {}", sakaiProxy.getCurrentUserId(),
                    gradebookUid, assignmentId);
            throw new GradebookSecurityException();
        }

        // validate the name
        final String validatedName = StringUtils.trimToNull(assignmentDefinition.getName());
        if (validatedName == null) {
            throw new ConflictingAssignmentNameException("You cannot save an assignment without a name");
        }

        // name cannot contain these chars as they are reserved for special columns in import/export
        GradebookHelper.validateGradeItemName(validatedName);

        final Gradebook gradebook = this.getGradebook(gradebookUid);

        final GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);
        if (assignment == null) {
            throw new AssessmentNotFoundException(
                    "There is no assignment with id " + assignmentId + " in gradebook " + gradebookUid);
        }

        // check if we need to scale the grades
        boolean scaleGrades = false;
        final Double originalPointsPossible = assignment.getPointsPossible();
        if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_PERCENTAGE
                && !assignment.getPointsPossible().equals(assignmentDefinition.getPoints())) {
            scaleGrades = true;
        }

        if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_POINTS && assignmentDefinition.isScaleGrades()) {
            scaleGrades = true;
        }

        // external assessments are supported, but not these fields
        if (!assignmentDefinition.isExternallyMaintained()) {
            assignment.setName(validatedName);
            assignment.setPointsPossible(assignmentDefinition.getPoints());
            assignment.setDueDate(assignmentDefinition.getDueDate());
        }
        assignment.setExtraCredit(assignmentDefinition.isExtraCredit());
        assignment.setCounted(assignmentDefinition.isCounted());
        assignment.setReleased(assignmentDefinition.isReleased());

        assignment.setExternalAppName(assignmentDefinition.getExternalAppName());
        assignment.setExternallyMaintained(assignmentDefinition.isExternallyMaintained());
        assignment.setExternalId(assignmentDefinition.getExternalId());
        assignment.setExternalData(assignmentDefinition.getExternalData());

        // if we have a category, get it and set it
        // otherwise clear it fully
        if (assignmentDefinition.getCategoryId() != null) {
            Category cat = categoryRepository.findOne(assignmentDefinition.getCategoryId());
            assignment.setCategory(cat);
        } else {
            assignment.setCategory(null);
        }

        updateAssignment(assignment);

        if (scaleGrades) {
            scaleGrades(gradebook, assignment, originalPointsPossible);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.sakaiproject.service.gradebook.shared.GradebookService#getViewableAssignmentsForCurrentUser(java.lang.String)
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<Assignment> getViewableAssignmentsForCurrentUser(String gradebookUid) throws GradebookNotFoundException {
        return getViewableAssignmentsForCurrentUser(gradebookUid, SortType.SORT_BY_SORTING);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.sakaiproject.service.gradebook.shared.GradebookService#getViewableAssignmentsForCurrentUser(java.lang.String, java.)
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<Assignment> getViewableAssignmentsForCurrentUser(String gradebookUid, SortType sortBy)
            throws GradebookNotFoundException {

        List<GradebookAssignment> viewableAssignments = new ArrayList<>();
        final LinkedHashSet<Assignment> assignmentsToReturn = new LinkedHashSet<>();

        final Gradebook gradebook = getGradebook(gradebookUid);

        // will send back all assignments if user can grade all
        if (sakaiProxy.isUserAbleToGradeAll(gradebookUid)) {
            viewableAssignments = getAssignments(gradebook.getId(), sortBy, true);
        } else if (sakaiProxy.isUserAbleToGrade(gradebookUid)) {
            // if user can grade and doesn't have grader perm restrictions, they
            // may view all assigns
            if (!sakaiProxy.isUserAbleToGrade(gradebookUid)) {
                viewableAssignments = getAssignments(gradebook.getId(), sortBy, true);
            } else {
                // this user has grader perms, so we need to filter the items returned
                // if this gradebook has categories enabled, we need to check for category-specific restrictions
                if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_NO_CATEGORY) {
                    assignmentsToReturn.addAll(getAssignments(gradebookUid, sortBy));
                } else {
                    String userUid = sakaiProxy.getCurrentUserId();
                    if (permissions.getPermissionForUserForAllAssignment(gradebook.getId(), userUid)) {
                        assignmentsToReturn.addAll(getAssignments(gradebookUid, sortBy));
                    } else {
                        List<Assignment> assignments = getAssignments(gradebookUid, sortBy);
                        List<Long> categoryIds = getCategories(gradebook.getId()).stream().map(Category::getId)
                            .collect(Collectors.toList());
                        // categories are enabled, so we need to check the category restrictions
                        if (!categoryIds.isEmpty()) {
                            List<Long> viewableCategoryIds = permissions.getCategoriesForUser(gradebook.getId(),
                                    userUid, categoryIds);
                            assignmentsToReturn.addAll(assignments.stream()
                                .filter(a -> a != null && viewableCategoryIds.contains(a.getCategoryId()))
                                .collect(Collectors.toList()));
                        }
                    }
                }
            }
        } else if (sakaiProxy.isUserAbleToViewOwnGrades(gradebookUid)) {
            // if user is just a student, we need to filter out unreleased items
            List<GradebookAssignment> allAssigns = getAssignments(gradebook.getId(), null, true);
            viewableAssignments.addAll(allAssigns.stream().filter(a -> a != null && a.isReleased()).collect(Collectors.toList()));
        }

        // Now we need to convert these to the assignment template objects
        assignmentsToReturn.addAll(
            viewableAssignments.stream().map(a -> getAssignmentDefinition(a)).collect(Collectors.toSet()));

        return new ArrayList<>(assignmentsToReturn);
    }

    /**
     * Oracle has a low limit on the maximum length of a parameter list
     * in SQL queries of the form "WHERE tbl.col IN (:paramList)".
     * Since enrollment lists can sometimes be very long, we've replaced
     * such queries with full selects followed by filtering. This helper
     * method filters out unwanted grade records. (Typically they're not
     * wanted because they're either no longer officially enrolled in the
     * course or they're not members of the selected section.)
     */
    private List<AssignmentGradeRecord> filterGradeRecordsByStudents(List<AssignmentGradeRecord> gradeRecords, List<String> studentIds) {
        return gradeRecords.stream().filter(r -> studentIds.contains(r.getStudentId())).collect(Collectors.toList());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<AssignmentGradeRecord> getAllAssignmentGradeRecordsForGbItem(Long gradableObjectId, List<String> studentUids) {

            if (studentUids.isEmpty()) {
                // If there are no enrollments, no need to execute the query.
                log.info("No enrollments were specified.  Returning an empty List of grade records");
                return new ArrayList<>();
            } else {
                List<AssignmentGradeRecord> list = assignmentGradeRecordRepository
                    .findByGradableObject_RemovedIsFalseAndGradableObject_IdOrderByPointsEarned(gradableObjectId);
                return filterGradeRecordsByStudents(list, studentUids);
            }
    }

     private Double calculateEquivalentPointValueForPercent(Double doublePointsPossible, Double doublePercentEarned) {

    	if (doublePointsPossible == null || doublePercentEarned == null) {
			return null;
		}

    	BigDecimal pointsPossible = new BigDecimal(doublePointsPossible.toString());
		BigDecimal percentEarned = new BigDecimal(doublePercentEarned.toString());
		BigDecimal equivPoints = pointsPossible.multiply(percentEarned.divide(new BigDecimal("100"), GradingConstants.MATH_CONTEXT));
		return equivPoints.doubleValue();
    }

    /**
     * Update the persistent grade points for an assignment when the total points is changed.
     *
     * @param gradebook the gradebook
     * @param assignment assignment with original total point value
     */
    private void scaleGrades(Gradebook gradebook, GradebookAssignment assignment, Double originalPointsPossible) {

        if (gradebook == null || assignment == null || assignment.getPointsPossible() == null) {
            throw new IllegalArgumentException("null values found in convertGradePointsForUpdatedTotalPoints.");
        }

        List<String> studentUids = sakaiProxy.getStudentsForGradebook(gradebook.getUid());
        List<AssignmentGradeRecord> gradeRecords = getAllAssignmentGradeRecordsForGbItem(assignment.getId(), studentUids);

        // scale for total points changed when on percentage grading
        // TODO could scale for total points changed when on a points grading as well, though needs different logic
        if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_PERCENTAGE && assignment.getPointsPossible() != null) {

            log.debug("Scaling percentage grades");

            for (AssignmentGradeRecord gr : gradeRecords) {
                if (gr.getPointsEarned() != null) {
                    BigDecimal scoreAsPercentage = (new BigDecimal(gr.getPointsEarned())
                            .divide(new BigDecimal(originalPointsPossible), GradingConstants.MATH_CONTEXT))
                                    .multiply(new BigDecimal(100));

                    Double scaledScore = calculateEquivalentPointValueForPercent(assignment.getPointsPossible(),
                            scoreAsPercentage.doubleValue());

                    log.debug("scoreAsPercentage: {}", scoreAsPercentage);
                    log.debug("scaledScore: {}", scaledScore);

                    gr.setPointsEarned(scaledScore);
                }
            }
        }

        if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_POINTS && assignment.getPointsPossible() != null) {

            log.debug("Scaling point grades");

            final BigDecimal previous = new BigDecimal(originalPointsPossible);
            final BigDecimal current = new BigDecimal(assignment.getPointsPossible());
            final BigDecimal factor = current.divide(previous, GradingConstants.MATH_CONTEXT);

            log.debug("previous points possible: {}", previous);
            log.debug("current points possible: {]", current);
            log.debug("factor: {}", factor);

            for (AssignmentGradeRecord gr : gradeRecords) {
                if (gr.getPointsEarned() != null) {

                    BigDecimal currentGrade = new BigDecimal(gr.getPointsEarned());
                    BigDecimal scaledGrade = currentGrade.multiply(factor, GradingConstants.MATH_CONTEXT);

                    log.debug("currentGrade: {}", currentGrade);
                    log.debug("scaledGrade: {}", scaledGrade);

                    gr.setPointsEarned(scaledGrade.doubleValue());
                }
            }
        }

        gradeRecords.forEach(assignmentGradeRecordRepository::save);
    }

	@Override
	public Map<String, String> getViewableStudentsForItemForCurrentUser(String gradebookUid, Long gradableObjectId) {
        return getViewableStudentsForItemForUser(sakaiProxy.getCurrentUserId(), gradebookUid, gradableObjectId);
	}

    //@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, String> getViewableStudentsForItemForUser(String userUid, String gradebookUid, Long gradableObjectId) {

        if (gradebookUid == null || gradableObjectId == null || userUid == null) {
            throw new IllegalArgumentException("null gradebookUid or gradableObjectId or " +
                    "userId passed to getViewableStudentsForUserForItem." +
                    " gradebookUid: " + gradebookUid + " gradableObjectId:" +
                    gradableObjectId + " userId: " + userUid);
        }

        if (!sakaiProxy.isUserAbleToGrade(gradebookUid, userUid)) {
            return new HashMap<>();
        }

        GradebookAssignment gradebookItem = persistence.getAssignmentWithoutStats(gradebookUid, gradableObjectId);

        if (gradebookItem == null) {
            log.debug("The gradebook item does not exist, so returning empty set");
            return new HashMap();
        }

        Long categoryId = gradebookItem.getCategory() == null ? null : gradebookItem.getCategory().getId();

        Map<EnrollmentRecord, String> enrRecFunctionMap = sakaiProxy.findMatchingEnrollmentsForItemForUser(userUid, gradebookUid,
                categoryId, getGradebook(gradebookUid).getCategory_type(), null, null);
        if (enrRecFunctionMap == null) {
            return new HashMap<>();
        }

        Map<String, String> studentIdFunctionMap = new HashMap();
        for (Map.Entry<EnrollmentRecord, String> entry : enrRecFunctionMap.entrySet()) {
            EnrollmentRecord enr = entry.getKey();
            if (enr != null && enrRecFunctionMap.get(enr) != null) {
                studentIdFunctionMap.put(enr.getUser().getUserUid(), entry.getValue());
            }
        }
        return studentIdFunctionMap;
    }

    @Override
	public Map<String, String> getImportCourseGrade(final String gradebookUid) {
		return getImportCourseGrade(gradebookUid, true, true);
	}

	@Override
	public Map<String, String> getImportCourseGrade(final String gradebookUid, final boolean useDefault) {
		return getImportCourseGrade(gradebookUid, useDefault, true);
	}

    private List<GradebookAssignment> getCountedAssignments(Long gradebookId) {
        return gradebookAssignmentRepository.findByGradebook_IdAndRemovedIsFalseAndNotCountedIsFalse(gradebookId);
    }

	@Override
	public Map<String, String> getImportCourseGrade(final String gradebookUid, final boolean useDefault, final boolean mapTheGrades) {
		final HashMap<String, String> returnMap = new HashMap<>();

		try {
			// There is a new permission for course grade visibility for TA's as part of GradebookNG.
			// However the permission cannot be added here as it is not backwards compatible with Gradebook classique
			// and would mean that all existing permissions need to be updated to add it.
			// See GradebookNgBusinessService.isCourseGradeVisible.
			// At some point it should be migrated and a DB conversion performed.

			final Gradebook thisGradebook = getGradebook(gradebookUid);

			final List assignList = getCountedAssignments(thisGradebook.getId());
			boolean nonAssignment = false;
			if (assignList == null || assignList.size() < 1) {
				nonAssignment = true;
			}

			final Long gradebookId = thisGradebook.getId();
			final CourseGrade courseGrade = getCourseGrade(gradebookId);

			final Map viewableEnrollmentsMap = sakaiProxy.findMatchingEnrollmentsForViewableCourseGrade(gradebookUid,
					thisGradebook.getCategory_type(), null, null);
			final Map<String, EnrollmentRecord> enrollmentMap = new HashMap<>();

			final Map<String, EnrollmentRecord> enrollmentMapUid = new HashMap<>();
			for (final Iterator iter = viewableEnrollmentsMap.keySet().iterator(); iter.hasNext();) {
				final EnrollmentRecord enr = (EnrollmentRecord) iter.next();
				enrollmentMap.put(enr.getUser().getUserUid(), enr);
				enrollmentMapUid.put(enr.getUser().getUserUid(), enr);
			}
			final List gradeRecords = getPointsEarnedCourseGradeRecords(courseGrade, enrollmentMap.keySet());
			for (final Iterator iter = gradeRecords.iterator(); iter.hasNext();) {
				final CourseGradeRecord gradeRecord = (CourseGradeRecord) iter.next();

				final GradeMapping gradeMap = thisGradebook.getSelectedGradeMapping();

				final EnrollmentRecord enr = enrollmentMapUid.get(gradeRecord.getStudentId());
				if (enr != null) {
					// SAK-29243: if we are not mapping grades, we don't want letter grade here
					if (mapTheGrades && StringUtils.isNotBlank(gradeRecord.getEnteredGrade())) {
						returnMap.put(enr.getUser().getDisplayId(), gradeRecord.getEnteredGrade());
					} else {
						if (!nonAssignment) {
							Double grade;

							if (useDefault) {
								grade = gradeRecord.getNonNullAutoCalculatedGrade();
							} else {
								grade = gradeRecord.getAutoCalculatedGrade();
							}

							if (mapTheGrades) {
								returnMap.put(enr.getUser().getDisplayId(), gradeMap.getMappedGrade(grade));
							} else {
								returnMap.put(enr.getUser().getDisplayId(), grade.toString());
							}

						}
					}
				}
			}
		} catch (final Exception e) {
			log.error("Error in getImportCourseGrade", e);
		}
		return returnMap;
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public List getPointsEarnedCourseGradeRecords(final CourseGrade courseGrade, final Collection studentUids) {

        if (studentUids == null || studentUids.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info("Returning no grade records for an empty collection of student UIDs");
            }
            return new ArrayList();
        }

        List unfilteredRecords = courseGradeRecordRepository.findByGradableObject_Id(courseGrade.getId());
        final List records = filterAndPopulateCourseGradeRecordsByStudents(courseGrade, unfilteredRecords, studentUids);

        final Long gradebookId = courseGrade.getGradebook().getId();
        final Gradebook gradebook = getGradebook(gradebookId);
        final List cates = getCategories(gradebookId);

        // get all of the AssignmentGradeRecords here to avoid repeated db calls
        final Map<String, List<AssignmentGradeRecord>> gradeRecMap = getGradeRecordMapForStudents(gradebookId, studentUids);

        // get all of the counted assignments
        final List<GradebookAssignment> assignments = getCountedAssignments(gradebookId);
        final List<GradebookAssignment> countedAssigns = new ArrayList<>();
        if (assignments != null) {
            for (final GradebookAssignment assign : assignments) {
                // extra check to account for new features like extra credit
                if (assign.isIncludedInCalculations()) {
                    countedAssigns.add(assign);
                }
            }
        }
        // double totalPointsPossible = getTotalPointsInternal(gradebookId, session);
        // if(log.isDebugEnabled()) log.debug("Total points = " + totalPointsPossible);

        for (final Iterator iter = records.iterator(); iter.hasNext();) {
            final CourseGradeRecord cgr = (CourseGradeRecord) iter.next();
            // double totalPointsEarned = getTotalPointsEarnedInternal(gradebookId, cgr.getStudentId(), session);
            final List<AssignmentGradeRecord> studentGradeRecs = gradeRecMap.get(cgr.getStudentId());

            applyDropScores(studentGradeRecs);
            final List totalEarned = getTotalPointsEarnedInternal(cgr.getStudentId(), gradebook, cates, studentGradeRecs,
                    countedAssigns);
            final double totalPointsEarned = ((Double) totalEarned.get(0));
            final double literalTotalPointsEarned = ((Double) totalEarned.get(1));
            final double totalPointsPossible = getTotalPointsInternal(gradebook, cates, cgr.getStudentId(), studentGradeRecs,
                    countedAssigns, false);
            cgr.initNonpersistentFields(totalPointsPossible, totalPointsEarned, literalTotalPointsEarned);
            if (log.isDebugEnabled()) {
                log.debug("Points earned = " + cgr.getPointsEarned());
            }
            if (log.isDebugEnabled()) {
                log.debug("Points possible = " + cgr.getTotalPointsPossible());
            }
        }

        return records;
	}

    private List<AssignmentGradeRecord> getAllAssignmentGradeRecords(Long gradebookId, Collection<String> studentIds) {
        return assignmentGradeRecordRepository.findByGradableObject_Gradebook_IdAndGradableObject_RemovedIsFalseAndStudentIdIn(gradebookId, studentIds);
    }

    /**
	 *
	 * @param gradebookId
	 * @param studentUids
	 * @return a map of studentUid to a list of that student's AssignmentGradeRecords for the given studentUids list in the given gradebook.
	 *         the grade records are all recs for assignments that are not removed and have a points possible > 0
	 */
	private Map<String, List<AssignmentGradeRecord>> getGradeRecordMapForStudents(Long gradebookId, Collection<String> studentUids) {
		final Map<String, List<AssignmentGradeRecord>> filteredGradeRecs = new HashMap<>();
		if (studentUids != null) {
            List<AssignmentGradeRecord> allGradeRecs = getAllAssignmentGradeRecords(gradebookId, studentUids);

			if (allGradeRecs != null) {
				for (final AssignmentGradeRecord gradeRec : allGradeRecs) {
					if (studentUids.contains(gradeRec.getStudentId())) {
						final String studentId = gradeRec.getStudentId();
						List<AssignmentGradeRecord> gradeRecList = filteredGradeRecs.get(studentId);
						if (gradeRecList == null) {
							gradeRecList = new ArrayList<>();
							gradeRecList.add(gradeRec);
							filteredGradeRecs.put(studentId, gradeRecList);
						} else {
							gradeRecList.add(gradeRec);
							filteredGradeRecs.put(studentId, gradeRecList);
						}
					}
				}
			}
		}

		return filteredGradeRecs;
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private List filterAndPopulateCourseGradeRecordsByStudents(final CourseGrade courseGrade, final Collection gradeRecords,
			final Collection studentUids) {
		final List filteredRecords = new ArrayList();
		final Set missingStudents = new HashSet(studentUids);
		for (final Iterator iter = gradeRecords.iterator(); iter.hasNext();) {
			final CourseGradeRecord cgr = (CourseGradeRecord) iter.next();
			if (studentUids.contains(cgr.getStudentId())) {
				filteredRecords.add(cgr);
				missingStudents.remove(cgr.getStudentId());
			}
		}
		for (final Iterator iter = missingStudents.iterator(); iter.hasNext();) {
			final String studentUid = (String) iter.next();
			final CourseGradeRecord cgr = new CourseGradeRecord(courseGrade, studentUid);
			filteredRecords.add(cgr);
		}
		return filteredRecords;
	}

    /**
	 * set the droppedFromGrade attribute of each of the n highest and the n lowest scores of a student based on the assignment's category
	 *
	 * @param gradeRecords
	 *
	 *            NOTE: When the UI changes, this needs to be made private again
	 */
	private void applyDropScores(final Collection<AssignmentGradeRecord> gradeRecords) {
		if (gradeRecords == null || gradeRecords.size() < 1) {
			return;
		}
		final long start = System.currentTimeMillis();

		final List<String> studentIds = new ArrayList<>();
		final List<Category> categories = new ArrayList<>();
		final Map<String, List<AssignmentGradeRecord>> gradeRecordMap = new HashMap<>();
		for (final AssignmentGradeRecord gradeRecord : gradeRecords) {

			if (gradeRecord == null
					|| gradeRecord.getPointsEarned() == null) { // don't consider grades that have null pointsEarned (this occurs when a
																// previously entered score for an assignment is removed; record stays in
																// database)
				continue;
			}

			// reset
			gradeRecord.setDroppedFromGrade(false);

			final GradebookAssignment assignment = gradeRecord.getAssignment();
			if (assignment.getUngraded() // GradingConstants.GRADE_TYPE_LETTER
					|| assignment.isNotCounted() // don't consider grades that are not counted toward course grade
					|| assignment.getItemType().equals(GradebookAssignment.item_type_adjustment)
					|| assignment.isRemoved()) {
				continue;
			}
			// get all the students represented
			final String studentId = gradeRecord.getStudentId();
			if (!studentIds.contains(studentId)) {
				studentIds.add(studentId);
			}
			// get all the categories represented
			final Category cat = gradeRecord.getAssignment().getCategory();
			if (cat != null) {
				if (!categories.contains(cat)) {
					categories.add(cat);
				}
				List<AssignmentGradeRecord> gradeRecordsByCatAndStudent = gradeRecordMap.get(studentId + cat.getId());
				if (gradeRecordsByCatAndStudent == null) {
					gradeRecordsByCatAndStudent = new ArrayList<>();
					gradeRecordsByCatAndStudent.add(gradeRecord);
					gradeRecordMap.put(studentId + cat.getId(), gradeRecordsByCatAndStudent);
				} else {
					gradeRecordsByCatAndStudent.add(gradeRecord);
				}
			}
		}

		if (categories.size() < 1) {
			return;
		}
		for (final Category cat : categories) {
			final Integer dropHighest = cat.getDropHighest();
			Integer dropLowest = cat.getDropLowest();
			final Integer keepHighest = cat.getKeepHighest();
			final Long catId = cat.getId();

			if ((dropHighest != null && dropHighest > 0) || (dropLowest != null && dropLowest > 0)
					|| (keepHighest != null && keepHighest > 0)) {

				for (final String studentId : studentIds) {
					// get the student's gradeRecords for this category
					final List<AssignmentGradeRecord> gradesByCategory = new ArrayList<>();
					final List<AssignmentGradeRecord> gradeRecordsByCatAndStudent = gradeRecordMap.get(studentId + cat.getId());
					if (gradeRecordsByCatAndStudent != null) {
						for (final AssignmentGradeRecord agr : gradeRecordsByCatAndStudent) {
							if (!BooleanUtils.toBoolean(agr.getExcludedFromGrade())) {
								gradesByCategory.add(agr);
							}
						}

						final int numGrades = gradesByCategory.size();

						if (dropHighest > 0 && numGrades > dropHighest + dropLowest) {
							for (int i = 0; i < dropHighest; i++) {
								final AssignmentGradeRecord highest = Collections.max(gradesByCategory,
										AssignmentGradeRecord.numericComparator);
								highest.setDroppedFromGrade(true);
								gradesByCategory.remove(highest);
								if (log.isDebugEnabled()) {
									log.debug("dropHighest applied to " + highest);
								}
							}
						}

						if (keepHighest > 0 && numGrades > (gradesByCategory.size() - keepHighest)) {
							dropLowest = gradesByCategory.size() - keepHighest;
						}

						if (dropLowest > 0 && numGrades > dropLowest + dropHighest) {
							for (int i = 0; i < dropLowest; i++) {
								final AssignmentGradeRecord lowest = Collections.min(gradesByCategory,
										AssignmentGradeRecord.numericComparator);
								lowest.setDroppedFromGrade(true);
								gradesByCategory.remove(lowest);
								if (log.isDebugEnabled()) {
									log.debug("dropLowest applied to " + lowest);
								}
							}
						}
					}
				}
				if (log.isDebugEnabled()) {
					log.debug("processed " + studentIds.size() + "students in category " + cat.getId());
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("GradebookManager.applyDropScores took " + (System.currentTimeMillis() - start) + " millis to execute");
		}
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private double getTotalPointsInternal(final Gradebook gradebook, final List categories, final String studentId,
			final List<AssignmentGradeRecord> studentGradeRecs, final List<GradebookAssignment> countedAssigns,
			final boolean literalTotal) {
		final int gbGradeType = gradebook.getGrade_type();
		if (gbGradeType != GradingConstants.GRADE_TYPE_POINTS && gbGradeType != GradingConstants.GRADE_TYPE_PERCENTAGE) {
			if (log.isErrorEnabled()) {
				log.error("Wrong grade type in GradebookCalculationImpl.getTotalPointsInternal");
			}
			return -1;
		}

		if (studentGradeRecs == null || countedAssigns == null) {
			if (log.isDebugEnabled()) {
				log.debug("Returning 0 from getTotalPointsInternal " +
						"since studentGradeRecs or countedAssigns was null");
			}
			return 0;
		}

		double totalPointsPossible = 0;

		final HashSet<GradebookAssignment> countedSet = new HashSet<>(countedAssigns);

		// we need to filter this list to identify only "counted" grade recs
		final List<AssignmentGradeRecord> countedGradeRecs = new ArrayList<>();
		for (final AssignmentGradeRecord gradeRec : studentGradeRecs) {
			final GradebookAssignment assign = gradeRec.getAssignment();
			boolean extraCredit = assign.isExtraCredit();
			if (gradebook.getCategory_type() != GradingConstants.CATEGORY_TYPE_NO_CATEGORY && assign.getCategory() != null
					&& assign.getCategory().isExtraCredit()) {
				extraCredit = true;
			}

			final boolean excused = BooleanUtils.toBoolean(gradeRec.getExcludedFromGrade());
			if (assign.isCounted() && !assign.getUngraded() && !assign.isRemoved() && countedSet.contains(assign) &&
					assign.getPointsPossible() != null && assign.getPointsPossible() > 0 && !gradeRec.getDroppedFromGrade() && !extraCredit
					&& !excused) {
				countedGradeRecs.add(gradeRec);
			}
		}

		final Set assignmentsTaken = new HashSet();
		final Set categoryTaken = new HashSet();
		for (final AssignmentGradeRecord gradeRec : countedGradeRecs) {
			if (gradeRec.getPointsEarned() != null && !gradeRec.getPointsEarned().equals("")) {
				final Double pointsEarned = gradeRec.getPointsEarned();
				final GradebookAssignment go = gradeRec.getAssignment();
				if (pointsEarned != null) {
					if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_NO_CATEGORY) {
						assignmentsTaken.add(go.getId());
					} else if ((gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_ONLY_CATEGORY || gradebook
							.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY)
							&& go != null && categories != null) {
						// assignmentsTaken.add(go.getId());
						// }
						// else if(gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY && go != null &&
						// categories != null)
						// {
						for (int i = 0; i < categories.size(); i++) {
							final Category cate = (Category) categories.get(i);
							if (cate != null && !cate.isRemoved() && go.getCategory() != null
									&& cate.getId().equals(go.getCategory().getId())
									&& ((cate.isExtraCredit() != null && !cate.isExtraCredit()) || cate.isExtraCredit() == null)) {
								assignmentsTaken.add(go.getId());
								categoryTaken.add(cate.getId());
								break;
							}
						}
					}
				}
			}
		}

		if (!assignmentsTaken.isEmpty()) {
			if (!literalTotal && gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
				for (int i = 0; i < categories.size(); i++) {
					final Category cate = (Category) categories.get(i);
					if (cate != null && !cate.isRemoved() && categoryTaken.contains(cate.getId())) {
						totalPointsPossible += cate.getWeight();
					}
				}
				return totalPointsPossible;
			}
			final Iterator assignmentIter = countedAssigns.iterator();
			while (assignmentIter.hasNext()) {
				final GradebookAssignment asn = (GradebookAssignment) assignmentIter.next();
				if (asn != null) {
					final Double pointsPossible = asn.getPointsPossible();

					if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_NO_CATEGORY
							&& assignmentsTaken.contains(asn.getId())) {
						totalPointsPossible += pointsPossible;
					} else if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_ONLY_CATEGORY
							&& assignmentsTaken.contains(asn.getId())) {
						totalPointsPossible += pointsPossible;
					} else if (literalTotal && gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY
							&& assignmentsTaken.contains(asn.getId())) {
						totalPointsPossible += pointsPossible;
					}
				}
			}
		} else {
			totalPointsPossible = -1;
		}

		return totalPointsPossible;
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private List getTotalPointsEarnedInternal(final String studentId, final Gradebook gradebook, final List categories,
			final List<AssignmentGradeRecord> gradeRecs, final List<GradebookAssignment> countedAssigns) {
		final int gbGradeType = gradebook.getGrade_type();
		if (gbGradeType != GradingConstants.GRADE_TYPE_POINTS && gbGradeType != GradingConstants.GRADE_TYPE_PERCENTAGE) {
			if (log.isErrorEnabled()) {
				log.error("Wrong grade type in GradebookCalculationImpl.getTotalPointsEarnedInternal");
			}
			return new ArrayList();
		}

		if (gradeRecs == null || countedAssigns == null) {
			if (log.isDebugEnabled()) {
				log.debug("getTotalPointsEarnedInternal for " +
						"studentId=" + studentId + " returning 0 because null gradeRecs or countedAssigns");
			}
			final List returnList = new ArrayList();
			returnList.add(new Double(0));
			returnList.add(new Double(0));
			returnList.add(new Double(0)); // 3rd one is for the pre-adjusted course grade
			return returnList;
		}

		double totalPointsEarned = 0;
		BigDecimal literalTotalPointsEarned = new BigDecimal(0d);

		final Map cateScoreMap = new HashMap();
		final Map cateTotalScoreMap = new HashMap();

		final Set assignmentsTaken = new HashSet();
		for (final AssignmentGradeRecord gradeRec : gradeRecs) {
			final boolean excused = BooleanUtils.toBoolean(gradeRec.getExcludedFromGrade());

			if (gradeRec.getPointsEarned() != null && !gradeRec.getPointsEarned().equals("") && !gradeRec.getDroppedFromGrade()) {
				final GradebookAssignment go = gradeRec.getAssignment();
				if (go.isIncludedInCalculations() && countedAssigns.contains(go)) {
					final Double pointsEarned = gradeRec.getPointsEarned();
					// if(gbGradeType == GradingConstants.GRADE_TYPE_POINTS)
					// {
					if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_NO_CATEGORY) {
						if (!excused) {
							totalPointsEarned += pointsEarned;
							literalTotalPointsEarned = (new BigDecimal(pointsEarned)).add(literalTotalPointsEarned);
							assignmentsTaken.add(go.getId());
						}
					} else if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_ONLY_CATEGORY && go != null) {
						if (!excused) {
							totalPointsEarned += pointsEarned;
							literalTotalPointsEarned = (new BigDecimal(pointsEarned)).add(literalTotalPointsEarned);
							assignmentsTaken.add(go.getId());
						}
					} else if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY && go != null
							&& categories != null) {
						for (int i = 0; i < categories.size(); i++) {
							final Category cate = (Category) categories.get(i);
							if (cate != null && !cate.isRemoved() && go.getCategory() != null
									&& cate.getId().equals(go.getCategory().getId())) {
								if (!excused) {
									assignmentsTaken.add(go.getId());
									literalTotalPointsEarned = (new BigDecimal(pointsEarned)).add(literalTotalPointsEarned);
									if (cateScoreMap.get(cate.getId()) != null) {
										cateScoreMap.put(cate.getId(), ((Double) cateScoreMap.get(cate.getId())) + pointsEarned);
									} else {
										cateScoreMap.put(cate.getId(), pointsEarned);
									}
								}
								break;
							}
						}
					}
				}
			}
		}

		if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY && categories != null) {
			final Iterator assgnsIter = countedAssigns.iterator();
			while (assgnsIter.hasNext()) {
				final GradebookAssignment asgn = (GradebookAssignment) assgnsIter.next();
				if (assignmentsTaken.contains(asgn.getId())) {
					for (int i = 0; i < categories.size(); i++) {
						final Category cate = (Category) categories.get(i);
						if (cate != null && !cate.isRemoved() && asgn.getCategory() != null
								&& cate.getId().equals(asgn.getCategory().getId()) && !asgn.isExtraCredit()) {

							if (cateTotalScoreMap.get(cate.getId()) == null) {
								cateTotalScoreMap.put(cate.getId(), asgn.getPointsPossible());
							} else {
								cateTotalScoreMap.put(cate.getId(),
										((Double) cateTotalScoreMap.get(cate.getId())) + asgn.getPointsPossible());
							}

						}
					}
				}
			}
		}

		if (assignmentsTaken.isEmpty()) {
			totalPointsEarned = -1;
		}

		if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
			for (int i = 0; i < categories.size(); i++) {
				final Category cate = (Category) categories.get(i);
				if (cate != null && !cate.isRemoved() && cateScoreMap.get(cate.getId()) != null
						&& cateTotalScoreMap.get(cate.getId()) != null) {
					totalPointsEarned += ((Double) cateScoreMap.get(cate.getId())) * cate.getWeight()
							/ ((Double) cateTotalScoreMap.get(cate.getId()));
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("getTotalPointsEarnedInternal for studentId=" + studentId + " returning " + totalPointsEarned);
		}
		final List returnList = new ArrayList();
		returnList.add(totalPointsEarned);
		returnList.add((new BigDecimal(literalTotalPointsEarned.doubleValue(), GradingConstants.MATH_CONTEXT)).doubleValue());

		return returnList;
	}

    @Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean checkStudentsNotSubmitted(final String gradebookUid) {
		final Gradebook gradebook = getGradebook(gradebookUid);
		final Set studentUids = sakaiProxy.getAllStudentUids(gradebookUid);
		if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_NO_CATEGORY
				|| gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_ONLY_CATEGORY) {
			final List records = getAllAssignmentGradeRecords(gradebook.getId(), studentUids);
			final List assigns = getAssignments(gradebook.getId(), SortType.SORT_BY_SORTING, true);
			final List filteredAssigns = new ArrayList();
			for (final Iterator iter = assigns.iterator(); iter.hasNext();) {
				final GradebookAssignment assignment = (GradebookAssignment) iter.next();
				if (assignment.isCounted() && !assignment.getUngraded()) {
					filteredAssigns.add(assignment);
				}
			}
			final List filteredRecords = new ArrayList();
			for (final Iterator iter = records.iterator(); iter.hasNext();) {
				final AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
				if (!agr.isCourseGradeRecord() && agr.getAssignment().isCounted() && !agr.getAssignment().getUngraded()) {
					if (agr.getPointsEarned() == null) {
						return true;
					}
					filteredRecords.add(agr);
				}
			}

			return filteredRecords.size() < (filteredAssigns.size() * studentUids.size());
		} else {
			final List assigns = getAssignments(gradebook.getId(), SortType.SORT_BY_SORTING, true);
			final List records = getAllAssignmentGradeRecords(gradebook.getId(), studentUids);
			final Set filteredAssigns = new HashSet();
			for (final Iterator iter = assigns.iterator(); iter.hasNext();) {
				final GradebookAssignment assign = (GradebookAssignment) iter.next();
				if (assign != null && assign.isCounted() && !assign.getUngraded()) {
					if (assign.getCategory() != null && !assign.getCategory().isRemoved()) {
						filteredAssigns.add(assign.getId());
					}
				}
			}

			final List filteredRecords = new ArrayList();
			for (final Iterator iter = records.iterator(); iter.hasNext();) {
				final AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
				if (filteredAssigns.contains(agr.getAssignment().getId()) && !agr.isCourseGradeRecord()) {
					if (agr.getPointsEarned() == null) {
						return true;
					}
					filteredRecords.add(agr);
				}
			}

			return filteredRecords.size() < filteredAssigns.size() * studentUids.size();
		}
	}

    @Override
	public boolean isGradableObjectDefined(final Long gradableObjectId) {
		if (gradableObjectId == null) {
			throw new IllegalArgumentException("null gradableObjectId passed to isGradableObjectDefined");
		}

		return isAssignmentDefined(gradableObjectId);
	}

    @Override
	public Map getViewableSectionUuidToNameMap(final String gradebookUid) {
		if (gradebookUid == null) {
			throw new IllegalArgumentException("Null gradebookUid passed to getViewableSectionIdToNameMap");
		}

		final Map<String, String> sectionIdNameMap = new HashMap<>();

		final List viewableCourseSections = getViewableSections(gradebookUid);
		if (viewableCourseSections == null || viewableCourseSections.isEmpty()) {
			return sectionIdNameMap;
		}

		for (final Iterator sectionIter = viewableCourseSections.iterator(); sectionIter.hasNext();) {
			final CourseSection section = (CourseSection) sectionIter.next();
			if (section != null) {
				sectionIdNameMap.put(section.getUuid(), section.getTitle());
			}
		}

		return sectionIdNameMap;
	}

	@Override
	public boolean currentUserHasGradeAllPerm(String gradebookUid) {
		return sakaiProxy.isUserAbleToGradeAll(gradebookUid);
	}

    @Override
	public boolean isUserAllowedToGradeAll(String gradebookUid, String userUid) {
		return sakaiProxy.isUserAbleToGradeAll(gradebookUid, userUid);
	}

    @Override
	public boolean currentUserHasGradingPerm(String gradebookUid) {
		return sakaiProxy.isUserAbleToGrade(gradebookUid);
	}

    @Override
	public boolean isUserAllowedToGrade(String gradebookUid, String userUid) {
		return sakaiProxy.isUserAbleToGrade(gradebookUid, userUid);
	}

    @Override
	public boolean currentUserHasEditPerm(String gradebookUid) {
		return sakaiProxy.isUserAbleToEditAssessments(gradebookUid);
	}

	@Override
	public boolean currentUserHasViewOwnGradesPerm(String gradebookUid) {
		return sakaiProxy.isUserAbleToViewOwnGrades(gradebookUid);
	}

	@Override
	public boolean currentUserHasViewStudentNumbersPerm(String gradebookUid) {
		return sakaiProxy.isUserAbleToViewStudentNumbers(gradebookUid);
	}

    private GradebookAssignment getAssignmentWithoutStatsByID(String gradebookUid, Long assignmentId) {
        return gradebookAssignmentRepository.findOneByIdAndGradebook_UidAndRemovedIsFalse(assignmentId, gradebookUid);
    }

    @Override
	public List<GradeDefinition> getGradesForStudentsForItem(String gradebookUid, Long gradableObjectId, List<String> studentIds) {

		if (gradableObjectId == null) {
			throw new IllegalArgumentException("null gradableObjectId passed to getGradesForStudentsForItem");
		}

		final List<GradeDefinition> studentGrades = new ArrayList<>();

		if (studentIds != null && !studentIds.isEmpty()) {
			// first, we need to make sure the current user is authorized to view the
			// grades for all of the requested students
			final GradebookAssignment gbItem = getAssignmentWithoutStatsByID(gradebookUid, gradableObjectId);

			if (gbItem != null) {
				final Gradebook gradebook = gbItem.getGradebook();

				if (!sakaiProxy.isUserAbleToGrade(gradebook.getUid())) {
					log.error(
							"User {} attempted to access grade information without permission in gb {} using gradebookService.getGradesForStudentsForItem",
							sakaiProxy.getCurrentUserId(), gradebook.getUid());
					throw new GradebookSecurityException();
				}

				final Long categoryId = gbItem.getCategory() != null ? gbItem.getCategory().getId() : null;
				final Map enrRecFunctionMap = sakaiProxy.findMatchingEnrollmentsForItem(gradebook.getUid(), categoryId,
						gradebook.getCategory_type(), null, null);
				final Set enrRecs = enrRecFunctionMap.keySet();
				final Map studentIdEnrRecMap = new HashMap();
				if (enrRecs != null) {
					for (final Iterator enrIter = enrRecs.iterator(); enrIter.hasNext();) {
						final EnrollmentRecord enr = (EnrollmentRecord) enrIter.next();
						if (enr != null) {
							studentIdEnrRecMap.put(enr.getUser().getUserUid(), enr);
						}
					}
				}

				// filter the provided studentIds if user doesn't have permissions
				studentIds.removeIf(studentId -> {
					return !studentIdEnrRecMap.containsKey(studentId);
				});

				// retrieve the grading comments for all of the students
				final List<Comment> commentRecs = getComments(gbItem, studentIds);
				final Map<String, String> studentIdCommentTextMap = new HashMap<>();
				if (commentRecs != null) {
					for (final Comment comment : commentRecs) {
						if (comment != null) {
							studentIdCommentTextMap.put(comment.getStudentId(), comment.getCommentText());
						}
					}
				}

				// now, we can populate the grade information
				final List<String> studentsWithGradeRec = new ArrayList<>();
				final List<AssignmentGradeRecord> gradeRecs = getAllAssignmentGradeRecordsForGbItem(gradableObjectId, studentIds);
				if (gradeRecs != null) {
					if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_LETTER) {
						convertPointsToLetterGrade(gradebook, gradeRecs);
					} else if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_PERCENTAGE) {
						convertPointsToPercentage(gradebook, gradeRecs);
					}

					for (final Object element : gradeRecs) {
						final AssignmentGradeRecord agr = (AssignmentGradeRecord) element;
						if (agr != null) {
							final String commentText = studentIdCommentTextMap.get(agr.getStudentId());
							final GradeDefinition gradeDef = convertGradeRecordToGradeDefinition(agr, gbItem, gradebook, commentText);

							studentGrades.add(gradeDef);
							studentsWithGradeRec.add(agr.getStudentId());
						}
					}

					// if student has a comment but no grade add an empty grade definition with the comment
					if (studentsWithGradeRec.size() < studentIds.size()) {
						for (final String studentId : studentIdCommentTextMap.keySet()) {
							if (!studentsWithGradeRec.contains(studentId)) {
								final String comment = studentIdCommentTextMap.get(studentId);
								final AssignmentGradeRecord emptyGradeRecord = new AssignmentGradeRecord(gbItem, studentId, null);
								final GradeDefinition gradeDef = convertGradeRecordToGradeDefinition(emptyGradeRecord, gbItem, gradebook,
										comment);
								studentGrades.add(gradeDef);
							}
						}
					}
				}
			}
		}

		return studentGrades;
	}

    /**
	 * Converts an AssignmentGradeRecord into a GradeDefinition object.
	 *
	 * @param gradeRecord
	 * @param gbo
	 * @param gradebook
	 * @param commentText - goes into the GradeComment attribute. Will be omitted if null
	 * @return a GradeDefinition object whose attributes match the passed in gradeRecord
	 */
	private GradeDefinition convertGradeRecordToGradeDefinition(AssignmentGradeRecord gradeRecord, GradebookAssignment gbo,
			Gradebook gradebook, String commentText) {

		final GradeDefinition gradeDef = new GradeDefinition();
		gradeDef.setStudentUid(gradeRecord.getStudentId());
		gradeDef.setGraderUid(gradeRecord.getGraderId());
		gradeDef.setDateRecorded(gradeRecord.getDateRecorded());
		final int gradeEntryType = gradebook.getGrade_type();
		gradeDef.setGradeEntryType(gradeEntryType);
		String grade = null;
		if (gradeEntryType == GradingConstants.GRADE_TYPE_LETTER) {
			grade = gradeRecord.getLetterEarned();
		} else if (gradeEntryType == GradingConstants.GRADE_TYPE_PERCENTAGE) {
			final Double percentEarned = gradeRecord.getPercentEarned();
			grade = percentEarned != null ? percentEarned.toString() : null;
		} else {
			final Double pointsEarned = gradeRecord.getPointsEarned();
			grade = pointsEarned != null ? pointsEarned.toString() : null;
		}
		gradeDef.setGrade(grade);
		gradeDef.setGradeReleased(gradebook.isAssignmentsDisplayed() && gbo.isReleased());

		if (commentText != null) {
			gradeDef.setGradeComment(commentText);
		}

		gradeDef.setExcused(gradeRecord.getExcludedFromGrade());

		return gradeDef;
	}

    public List<Comment> getComments(GradebookAssignment assignment, List<String> studentIds) {

    	if (studentIds.isEmpty()) {
    		return new ArrayList<>();
    	}

        return commentRepository.findByGradableObjectAndStudentIdIn(assignment, studentIds);
    }

	@Override
	public Map<Long, List<GradeDefinition>> getGradesWithoutCommentsForStudentsForItems(final String gradebookUid,
			final List<Long> gradableObjectIds, final List<String> studentIds) {

		if (!sakaiProxy.isUserAbleToGrade(gradebookUid)) {
			throw new GradebookSecurityException();
		}

		if (gradableObjectIds == null || gradableObjectIds.isEmpty()) {
			throw new IllegalArgumentException("null or empty gradableObjectIds passed to getGradesWithoutCommentsForStudentsForItems");
		}

		final Map<Long, List<GradeDefinition>> gradesMap = new HashMap<>();
		if (studentIds == null || studentIds.isEmpty()) {
			// We could populate the map with (gboId : new ArrayList()), but it's cheaper to allow get(gboId) to return null.
			return gradesMap;
		}

		// Get all the grades for the gradableObjectIds
		final List<AssignmentGradeRecord> gradeRecords = getAllAssignmentGradeRecordsForGbItems(gradableObjectIds, studentIds);
		// AssignmentGradeRecord is not in the API. So we need to convert grade records into GradeDefinition objects.
		// GradeDefinitions are not tied to their gbos, so we need to return a map associating them back to their gbos
		final List<GradeDefinition> gradeDefinitions = new ArrayList<>();
		for (final AssignmentGradeRecord gradeRecord : gradeRecords) {
			final GradebookAssignment gbo = (GradebookAssignment) gradeRecord.getGradableObject();
			final Long gboId = gbo.getId();
			final Gradebook gradebook = gbo.getGradebook();
			if (!gradebookUid.equals(gradebook.getUid())) {
				// The user is authorized against gradebookUid, but we have grades for another gradebook.
				// This is an authorization issue caused by gradableObjectIds violating the method contract.
				throw new IllegalArgumentException("gradableObjectIds must belong to grades within this gradebook");
			}

			final GradeDefinition gradeDef = convertGradeRecordToGradeDefinition(gradeRecord, gbo, gradebook, null);

			List<GradeDefinition> gradeList = gradesMap.get(gboId);
			if (gradeList == null) {
				gradeList = new ArrayList<>();
				gradesMap.put(gboId, gradeList);
			}
			gradeList.add(gradeDef);
		}

		return gradesMap;
	}

    /**
	 * Gets all AssignmentGradeRecords on the gradableObjectIds limited to students specified by studentUids
	 */
	private List<AssignmentGradeRecord> getAllAssignmentGradeRecordsForGbItems(final List<Long> gradableObjectIds, final List studentUids) {

        final List<AssignmentGradeRecord> gradeRecords = new ArrayList<>();
        if (studentUids.isEmpty()) {
            // If there are no enrollments, no need to execute the query.
            if (log.isDebugEnabled()) {
                log.debug("No enrollments were specified. Returning an empty List of grade records");
            }
            return gradeRecords;
        }
        /*
         * Watch out for Oracle's "in" limit. Ignoring oracle, the query would be:
         * "from AssignmentGradeRecord as agr where agr.gradableObject.removed = false and agr.gradableObject.id in (:gradableObjectIds) and agr.studentId in (:studentUids)"
         * Note: the order is not important. The calling methods will iterate over all entries and add them to a map. We could have
         * made this method return a map, but we'd have to iterate over the items in order to add them to the map anyway. That would
         * be a waste of a loop that the calling method could use to perform additional tasks.
         */
        // For Oracle, iterate over gbItems 1000 at a time (sympathies to whoever needs to query grades for a thousand gbItems)
        int minGbo = 0;
        int maxGbo = Math.min(gradableObjectIds.size(), 1000);
        while (minGbo < gradableObjectIds.size()) {
            // For Oracle, iterate over students 1000 at a time
            int minStudent = 0;
            int maxStudent = Math.min(studentUids.size(), 1000);
            while (minStudent < studentUids.size()) {
                List<AssignmentGradeRecord> list
                    = assignmentGradeRecordRepository.findByGradableObject_RemovedIsFalseAndGradableObject_IdInAndStudentIdIn(gradableObjectIds.subList(minGbo, maxGbo), studentUids.subList(minStudent, maxStudent));
                // Add the query results to our overall results (in case there's over a thousand things)
                gradeRecords.addAll(list);
                minStudent += 1000;
                maxStudent = Math.min(studentUids.size(), minStudent + 1000);
            }
            minGbo += 1000;
            maxGbo = Math.min(gradableObjectIds.size(), minGbo + 1000);
        }
        return gradeRecords;
    }

    @Override
	public boolean isGradeValid(String gradebookUuid, String grade) {

		if (gradebookUuid == null) {
			throw new IllegalArgumentException("Null gradebookUuid passed to isGradeValid");
		}
		Gradebook gradebook;
		try {
			gradebook = getGradebook(gradebookUuid);
		} catch (final GradebookNotFoundException gnfe) {
			throw new GradebookNotFoundException("No gradebook exists with the given gradebookUid: " +
					gradebookUuid + "Error: " + gnfe.getMessage());
		}

		final int gradeEntryType = gradebook.getGrade_type();
		LetterGradePercentMapping mapping = null;
		if (gradeEntryType == GradingConstants.GRADE_TYPE_LETTER) {
			mapping = persistence.getLetterGradePercentMapping(gradebook);
		}

		return isGradeValid(grade, gradeEntryType, mapping);
	}

	@Override
	public boolean isValidNumericGrade(final String grade) {

		boolean gradeIsValid = false;

		try {
			final NumberFormat nbFormat = NumberFormat.getInstance(new ResourceLoader().getLocale());
			final Double gradeAsDouble = nbFormat.parse(grade).doubleValue();
			final String decSeparator = ((DecimalFormat) nbFormat).getDecimalFormatSymbols().getDecimalSeparator() + "";

			// grade must be greater than or equal to 0
			if (gradeAsDouble >= 0) {
				final String[] splitOnDecimal = grade.split("\\" + decSeparator);
				// check that there are no more than 2 decimal places
				if (splitOnDecimal == null) {
					gradeIsValid = true;

					// check for a valid score matching ##########.##
					// where integer is maximum of 10 integers in length
					// and maximum of 2 decimal places
				} else if (grade.matches("[0-9]{0,10}(\\" + decSeparator + "[0-9]{0,2})?")) {
					gradeIsValid = true;
				}
			}
		} catch (NumberFormatException | ParseException nfe) {
			log.debug("Passed grade is not a numeric value");
		}

		return gradeIsValid;
	}

	private boolean isGradeValid(final String grade, final int gradeEntryType, final LetterGradePercentMapping gradeMapping) {

		boolean gradeIsValid = false;

		if (grade == null || "".equals(grade)) {

			gradeIsValid = true;

		} else {

			if (gradeEntryType == GradingConstants.GRADE_TYPE_POINTS ||
					gradeEntryType == GradingConstants.GRADE_TYPE_PERCENTAGE) {
				try {
					final NumberFormat nbFormat = NumberFormat.getInstance(new ResourceLoader().getLocale());
					final Double gradeAsDouble = nbFormat.parse(grade).doubleValue();
					final String decSeparator = ((DecimalFormat) nbFormat).getDecimalFormatSymbols().getDecimalSeparator() + "";
					// grade must be greater than or equal to 0
					if (gradeAsDouble >= 0) {
						final String[] splitOnDecimal = grade.split("\\" + decSeparator);
						// check that there are no more than 2 decimal places
						if (splitOnDecimal == null) {
							gradeIsValid = true;

							// check for a valid score matching ##########.##
							// where integer is maximum of 10 integers in length
							// and maximum of 2 decimal places
						} else if (grade.matches("[0-9]{0,10}(\\" + decSeparator + "[0-9]{0,2})?")) {
							gradeIsValid = true;
						}
					}
				} catch (NumberFormatException | ParseException nfe) {
					log.debug("Passed grade is not a numeric value");
				}

			} else if (gradeEntryType == GradingConstants.GRADE_TYPE_LETTER) {
				if (gradeMapping == null) {
					throw new IllegalArgumentException("Null mapping passed to isGradeValid for a letter grade-based gradeook");
				}

				final String standardizedGrade = gradeMapping.standardizeInputGrade(grade);
				if (standardizedGrade != null) {
					gradeIsValid = true;
				}
			} else {
				throw new IllegalArgumentException("Invalid gradeEntryType passed to isGradeValid");
			}
		}

		return gradeIsValid;
	}

    @Override
	public List<String> identifyStudentsWithInvalidGrades(String gradebookUid, Map<String, String> studentIdToGradeMap) {

		if (gradebookUid == null) {
			throw new IllegalArgumentException("null gradebookUid passed to identifyStudentsWithInvalidGrades");
		}

		final List<String> studentsWithInvalidGrade = new ArrayList<>();

		if (studentIdToGradeMap != null) {
			Gradebook gradebook;

			try {
				gradebook = getGradebook(gradebookUid);
			} catch (GradebookNotFoundException gnfe) {
				throw new GradebookNotFoundException("No gradebook exists with the given gradebookUid: " +
						gradebookUid + "Error: " + gnfe.getMessage());
			}

			LetterGradePercentMapping gradeMapping = null;
			if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_LETTER) {
				gradeMapping = persistence.getLetterGradePercentMapping(gradebook);
			}

			for (String studentId : studentIdToGradeMap.keySet()) {
				String grade = studentIdToGradeMap.get(studentId);
				if (!isGradeValid(grade, gradebook.getGrade_type(), gradeMapping)) {
					studentsWithInvalidGrade.add(studentId);
				}
			}
		}
		return studentsWithInvalidGrade;
	}

    @Override
	public void saveGradeAndCommentForStudent(String gradebookUid, Long gradableObjectId, String studentUid,
        String grade, String comment) {

		if (gradebookUid == null || gradableObjectId == null || studentUid == null) {
			throw new IllegalArgumentException(
					"Null gradebookUid or gradableObjectId or studentUid passed to saveGradeAndCommentForStudent");
		}

		final GradeDefinition gradeDef = new GradeDefinition();
		gradeDef.setStudentUid(studentUid);
		gradeDef.setGrade(grade);
		gradeDef.setGradeComment(comment);

		final List<GradeDefinition> gradeDefList = new ArrayList<>();
		gradeDefList.add(gradeDef);

        GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, gradableObjectId);

		AssignmentGradeRecord record = persistence.getAssignmentGradeRecord(assignment, studentUid);
		if (record != null) {
			gradeDef.setExcused(BooleanUtils.toBoolean(record.getExcludedFromGrade()));
		} else {
			gradeDef.setExcused(false);
		}
		saveGradesAndComments(gradebookUid, gradableObjectId, gradeDefList);
	}

    @Override
	public void saveGradesAndComments(final String gradebookUid, final Long gradableObjectId, final List<GradeDefinition> gradeDefList) {

		if (gradebookUid == null || gradableObjectId == null) {
			throw new IllegalArgumentException("Null gradebookUid or gradableObjectId passed to saveGradesAndComments");
		}

		if (CollectionUtils.isNotEmpty(gradeDefList)) {
			Gradebook gradebook;

			try {
				gradebook = getGradebook(gradebookUid);
			} catch (GradebookNotFoundException gnfe) {
				throw new GradebookNotFoundException("No gradebook exists with the given gradebookUid: " +
						gradebookUid + "Error: " + gnfe.getMessage());
			}

			final GradebookAssignment assignment = getAssignmentWithoutStatsByID(gradebookUid, gradableObjectId);
			if (assignment == null) {
				throw new AssessmentNotFoundException("No gradebook item exists with gradable object id = " + gradableObjectId);
			}

			if (!currentUserHasGradingPerm(gradebookUid)) {
				log.warn("User attempted to save grades and comments without authorization");
				throw new GradebookSecurityException("User attempted to save grades and comments without authorization");
			}

			// identify all of the students being updated first
			final Map<String, GradeDefinition> studentIdGradeDefMap = new HashMap<>();
			final Map<String, String> studentIdToGradeMap = new HashMap<>();

			for (GradeDefinition gradeDef : gradeDefList) {
				studentIdGradeDefMap.put(gradeDef.getStudentUid(), gradeDef);
				studentIdToGradeMap.put(gradeDef.getStudentUid(), gradeDef.getGrade());
			}

			// Check for invalid grades
			final List<String> invalidStudentUUIDs = identifyStudentsWithInvalidGrades(gradebookUid, studentIdToGradeMap);
			if (CollectionUtils.isNotEmpty(invalidStudentUUIDs)) {
				throw new InvalidGradeException(
						"At least one grade passed to be updated is " + "invalid. No grades or comments were updated.");
			}

			// Retrieve all existing grade records for the given students and assignment
			final List<AssignmentGradeRecord> existingGradeRecords = getAllAssignmentGradeRecordsForGbItem(gradableObjectId,
					new ArrayList<String>(studentIdGradeDefMap.keySet()));
			final Map<String, AssignmentGradeRecord> studentIdGradeRecordMap = new HashMap<>();
			if (CollectionUtils.isNotEmpty(existingGradeRecords)) {
				for (final AssignmentGradeRecord agr : existingGradeRecords) {
					studentIdGradeRecordMap.put(agr.getStudentId(), agr);
				}
			}

			// Retrieve all existing comments for the given students and assignment
			final List<Comment> existingComments = getComments(assignment, new ArrayList<String>(studentIdGradeDefMap.keySet()));
			final Map<String, Comment> studentIdCommentMap = new HashMap<>();
			if (CollectionUtils.isNotEmpty(existingComments)) {
				for (final Comment comment : existingComments) {
					studentIdCommentMap.put(comment.getStudentId(), comment);
				}
			}

			final boolean userHasGradeAllPerm = currentUserHasGradeAllPerm(gradebookUid);
			final String graderId = sakaiProxy.getCurrentUserId();
			final Date now = new Date();
			LetterGradePercentMapping mapping = null;
			if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_LETTER) {
				mapping = persistence.getLetterGradePercentMapping(gradebook);
			}

			// Don't use a HashSet because you may have multiple Comments with null ID and the same comment at this point.
			// The Comment object defines objects as equal if they have the same ID, comment text, and gradebook item. The
			// only difference may be the student IDs
			final List<Comment> commentsToUpdate = new ArrayList<>();
			final Set<GradingEvent> eventsToAdd = new HashSet<>();
			final Set<AssignmentGradeRecord> gradeRecordsToUpdate = new HashSet<>();
			for (final GradeDefinition gradeDef : gradeDefList) {
				final String studentId = gradeDef.getStudentUid();

				// use the grader ID from the definition if it is not null, otherwise use the current user ID
				final String graderUid = gradeDef.getGraderUid() != null ? gradeDef.getGraderUid() : graderId;
				// use the grade date from the definition if it is not null, otherwise use the current date
				final Date gradedDate = gradeDef.getDateRecorded() != null ? gradeDef.getDateRecorded() : now;

				final boolean excuse = gradeDef.isExcused();

				// check specific grading privileges if user does not have
				// grade all perm
				if (!userHasGradeAllPerm) {
					if (!isUserAbleToGradeItemForStudent(gradebookUid, gradableObjectId, studentId)) {
						log.warn("User {} attempted to save a grade for {} without authorization", graderId, studentId);
						throw new GradebookSecurityException();
					}
				}
				// Determine if the AssignmentGradeRecord needs to be updated
				final String newGrade = StringUtils.trimToEmpty(gradeDef.getGrade());
				final Double convertedGrade = convertInputGradeToPoints(gradebook.getGrade_type(), mapping, assignment.getPointsPossible(),
						newGrade);
				AssignmentGradeRecord gradeRec = studentIdGradeRecordMap.get(studentId);
				boolean currentExcuse;
				if (gradeRec == null) {
					currentExcuse = false;
				} else {
					currentExcuse = BooleanUtils.toBoolean(gradeRec.getExcludedFromGrade());
				}

				if (gradeRec != null) {
					final Double pointsEarned = gradeRec.getPointsEarned();
					if ((convertedGrade == null && pointsEarned != null)
							|| (convertedGrade != null && pointsEarned == null)
							|| (convertedGrade != null && pointsEarned != null && !convertedGrade.equals(pointsEarned))
							|| (excuse != currentExcuse)) {

						gradeRec.setPointsEarned(convertedGrade);
						gradeRec.setGraderId(graderUid);
						gradeRec.setDateRecorded(gradedDate);
						gradeRec.setExcludedFromGrade(excuse);
						gradeRecordsToUpdate.add(gradeRec);

						// Add a GradingEvent, which stores the actual input grade rather than the converted one
						eventsToAdd.add(new GradingEvent(assignment, graderId, studentId, newGrade));
					}
				} else {
					// if the grade is something other than null, add a new AGR
					if (StringUtils.isNotBlank(newGrade) && (StringUtils.isNotBlank(gradeDef.getGrade()) || excuse != currentExcuse)) {
						gradeRec = new AssignmentGradeRecord(assignment, studentId, convertedGrade);
						gradeRec.setGraderId(graderUid);
						gradeRec.setDateRecorded(gradedDate);
						gradeRecordsToUpdate.add(gradeRec);
						gradeRec.setExcludedFromGrade(excuse);

						// Add a GradingEvent, which stores the actual input grade rather than the converted one
						GradingEvent event = new GradingEvent(assignment, graderId, studentId, newGrade);
						eventsToAdd.add(event);
					}
				}
				// Determine if the Comment needs to be updated
				Comment comment = studentIdCommentMap.get(studentId);
				final String newCommentText = StringUtils.trimToEmpty(gradeDef.getGradeComment());
				if (comment != null) {
					final String existingCommentText = StringUtils.trimToEmpty(comment.getCommentText());
					final boolean existingCommentTextIsEmpty = existingCommentText.isEmpty();
					final boolean newCommentTextIsEmpty = newCommentText.isEmpty();
					if ((existingCommentTextIsEmpty && !newCommentTextIsEmpty)
							|| (!existingCommentTextIsEmpty && newCommentTextIsEmpty)
							|| (!existingCommentTextIsEmpty && !newCommentTextIsEmpty && !newCommentText.equals(existingCommentText))) {
						comment.setCommentText(newCommentText);
						comment.setGraderId(graderId);
						comment.setDateRecorded(gradedDate);
						commentsToUpdate.add(comment);
					}
				} else {
					// If the comment is something other than null, add a new Comment
					if (!newCommentText.isEmpty()) {
						comment = new Comment(studentId, newCommentText, assignment);
						comment.setGraderId(graderId);
						comment.setDateRecorded(gradedDate);
						commentsToUpdate.add(comment);
					}
				}
			}

			// Save or update the necessary items
			try {
                gradeRecordsToUpdate.forEach(gr -> assignmentGradeRecordRepository.save(gr));
                commentsToUpdate.forEach(c -> commentRepository.save(c));
                eventsToAdd.forEach(e -> gradingEventRepository.save(e));
			} catch (Exception  e) {
                e.printStackTrace();
                log.error("An error occurred while attempting to save scores and comments for gb Item " + gradableObjectId, e);
			}
		}
	}

    /**
	 *
	 * @param gradeEntryType
	 * @param mapping
	 * @param gbItemPointsPossible
	 * @param grade
	 * @return given a generic String grade, converts it to the equivalent Double point value that will be stored in the db based upon the
	 *         gradebook's grade entry type
	 */
	private Double convertInputGradeToPoints(final int gradeEntryType, final LetterGradePercentMapping mapping,
			final Double gbItemPointsPossible, final String grade) throws InvalidGradeException {

		Double convertedValue = null;

		if (grade != null && !"".equals(grade)) {
			if (gradeEntryType == GradingConstants.GRADE_TYPE_POINTS) {
				try {
					final NumberFormat nbFormat = NumberFormat.getInstance(new ResourceLoader().getLocale());
					final Double pointValue = nbFormat.parse(grade).doubleValue();
					convertedValue = pointValue;
				} catch (NumberFormatException | ParseException nfe) {
					throw new InvalidGradeException("Invalid grade passed to convertInputGradeToPoints");
				}
			} else if (gradeEntryType == GradingConstants.GRADE_TYPE_PERCENTAGE ||
					gradeEntryType == GradingConstants.GRADE_TYPE_LETTER) {

				// for letter or %-based grading, we need to calculate the equivalent point value
				if (gbItemPointsPossible == null) {
					throw new IllegalArgumentException("Null points possible passed" +
							" to convertInputGradeToPoints for letter or % based grading");
				}

				Double percentage = null;
				if (gradeEntryType == GradingConstants.GRADE_TYPE_LETTER) {
					if (mapping == null) {
						throw new IllegalArgumentException("No mapping passed to convertInputGradeToPoints for a letter-based gb");
					}

					if (mapping.getGradeMap() != null) {
						// standardize the grade mapping
						final String standardizedGrade = mapping.standardizeInputGrade(grade);
						percentage = mapping.getValue(standardizedGrade);
						if (percentage == null) {
							throw new IllegalArgumentException("Invalid grade passed to convertInputGradeToPoints");
						}
					}
				} else {
					try {
						final NumberFormat nbFormat = NumberFormat.getInstance(new ResourceLoader().getLocale());
						percentage = nbFormat.parse(grade).doubleValue();
					} catch (NumberFormatException | ParseException nfe) {
						throw new IllegalArgumentException("Invalid % grade passed to convertInputGradeToPoints");
					}
				}

				convertedValue = calculateEquivalentPointValueForPercent(gbItemPointsPossible, percentage);

			} else {
				throw new InvalidGradeException("invalid grade entry type passed to convertInputGradeToPoints");
			}
		}

		return convertedValue;
	}

    @Override
	public void saveGradeAndExcuseForStudent(String gradebookUid, Long gradableObjectId, String studentUid,
			String grade, boolean excuse) {

		if (gradebookUid == null || gradableObjectId == null || studentUid == null) {
			throw new IllegalArgumentException(
					"Null gradebookUid, gradeableObjectId, or studentUid passed to saveGradeAndExcuseForStudent");
		}

		GradeDefinition gradeDef = new GradeDefinition();
		gradeDef.setStudentUid(studentUid);
		gradeDef.setGrade(grade);
		gradeDef.setExcused(excuse);

		List<GradeDefinition> gradeDefList = new ArrayList<>();
		gradeDefList.add(gradeDef);

		saveGradesAndComments(gradebookUid, gradableObjectId, gradeDefList);
	}

    @Override
	public int getGradeEntryType(String gradebookUid) {

		if (gradebookUid == null) {
			throw new IllegalArgumentException("null gradebookUid passed to getGradeEntryType");
		}

		try {
			Gradebook gradebook = getGradebook(gradebookUid);
			return gradebook.getGrade_type();
		} catch (GradebookNotFoundException gnfe) {
			throw new GradebookNotFoundException("No gradebook exists with the given gradebookUid: " + gradebookUid);
		}
	}

    @Override
	public Map getEnteredCourseGrade(final String gradebookUid) {

        final Gradebook thisGradebook = getGradebook(gradebookUid);

        final Long gradebookId = thisGradebook.getId();
        final CourseGrade courseGrade = courseGradeRepository.findOneByGradebook_Id(gradebookId);

        Map enrollmentMap;

        final Map viewableEnrollmentsMap
            = sakaiProxy.findMatchingEnrollmentsForViewableCourseGrade(
                gradebookUid, thisGradebook.getCategory_type(), null, null);

        enrollmentMap = new HashMap();

        final Map enrollmentMapUid = new HashMap();
        for (final Iterator iter = viewableEnrollmentsMap.keySet().iterator(); iter.hasNext();) {
            final EnrollmentRecord enr = (EnrollmentRecord) iter.next();
            enrollmentMap.put(enr.getUser().getUserUid(), enr);
            enrollmentMapUid.put(enr.getUser().getUserUid(), enr);
        }

        List<CourseGradeRecord> unfiltered = courseGradeRecordRepository.findByGradableObject_Id(courseGrade.getId());
        final List records = filterAndPopulateCourseGradeRecordsByStudents(courseGrade, unfiltered, enrollmentMap.keySet());

        final Map returnMap = new HashMap();

        for (int i = 0; i < records.size(); i++) {
            final CourseGradeRecord cgr = (CourseGradeRecord) records.get(i);
            if (cgr.getEnteredGrade() != null && !cgr.getEnteredGrade().equalsIgnoreCase("")) {
                final EnrollmentRecord enr = (EnrollmentRecord) enrollmentMapUid.get(cgr.getStudentId());
                if (enr != null) {
                    returnMap.put(enr.getUser().getDisplayId(), cgr.getEnteredGrade());
                }
            }
        }

        return returnMap;
	}

	@Override
	public String getAssignmentScoreString(final String gradebookUid, final Long assignmentId, final String studentUid)
			throws GradebookNotFoundException, AssessmentNotFoundException {

		final boolean studentRequestingOwnScore = sakaiProxy.getCurrentUserId().equals(studentUid);

		if (gradebookUid == null || assignmentId == null || studentUid == null) {
			throw new IllegalArgumentException("null parameter passed to getAssignment. Values are gradebookUid:"
					+ gradebookUid + " assignmentId:" + assignmentId + " studentUid:" + studentUid);
		}

        final GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);
        if (assignment == null) {
            throw new AssessmentNotFoundException(
                    "There is no assignment with id " + assignmentId + " in gradebook " + gradebookUid);
        }

        if (!studentRequestingOwnScore && !isUserAbleToViewItemForStudent(gradebookUid, assignmentId, studentUid)) {
            log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to retrieve grade for student {} for assignment {}",
                    sakaiProxy.getCurrentUserId(), gradebookUid, studentUid, assignment.getName());
            throw new GradebookSecurityException();
        }

        // If this is the student, then the assignment needs to have
        // been released.
        if (studentRequestingOwnScore && !assignment.isReleased()) {
            log.error("AUTHORIZATION FAILURE: Student {} in gradebook {} attempted to retrieve score for unreleased assignment {}",
                    sakaiProxy.getCurrentUserId(), gradebookUid, assignment.getName());
            throw new GradebookSecurityException();
        }

        final AssignmentGradeRecord gradeRecord = persistence.getAssignmentGradeRecord(assignment, studentUid);
        log.debug("gradeRecord={}", gradeRecord);
        Double assignmentScore = gradeRecord == null ? null : gradeRecord.getPointsEarned();

        log.debug("returning {}", assignmentScore);

		// TODO: when ungraded items is considered, change column to ungraded-grade
		// its possible that the assignment score is null
		if (assignmentScore == null) {
			return null;
		}

		// avoid scientific notation on large scores by using a formatter
		final NumberFormat numberFormat = NumberFormat.getInstance(new ResourceLoader().getLocale());
		final DecimalFormat df = (DecimalFormat) numberFormat;
		df.setGroupingUsed(false);

		return df.format(assignmentScore);
	}

    @Override
	public String getAssignmentScoreString(String gradebookUid, String assignmentName, String studentUid)
			throws GradebookNotFoundException, AssessmentNotFoundException {

		if (gradebookUid == null || assignmentName == null || studentUid == null) {
			throw new IllegalArgumentException("null parameter passed to getAssignment. Values are gradebookUid:"
					+ gradebookUid + " assignmentName:" + assignmentName + " studentUid:" + studentUid);
		}

		final GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentName);

		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assignment with name " + assignmentName + " in gradebook " + gradebookUid);
		}

		return getAssignmentScoreString(gradebookUid, assignment.getId(), studentUid);
	}

	@Override
	public String getAssignmentScoreStringByNameOrId(String gradebookUid, String assignmentName, String studentUid)
			throws GradebookNotFoundException, AssessmentNotFoundException {

		String score = null;
		try {
			score = getAssignmentScoreString(gradebookUid, assignmentName, studentUid);
		} catch (AssessmentNotFoundException e) {
			// Don't fail on this exception
			log.debug("Assessment not found by name", e);
		} catch (GradebookSecurityException gse) {
			log.warn("User {} does not have permission to retrieve score for assignment {}", studentUid, assignmentName, gse);
			return null;
		}

		if (score == null) {
			// Try to get the assignment by id
			if (NumberUtils.isCreatable(assignmentName)) {
				final Long assignmentId = NumberUtils.toLong(assignmentName, -1L);
				try {
					score = getAssignmentScoreString(gradebookUid, assignmentId, studentUid);
				} catch (AssessmentNotFoundException anfe) {
					log.debug("Assessment could not be found for gradebook id {} and assignment id {} and student id {}", gradebookUid, assignmentName, studentUid);
				}
			}
		}
		return score;
	}

	@Override
	public void setAssignmentScoreString(String gradebookUid, Long assignmentId, String studentUid, String score,
			String clientServiceDescription)
			throws GradebookNotFoundException, AssessmentNotFoundException {

        final GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentId);
        if (assignment == null) {
            throw new AssessmentNotFoundException(
                    "There is no assignment with id " + assignmentId + " in gradebook " + gradebookUid);
        }
        if (assignment.isExternallyMaintained()) {
            log.error(
                    "AUTHORIZATION FAILURE: User {} in gradebook {} attempted to grade externally maintained assignment {} from {}",
                    sakaiProxy.getCurrentUserId(), gradebookUid, assignmentId, clientServiceDescription);
            throw new GradebookSecurityException();
        }

        if (!isUserAbleToGradeItemForStudent(gradebookUid, assignment.getId(), studentUid)) {
            log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to grade student {} from {} for item {}",
                    sakaiProxy.getCurrentUserId(), gradebookUid, studentUid, clientServiceDescription, assignmentId);
            throw new GradebookSecurityException();
        }

        final Date now = new Date();
        final String graderId = sakaiProxy.getCurrentUserId();
        AssignmentGradeRecord gradeRecord = persistence.getAssignmentGradeRecord(assignment, studentUid);
        if (gradeRecord == null) {
            // Creating a new grade record.
            gradeRecord = new AssignmentGradeRecord(assignment, studentUid, convertStringToDouble(score));
            // TODO: test if it's ungraded item or not. if yes, set ungraded grade for this record. if not, need validation??
        } else {
            // TODO: test if it's ungraded item or not. if yes, set ungraded grade for this record. if not, need validation??
            gradeRecord.setPointsEarned(convertStringToDouble(score));
        }
        gradeRecord.setGraderId(graderId);
        gradeRecord.setDateRecorded(now);
        assignmentGradeRecordRepository.save(gradeRecord);

        gradingEventRepository.save(new GradingEvent(assignment, graderId, studentUid, score));

        // Post an event in SAKAI_EVENT table
        postUpdateGradeEvent(gradebookUid, assignment.getName(), studentUid, convertStringToDouble(score));

		if (log.isDebugEnabled()) {
			log.debug("Score updated in gradebookUid=" + gradebookUid + ", assignmentId=" + assignmentId + " by userUid=" + sakaiProxy.getCurrentUserId()
					+ " from client=" + clientServiceDescription + ", new score=" + score);
		}
	}

	@Override
	public void setAssignmentScoreString(final String gradebookUid, final String assignmentName, final String studentUid,
			final String score, final String clientServiceDescription)
			throws GradebookNotFoundException, AssessmentNotFoundException {

		GradebookAssignment assignment = persistence.getAssignmentWithoutStats(gradebookUid, assignmentName);

		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assignment with name " + assignmentName + " in gradebook " + gradebookUid);
		}

		setAssignmentScoreString(gradebookUid, assignment.getId(), studentUid, score, clientServiceDescription);
	}

    /**
	 * Post an event to Sakai's event table
	 *
	 * @param gradebookUid
	 * @param assignmentName
	 * @param studentUid
	 * @param pointsEarned
	 * @return
	 */
	private void postUpdateGradeEvent(String gradebookUid, String assignmentName, String studentUid, Double pointsEarned) {
		sakaiProxy.postEvent("gradebook.updateItemScore",
				"/gradebook/" + gradebookUid + "/" + assignmentName + "/" + studentUid + "/" + pointsEarned + "/student");
	}

    /**
	 *
	 * @param doubleAsString
	 * @return a locale-aware Double value representation of the given String
	 * @throws ParseException
	 */
	private Double convertStringToDouble(String doubleAsString) {

		Double scoreAsDouble = null;
		if (doubleAsString != null && !"".equals(doubleAsString)) {
			try {
				NumberFormat numberFormat = NumberFormat.getInstance(new ResourceLoader().getLocale());
				Number numericScore = numberFormat.parse(doubleAsString.trim());
				scoreAsDouble = numericScore.doubleValue();
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		}

		return scoreAsDouble;
	}

    private Map<String, Set<GradebookAssignment>> getVisibleExternalAssignments(final Gradebook gradebook, final Collection<String> studentIds, final List<GradebookAssignment> assignments) {

        final String gradebookUid = gradebook.getUid();
        //final Map<String, GradebookAssignment> allRequested = new HashMap<String, GradebookAssignment>();
        final Map<String, GradebookAssignment> allRequested
            = assignments.stream().collect(Collectors.toMap(GradebookAssignment::getExternalId, Function.identity()));

        /*
        for (GradebookAssignment a : assignments) {
            if (a.isExternallyMaintained()) {
                allRequested.put(a.getExternalId(), a);
            }
        }
        */

        final Map<String, List<String>> allExternals = getVisibleExternalAssignments(gradebookUid, studentIds);

        final Map<String, Set<GradebookAssignment>> visible = new HashMap<String, Set<GradebookAssignment>>();
        for (final String studentId : allExternals.keySet()) {
            if (studentIds.contains(studentId)) {
                final Set<GradebookAssignment> studentAssignments = new HashSet<GradebookAssignment>();
                for (final String assignmentId : allExternals.get(studentId)) {
                    if (allRequested.containsKey(assignmentId)) {
                        studentAssignments.add(allRequested.get(assignmentId));
                    }
                }
                visible.put(studentId, studentAssignments);
            }
        }
        return visible;
    }



    private void finalizeNullGradeRecords(final Gradebook gradebook) {

    	final Set<String> studentUids = sakaiProxy.getAllStudentUids(gradebook.getUid());
		final Date now = new Date();
		final String graderId = sakaiProxy.getCurrentUserId();

        final List<GradebookAssignment> countedAssignments
            = gradebookAssignmentRepository
                .findByGradebook_IdAndRemovedIsFalseAndNotCountedIsFalseAndUngradedIsFalse(gradebook.getId());

        final Map<String, Set<GradebookAssignment>> visible = getVisibleExternalAssignments(gradebook, studentUids, countedAssignments);

        for (final GradebookAssignment assignment : countedAssignments) {
            final List<AssignmentGradeRecord> scoredGradeRecords = assignmentGradeRecordRepository.findByGradableObject_Id(gradebook.getId());
            /*
            final List<AssignmentGradeRecord> scoredGradeRecords = session
                    .createQuery("from AssignmentGradeRecord as agr where agr.gradableObject.id = :go")
                    .setLong("go", assignment.getId())
                    .list();
            */

            final Map<String, AssignmentGradeRecord> studentToGradeRecordMap = new HashMap<>();
            for (final AssignmentGradeRecord scoredGradeRecord : scoredGradeRecords) {
                studentToGradeRecordMap.put(scoredGradeRecord.getStudentId(), scoredGradeRecord);
            }

            for (String studentUid : studentUids) {
                // SAK-11485 - We don't want to add scores for those grouped activities
                //             that this student should not see or be scored on.
                if (assignment.isExternallyMaintained() && (!visible.containsKey(studentUid) || !visible.get(studentUid).contains(assignment))) {
                    continue;
                }
                AssignmentGradeRecord gradeRecord = studentToGradeRecordMap.get(studentUid);
                if (gradeRecord != null) {
                    if (gradeRecord.getPointsEarned() == null) {
                        gradeRecord.setPointsEarned(0d);
                    } else {
                        continue;
                    }
                } else {
                    gradeRecord = new AssignmentGradeRecord(assignment, studentUid, 0d);
                }
                gradeRecord.setGraderId(graderId);
                gradeRecord.setDateRecorded(now);
                assignmentGradeRecordRepository.save(gradeRecord);
                gradingEventRepository.save(new GradingEvent(assignment, graderId, studentUid, gradeRecord.getPointsEarned()));
            }
        }
    }

    @Override
	public void finalizeGrades(String gradebookUid)
			throws GradebookNotFoundException {

		if (!sakaiProxy.isUserAbleToGradeAll(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to finalize grades", sakaiProxy.getCurrentUserId(), gradebookUid);
			throw new GradebookSecurityException();
		}
		finalizeNullGradeRecords(getGradebook(gradebookUid));
	}

	@Override
	public String getLowestPossibleGradeForGbItem(final String gradebookUid, final Long gradebookItemId) {

		if (gradebookUid == null || gradebookItemId == null) {
			throw new IllegalArgumentException("Null gradebookUid and/or gradebookItemId " +
					"passed to getLowestPossibleGradeForGbItem. gradebookUid:" +
					gradebookUid + " gradebookItemId:" + gradebookItemId);
		}

		final GradebookAssignment gbItem = getAssignmentWithoutStatsByID(gradebookUid, gradebookItemId);

		if (gbItem == null) {
			throw new AssessmentNotFoundException("No gradebook item found with id " + gradebookItemId);
		}

		final Gradebook gradebook = gbItem.getGradebook();

		// double check that user has some permission to access gb items in this site
		if (!isUserAbleToViewAssignments(gradebookUid) && !currentUserHasViewOwnGradesPerm(gradebookUid)) {
			throw new GradebookSecurityException();
		}

		String lowestPossibleGrade = null;

		if (gbItem.getUngraded()) {
			lowestPossibleGrade = null;
		} else if (gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_PERCENTAGE ||
				gradebook.getGrade_type() == GradingConstants.GRADE_TYPE_POINTS) {
			lowestPossibleGrade = "0";
		} else if (gbItem.getGradebook().getGrade_type() == GradingConstants.GRADE_TYPE_LETTER) {
			final LetterGradePercentMapping mapping = persistence.getLetterGradePercentMapping(gradebook);
			lowestPossibleGrade = mapping.getGrade(0d);
		}

		return lowestPossibleGrade;
	}

    public ConcurrentMap<String, ExternalAssignmentProvider> getExternalAssignmentProviders() {

		if (this.externalProviders == null) {
			this.externalProviders = new ConcurrentHashMap<>(0);
		}
		return this.externalProviders;
	}

    private Set<String> getProvidedExternalAssignments(final String gradebookUid) {

		final Set<String> allAssignments = new HashSet<>();
		for (final ExternalAssignmentProvider provider : getExternalAssignmentProviders().values()) {
			// TODO: This is a temporary cast; if this method proves to be the right fit
			// and perform well enough, it will be moved to the regular interface.
			if (provider instanceof ExternalAssignmentProviderCompat) {
				allAssignments.addAll(
						((ExternalAssignmentProviderCompat) provider).getAllExternalAssignments(gradebookUid));
			} else if (this.providerMethods.containsKey(provider)) {
				final Method m = this.providerMethods.get(provider);
				try {
					@SuppressWarnings("unchecked")
					final List<String> reflectedAssignments = (List<String>) m.invoke(provider, gradebookUid);
					allAssignments.addAll(reflectedAssignments);
				} catch (final Exception e) {
					log.debug("Exception calling getAllExternalAssignments", e);
				}
			}
		}
		return allAssignments;
	}

    @Override
	public Map<String, List<String>> getVisibleExternalAssignments(final String gradebookUid, final Collection<String> studentIds)
			throws GradebookNotFoundException {

		final Set<String> providedAssignments = getProvidedExternalAssignments(gradebookUid);

		final Map<String, Set<String>> visible = new HashMap<>();
		for (final String studentId : studentIds) {
			visible.put(studentId, new HashSet<String>());
		}

		for (final ExternalAssignmentProvider provider : getExternalAssignmentProviders().values()) {
			// SAK-24407 - Some tools modify this set so we can't pass it. I considered making it an unmodifableCollection but that would
			// require changing a number of tools
			final Set<String> studentIdsCopy = new HashSet<>(studentIds);
			final Map<String, List<String>> externals = provider.getAllExternalAssignments(gradebookUid, (studentIdsCopy));
			for (final String studentId : externals.keySet()) {
				if (visible.containsKey(studentId)) {
					visible.get(studentId).addAll(externals.get(studentId));
				}
			}
		}

		// SAK-23733 - This covers a tricky case where items that the gradebook thinks are external
		// but are not reported by any provider should be included for everyone. This is
		// to accommodate tools that use the external assessment mechanisms but have not
		// implemented an ExternalAssignmentProvider.
		final List<Assignment> allAssignments = getViewableAssignmentsForCurrentUser(gradebookUid);
		for (Assignment assignment : allAssignments) {
			final String id = assignment.getExternalId();
			if (assignment.isExternallyMaintained() && !providedAssignments.contains(id)) {
				for (final String studentId : visible.keySet()) {
					visible.get(studentId).add(id);
				}
			}
		}

		final Map<String, List<String>> visibleList = new HashMap<>();
		for (final String studentId : visible.keySet()) {
			visibleList.put(studentId, new ArrayList<String>(visible.get(studentId)));
		}
		return visibleList;
	}

    @Override
	public PointsPossibleValidation isPointsPossibleValid(String gradebookUid, Assignment gradebookItem, Double pointsPossible) {

		if (gradebookUid == null) {
			throw new IllegalArgumentException("Null gradebookUid passed to isPointsPossibleValid");
		}
		if (gradebookItem == null) {
			throw new IllegalArgumentException("Null gradebookItem passed to isPointsPossibleValid");
		}

		// At this time, all gradebook items follow the same business rules for
		// points possible (aka relative weight in % gradebooks) so special logic
		// using the properties of the gradebook item is unnecessary.
		// In the future, we will have the flexibility to change
		// that behavior without changing the method signature

		// the points possible must be a non-null value greater than 0 with
		// no more than 2 decimal places

		if (pointsPossible == null) {
			return PointsPossibleValidation.INVALID_NULL_VALUE;
		}

		if (pointsPossible <= 0) {
			return PointsPossibleValidation.INVALID_NUMERIC_VALUE;
		}
		// ensure there are no more than 2 decimal places
		BigDecimal bd = new BigDecimal(pointsPossible);
		bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP); // Two decimal places
		final double roundedVal = bd.doubleValue();
		final double diff = pointsPossible - roundedVal;
		if (diff != 0) {
			return PointsPossibleValidation.INVALID_DECIMAL;
		}

		return PointsPossibleValidation.VALID;
	}

    /**
	 * Retrieves the calculated average course grade.
	 */
	@Override
	public String getAverageCourseGrade(final String gradebookUid) {

		if (gradebookUid == null) {
			throw new IllegalArgumentException("Null gradebookUid passed to getAverageCourseGrade");
		}
		// Check user has permission to invoke method.
		if (!currentUserHasGradeAllPerm(gradebookUid)) {
			final StringBuilder sb = new StringBuilder()
					.append("User ")
					.append(sakaiProxy.getCurrentUserId())
					.append(" attempted to access the average course grade without permission in gb ")
					.append(gradebookUid)
					.append(" using gradebookService.getAverageCourseGrade");
			throw new GradebookSecurityException(sb.toString());
		}

		String courseGradeLetter = null;
		final Gradebook gradebook = getGradebook(gradebookUid);
		if (gradebook != null) {
			final CourseGrade courseGrade = courseGradeRepository.findOneByGradebook_Id(gradebook.getId());
		    final Set<String> studentUids = sakaiProxy.getAllStudentUids(gradebookUid);
			// This call handles the complex rules of which assignments and grades to include in the calculation
			final List<CourseGradeRecord> courseGradeRecs = getPointsEarnedCourseGradeRecords(courseGrade, studentUids);
			if (courseGrade != null) {
				// Calculate the course mean grade whether the student grade was manually entered or auto-calculated.
				courseGrade.calculateStatistics(courseGradeRecs, studentUids.size());
				if (courseGrade.getMean() != null) {
					courseGradeLetter = gradebook.getSelectedGradeMapping().getMappedGrade(courseGrade.getMean());
				}
			}
		}
		return courseGradeLetter;
	}

    /**
	 * Updates the order of an assignment
	 *
	 * @see GradebookService.updateAssignmentOrder(java.lang.String gradebookUid, java.lang.Long assignmentId, java.lang.Integer order)
	 */
	@Override
	public void updateAssignmentOrder(final String gradebookUid, final Long assignmentId, Integer order) {

		if (!sakaiProxy.isUserAbleToEditAssessments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to change the order of assignment {}", sakaiProxy.getCurrentUserId(),
					gradebookUid, assignmentId);
			throw new GradebookSecurityException();
		}

		if (order == null) {
			throw new IllegalArgumentException("Order cannot be null");
		}

		final Long gradebookId = getGradebook(gradebookUid).getId();

		// get all assignments for this gradebook
		final List<GradebookAssignment> assignments = getAssignments(gradebookId, SortType.SORT_BY_SORTING, true);

		// adjust order to be within bounds
		if (order < 0) {
			order = 0;
		} else if (order > assignments.size()) {
			order = assignments.size();
		}

		// find the assignment
		GradebookAssignment target = null;
		for (final GradebookAssignment a : assignments) {
			if (a.getId().equals(assignmentId)) {
				target = a;
				break;
			}
		}

		// add the assignment to the list via a 'pad, remove, add' approach
		assignments.add(null); // ensure size remains the same for the remove
		assignments.remove(target); // remove item
		assignments.add(order, target); // add at ordered position, will shuffle others along

		// the assignments are now in the correct order within the list, we just need to update the sort order for each one
		// create a new list for the assignments we need to update in the database
		final List<GradebookAssignment> assignmentsToUpdate = new ArrayList<>();

		int i = 0;
		for (final GradebookAssignment a : assignments) {

			// skip if null
			if (a == null) {
				continue;
			}

			// if the sort order is not the same as the counter, update the order and add to the other list
			// this allows us to skip items that have not had their position changed and saves some db work later on
			// sort order may be null if never previously sorted, so give it the current index
			if (a.getSortOrder() == null || !a.getSortOrder().equals(i)) {
				a.setSortOrder(i);
				assignmentsToUpdate.add(a);
			}

			i++;
		}

		// do the updates
		for (final GradebookAssignment assignmentToUpdate : assignmentsToUpdate) {
            updateAssignment(assignmentToUpdate);
		}
	}

    /**
	 * {@inheritDoc}
	 */
	@Override
	public List<GradingEvent> getGradingEvents(final String studentId, final long assignmentId) {

		if (log.isDebugEnabled()) {
			log.debug("getGradingEvents called for studentId:" + studentId);
		}

		List<GradingEvent> rval = new ArrayList<>();

		if (studentId == null) {
			log.debug("No student id was specified.  Returning an empty GradingEvents object");
			return rval;
		}

        return gradingEventRepository.findByStudentIdAndGradableObject_Id(studentId, assignmentId);
	}

    @Override
	public Optional<CategoryScoreData> calculateCategoryScore(final Object gradebook, final String studentUuid,
			final CategoryDefinition category, final List<Assignment> categoryAssignments,
			final Map<Long, String> gradeMap, final boolean includeNonReleasedItems) {

		final Gradebook gb = (Gradebook) gradebook;

		// used for translating letter grades
		final Map<String, Double> gradingSchema = gb.getSelectedGradeMapping().getGradeMap();

		// collect the data and turn it into a list of AssignmentGradeRecords
		// this is the info that is compatible with both applyDropScores and the calculateCategoryScore method
		final List<AssignmentGradeRecord> gradeRecords = new ArrayList<>();
		for (Assignment assignment : categoryAssignments) {

			final Long assignmentId = assignment.getId();

			final String rawGrade = gradeMap.get(assignmentId);
			final Double pointsPossible = assignment.getPoints();
			Double grade;

			// determine the grade we should be using depending on the grading type
			if (gb.getGrade_type() == GradingConstants.GRADE_TYPE_PERCENTAGE) {
				grade = calculateEquivalentPointValueForPercent(pointsPossible, NumberUtils.createDouble(rawGrade));
			} else if (gb.getGrade_type() == GradingConstants.GRADE_TYPE_LETTER) {
				grade = gradingSchema.get(rawGrade);
			} else {
				grade = NumberUtils.createDouble(rawGrade);
			}

			// recreate the category (required fields only)
			final Category c = new Category();
			c.setId(category.getId());
			c.setDropHighest(category.getDropHighest());
			c.setDropLowest(category.getDropLowest());
			c.setKeepHighest(category.getKeepHighest());

			// recreate the assignment (required fields only)
			final GradebookAssignment a = new GradebookAssignment();
			a.setPointsPossible(assignment.getPoints());
			a.setUngraded(assignment.isUngraded());
			a.setCounted(assignment.isCounted());
			a.setExtraCredit(assignment.isExtraCredit());
			a.setReleased(assignment.isReleased());
			a.setRemoved(false); // shared.GradebookAssignment doesn't include removed so this will always be false
			a.setGradebook(gb);
			a.setCategory(c);
			a.setId(assignment.getId()); // store the id so we can find out later which grades were dropped, if any

			// create the AGR
			final AssignmentGradeRecord gradeRecord = new AssignmentGradeRecord(a, studentUuid, grade);

			if (!a.isNotCounted()) {
				gradeRecords.add(gradeRecord);
			}
		}

		return calculateCategoryScore(studentUuid, category.getId(), gradeRecords, includeNonReleasedItems);
	}

	@Override
	public Optional<CategoryScoreData> calculateCategoryScore(Long gradebookId, String studentUuid, Long categoryId, boolean includeNonReleasedItems) {

		// get all grade records for the student
		final Map<String, List<AssignmentGradeRecord>> gradeRecMap
            = getGradeRecordMapForStudents(gradebookId, Collections.singletonList(studentUuid));

		// apply the settings
		final List<AssignmentGradeRecord> gradeRecords = gradeRecMap.get(studentUuid);

		return calculateCategoryScore(studentUuid, categoryId, gradeRecords, includeNonReleasedItems);
	}

	/**
	 * Does the heavy lifting for the category calculations. Requires the List of AssignmentGradeRecord so that we can applyDropScores.
	 *
	 * @param studentUuid the student uuid
	 * @param categoryId the category id we are interested in
	 * @param gradeRecords all grade records for the student
	 * @return
	 */
	private Optional<CategoryScoreData> calculateCategoryScore(final String studentUuid, final Long categoryId,
			final List<AssignmentGradeRecord> gradeRecords, final boolean includeNonReleasedItems) {

		// validate
		if (gradeRecords == null) {
			log.debug("No grade records for student: {}. Nothing to do.", studentUuid);
			return Optional.empty();
		}

		if (categoryId == null) {
			log.debug("No category supplied, nothing to do.");
			return Optional.empty();
		}

		// setup
		int numScored = 0;
		int numOfAssignments = 0;
		BigDecimal totalEarned = new BigDecimal("0");
		BigDecimal totalPossible = new BigDecimal("0");

		// apply any drop/keep settings for this category
		applyDropScores(gradeRecords);

		// find the records marked as dropped (highest/lowest) before continuing,
		// as gradeRecords will be modified in place after this and these records will be removed
		final List<Long> droppedItemIds = gradeRecords.stream()
				.filter(AssignmentGradeRecord::getDroppedFromGrade)
				.map(agr -> agr.getAssignment().getId())
				.collect(Collectors.toList());

		// Since all gradeRecords for the student are passed in, not just for this category,
		// plus they may not meet the criteria for including in the calculation,
		// this list is filtered down according to the following rules:
		// Rule 1. remove gradeRecords that don't match the given category
		// Rule 2. the assignment must have points to be assigned
		// Rule 3. there is a non blank grade for the student
		// Rule 4. the assignment is included in course grade calculations
		// Rule 5. the assignment is released to the student (instructor gets to see category grade regardless of release status; student does not)
		// Rule 6. the grade is not dropped from the calc
		// Rule 7. extra credit items have their grade value counted only. Their total points possible does not apply to the calculations
		log.debug("categoryId: {}", categoryId);

		log.debug("Unfiltered gradeRecords.size(): {}", gradeRecords.size());

		gradeRecords.removeIf(gradeRecord -> {
			final GradebookAssignment assignment = gradeRecord.getAssignment();

			// remove if not for this category (rule 1)
			if (assignment.getCategory() == null) {
				return true;
			}
			if (categoryId.longValue() != assignment.getCategory().getId().longValue()) {
				return true;
			}


			final boolean excluded = BooleanUtils.toBoolean(gradeRecord.getExcludedFromGrade());
			// remove if the assignment/graderecord doesn't meet the criteria for the calculation (rule 2-6)
			if (excluded || assignment.getPointsPossible() == null || gradeRecord.getPointsEarned() == null || !assignment.isCounted()
					|| (!assignment.isReleased() && !includeNonReleasedItems) || gradeRecord.getDroppedFromGrade()) {
				return true;
			}

			return false;
		});

		log.debug("Filtered gradeRecords.size(): {}", gradeRecords.size());

		// pre-calculation
		// Rule 1. If category only has a single EC item, don't try to calculate category total.
		if (gradeRecords.size() == 1 && gradeRecords.get(0).getAssignment().isExtraCredit()) {
			return Optional.empty();
		}

		// iterate the filtered list and set the variables for the calculation
		for (final AssignmentGradeRecord gradeRecord : gradeRecords) {

			final GradebookAssignment assignment = gradeRecord.getAssignment();

			// EC item, don't count points possible
			if (!assignment.isExtraCredit()) {
				totalPossible = totalPossible.add(new BigDecimal(assignment.getPointsPossible().toString()));
				numOfAssignments++;
				numScored++;
			}

			// sanitise grade, null values to "0";
			final String grade = (gradeRecord.getPointsEarned() != null) ? String.valueOf(gradeRecord.getPointsEarned()) : "0";

			// update total points earned
			totalEarned = totalEarned.add(new BigDecimal(grade));
		}

		if (numScored == 0 || numOfAssignments == 0 || totalPossible.doubleValue() == 0) {
			return Optional.empty();
		}

		final BigDecimal mean = totalEarned.divide(new BigDecimal(numScored), GradingConstants.MATH_CONTEXT)
				.divide((totalPossible.divide(new BigDecimal(numOfAssignments), GradingConstants.MATH_CONTEXT)),
						GradingConstants.MATH_CONTEXT)
				.multiply(new BigDecimal("100"));

        if (log.isDebugEnabled()) {
		    log.debug("Mean score: {}", mean.doubleValue());
        }

		return Optional.of(new CategoryScoreData(mean.doubleValue(), droppedItemIds));
	}

    @Override
	public CourseGrade getCourseGradeForStudent(String gradebookUid, String userUuid) {
		return getCourseGradeForStudents(gradebookUid, Collections.singletonList(userUuid)).get(userUuid);
	}

	@Override
	public Map<String, CourseGrade> getCourseGradeForStudents(String gradebookUid, List<String> userUuids) {

		final Map<String, CourseGrade> rval = new HashMap<>();

		try {
			final Gradebook gradebook = getGradebook(gradebookUid);
			final GradeMapping gradeMap = gradebook.getSelectedGradeMapping();

			rval.putAll(getCourseGradeForStudents(gradebookUid, userUuids, gradeMap.getGradeMap()));
		} catch (final Exception e) {
			log.error("Error in getCourseGradeForStudents", e);
		}
		return rval;
	}

    private CourseGrade getCourseGrade(Long gradebookId) {
        return courseGradeRepository.findOneByGradebook_Id(gradebookId);
    }

	@Override
	public Map<String, CourseGrade> getCourseGradeForStudents(String gradebookUid, List<String> userUuids, Map<String, Double> gradeMap) {

		final Map<String, CourseGrade> rval = new HashMap<>();

		try {
			final Gradebook gradebook = getGradebook(gradebookUid);

			// if not released, and not instructor or TA, don't do any work
			// note that this will return a course grade for Instructor and TA even if not released, see SAK-30119
			if (!gradebook.isCourseGradeDisplayed() && !(currentUserHasEditPerm(gradebookUid) || currentUserHasGradingPerm(gradebookUid))) {
				return rval;
			}

			final List<GradebookAssignment> assignments = getCountedAssignments(gradebook.getId());

			// this takes care of drop/keep scores
			final List<CourseGradeRecord> gradeRecords = getPointsEarnedCourseGradeRecords(getCourseGrade(gradebook.getId()), userUuids);

			// gradeMap MUST be sorted for the grade mapping to apply correctly
			final Map<String, Double> sortedGradeMap = GradeMappingDefinition.sortGradeMapping(gradeMap);

			gradeRecords.forEach(gr -> {

				final CourseGrade cg = new CourseGrade();

				// ID of the course grade item
				cg.setId(gr.getCourseGrade().getId());

				// set entered grade
				cg.setEnteredGrade(gr.getEnteredGrade());

				// set date recorded
				cg.setDateRecorded(gr.getDateRecorded());

				if (!assignments.isEmpty()) {

					// calculated grade
					// may be null if no grade entries to calculate
					Double calculatedGrade = gr.getAutoCalculatedGrade();
					if (calculatedGrade != null) {
					    log.debug("calculatedGrade: {}", calculatedGrade);
						cg.setCalculatedGrade(calculatedGrade.toString());

						// SAK-33997 Adjust the rounding of the calculated grade so we get the appropriate
						// grade mapping
						BigDecimal bd = new BigDecimal(calculatedGrade)
								.setScale(10, RoundingMode.HALF_UP)
								.setScale(2, RoundingMode.HALF_UP);
						calculatedGrade = bd.doubleValue();
					}

					// mapped grade
					String mappedGrade = GradeMapping.getMappedGrade(sortedGradeMap, calculatedGrade);
					log.debug("calculatedGrade: {} -> mappedGrade: {}", calculatedGrade, mappedGrade);
					cg.setMappedGrade(mappedGrade);

					// points
					cg.setPointsEarned(gr.getPointsEarned()); // synonymous with gradeRecord.getCalculatedPointsEarned()
					cg.setTotalPointsPossible(gr.getTotalPointsPossible());

				}
				rval.put(gr.getStudentId(), cg);
			});
		} catch (Exception e) {
			log.error("Error in getCourseGradeForStudents", e);
		}
		return rval;
	}

	@Override
	public List<CourseSection> getViewableSections(String gradebookUid) {
		return sakaiProxy.getViewableSections(gradebookUid);
	}

    /**
     * Get's all course grade overrides for a given gradebook
     *
     * @param gradebook The gradebook
     * @return A list of {@link CourseGradeRecord} that have overrides
     *
     * @throws HibernateException
     */
    private List<CourseGradeRecord> getCourseGradeOverrides(Gradebook gradebook) {
        return courseGradeRecordRepository.findByGradableObject_GradebookAndEnteredGradeNotNull(gradebook);
    }

    public void updateGradeMapping(Long gradeMappingId, Map<String, Double> gradeMap) {

        GradeMapping gradeMapping = gradeMappingRepository.findOne( gradeMappingId);
        gradeMapping.setGradeMap(gradeMap);
        gradeMappingRepository.save(gradeMapping);
	}

    @Override
	public void updateGradebookSettings(final String gradebookUid, final GradebookInformation gbInfo) {

		if (gradebookUid == null) {
			throw new IllegalArgumentException("null gradebookUid " + gradebookUid);
		}

		// must be instructor type person
		if (!currentUserHasEditPerm(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to edit gb information", sakaiProxy.getCurrentUserId(), gradebookUid);
			throw new GradebookSecurityException("You do not have permission to edit gradebook information in site " + gradebookUid);
		}

		final Gradebook gradebook = getGradebook(gradebookUid);
		if (gradebook == null) {
			throw new IllegalArgumentException("There is no gradebook associated with this id: " + gradebookUid);
		}

		final Map<String, Double> bottomPercents = gbInfo.getSelectedGradingScaleBottomPercents();

		// Before we do any work, check if any existing course grade overrides might be left in an unmappable state
		final List<CourseGradeRecord> courseGradeOverrides = getCourseGradeOverrides(gradebook);
		courseGradeOverrides.forEach(cgr -> {
			if (!bottomPercents.containsKey(cgr.getEnteredGrade())) {
				throw new UnmappableCourseGradeOverrideException(
						"The grading schema could not be updated as it would leave some course grade overrides in an unmappable state.");
			}
		});

		// iterate all available grademappings for this gradebook and set the one that we have the ID and bottomPercents for
		final Set<GradeMapping> gradeMappings = gradebook.getGradeMappings();
		gradeMappings.forEach(gradeMapping -> {
			if (StringUtils.equals(Long.toString(gradeMapping.getId()), gbInfo.getSelectedGradeMappingId())) {
				gradebook.setSelectedGradeMapping(gradeMapping);

				// update the map values
				updateGradeMapping(gradeMapping.getId(), bottomPercents);
			}
		});

		// set grade type, but only if sakai.property is true OR user is admin
		final boolean gradeTypeAvailForNonAdmins = sakaiProxy.getBooleanConfig("gradebook.settings.gradeEntry.showToNonAdmins", true);
		if (gradeTypeAvailForNonAdmins || sakaiProxy.isSuperUser()) {
			gradebook.setGrade_type(gbInfo.getGradeType());
		}

		// set category type
		gradebook.setCategory_type(gbInfo.getCategoryType());

		// set display release items to students
		gradebook.setAssignmentsDisplayed(gbInfo.isDisplayReleasedGradeItemsToStudents());

		// set course grade display settings
		gradebook.setCourseGradeDisplayed(gbInfo.isCourseGradeDisplayed());
		gradebook.setCourseLetterGradeDisplayed(gbInfo.isCourseLetterGradeDisplayed());
		gradebook.setCoursePointsDisplayed(gbInfo.isCoursePointsDisplayed());
		gradebook.setCourseAverageDisplayed(gbInfo.isCourseAverageDisplayed());

		// set stats display settings
		gradebook.setAssignmentStatsDisplayed(gbInfo.isAssignmentStatsDisplayed());
		gradebook.setCourseGradeStatsDisplayed(gbInfo.isCourseGradeStatsDisplayed());

		final List<CategoryDefinition> newCategoryDefinitions = gbInfo.getCategories();

		// if we have categories and they are weighted, check the weightings sum up to 100% (or 1 since it's a fraction)
		if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
			double totalWeight = 0;
			for (final CategoryDefinition newDef : newCategoryDefinitions) {

				if (newDef.getWeight() == null) {
					throw new IllegalArgumentException("No weight specified for a category, but weightings enabled");
				}

				totalWeight += newDef.getWeight();
			}
			if (Math.rint(totalWeight) != 1) {
				throw new IllegalArgumentException("Weightings for the categories do not equal 100%");
			}
		}

		// get current categories and build a mapping list of Category.id to Category
		final List<Category> currentCategories = getCategories(gradebook.getId());
		final Map<Long, Category> currentCategoryMap = new HashMap<>();
		for (final Category c : currentCategories) {
			currentCategoryMap.put(c.getId(), c);
		}

		// compare current list with given list, add/update/remove as required
		// Rules:
		// If category does not have an ID it is new; add these later after all removals have been processed
		// If category has an ID it is to be updated. Update and remove from currentCategoryMap.
		// Any categories remaining in currentCategoryMap are to be removed.
		// Sort by category order as we resequence the order values to avoid gaps
		Collections.sort(newCategoryDefinitions, CategoryDefinition.orderComparator);
		final Map<CategoryDefinition, Integer> newCategories = new HashMap<>();
		int categoryIndex = 0;
		for (final CategoryDefinition newDef : newCategoryDefinitions) {

			// preprocessing and validation
			// Rule 1: If category has no name, it is to be removed/skipped
			// Note that we no longer set weights to 0 even if unweighted category type selected. The weights are not considered if its not
			// a weighted category type
			// so this allows us to switch back and forth between types without losing information

			if (StringUtils.isBlank(newDef.getName())) {
				continue;
			}

			// new
			if (newDef.getId() == null) {
				newCategories.put(newDef, categoryIndex);
				categoryIndex++;
			}

			// update
			else {
				final Category existing = currentCategoryMap.get(newDef.getId());
				existing.setName(newDef.getName());
				existing.setWeight(newDef.getWeight());
				existing.setDropLowest(newDef.getDropLowest());
				existing.setDropHighest(newDef.getDropHighest());
				existing.setKeepHighest(newDef.getKeepHighest());
				existing.setExtraCredit(newDef.getExtraCredit());
				existing.setCategoryOrder(categoryIndex);
				updateCategory(existing);

				// remove from currentCategoryMap so we know not to delete it
				currentCategoryMap.remove(newDef.getId());

				categoryIndex++;
			}

		}

		// handle deletes
		// anything left in currentCategoryMap was not included in the new list, delete them
		for (final Map.Entry<Long, Category> cat : currentCategoryMap.entrySet()) {
			removeCategory(cat.getKey());
		}

		// Handle the additions
		for (final Map.Entry<CategoryDefinition, Integer> entry : newCategories.entrySet()) {
			final CategoryDefinition newCat = entry.getKey();
			this.createCategory(gradebook.getId(), newCat.getName(), newCat.getWeight(), newCat.getDropLowest(),
					newCat.getDropHighest(), newCat.getKeepHighest(), newCat.getExtraCredit(), entry.getValue());
		}

		// if weighted categories, all uncategorised assignments are to be removed from course grade calcs
		if (gradebook.getCategory_type() == GradingConstants.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
			excludeUncategorisedItemsFromCourseGradeCalculations(gradebook);
		}

		// persist
		updateGradebook(gradebook);
	}

    private void updateCategory(Category category) throws ConflictingCategoryNameException {

        //session.evict(category);
        Category persistentCat = categoryRepository.findOne(category.getId());
        long numNameConflicts
            = categoryRepository.countByNameAndGradebookAndIdNotAndRemovedIsFalse(
                category.getName(), category.getGradebook(), category.getId());

        if (numNameConflicts > 0L) {
            throw new ConflictingCategoryNameException("You can not save multiple category in a gradebook with the same name");
        }
        if (category.getWeight().doubleValue() > 1 || category.getWeight().doubleValue() < 0) {
            throw new IllegalArgumentException("weight for category is greater than 1 or less than 0 in updateCategory of BaseHibernateManager");
        }
        //session.evict(persistentCat);
        categoryRepository.save(category);
    }

    /**
	 * Updates all uncategorised items to exclude them from the course grade calcs
	 *
	 * @param gradebook
	 */
	private void excludeUncategorisedItemsFromCourseGradeCalculations(Gradebook gradebook) {

		List<GradebookAssignment> allAssignments = persistence.getAssignments(gradebook.getId());

		List<GradebookAssignment> assignments = allAssignments.stream().filter(a -> a.getCategory() == null)
				.collect(Collectors.toList());
		assignments.forEach(a -> { a.setCounted(false); gradebookAssignmentRepository.save(a); });
	}

    @Override
	public Set getGradebookGradeMappings(Long gradebookId) {

        /*
		return (Set) getHibernateTemplate().execute(new HibernateCallback() {
			@Override
			public Set doInHibernate(final Session session) throws HibernateException {
				final Gradebook gradebook = (Gradebook) session.load(Gradebook.class, gradebookId);
				Hibernate.initialize(gradebook.getGradeMappings());
				return gradebook.getGradeMappings();
			}
		});
        */

        return gradeMappingRepository.findByGradebook_Id(gradebookId);
	}

	@Override
	public Set getGradebookGradeMappings(String gradebookUid) {

		//final Long gradebookId = getGradebook(gradebookUid).getId();
		//return this.getGradebookGradeMappings(gradebookId);
        return gradeMappingRepository.findByGradebook_Uid(gradebookUid);
	}

    @Override
	public void updateCourseGradeForStudent(String gradebookUid, String studentUuid, String grade) {

		// must be instructor type person
		if (!currentUserHasEditPerm(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to update course grade for student: {}", sakaiProxy.getCurrentUserId(),
					gradebookUid, studentUuid);
			throw new GradebookSecurityException("You do not have permission to update course grades in " + gradebookUid);
		}

		final Gradebook gradebook = getGradebook(gradebookUid);
		if (gradebook == null) {
			throw new IllegalArgumentException("There is no gradebook associated with this id: " + gradebookUid);
		}

		// get course grade for the student
        CourseGradeRecord courseGradeRecord = courseGradeRecordRepository.findOneByStudentIdAndGradableObject_Gradebook(studentUuid, gradebook);

		// if user doesn't have an entered course grade, we need to find the course grade and create a record
		if (courseGradeRecord == null) {

			final CourseGrade courseGrade = getCourseGrade(gradebook.getId());

			courseGradeRecord = new CourseGradeRecord(courseGrade, studentUuid);
			courseGradeRecord.setGraderId(sakaiProxy.getCurrentUserId());

		} else {
			// if passed in grade override is same as existing grade override, nothing to do
			if (StringUtils.equals(courseGradeRecord.getEnteredGrade(), grade)) {
				return;
			}
		}

		// set the grade override
		courseGradeRecord.setEnteredGrade(grade);
		// record the last grade override date
		courseGradeRecord.setDateRecorded(new Date());

		// create a grading event
		GradingEvent gradingEvent = new GradingEvent();
		gradingEvent.setGradableObject(courseGradeRecord.getCourseGrade());
		gradingEvent.setGraderId(sakaiProxy.getCurrentUserId());
		gradingEvent.setStudentId(studentUuid);
		gradingEvent.setGrade(courseGradeRecord.getEnteredGrade());

		// save
		courseGradeRecordRepository.save(courseGradeRecord);
		gradingEventRepository.save(gradingEvent);
	}

    /**
	 * Updates the categorized order of an assignment
	 *
	 * @see GradebookService.updateAssignmentCategorizedOrder(java.lang.String gradebookUid, java.lang.Long assignmentId, java.lang.Integer
	 *      order)
	 */
	@Override
	public void updateAssignmentCategorizedOrder(final String gradebookUid, final Long categoryId, final Long assignmentId, Integer order) {

		if (!sakaiProxy.isUserAbleToEditAssessments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User {} in gradebook {} attempted to change the order of assignment {}", sakaiProxy.getCurrentUserId(),
					gradebookUid, assignmentId);
			throw new GradebookSecurityException();
		}

		if (order == null) {
			throw new IllegalArgumentException("Categorized Order cannot be null");
		}

		final Long gradebookId = getGradebook(gradebookUid).getId();

		// get all assignments for this gradebook
		final List<GradebookAssignment> assignments = getAssignments(gradebookId, SortType.SORT_BY_CATEGORY, true);
		final List<GradebookAssignment> assignmentsInNewCategory = new ArrayList<>();
		for (final GradebookAssignment assignment : assignments) {
			if (assignment.getCategory() == null) {
				if (categoryId == null) {
					assignmentsInNewCategory.add(assignment);
				}
			} else if (assignment.getCategory().getId().equals(categoryId)) {
				assignmentsInNewCategory.add(assignment);
			}
		}

		// adjust order to be within bounds
		if (order < 0) {
			order = 0;
		} else if (order > assignmentsInNewCategory.size()) {
			order = assignmentsInNewCategory.size();
		}

		// find the assignment
		GradebookAssignment target = null;
		for (final GradebookAssignment a : assignmentsInNewCategory) {
			if (a.getId().equals(assignmentId)) {
				target = a;
				break;
			}
		}

		// add the assignment to the list via a 'pad, remove, add' approach
		assignmentsInNewCategory.add(null); // ensure size remains the same for the remove
		assignmentsInNewCategory.remove(target); // remove item
		assignmentsInNewCategory.add(order, target); // add at ordered position, will shuffle others along

		// the assignments are now in the correct order within the list, we just need to update the sort order for each one
		// create a new list for the assignments we need to update in the database
		final List<GradebookAssignment> assignmentsToUpdate = new ArrayList<>();

		int i = 0;
		for (final GradebookAssignment a : assignmentsInNewCategory) {

			// skip if null
			if (a == null) {
				continue;
			}

			// if the sort order is not the same as the counter, update the order and add to the other list
			// this allows us to skip items that have not had their position changed and saves some db work later on
			// sort order may be null if never previously sorted, so give it the current index
			if (a.getCategorizedSortOrder() == null || !a.getCategorizedSortOrder().equals(i)) {
				a.setCategorizedSortOrder(i);
				assignmentsToUpdate.add(a);
			}

			i++;
		}

		// do the updates
		assignmentsToUpdate.forEach(a -> updateAssignment(a));
	}

    /**
	 * Return the grade changes made since a given time
	 *
	 * @param assignmentIds ids of assignments to check
	 * @param since timestamp from which to check for changes
	 * @return set of changes made
	 */
	@Override
	public List<GradingEvent> getGradingEvents(List<Long> assignmentIds, Date since) {

		if (assignmentIds == null || assignmentIds.isEmpty() || since == null) {
			return new ArrayList<>();
		}

        return gradingEventRepository.findByDateGradedGreaterThanEqualAndGradableObject_IdIn(since, assignmentIds);
	}

    @Override
	public void setAvailableGradingScales(final Collection gradingScaleDefinitions) {

        mergeGradeMappings(gradingScaleDefinitions);
	}

    private void copyDefinitionToScale(GradingScaleDefinition bean, GradingScale gradingScale) {

		gradingScale.setUnavailable(false);
		gradingScale.setName(bean.getName());
		gradingScale.setGrades(bean.getGrades());
		Map<String, Double> defaultBottomPercents = new HashMap<>();
		Iterator gradesIter = bean.getGrades().iterator();
		Iterator defaultBottomPercentsIter = bean.getDefaultBottomPercentsAsList().iterator();
		while (gradesIter.hasNext() && defaultBottomPercentsIter.hasNext()) {
			String grade = (String)gradesIter.next();
			Double value = (Double)defaultBottomPercentsIter.next();
			defaultBottomPercents.put(grade, value);
		}
		gradingScale.setDefaultBottomPercents(defaultBottomPercents);
	}



    private void mergeGradeMappings(final Collection<GradingScaleDefinition> gradingScaleDefinitions) {

		final Map<String, GradingScaleDefinition> newMappingDefinitionsMap = new HashMap<>();
		Set<String> uidsToSet = new HashSet<>();
		for (final GradingScaleDefinition bean : gradingScaleDefinitions) {
			newMappingDefinitionsMap.put(bean.getUid(), bean);
			uidsToSet.add(bean.getUid());
		}

		// Until we move to Hibernate 3 syntax, we need to update one record at a time.
		// Toggle any scales that are no longer specified.
		List<GradingScale> gmtList = gradingScaleRepository.findByUidNotInAndUnavailableIsFalse(uidsToSet);
		for (GradingScale gradingScale : gmtList) {
			gradingScale.setUnavailable(true);
			gradingScaleRepository.save(gradingScale);
            log.info("Set Grading Scale {} unavailable",  gradingScale.getUid());
		}

		// Modify any specified scales that already exist.
		gmtList = gradingScaleRepository.findByUidIn(uidsToSet);
		for (GradingScale gradingScale : gmtList) {
			copyDefinitionToScale(newMappingDefinitionsMap.get(gradingScale.getUid()), gradingScale);
			uidsToSet.remove(gradingScale.getUid());
			gradingScaleRepository.save(gradingScale);
            log.info("Updated Grading Scale {}", gradingScale.getUid());
		}

		// Add any new scales.
		for (String uid : uidsToSet) {
			GradingScale gradingScale = new GradingScale();
			gradingScale.setUid(uid);
			GradingScaleDefinition bean = newMappingDefinitionsMap.get(uid);
			copyDefinitionToScale(bean, gradingScale);
			gradingScaleRepository.save(gradingScale);
            log.info("Added Grading Scale {}", gradingScale.getUid());
		}
	}

    @Override
	public void setDefaultGradingScale(String uid) {
		setPropertyValue(UID_OF_DEFAULT_GRADING_SCALE_PROPERTY, uid);
	}

    @Override
	public List<GradingScale> getAvailableGradingScales() {

        // Get available grade mapping templates.
        List<GradingScale> gradingScales = gradingScaleRepository.findByUnavailableIsFalse();

        // The application won't be able to run without grade mapping
        // templates, so if for some reason none have been defined yet,
        // do that now.
        if (gradingScales.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info("No Grading Scale defined yet. This is probably because you have upgraded or you are working with a new database. Default grading scales will be created. Any customized system-wide grade mappings you may have defined in previous versions will have to be reconfigured.");
            }
            gradingScales = addDefaultGradingScales();
        }
        return gradingScales;
	}

	@Override
	public List<GradingScaleDefinition> getAvailableGradingScaleDefinitions() {

		List<GradingScale> gradingScales = getAvailableGradingScales();

		List<GradingScaleDefinition> rval = new ArrayList<>();
		for(GradingScale gradingScale: gradingScales) {
			rval.add(gradingScale.toGradingScaleDefinition());
		}
		return rval;
	}

    @Override
	public void saveGradeMappingToGradebook(String scaleUuid, String gradebookUid) {

        List<GradingScale> gradingScales = gradingScaleRepository.findByUnavailableIsFalse();

        for (GradingScale gradingScale : gradingScales) {
            if (gradingScale.getUid().equals(scaleUuid)) {
                GradeMapping gradeMapping = new GradeMapping(gradingScale);
                Gradebook gradebookToSet = getGradebook(gradebookUid);
                gradeMapping.setGradebook(gradebookToSet);
                gradeMappingRepository.save(gradeMapping);
            }
        }
	}

    @Override
	public synchronized void addExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl,
			final String title, final double points, final Date dueDate, final String externalServiceDescription, String externalData)
			throws ConflictingAssignmentNameException, ConflictingExternalIdException, GradebookNotFoundException {

		// Ensure that the required strings are not empty
		if (StringUtils.trimToNull(externalServiceDescription) == null ||
				StringUtils.trimToNull(externalId) == null ||
				StringUtils.trimToNull(title) == null) {
			throw new RuntimeException("External service description, externalId, and title must not be empty");
		}

		// Ensure that points is > zero
		if (points <= 0) {
			throw new AssignmentHasIllegalPointsException("Points must be > 0");
		}

		// Ensure that the assessment name is unique within this gradebook
		if (isAssignmentDefined(gradebookUid, title)) {
			throw new ConflictingAssignmentNameException("An assignment with that name already exists in gradebook uid=" + gradebookUid);
		}

		// name cannot contain these chars as they are reserved for special columns in import/export
		GradebookHelper.validateGradeItemName(title);

        final long externalIdConflicts = gradebookAssignmentRepository.countByExternalIdAndGradebook_Uid(externalId, gradebookUid);

        if (externalIdConflicts > 0L) {
            throw new ConflictingExternalIdException(
                    "An external assessment with ID=" + externalId + " already exists in gradebook uid=" + gradebookUid);
        }

        // Get the gradebook
        final Gradebook gradebook = getGradebook(gradebookUid);

        // Create the external assignment
        final GradebookAssignment asn = new GradebookAssignment(gradebook, title, Double.valueOf(points), dueDate);
        asn.setExternallyMaintained(true);
        asn.setExternalId(externalId);
        asn.setExternalInstructorLink(externalUrl);
        asn.setExternalStudentLink(externalUrl);
        asn.setExternalAppName(externalServiceDescription);
        asn.setExternalData(externalData);
        // set released to be true to support selective release
        asn.setReleased(true);
        asn.setUngraded(false);

        gradebookAssignmentRepository.save(asn);
		log.info("External assessment added to gradebookUid={}, externalId={} by userUid={} from externalApp={}", gradebookUid, externalId,
				sakaiProxy.getCurrentUserId(), externalServiceDescription);
	}

    /**
	 * Wrapper created when category was added for assignments tool
	 */
	@Override
    public void addExternalAssessment(String gradebookUid, String externalId, String externalUrl, String title, Double points,
                                      Date dueDate, String externalServiceDescription, String externalData, Boolean ungraded)
            throws GradebookNotFoundException, ConflictingAssignmentNameException, ConflictingExternalIdException, AssignmentHasIllegalPointsException {
        addExternalAssessment(gradebookUid, externalId, externalUrl, title, points, dueDate, externalServiceDescription, externalData, ungraded, null);
    }

    @Override
    public synchronized void addExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl, final String title, final Double points,
                                                   final Date dueDate, final String externalServiceDescription, String externalData, final Boolean ungraded, final Long categoryId)
            throws GradebookNotFoundException, ConflictingAssignmentNameException, ConflictingExternalIdException, AssignmentHasIllegalPointsException {
        // Ensure that the required strings are not empty
		if (StringUtils.trimToNull(externalServiceDescription) == null ||
				StringUtils.trimToNull(externalId) == null ||
				StringUtils.trimToNull(title) == null) {
			throw new RuntimeException("External service description, externalId, and title must not be empty");
		}

		// Ensure that points is > zero
		if ((ungraded != null && !ungraded.booleanValue() && (points == null || points.doubleValue() <= 0))
				|| (ungraded == null && (points == null || points.doubleValue() <= 0))) {
			throw new AssignmentHasIllegalPointsException("Points can't be null or Points must be > 0");
		}

		// Ensure that the assessment name is unique within this gradebook
		if (isAssignmentDefined(gradebookUid, title)) {
			throw new ConflictingAssignmentNameException("An assignment with that name already exists in gradebook uid=" + gradebookUid);
		}

		// name cannot contain these chars as they are reserved for special columns in import/export
		GradebookHelper.validateGradeItemName(title);

        // Ensure that the externalId is unique within this gradebook
        final long externalIdConflicts = gradebookAssignmentRepository.countByExternalIdAndGradebook_Uid(externalId, gradebookUid);

        if (externalIdConflicts > 0L) {
            throw new ConflictingExternalIdException(
                    "An external assessment with that ID already exists in gradebook uid=" + gradebookUid);
        }

        // Get the gradebook
        final Gradebook gradebook = getGradebook(gradebookUid);

        // if a category was indicated, double check that it is valid
        Category persistedCategory = null;
        if (categoryId != null) {
            persistedCategory = categoryRepository.findOne(categoryId);
            if (persistedCategory.isDropScores()) {
                List<GradebookAssignment> thisCategoryAssignments = getAssignmentsForCategory(categoryId);
                for (GradebookAssignment thisAssignment : thisCategoryAssignments) {
                    if (!Objects.equals(thisAssignment.getPointsPossible(), points)) {
                        String errorMessage = "Assignment points mismatch the selected Gradebook Category ("
                            + thisAssignment.getPointsPossible().toString() + ") and cannot be added to Gradebook )";
                        throw new InvalidCategoryException(errorMessage);
                    }
                }
            }
            if (persistedCategory == null || persistedCategory.isRemoved() ||
                    !persistedCategory.getGradebook().getId().equals(gradebook.getId())) {
                throw new InvalidCategoryException("The category with id " + categoryId +
                        " is not valid for gradebook " + gradebook.getUid());
            }
        }

        // Create the external assignment
        final GradebookAssignment asn = new GradebookAssignment(gradebook, title, points, dueDate);
        asn.setExternallyMaintained(true);
        asn.setExternalId(externalId);
        asn.setExternalInstructorLink(externalUrl);
        asn.setExternalStudentLink(externalUrl);
        asn.setExternalAppName(externalServiceDescription);
        asn.setExternalData(externalData);
        if (persistedCategory != null) {
            asn.setCategory(persistedCategory);
        }
        // set released to be true to support selective release
        asn.setReleased(true);
        if (ungraded != null) {
            asn.setUngraded(ungraded);
        } else {
            asn.setUngraded(false);
        }

        gradebookAssignmentRepository.save(asn);
		log.info("External assessment added to gradebookUid={}, externalId={} by userUid={} from externalApp={}", gradebookUid, externalId,
				sakaiProxy.getCurrentUserId(), externalServiceDescription);
	}

    @Override
	public void updateExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl,
										 String externalData, final String title, final double points, final Date dueDate)
			throws GradebookNotFoundException, AssessmentNotFoundException, AssignmentHasIllegalPointsException {

        this.updateExternalAssessment(gradebookUid, externalId, externalUrl, externalData, title, points, dueDate, null);
	}

    @Override
    public void updateExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl, String externalData, final String title,
                                         final Double points, final Date dueDate, final Boolean ungraded)
            throws GradebookNotFoundException, AssessmentNotFoundException, ConflictingAssignmentNameException, AssignmentHasIllegalPointsException {

        final GradebookAssignment asn = getExternalAssignment(gradebookUid, externalId);

		if (asn == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}

		// Ensure that points is > zero
		if ((ungraded != null && !ungraded.booleanValue() && (points == null || points.doubleValue() <= 0))
				|| (ungraded == null && (points == null || points.doubleValue() <= 0))) {
			throw new AssignmentHasIllegalPointsException("Points can't be null or Points must be > 0");
		}

		// Ensure that the required strings are not empty
		if (StringUtils.trimToNull(externalId) == null ||
				StringUtils.trimToNull(title) == null) {
			throw new RuntimeException("ExternalId, and title must not be empty");
		}

		// name cannot contain these chars as they are reserved for special columns in import/export
		GradebookHelper.validateGradeItemName(title);

        asn.setExternalInstructorLink(externalUrl);
        asn.setExternalStudentLink(externalUrl);
        asn.setExternalData(externalData);
        asn.setName(title);
        asn.setDueDate(dueDate);
        // support selective release
        asn.setReleased(BooleanUtils.isTrue(asn.getReleased()));
        asn.setPointsPossible(points);
        if (ungraded != null) {
            asn.setUngraded(ungraded.booleanValue());
        } else {
            asn.setUngraded(false);
        }
        gradebookAssignmentRepository.save(asn);
        log.info("External assessment updated in gradebookUid={}, externalId={} by userUid={}", gradebookUid, externalId, sakaiProxy.getCurrentUserId());
	}

    private GradebookAssignment getExternalAssignment(String gradebookUid, String externalId)
			throws GradebookNotFoundException {
        return gradebookAssignmentRepository.findOneByGradebook_UidAndExternalId(gradebookUid, externalId);
	}

    /**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#removeExternalAssessment(java.lang.String, java.lang.String)
	 */
	@Override
	public void removeExternalAssessment(final String gradebookUid,
			final String externalId) throws GradebookNotFoundException, AssessmentNotFoundException {

		// Get the external assignment
		final GradebookAssignment asn = getExternalAssignment(gradebookUid, externalId);
		if (asn == null) {
			throw new AssessmentNotFoundException("There is no external assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}

        long numDeleted = gradingEventRepository.deleteByGradableObject(asn);
        log.debug("Deleted {} records from gb_grading_event_t", numDeleted);

        numDeleted = assignmentGradeRecordRepository.deleteByGradableObject(asn);
        log.info("Deleted {} externally defined scores", numDeleted);

        numDeleted = commentRepository.deleteByGradableObject(asn);
        log.info("Deleted {} externally defined comments", numDeleted);

		// Delete the assessment.
		gradebookAssignmentRepository.delete(asn);

		log.info("External assessment removed from gradebookUid={}, externalId={} by userUid={}", gradebookUid, externalId, sakaiProxy.getCurrentUserId());
	}

    @Override
	public void updateExternalAssessmentScore(final String gradebookUid, final String externalId, final String studentUid,
			final String points)
			throws GradebookNotFoundException, AssessmentNotFoundException {

		final GradebookAssignment asn = getExternalAssignment(gradebookUid, externalId);

		if (asn == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}

		log.debug("BEGIN: Update 1 score for gradebookUid={}, external assessment={} from {}", gradebookUid, externalId,
				asn.getExternalAppName());

        final Date now = new Date();

        AssignmentGradeRecord agr = persistence.getAssignmentGradeRecord(asn, studentUid);

        // Try to reduce data contention by only updating when the
        // score has actually changed or property has been set forcing a db update every time.
        final boolean alwaysUpdate = sakaiProxy.isUpdateSameScore();

        // TODO: for ungraded items, needs to set ungraded-grades later...
        final Double oldPointsEarned = (agr == null) ? null : agr.getPointsEarned();
        final Double newPointsEarned = (points == null) ? null : convertStringToDouble(points);
        if (alwaysUpdate || (newPointsEarned != null && !newPointsEarned.equals(oldPointsEarned)) ||
                (newPointsEarned == null && oldPointsEarned != null)) {
            if (agr == null) {
                if (newPointsEarned != null) {
                    agr = new AssignmentGradeRecord(asn, studentUid, Double.valueOf(newPointsEarned));
                } else {
                    agr = new AssignmentGradeRecord(asn, studentUid, null);
                }
            } else {
                if (newPointsEarned != null) {
                    agr.setPointsEarned(Double.valueOf(newPointsEarned));
                } else {
                    agr.setPointsEarned(null);
                }
            }

            agr.setDateRecorded(now);
            agr.setGraderId(sakaiProxy.getCurrentUserId());
            log.debug("About to save AssignmentGradeRecord id={}, version={}, studenttId={}, pointsEarned={}", agr.getId(),
                    agr.getVersion(), agr.getStudentId(), agr.getPointsEarned());
            assignmentGradeRecordRepository.save(agr);

            // Sync database.
            postUpdateGradeEvent(gradebookUid, asn.getName(), studentUid, newPointsEarned);
        } else {
            log.debug("Ignoring updateExternalAssessmentScore, since the new points value is the same as the old");
        }
		log.debug("END: Update 1 score for gradebookUid={}, external assessment={} from {}", gradebookUid, externalId,
				asn.getExternalAppName());
		log.debug("External assessment score updated in gradebookUid={}, externalId={} by userUid={}, new score={}", gradebookUid,
				externalId, sakaiProxy.getCurrentUserId(), points);
	}

    @Override
	public void updateExternalAssessmentScores(final String gradebookUid, final String externalId,
			final Map<String, Double> studentUidsToScores) throws GradebookNotFoundException, AssessmentNotFoundException {

		final GradebookAssignment assignment = getExternalAssignment(gradebookUid, externalId);
		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}
		final Set<String> studentIds = studentUidsToScores.keySet();
		if (studentIds.isEmpty()) {
			return;
		}
		final Date now = new Date();
		final String graderId = sakaiProxy.getCurrentUserId();

        final List<AssignmentGradeRecord> existingScores
            = assignmentGradeRecordRepository.findByGradableObjectAndStudentIdIn(assignment, studentIds);

        final Set<String> previouslyUnscoredStudents = new HashSet<>(studentIds);
        final Set<String> changedStudents = new HashSet<>();
        for (final AssignmentGradeRecord agr : existingScores) {
            final String studentUid = agr.getStudentId();
            previouslyUnscoredStudents.remove(studentUid);

            // Try to reduce data contention by only updating when a score
            // has changed or property has been set forcing a db update every time.
            final boolean alwaysUpdate = sakaiProxy.isUpdateSameScore();

            final Double oldPointsEarned = agr.getPointsEarned();
            final Double newPointsEarned = studentUidsToScores.get(studentUid);
            if (alwaysUpdate || (newPointsEarned != null && !newPointsEarned.equals(oldPointsEarned))
                    || (newPointsEarned == null && oldPointsEarned != null)) {
                agr.setDateRecorded(now);
                agr.setGraderId(graderId);
                agr.setPointsEarned(newPointsEarned);
                assignmentGradeRecordRepository.save(agr);
                changedStudents.add(studentUid);
                postUpdateGradeEvent(gradebookUid, assignment.getName(), studentUid, newPointsEarned);
            }
        }
        for (final String studentUid : previouslyUnscoredStudents) {
            // Don't save unnecessary null scores.
            final Double newPointsEarned = studentUidsToScores.get(studentUid);
            if (newPointsEarned != null) {
                final AssignmentGradeRecord agr = new AssignmentGradeRecord(assignment, studentUid, newPointsEarned);
                agr.setDateRecorded(now);
                agr.setGraderId(graderId);
                assignmentGradeRecordRepository.save(agr);
                changedStudents.add(studentUid);
                postUpdateGradeEvent(gradebookUid, assignment.getName(), studentUid, newPointsEarned);
            }
        }

        log.debug("updateExternalAssessmentScores sent {} records, actually changed {}", studentIds.size(), changedStudents.size());
	}

    @Override
	public void updateExternalAssessmentScoresString(final String gradebookUid, final String externalId,
			final Map<String, String> studentUidsToScores) throws GradebookNotFoundException, AssessmentNotFoundException {

		final GradebookAssignment assignment = getExternalAssignment(gradebookUid, externalId);
		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}
		final Set<String> studentIds = studentUidsToScores.keySet();
		if (studentIds.isEmpty()) {
			return;
		}
		final Date now = new Date();
		final String graderId = sakaiProxy.getCurrentUserId();

        final List<AssignmentGradeRecord> existingScores
            = assignmentGradeRecordRepository.findByGradableObjectAndStudentIdIn(assignment, studentIds);

        final Set<String> previouslyUnscoredStudents = new HashSet<>(studentIds);
        final Set<String> changedStudents = new HashSet<>();
        for (final AssignmentGradeRecord agr : existingScores) {
            final String studentUid = agr.getStudentId();
            previouslyUnscoredStudents.remove(studentUid);

            // Try to reduce data contention by only updating when a score
            // has changed or property has been set forcing a db update every time.
            final boolean alwaysUpdate = sakaiProxy.isUpdateSameScore();

            // TODO: for ungraded items, needs to set ungraded-grades later...
            final Double oldPointsEarned = agr.getPointsEarned();
            final String newPointsEarnedString = studentUidsToScores.get(studentUid);
            final Double newPointsEarned = (newPointsEarnedString == null) ? null : convertStringToDouble(newPointsEarnedString);
            if (alwaysUpdate || (newPointsEarned != null && !newPointsEarned.equals(oldPointsEarned))
                    || (newPointsEarned == null && oldPointsEarned != null)) {
                agr.setDateRecorded(now);
                agr.setGraderId(graderId);
                if (newPointsEarned != null) {
                    agr.setPointsEarned(newPointsEarned);
                } else {
                    agr.setPointsEarned(null);
                }
                assignmentGradeRecordRepository.save(agr);
                changedStudents.add(studentUid);
                postUpdateGradeEvent(gradebookUid, assignment.getName(), studentUid, newPointsEarned);
            }
        }
        for (final String studentUid : previouslyUnscoredStudents) {
            // Don't save unnecessary null scores.
            final String newPointsEarned = studentUidsToScores.get(studentUid);
            if (newPointsEarned != null) {
                final AssignmentGradeRecord agr = new AssignmentGradeRecord(assignment, studentUid,
                        convertStringToDouble(newPointsEarned));
                agr.setDateRecorded(now);
                agr.setGraderId(graderId);
                assignmentGradeRecordRepository.save(agr);
                changedStudents.add(studentUid);
                postUpdateGradeEvent(gradebookUid, assignment.getName(), studentUid, convertStringToDouble(newPointsEarned));
            }
        }

        log.debug("updateExternalAssessmentScores sent {} records, actually changed {}", studentIds.size(), changedStudents.size());
	}

    @Override
	public void updateExternalAssessmentComment(final String gradebookUid, final String externalId, final String studentUid,
			final String comment)
			throws GradebookNotFoundException, AssessmentNotFoundException {

		final GradebookAssignment asn = getExternalAssignment(gradebookUid, externalId);

		if (asn == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}

		log.debug("BEGIN: Update 1 score for gradebookUid={}, external assessment={} from {}", gradebookUid, externalId,
				asn.getExternalAppName());

        // Try to reduce data contention by only updating when the
        // score has actually changed or property has been set forcing a db update every time.
        final boolean alwaysUpdate = sakaiProxy.isUpdateSameScore();

        final CommentDefinition gradeComment = persistence.getAssignmentScoreComment(gradebookUid, asn.getId(), studentUid);
        final String oldComment = gradeComment != null ? gradeComment.getCommentText() : null;

        if (alwaysUpdate || (comment != null && !comment.equals(oldComment)) ||
                (comment == null && oldComment != null)) {
            if (comment != null) {
                setAssignmentScoreComment(gradebookUid, asn.getId(), studentUid, comment);
            } else {
                setAssignmentScoreComment(gradebookUid, asn.getId(), studentUid, null);
            }
            log.debug("updateExternalAssessmentComment: grade record saved");
        } else {
            log.debug("Ignoring updateExternalAssessmentComment, since the new comment is the same as the old");
        }
		log.debug("END: Update 1 score for gradebookUid={}, external assessment={} from {}", gradebookUid, externalId,
				asn.getExternalAppName());
		log.debug("External assessment comment updated in gradebookUid={}, externalId={} by userUid={}, new score={}", gradebookUid,
				externalId, sakaiProxy.getCurrentUserId(), comment);
	}

    @Override
	public void updateExternalAssessmentComments(final String gradebookUid, final String externalId,
			final Map<String, String> studentUidsToComments) throws GradebookNotFoundException, AssessmentNotFoundException {

		final GradebookAssignment asn = getExternalAssignment(gradebookUid, externalId);
		if (asn == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}
		final Set<String> studentIds = studentUidsToComments.keySet();
		if (studentIds.isEmpty()) {
			return;
		}

        final List<AssignmentGradeRecord> existingScores
            = assignmentGradeRecordRepository.findByGradableObjectAndStudentIdIn(asn, studentIds);

        final Set<String> changedStudents = new HashSet<>();
        for (final AssignmentGradeRecord agr : existingScores) {
            final String studentUid = agr.getStudentId();

            // Try to reduce data contention by only updating when a score
            // has changed or property has been set forcing a db update every time.
            final boolean alwaysUpdate = sakaiProxy.isUpdateSameScore();

            final CommentDefinition gradeComment = getAssignmentScoreComment(gradebookUid, asn.getId(), studentUid);
            final String oldComment = gradeComment != null ? gradeComment.getCommentText() : null;
            final String newComment = studentUidsToComments.get(studentUid);

            if (alwaysUpdate || (newComment != null && !newComment.equals(oldComment)) || (newComment == null && oldComment != null)) {
                changedStudents.add(studentUid);
                setAssignmentScoreComment(gradebookUid, asn.getId(), studentUid, newComment);
            }
        }

        log.debug("updateExternalAssessmentScores sent {} records, actually changed {}", studentIds.size(), changedStudents.size());
	}

	@Override
	public boolean isExternalAssignmentDefined(String gradebookUid, String externalId) throws GradebookNotFoundException {

		// SAK-19668
		final GradebookAssignment assignment = getExternalAssignment(gradebookUid, externalId);
		return (assignment != null);
	}

    @Override
	public boolean isExternalAssignmentGrouped(String gradebookUid, String externalId)
			throws GradebookNotFoundException {

		// SAK-19668
		final GradebookAssignment assignment = getExternalAssignment(gradebookUid, externalId);
		// If we check all available providers for an existing, externally maintained assignment
		// and none manage it, return false since grouping is the outlier case and all items
		// showed for all users until the 2.9 release.
		boolean result = false;
		boolean providerResponded = false;
		if (assignment == null) {
			log.info("No assignment found for external assignment check: gradebookUid=" + gradebookUid + ", externalId=" + externalId);
		} else {
			for (ExternalAssignmentProvider provider : getExternalAssignmentProviders().values()) {
				if (provider.isAssignmentDefined(assignment.getExternalAppName(), externalId)) {
					providerResponded = true;
					result = result || provider.isAssignmentGrouped(externalId);
				}
			}
		}
		return result || !providerResponded;
	}

    @Override
	public boolean isExternalAssignmentVisible(final String gradebookUid, final String externalId, final String userId)
			throws GradebookNotFoundException {

		// SAK-19668
		final GradebookAssignment assignment = getExternalAssignment(gradebookUid, externalId);
		// If we check all available providers for an existing, externally maintained assignment
		// and none manage it, assume that it should be visible. This matches the pre-2.9 behavior
		// when a provider is not implemented to handle the assignment. Also, any provider that
		// returns true will allow access (logical OR of responses).
		boolean result = false;
		boolean providerResponded = false;
		if (assignment == null) {
			log.info("No assignment found for external assignment check: gradebookUid=" + gradebookUid + ", externalId=" + externalId);
		} else {
			for (final ExternalAssignmentProvider provider : getExternalAssignmentProviders().values()) {
				if (provider.isAssignmentDefined(assignment.getExternalAppName(), externalId)) {
					providerResponded = true;
					result = result || provider.isAssignmentVisible(externalId, userId);
				}
			}
		}
		return result || !providerResponded;
	}

    @Override
	public Map<String, String> getExternalAssignmentsForCurrentUser(final String gradebookUid)
			throws GradebookNotFoundException {

		final Map<String, String> visibleAssignments = new HashMap<>();
		final Set<String> providedAssignments = getProvidedExternalAssignments(gradebookUid);

		for (final ExternalAssignmentProvider provider : getExternalAssignmentProviders().values()) {
			final String appKey = provider.getAppKey();
			final List<String> assignments = provider.getExternalAssignmentsForCurrentUser(gradebookUid);
			for (final String externalId : assignments) {
				visibleAssignments.put(externalId, appKey);
			}
		}

		// We include those items that the gradebook has marked as externally maintained, but no provider has
		// identified as items under its authority. This maintains the behavior prior to the grouping support
		// introduced for the 2.9 release (SAK-11485 and SAK-19688), where a tool that does not have a provider
		// implemented does not have its items filtered for student views and grading.
		List<Assignment> gbAssignments = getViewableAssignmentsForCurrentUser(gradebookUid);
		for (Assignment assignment : gbAssignments) {
			String id = assignment.getExternalId();
			if (assignment.isExternallyMaintained() && !providedAssignments.contains(id) && !visibleAssignments.containsKey(id)) {
				log.debug("External assignment in gradebook [{}] is not handled by a provider; ID: {}", gradebookUid, id);
				visibleAssignments.put(id, null);
			}
		}

		return visibleAssignments;
	}

    @Override
	public void registerExternalAssignmentProvider(final ExternalAssignmentProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider cannot be null");
		} else {
			getExternalAssignmentProviders().put(provider.getAppKey(), provider);

			// Try to duck-type the provider so it doesn't have to declare the Compat interface.
			// TODO: Remove this handling once the Compat interface has been merged or the issue is otherwise resolved.
			if (!(provider instanceof ExternalAssignmentProviderCompat)) {
				try {
					final Method m = provider.getClass().getDeclaredMethod("getAllExternalAssignments", String.class);
					if (m.getReturnType().equals(List.class)) {
						this.providerMethods.put(provider, m);
					}
				} catch (final Exception e) {
					log.warn("ExternalAssignmentProvider [" + provider.getAppKey() + " / " + provider.getClass().toString()
							+ "] does not implement getAllExternalAssignments. It will not be able to exclude items from student views/grades. "
							+ "See the ExternalAssignmentProviderCompat interface and SAK-23733 for details.");
				}
			}
		}
	}

	@Override
	public void unregisterExternalAssignmentProvider(final String providerAppKey) {

		if (providerAppKey == null || "".equals(providerAppKey)) {
			throw new IllegalArgumentException("providerAppKey must be set");
		} else if (getExternalAssignmentProviders().containsKey(providerAppKey)) {
			final ExternalAssignmentProvider provider = getExternalAssignmentProviders().get(providerAppKey);
			this.providerMethods.remove(provider);
			getExternalAssignmentProviders().remove(providerAppKey);
		}
	}

    @Override
	public void setExternalAssessmentToGradebookAssignment(final String gradebookUid, final String externalId) {

		final GradebookAssignment assignment = getExternalAssignment(gradebookUid, externalId);
		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
		}
		assignment.setExternalAppName(null);
		assignment.setExternalId(null);
		assignment.setExternalInstructorLink(null);
		assignment.setExternalStudentLink(null);
		assignment.setExternalData(null);
		assignment.setExternallyMaintained(false);
        gradebookAssignmentRepository.save(assignment);
        log.info("Externally-managed assignment {} moved to Gradebook management in gradebookUid={} by userUid={}", externalId,
                gradebookUid, sakaiProxy.getCurrentUserId());
	}

    @Override
	public Long getExternalAssessmentCategoryId(final String gradebookUId, final String externalId) {

		Long categoryId = null;
		final GradebookAssignment assignment = getExternalAssignment(gradebookUId, externalId);
		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUId);
		}
		if (assignment.getCategory() != null) {
			categoryId = assignment.getCategory().getId();
		}
		return categoryId;
	}


}
