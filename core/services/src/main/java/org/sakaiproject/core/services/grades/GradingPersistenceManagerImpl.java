package org.sakaiproject.core.services.grades;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.hibernate.HibernateCriterionUtils;
import org.sakaiproject.section.api.SectionAwareness;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.section.api.facade.Role;

import org.sakaiproject.core.api.grades.AssessmentNotFoundException;
import org.sakaiproject.core.api.grades.CommentDefinition;
import org.sakaiproject.core.api.grades.ConflictingAssignmentNameException;
import org.sakaiproject.core.api.grades.ConflictingCategoryNameException;
import org.sakaiproject.core.api.grades.GradebookHelper;
import org.sakaiproject.core.api.grades.GradebookNotFoundException;
import org.sakaiproject.core.api.grades.SakaiProxy;

import org.sakaiproject.core.api.grades.GraderPermission;
import org.sakaiproject.core.api.grades.GradingPersistenceManager;
import org.sakaiproject.core.api.grades.StaleObjectModificationException;
import org.sakaiproject.core.persistence.grades.model.AbstractGradeRecord;
import org.sakaiproject.core.persistence.grades.model.GradebookAssignment;
import org.sakaiproject.core.persistence.grades.model.AssignmentGradeRecord;
import org.sakaiproject.core.persistence.grades.model.Category;
import org.sakaiproject.core.persistence.grades.model.Comment;
import org.sakaiproject.core.persistence.grades.model.CourseGrade;
import org.sakaiproject.core.persistence.grades.model.CourseGradeRecord;
import org.sakaiproject.core.persistence.grades.model.GradableObject;
import org.sakaiproject.core.persistence.grades.model.GradeMapping;
import org.sakaiproject.core.persistence.grades.model.Gradebook;
import org.sakaiproject.core.persistence.grades.model.GradebookProperty;
import org.sakaiproject.core.persistence.grades.model.GradingEvent;
import org.sakaiproject.core.persistence.grades.model.LetterGradePercentMapping;
import org.sakaiproject.core.persistence.grades.model.Permission;
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
import org.sakaiproject.core.persistence.grades.repository.GradingScaleRepository;
import org.sakaiproject.core.persistence.grades.repository.LetterGradePercentMappingRepository;
import org.sakaiproject.core.persistence.grades.repository.PermissionRepository;
import org.sakaiproject.core.utils.grades.GradingConstants;
import org.sakaiproject.event.api.EventTrackingService;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

 /**
 * Provides methods which are shared between service business logic and application business
 * logic, but not exposed to external callers.
 */
@Service
@Slf4j
public class GradingPersistenceManagerImpl implements GradingPersistenceManager {

    @Resource
    private SectionAwareness sectionAwareness;

    @Resource
    protected EventTrackingService eventTrackingService;

    @Resource
    protected ServerConfigurationService serverConfigurationService;

    @Resource
    private AssignmentGradeRecordRepository assignmentGradeRecordRepository;

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private CourseGradeRecordRepository courseGradeRecordRepository;

    @Resource
    private CourseGradeRepository courseGradeRepository;

    @Resource
    private GradableObjectRepository gradableObjectRepository;

    @Resource
    private GradebookRepository gradebookRepository;

    @Resource
    private GradebookAssignmentRepository gradebookAssignmentRepository;

    @Resource
    private GradebookPropertyRepository gradebookPropertyRepository;

    @Resource
    private GradeMappingRepository gradeMappingRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private LetterGradePercentMappingRepository letterGradePercentMappingRepository;

    @Resource
    private PermissionRepository permissionRepository;

    @Resource
    private SakaiProxy sakaiProxy;

    //protected GradebookExternalAssessmentService externalAssessmentService;

    // Local cache of static-between-deployment properties.
	protected Map<String, String> propertiesMap = new HashMap<>();

    public Gradebook getGradebook(Long id) throws GradebookNotFoundException {

        Gradebook gradebook = gradebookRepository.findOne(id);

        if (gradebook == null) {
            throw new GradebookNotFoundException("Could not find gradebook id=" + id);
        }

        return gradebook;
    }

    public Gradebook getGradebook(String uid) throws GradebookNotFoundException {

        Gradebook gradebook = gradebookRepository.findByUid(uid);

        if (gradebook == null) {
            throw new GradebookNotFoundException("Could not find gradebook uid=" + uid);
        }

        return gradebook;
    }

    public boolean isGradebookDefined(String gradebookUid) {
        return gradebookRepository.existsByUid(gradebookUid);
    }

    public List<GradebookAssignment> getAssignments(Long gradebookId) {
        return gradebookAssignmentRepository.findByGradebook_IdAndRemovedIsFalse(gradebookId);
    }

    protected List getCountedStudentGradeRecords(final Long gradebookId, final String studentId) throws HibernateException {
        /*
        return getSessionFactory().getCurrentSession().createQuery(
        	"select agr from AssignmentGradeRecord as agr, GradebookAssignment as asn where agr.studentId = :studentid and agr.gradableObject = asn and asn.removed is false and asn.notCounted is false and asn.gradebook.id = :gradebookid and asn.ungraded is false")
        	.setString("studentid", studentId)
        	.setLong("gradebookid", gradebookId)
        	.list();
        */
        return new ArrayList();
    }

    /**
     */
    public CourseGrade getCourseGrade(Long gradebookId) {
        return courseGradeRepository.findOneByGradebook_Id(gradebookId);
    }

    /**
     * Gets the course grade record for a student, or null if it does not yet exist.
     *
     * @param studentId The student ID
     * @return A List of grade records
     *
     * @throws HibernateException
     */
    protected CourseGradeRecord getCourseGradeRecord(final Gradebook gradebook, final String studentId) throws HibernateException {
        return courseGradeRecordRepository.findOneByStudentIdAndGradableObject_Gradebook(studentId, gradebook);
    }

    public String getGradebookUid(Long id) {
        return gradebookRepository.findOne(id).getUid();
    }

	private Set<String> getAllStudentUids(String gradebookUid) {

		List<EnrollmentRecord> enrollments = sectionAwareness.getSiteMembersInRole(gradebookUid, Role.STUDENT);
        return enrollments.stream().map(e -> e.getUser().getUserUid()).collect(Collectors.toSet());
	}

    public String getPropertyValue(final String name) {

        String value = this.propertiesMap.get(name);
		if (value == null) {
            List<GradebookProperty> list = gradebookPropertyRepository.findByName(name);
			if (!list.isEmpty()) {
				final GradebookProperty property = list.get(0);
				value = property.getValue();
				this.propertiesMap.put(name, value);
			}
		}
		return value;
	}

	public void setPropertyValue(String name, String value) {

		List<GradebookProperty> list = gradebookPropertyRepository.findByName(name);
		GradebookProperty property = (list.isEmpty()) ? new GradebookProperty(name) : list.get(0);
		property.setValue(value);
		this.gradebookPropertyRepository.save(property);
		this.propertiesMap.put(name, value);
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
    /*
	protected List filterGradeRecordsByStudents(final Collection gradeRecords, final Collection studentUids) {
		final List filteredRecords = new ArrayList();
		for (final Iterator iter = gradeRecords.iterator(); iter.hasNext(); ) {
			final AbstractGradeRecord agr = (AbstractGradeRecord)iter.next();
			if (studentUids.contains(agr.getStudentId())) {
				filteredRecords.add(agr);
			}
		}
		return filteredRecords;
	}
    */

	@Deprecated
	public GradebookAssignment getAssignmentWithoutStats(String gradebookUid, String assignmentName) {
        return gradebookAssignmentRepository.findOneByNameAndGradebook_UidAndRemovedIsFalse(assignmentName, gradebookUid);
	}

	public GradebookAssignment getAssignmentWithoutStats(String gradebookUid, Long assignmentId) {
        return gradebookAssignmentRepository.findOneByIdAndGradebook_UidAndRemovedIsFalse(assignmentId, gradebookUid);
	}

	protected void updateAssignment(final GradebookAssignment assignment) throws ConflictingAssignmentNameException, HibernateException {
		// Ensure that we don't have the assignment in the session, since
		// we need to compare the existing one in the db to our edited assignment
        //final Session session = getSessionFactory().getCurrentSession();
		//session.evict(assignment);

		final GradebookAssignment asnFromDb = gradebookAssignmentRepository.findOne(assignment.getId());

        final Long count
            = gradableObjectRepository.countByNameAndGradebookAndIdNotAndRemovedIsFalse(
                assignment.getName(),assignment.getGradebook(),assignment.getId());

		if(count > 0) {
			throw new ConflictingAssignmentNameException("You can not save multiple assignments in a gradebook with the same name");
		}

		//session.evict(asnFromDb);
		gradebookAssignmentRepository.save(assignment);
	}

    public AssignmentGradeRecord getAssignmentGradeRecord(GradebookAssignment assignment, String studentId) {
        return assignmentGradeRecordRepository.findOneByStudentIdAndGradableObject_Id(studentId, assignment.getId());
	}

    public Long createAssignment(Long gradebookId, String name, Double points, Date dueDate, Boolean isNotCounted,
           Boolean isReleased, Boolean isExtraCredit, Integer sortOrder) throws ConflictingAssignmentNameException, StaleObjectModificationException {
        return createNewAssignment(gradebookId, null, name, points, dueDate, isNotCounted, isReleased, isExtraCredit, sortOrder, null);
    }

    public Long createAssignmentForCategory(Long gradebookId, Long categoryId, String name, Double points, Date dueDate, Boolean isNotCounted, 
           Boolean isReleased, Boolean isExtraCredit, Integer categorizedSortOrder)
    throws ConflictingAssignmentNameException, StaleObjectModificationException, IllegalArgumentException {
    	if (gradebookId == null || categoryId == null) {
    		throw new IllegalArgumentException("gradebookId or categoryId is null in BaseHibernateManager.createAssignmentForCategory");
    	}

        return createNewAssignment(gradebookId, categoryId, name, points, dueDate, isNotCounted, isReleased, isExtraCredit, null, categorizedSortOrder);
    }

    private Long createNewAssignment(Long gradebookId, Long categoryId, String name, Double points, Date dueDate, Boolean isNotCounted,
            Boolean isReleased, Boolean isExtraCredit, Integer sortOrder, Integer categorizedSortOrder) 
                    throws ConflictingAssignmentNameException, StaleObjectModificationException {
        GradebookAssignment asn = prepareNewAssignment(name, points, dueDate, isNotCounted, isReleased, isExtraCredit, sortOrder, categorizedSortOrder);

        return saveNewAssignment(gradebookId, categoryId, asn);
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

    private void loadAssignmentGradebookAndCategory(GradebookAssignment asn, Long gradebookId, Long categoryId) {

        asn.setGradebook(getGradebook(gradebookId));
        if (categoryId != null) {
            asn.setCategory(categoryRepository.findOne(categoryId));
        }
    }

    protected Long saveNewAssignment(Long gradebookId, Long categoryId, GradebookAssignment asn) throws ConflictingAssignmentNameException {

        loadAssignmentGradebookAndCategory(asn, gradebookId, categoryId);

        if (assignmentNameExists(asn.getName(), asn.getGradebook())) {
            throw new ConflictingAssignmentNameException("You cannot save multiple assignments in a gradebook with the same name");
        }

        return gradebookAssignmentRepository.save(asn).getId();
    }

    public void updateGradebook(Gradebook gradebook) throws StaleObjectModificationException {

        // Get the gradebook and selected mapping from persistence
        Gradebook gradebookFromPersistence = gradebookRepository.findOne(gradebook.getId());
        GradeMapping mappingFromPersistence = gradebookFromPersistence.getSelectedGradeMapping();

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
        /*
        for (GradeMapping mapping : gradebookFromPersistence.getGradeMappings()) {
            session.evict(mapping);
        }
        session.evict(gradebookFromPersistence);
        */
        try {
            gradebookRepository.save(gradebook);
        } catch (StaleObjectStateException e) {
            throw new StaleObjectModificationException(e);
        }
    }

    public boolean isExplicitlyEnteredCourseGradeRecords(final Long gradebookId) {

        final List<String> studentIds = new ArrayList<>(getAllStudentUids(getGradebookUid(gradebookId)));

        if (studentIds.isEmpty()) {
            return false;
        }

        return courseGradeRecordRepository.countByGradableObject_Gradebook_IdAndEnteredGradeNotNullAndStudentIdIn(
            gradebookId, studentIds) > 0L;
    }

	public void postEvent(final String event, final String objectReference) {
		this.eventTrackingService.post(this.eventTrackingService.newEvent(event, objectReference, true));
    }


    public Long createCategory(final Long gradebookId, final String name, final Double weight, final Integer drop_lowest,
                               final Integer dropHighest, final Integer keepHighest, final Boolean is_extra_credit) {
        return createCategory(gradebookId, name, weight, drop_lowest, dropHighest, keepHighest, is_extra_credit, null);
    }

    public Long createCategory(final Long gradebookId, final String name, final Double weight, final Integer drop_lowest,
                               final Integer dropHighest, final Integer keepHighest, final Boolean is_extra_credit,
                               final Integer categoryOrder) throws ConflictingCategoryNameException, StaleObjectModificationException {

        if (weight > 1 || weight < 0) {
            throw new IllegalArgumentException("weight for category is greater than 1 or less than 0 in createCategory of BaseHibernateManager");
        }

        if (((drop_lowest != null && drop_lowest > 0) || (dropHighest != null && dropHighest > 0)) && (keepHighest != null && keepHighest > 0)) {
            throw new IllegalArgumentException("a combination of positive values for keepHighest and either drop_lowest or dropHighest occurred in createCategory of BaseHibernateManager");
        }

        Gradebook gb = gradebookRepository.findOne(gradebookId);
        long numNameConflicts = categoryRepository.countByNameAndGradebookAndRemovedIsFalse(name, gb);

        if (numNameConflicts > 0L) {
            throw new ConflictingCategoryNameException("You can not save multiple catetories in a gradebook with the same name");
        }


        Category ca = new Category();
        ca.setGradebook(gb);
        ca.setName(name);
        ca.setWeight(weight);
        ca.setDropLowest(drop_lowest);
        ca.setDropHighest(dropHighest);
        ca.setKeepHighest(keepHighest);
        ca.setRemoved(false);
        ca.setExtraCredit(is_extra_credit);
        ca.setCategoryOrder(categoryOrder);

        return  categoryRepository.save(ca).getId();
    }

    public List<Category> getCategories(Long gradebookId) throws HibernateException {
        return categoryRepository.findByGradebook_IdAndRemovedIsFalse(gradebookId);
    }

    public List<Category> getCategoriesWithAssignments(Long gradebookId) {

    	List<Category> categories = getCategories(gradebookId);

        if (categories == null) {
            return Collections.emptyList();
        }

        categories.stream().filter(c -> c != null).forEach(c -> {
            List<GradebookAssignment> assignments = getAssignmentsForCategory(c.getId());
            c.setAssignmentList(assignments);
        });//.collect(Collectors.toList());
        return categories;
    }

    public List<GradebookAssignment> getAssignmentsForCategory(Long categoryId) throws HibernateException{
        return gradebookAssignmentRepository.findByCategory_IdAndRemovedIsFalse(categoryId);
    }

    public Category getCategory(Long categoryId) throws HibernateException{
        return categoryRepository.findOne(categoryId);
    }

    public void updateCategory(Category category) throws ConflictingCategoryNameException, StaleObjectModificationException {

        if (category.getWeight().doubleValue() > 1 || category.getWeight().doubleValue() < 0) {
            throw new IllegalArgumentException("weight for category is greater than 1 or less than 0 in updateCategory of BaseHibernateManager");
        }

        //session.evict(category);
        //final persistentCat = categoryRepository.findOne(categoryId);
        long numNameConflicts
            = categoryRepository.countByNameAndGradebookAndIdAndRemovedIsFalse(
                category.getName(), category.getGradebook(), category.getId());

        if (numNameConflicts > 0L) {
            throw new ConflictingCategoryNameException("You can not save multiple category in a gradebook with the same name");
        }
        //session.evict(persistentCat);
        try {
            categoryRepository.save(category);
    	} catch (Exception e) {
    		throw new StaleObjectModificationException(e);
    	}
    }

    public void removeCategory(final Long categoryId) throws StaleObjectModificationException{

        getAssignmentsForCategory(categoryId).forEach(a -> { a.setCategory(null); updateAssignment(a); });

        Category category = categoryRepository.findOne(categoryId);

        category.setRemoved(true);

    	try {
            categoryRepository.save(category);
    	} catch (final Exception e) {
    		throw new StaleObjectModificationException(e);
    	}
    }

    public LetterGradePercentMapping getDefaultLetterGradePercentMapping() {


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

    public void createOrUpdateDefaultLetterGradePercentMapping(Map<String, Double> gradeMap) {

    	if (gradeMap == null) {
			throw new IllegalArgumentException("gradeMap is null in BaseHibernateManager.createOrUpdateDefaultLetterGradePercentMapping");
		}

    	LetterGradePercentMapping lgpm = getDefaultLetterGradePercentMapping();

    	if (lgpm != null) {
    		updateDefaultLetterGradePercentMapping(gradeMap, lgpm);
    	} else {
    		createDefaultLetterGradePercentMapping(gradeMap);
    	}
    }

    private void updateDefaultLetterGradePercentMapping(Map<String, Double> gradeMap, LetterGradePercentMapping lgpm) {

  		if (!validateLetterGradeMapping(gradeMap)) {
			throw new IllegalArgumentException("gradeMap contains invalid letter in BaseHibernateManager.updateDefaultLetterGradePercentMapping");
		}

  		Set<String> keySet = gradeMap.keySet();

  		if (keySet.size() != GradingConstants.validLetterGrade.length) {
			throw new IllegalArgumentException("gradeMap doesn't have right size in BaseHibernateManager.updateDefaultLetterGradePercentMapping");
		}

        Map<String, Double> saveMap = new HashMap<>(gradeMap);
        lgpm.setGradeMap(saveMap);
        letterGradePercentMappingRepository.save(lgpm);
    }

    public void createDefaultLetterGradePercentMapping(final Map<String, Double> gradeMap) {

    	if (gradeMap == null) {
			throw new IllegalArgumentException("gradeMap is null in BaseHibernateManager.createDefaultLetterGradePercentMapping");
		}

    	if (getDefaultLetterGradePercentMapping() != null) {
			throw new IllegalArgumentException("gradeMap has already been created in BaseHibernateManager.createDefaultLetterGradePercentMapping");
		}

    	final Set<String> keySet = gradeMap.keySet();

    	if (keySet.size() != GradingConstants.validLetterGrade.length) {
			throw new IllegalArgumentException("gradeMap doesn't have right size in BaseHibernateManager.createDefaultLetterGradePercentMapping");
		}

    	if (!validateLetterGradeMapping(gradeMap)) {
			throw new IllegalArgumentException("gradeMap contains invalid letter in BaseHibernateManager.createDefaultLetterGradePercentMapping");
		}

        LetterGradePercentMapping lgpm = new LetterGradePercentMapping();
        Map<String, Double> saveMap = new HashMap<>(gradeMap);
        lgpm.setGradeMap(saveMap);
        lgpm.setMappingType(1);
        letterGradePercentMappingRepository.save(lgpm);
    }

    public LetterGradePercentMapping getLetterGradePercentMapping(Gradebook gradebook) {

        LetterGradePercentMapping mapping
            = letterGradePercentMappingRepository.findByGradebookIdAndMappingType(gradebook.getId(), 2);
        if (mapping == null) {
            mapping = new LetterGradePercentMapping();
            mapping.setGradebookId(gradebook.getId());
            mapping.setGradeMap(getDefaultLetterGradePercentMapping().getGradeMap());
            mapping.setMappingType(2);
        }
        return mapping;
    }

    /**
     * this method is different with getLetterGradePercentMapping -
     * it returns null if no mapping exists for gradebook instead of
     * returning default mapping.
     */
    private LetterGradePercentMapping getLetterGradePercentMappingForGradebook(Gradebook gradebook) {
        return letterGradePercentMappingRepository.findByGradebookIdAndMappingType(gradebook.getId(), 2);
    }

    public void saveOrUpdateLetterGradePercentMapping(Map<String, Double> gradeMap, Gradebook gradebook) {

    	if (gradeMap == null) {
            throw new IllegalArgumentException("gradeMap is null in BaseHibernateManager.saveOrUpdateLetterGradePercentMapping");
        }

    	LetterGradePercentMapping lgpm = getLetterGradePercentMappingForGradebook(gradebook);

    	if (lgpm == null) {
    		Set<String> keySet = gradeMap.keySet();

    		if (keySet.size() != GradingConstants.validLetterGrade.length) { //we only consider letter grade with -/+ now.
                throw new IllegalArgumentException("gradeMap doesn't have right size in BaseHibernateManager.saveOrUpdateLetterGradePercentMapping");
            }

    		if (!validateLetterGradeMapping(gradeMap)) {
                throw new IllegalArgumentException("gradeMap contains invalid letter in BaseHibernateManager.saveOrUpdateLetterGradePercentMapping");
            }

            LetterGradePercentMapping lgpm1 = new LetterGradePercentMapping();
            Map<String, Double> saveMap = new HashMap<>(gradeMap);
            lgpm1.setGradeMap(saveMap);
            lgpm1.setGradebookId(gradebook.getId());
            lgpm1.setMappingType(2);
            letterGradePercentMappingRepository.save(lgpm1);
    	} else {
    		updateLetterGradePercentMapping(gradeMap, gradebook);
    	}
    }

    private void updateLetterGradePercentMapping(Map<String, Double> gradeMap, Gradebook gradebook) {

        LetterGradePercentMapping lgpm = getLetterGradePercentMapping(gradebook);

        if (lgpm == null) {
            throw new IllegalArgumentException("LetterGradePercentMapping is null in BaseHibernateManager.updateLetterGradePercentMapping");
        }
        if (gradeMap == null) {
            throw new IllegalArgumentException("gradeMap is null in BaseHibernateManager.updateLetterGradePercentMapping");
        }

        Set<String> keySet = gradeMap.keySet();

        if (keySet.size() != GradingConstants.validLetterGrade.length) { //we only consider letter grade with -/+ now.
            throw new IllegalArgumentException("gradeMap doesn't have right size in BaseHibernateManager.udpateLetterGradePercentMapping");
        }
        if (validateLetterGradeMapping(gradeMap) == false) {
            throw new IllegalArgumentException("gradeMap contains invalid letter in BaseHibernateManager.udpateLetterGradePercentMapping");
        }
        Map<String, Double> saveMap = new HashMap<>(gradeMap);
        lgpm.setGradeMap(saveMap);
        letterGradePercentMappingRepository.save(lgpm);
    }

    protected boolean validateLetterGradeMapping(Map<String, Double> gradeMap) {

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

    public Long createUngradedAssignment(Long gradebookId, String name, Date dueDate, Boolean isNotCounted,
                Boolean isReleased) throws ConflictingAssignmentNameException, StaleObjectModificationException {

        Gradebook gb = gradebookRepository.findOne(gradebookId);

        String trimmedName = StringUtils.trimToEmpty(name);

        if (assignmentNameExists(trimmedName, gb)) {
            throw new ConflictingAssignmentNameException("You can not save multiple assignments in a gradebook with the same name");
        }

        GradebookAssignment asn = new GradebookAssignment();
        asn.setGradebook(gb);
        asn.setName(trimmedName);
        asn.setDueDate(dueDate);
        asn.setUngraded(true);
        if (isNotCounted != null) {
            asn.setNotCounted(isNotCounted);
        }
        if (isReleased != null) {
            asn.setReleased(isReleased);
        }

        return gradebookAssignmentRepository.save(asn).getId();
    }

    public Long createUngradedAssignmentForCategory(Long gradebookId, Long categoryId, String name, Date dueDate, Boolean isNotCounted,
                                                    Boolean isReleased) throws ConflictingAssignmentNameException, StaleObjectModificationException, IllegalArgumentException {

        if (gradebookId == null || categoryId == null) {
            throw new IllegalArgumentException("gradebookId or categoryId is null in BaseHibernateManager.createUngradedAssignmentForCategory");
    	}

        Gradebook gb = gradebookRepository.findOne(gradebookId);
        Category cat = categoryRepository.findOne(categoryId);

        // trim the name before the validation
        String trimmedName = StringUtils.trimToEmpty(name);

        if (assignmentNameExists(trimmedName, gb)) {
            throw new ConflictingAssignmentNameException("You can not save multiple assignments in a gradebook with the same name");
        }

        GradebookAssignment asn = new GradebookAssignment();
        asn.setGradebook(gb);
        asn.setCategory(cat);
        asn.setName(trimmedName);
        asn.setDueDate(dueDate);
        asn.setUngraded(true);
        if (isNotCounted != null) {
            asn.setNotCounted(isNotCounted);
        }
        if (isReleased != null) {
            asn.setReleased(isReleased);
        }

        return gradebookAssignmentRepository.save(asn).getId();
    }

    public Long addPermission(Long gradebookId, String userId, String function, Long categoryId,
                              String groupId) throws IllegalArgumentException {

        if (gradebookId == null || userId == null || function == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.addPermission");
        }
        if (!function.equalsIgnoreCase(GradingConstants.gradePermission)
                && !function.equalsIgnoreCase(GradingConstants.viewPermission)
                && !function.equalsIgnoreCase(GradingConstants.noPermission)) {
            throw new IllegalArgumentException("Function is not grade, view or none in BaseHibernateManager.addPermission");
        }
        Permission permission = new Permission();
        permission.setCategoryId(categoryId);
        permission.setGradebookId(gradebookId);
        permission.setGroupId(groupId);
        permission.setFunction(function);
        permission.setUserId(userId);

        return permissionRepository.save(permission).getId();
    }

    @Deprecated
    public List<Permission> getPermissionsForGB(Long gradebookId) throws IllegalArgumentException {

        if (gradebookId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForGB");
        }

        return permissionRepository.findByGradebookId(gradebookId);
    }

    @Deprecated
    public void updatePermission(Collection perms) {

    	for (Iterator iter = perms.iterator(); iter.hasNext();) {
    		Permission perm = (Permission) iter.next();
    		if (perm != null) {
				updatePermission(perm);
			}
    	}
    }

    @Deprecated
    public void updatePermission(Permission perm) throws IllegalArgumentException {

        if (perm == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.updatePermission");
        }

        if (perm.getId() == null) {
            throw new IllegalArgumentException("Object is not persistent in BaseHibernateManager.updatePermission");
        }

        permissionRepository.save(perm);
    }

    public void replacePermissions(List<Permission> currentPermissions, List<Permission> newPermissions) {

        permissionRepository.delete(currentPermissions);
        newPermissions.forEach(p -> permissionRepository.save(p));
    }

    public void deletePermissionsForUser(Long gradebookId, String userId) {
        permissionRepository.deleteByGradebookIdAndUserId(gradebookId, userId);
    }

    @Deprecated
    public void deletePermission(Permission perm) throws IllegalArgumentException {

        if (perm == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.deletePermission");
        }
        if (perm.getId() == null) {
            throw new IllegalArgumentException("Object is not persistent in BaseHibernateManager.deletePermission");
        }

        permissionRepository.delete(perm);
    }

    public List<Permission> getPermissionsForUser(Long gradebookId, String userId) throws IllegalArgumentException {

    	if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUser");
        }

        return permissionRepository.findByGradebookIdAndUserId(gradebookId, userId);
    }

    public List<Permission> getPermissionsForUserForCategory(Long gradebookId, String userId, List categoryIds)
        throws IllegalArgumentException {

    	if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserForCategory");
        }

        if (categoryIds != null && categoryIds.size() > 0) {
            return permissionRepository.findByGradebookIdAndUserIdAndCategoryIdIn(gradebookId, userId, categoryIds);
    	}
        return null;
    }

    public List<Permission> getPermissionsForUserAnyCategory(final Long gradebookId, final String userId) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserAnyCategory");
        }

        return permissionRepository.findByGradebookIdAndUserIdAndCategoryIdIsNullAndFunctionIn(
                gradebookId, userId, GraderPermission.getStandardPermissions());
    }

    public List<Permission> getPermissionsForUserAnyGroup(Long gradebookId, String userId)
        throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserAnyGroup");
        }

        return permissionRepository.findByGradebookIdAndUserIdAndGroupIdIsNullAndFunctionIn(
                gradebookId, userId, GraderPermission.getStandardPermissions());
    }

    public List<Permission> getPermissionsForUserAnyGroupForCategory(Long gradebookId, String userId, List<Long> categoryIds)
        throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserAnyGroupForCategory");
        }

        if (categoryIds != null && categoryIds.size() > 0) {
            return permissionRepository.findByGradebookIdAndUserIdAndCategoryIdInAndGroupIdIsNull(gradebookId, userId, categoryIds);
    	}
        return null;
    }

    public List<Permission> getPermissionsForGBForCategoryIds(Long gradebookId, List<Long> categoryIds) throws IllegalArgumentException {

        if (gradebookId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserAnyGroupForCategory");
        }

        if (categoryIds != null && categoryIds.size() > 0) {
            return permissionRepository.findByGradebookIdAndCategoryIdIn(gradebookId, categoryIds);
    	}
        return null;
    }

    public List<Permission> getPermissionsForUserAnyGroupAnyCategory(final Long gradebookId, final String userId) throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserAnyGroupForCategory");
        }

        return permissionRepository.findByGradebookIdAndUserIdAndCategoryIdIsNullAndGroupIdIsNull(gradebookId, userId);
    }

    public List<Permission> getPermissionsForUserForGoupsAnyCategory(Long gradebookId, String userId, List<String> groupIds)
        throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserForGoupsAnyCategory");
        }
    	if (groupIds != null && groupIds.size() > 0) {
            return permissionRepository.findByGradebookIdAndUserIdAndCategoryIdIsNullAndGroupIdIn(gradebookId, userId, groupIds);
    	}
        return null;
    }

    public List getPermissionsForUserForGroup(Long gradebookId, String userId, List<String> groupIds)
        throws IllegalArgumentException {

        if (gradebookId == null || userId == null) {
            throw new IllegalArgumentException("Null parameter(s) in BaseHibernateManager.getPermissionsForUserForGroup");
        }

    	if (groupIds != null && groupIds.size() > 0) {
            return permissionRepository.findByGradebookIdAndUserIdAndGroupIdIn(gradebookId, userId, groupIds);
    	}

        return null;
    }

    public boolean isAssignmentDefined(Long gradableObjectId) {
        return gradebookAssignmentRepository.countByIdAndRemovedIsFalse(gradableObjectId) == 1L;
    }

    /**
     *
     * @param gradableObjectId
     * @return the GradebookAssignment object with the given id
     */
    public GradebookAssignment getAssignment(final Long gradableObjectId) {
        return gradebookAssignmentRepository.findOne(gradableObjectId);
    }

    /**
     *
     * @param doublePointsPossible
     * @param doublePointsEarned
     * @return the % equivalent for the given points possible and points earned
     */
    protected Double calculateEquivalentPercent(final Double doublePointsPossible, final Double doublePointsEarned) {

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
     * Converts points to percentage for the given AssignmentGradeRecords
     * @param gradebook
     * @param studentRecordsFromDB
     * @return
     */
    protected List<AssignmentGradeRecord> convertPointsToPercentage(Gradebook gradebook, List<AssignmentGradeRecord> studentRecordsFromDB) {

    	List<AssignmentGradeRecord> percentageList = new ArrayList<>();
    	for (AssignmentGradeRecord agr : studentRecordsFromDB) {
    		if (agr != null) {
    			Double pointsPossible = agr.getAssignment().getPointsPossible();
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

    /**
     * Converts points to letter grade for the given AssignmentGradeRecords
     * @param gradebook
     * @param studentRecordsFromDB
     * @return
     */
    protected List convertPointsToLetterGrade(final Gradebook gradebook, final List<AssignmentGradeRecord> studentRecordsFromDB) {

        final List<AssignmentGradeRecord> letterGradeList = new ArrayList<>();
        final LetterGradePercentMapping lgpm = getLetterGradePercentMapping(gradebook);
        for (AssignmentGradeRecord agr : studentRecordsFromDB) {
            if (agr != null) {
                final Double pointsPossible = agr.getAssignment().getPointsPossible();
                agr.setDateRecorded(agr.getDateRecorded());
                agr.setGraderId(agr.getGraderId());
                if (pointsPossible == null || agr.getPointsEarned() == null) {
                    agr.setLetterEarned(null);
                    letterGradeList.add(agr);
                } else {
                    final String letterGrade = lgpm.getGrade(calculateEquivalentPercent(pointsPossible, agr.getPointsEarned()));
                    agr.setLetterEarned(letterGrade);
                    letterGradeList.add(agr);
                }
            }
        }
        return letterGradeList;
    }

    protected Double calculateEquivalentPointValueForPercent(final Double doublePointsPossible, final Double doublePercentEarned) {

    	if (doublePointsPossible == null || doublePercentEarned == null) {
			return null;
		}

    	final BigDecimal pointsPossible = new BigDecimal(doublePointsPossible.toString());
		final BigDecimal percentEarned = new BigDecimal(doublePercentEarned.toString());
		final BigDecimal equivPoints = pointsPossible.multiply(percentEarned.divide(new BigDecimal("100"), GradingConstants.MATH_CONTEXT));
		return equivPoints.doubleValue();
    }

    public List<Comment> getComments(GradebookAssignment assignment, List<String> studentIds) {

    	if (studentIds.isEmpty()) {
    		return new ArrayList<>();
    	}
        return commentRepository.findByGradableObjectAndStudentIdIn(assignment, studentIds);
    }

    /*
    protected Set<String> getProvidedExternalAssignments(final String gradebookUid) {

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

    // FROM EXTERNAL
    @Override
	public Map<String, List<String>> getVisibleExternalAssignments(String gradebookUid, Collection<String> studentIds)
			throws GradebookNotFoundException {

		final Set<String> providedAssignments = getProvidedExternalAssignments(gradebookUid);

		final Map<String, Set<String>> visible
            = studentIds.stream().collect(Collectors.toMap(Function.identity(), new HashSet<String>()));

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
		final List<org.sakaiproject.service.gradebook.shared.Assignment> allAssignments = getGradebookService()
				.getViewableAssignmentsForCurrentUser(gradebookUid);
		for (final org.sakaiproject.service.gradebook.shared.Assignment assignment : allAssignments) {
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

    protected Map<String, Set<GradebookAssignment>> getVisibleExternalAssignments(final Gradebook gradebook, final Collection<String> studentIds, final List<GradebookAssignment> assignments) {

        final String gradebookUid = gradebook.getUid();
        final Map<String, List<String>> allExternals = this.externalAssessmentService.getVisibleExternalAssignments(gradebookUid, studentIds);
        final Map<String, GradebookAssignment> allRequested = new HashMap<String, GradebookAssignment>();

        for (final GradebookAssignment a : assignments) {
            if (a.isExternallyMaintained()) {
                allRequested.put(a.getExternalId(), a);
            }
        }

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

	// NOTE: This should not be called in a loop. Anything for sets should use getVisibleExternalAssignments
	protected boolean studentCanView(final String studentId, final GradebookAssignment assignment) {

		if (assignment.isExternallyMaintained()) {
			try {
				final String gbUid = assignment.getGradebook().getUid();
				final String extId = assignment.getExternalId();

				if (this.externalAssessmentService.isExternalAssignmentGrouped(gbUid, extId)) {
					return this.externalAssessmentService.isExternalAssignmentVisible(gbUid, extId, studentId);
				}
			} catch (final GradebookNotFoundException e) {
				if (log.isDebugEnabled()) { log.debug("Bogus graded assignment checked for course grades: " + assignment.getId()); }
			}
		}

		// We assume that the only disqualifying condition is that the external assignment
		// is grouped and the student is not a member of one of the groups allowed.
		return true;
	}

    protected void finalizeNullGradeRecords(final Gradebook gradebook) {

    	final Set<String> studentUids = getAllStudentUids(gradebook.getUid());
		final Date now = new Date();
		final String graderId = sakaiProxy.getCurrentUserId();

        final List<GradebookAssignment> countedAssignments =
            gradebookAssignmentRepository.findByGradebook_IdAndRemovedAndNotCountedAndUngraded(
                gradebook.getId(), false, false, false);

        final Map<String, Set<GradebookAssignment>> visible = getVisibleExternalAssignments(gradebook, studentUids, countedAssignments);

        for (final GradebookAssignment assignment : countedAssignments) {

            final List<AssignmentGradeRecord> scoredGradeRecords
                = assignmentGradeRecord.findByGradableObject_Id(assignment.getId());

            final Map<String, AssignmentGradeRecord> studentToGradeRecordMap = new HashMap<>();
            for (final AssignmentGradeRecord scoredGradeRecord : scoredGradeRecords) {
                studentToGradeRecordMap.put(scoredGradeRecord.getStudentId(), scoredGradeRecord);
            }

            for (final String studentUid : studentUids) {
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
    */

    /**
     *
     * @param name the assignment name (will not be trimmed)
     * @param gradebook the gradebook to check
     * @return true if an assignment with the given name already exists in this gradebook.
     */
    protected boolean assignmentNameExists(String name, Gradebook gradebook) {
        return gradableObjectRepository.countByNameAndGradebook_UidAndRemoved(name, gradebook.getUid(), false) > 0L;
    }

	public Comment getInternalComment(String gradebookUid, Long assignmentId, String studentId) {

        return commentRepository
                .findOneByStudentIdAndGradableObject_Gradebook_UidAndGradableObject_IdAndGradableObject_RemovedIsFalse(
                studentId, gradebookUid, assignmentId);
	}

	public CommentDefinition getAssignmentScoreComment(final String gradebookUid, final Long assignmentId, final String studentUid) throws GradebookNotFoundException, AssessmentNotFoundException {

		if (gradebookUid == null || assignmentId == null || studentUid == null) {
			throw new IllegalArgumentException("null parameter passed to getAssignmentScoreComment. Values are gradebookUid:" + gradebookUid + " assignmentId:" + assignmentId + " studentUid:"+ studentUid);
		}
		final GradebookAssignment assignment = getAssignmentWithoutStats(gradebookUid, assignmentId);
		if (assignment == null) {
			throw new AssessmentNotFoundException("There is no assignmentId " + assignmentId + " for gradebookUid " + gradebookUid);
		}

		CommentDefinition commentDefinition = null;
        final Comment comment = getInternalComment(gradebookUid, assignmentId, studentUid);
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

	public void setAssignmentScoreComment(final String gradebookUid, final Long assignmentId, final String studentUid, final String commentText) throws GradebookNotFoundException, AssessmentNotFoundException {

        Comment comment = getInternalComment(gradebookUid, assignmentId, studentUid);
        if (comment == null) {
            comment = new Comment(studentUid, commentText, getAssignmentWithoutStats(gradebookUid, assignmentId));
        } else {
            comment.setCommentText(commentText);
        }
        comment.setGraderId(sakaiProxy.getCurrentUserId());
        comment.setDateRecorded(new Date());
        commentRepository.save(comment);
	}

	public boolean getIsAssignmentExcused(final String gradebookUid, final Long assignmentId, final String studentUid) throws GradebookNotFoundException, AssessmentNotFoundException {

        if (gradebookUid == null || assignmentId == null || studentUid == null) {
            throw new IllegalArgumentException("null parameter passed to getAssignmentScoreComment. Values are gradebookUid:" + gradebookUid + " assignmentId:" + assignmentId + " studentUid:"+ studentUid);
        }
        GradebookAssignment assignment = getAssignmentWithoutStats(gradebookUid, assignmentId);
        AssignmentGradeRecord agr = getAssignmentGradeRecord(assignment, studentUid);

        if (agr == null) {
            return false;
        } else {
            return BooleanUtils.toBoolean(agr.getExcludedFromGrade());
        }
    }

	public void updateGradeMapping(Long gradeMappingId, Map<String, Double> gradeMap) {

        GradeMapping gradeMapping = gradeMappingRepository.findOne(gradeMappingId);
        gradeMapping.setGradeMap(gradeMap);
        gradeMappingRepository.save(gradeMapping);
	}

	 /**
     * Get's all course grade overrides for a given gradebook
     *
     * @param gradebook The gradebook
     * @return A list of {@link CourseGradeRecord} that have overrides
     *
     * @throws HibernateException
     */
    protected List<CourseGradeRecord> getCourseGradeOverrides(Gradebook gradebook) {
        return courseGradeRecordRepository.findByGradableObject_GradebookAndEnteredGradeNotNull(gradebook);
    }
}
