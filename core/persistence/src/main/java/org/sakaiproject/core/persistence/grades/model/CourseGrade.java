/**
 * Copyright (c) 2003-2012 The Apereo Foundation
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

package org.sakaiproject.core.persistence.grades.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.sakaiproject.core.utils.grades.GradingConstants;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * A CourseGrade is a GradableObject that represents the overall course grade in a gradebook.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman </a>
 */
@Entity
public class CourseGrade extends GradableObject {

    private static final long serialVersionUID = -7607255825842609208L;

    // Should only be used to fill in the DB column.
    private static final String COURSE_GRADE_NAME = "Course Grade";

    public static String SORT_BY_OVERRIDE_GRADE = "override";
    public static String SORT_BY_CALCULATED_GRADE = "autoCalc";
    public static String SORT_BY_POINTS_EARNED = "pointsEarned";

    @Getter @Setter
    private Double averageScore;

    @Transient
    @Getter @Setter
	private String enteredGrade;

    @Transient
    @Getter @Setter
	private Date dateRecorded;

    @Transient
    @Getter @Setter
	private String calculatedGrade;

    @Transient
    @Getter @Setter
	private String mappedGrade;

    @Transient
    @Getter @Setter
	private Double pointsEarned;

    @Transient
    @Getter @Setter
	private Double totalPointsPossible;

    public CourseGrade() {
        setName(COURSE_GRADE_NAME);
    }

    /**
     * @see org.sakaiproject.tool.gradebook.GradableObject#isCourseGrade()
     */
    @Override
    public boolean isCourseGrade() {
        return true;
    }

    /**
     * @see org.sakaiproject.tool.gradebook.GradableObject#isAssignment()
     */
    @Override
    public boolean isAssignment() {
        return false;
    }

    /**
     * @see org.sakaiproject.tool.gradebook.GradableObject#isCategory()
     */
    @Override
    public boolean getIsCategory() {
        return false;
    }

    /**
     * Calculate the mean course grade (whether entered or calulated) as a percentage for all enrollments, leaving students who've
     * explicitly been given non-percentage-valued manual-only course grades (such as "I" for incomplete) or null scores out of the
     * calculation.
     */
    public void calculateStatistics(final Collection<CourseGradeRecord> gradeRecords, final int numEnrollments) {

        // Ungraded but enrolled students count as if they have 0% in the course.
        int numScored = numEnrollments - gradeRecords.size();
        BigDecimal total = new BigDecimal("0");
        BigDecimal average = new BigDecimal("0");

        for (final CourseGradeRecord record : gradeRecords) {
            final Double score = record.getGradeAsPercentage();

            // Skip manual-only course grades.
            if ((record.getEnteredGrade() != null) && (score == null)) {
                continue;
            }

            if (score != null && record.getPointsEarned() != null) {
                average = average.add(new BigDecimal(record.getPointsEarned().toString()));
                total = total.add(new BigDecimal(score.toString()));
                numScored++;
            }
        }
        if (numScored == 0) {
            this.mean = null;
            this.averageScore = null;
        } else {
            this.mean = Double.valueOf(total.divide(new BigDecimal(numScored), GradingConstants.MATH_CONTEXT).doubleValue());
            this.averageScore = Double.valueOf(average.divide(new BigDecimal(numScored), GradingConstants.MATH_CONTEXT).doubleValue());
        }
    }

    /**
     * Helper to get a grade override preferentially, or fallback to the standard mapped grade.
     * @return
     */
    public String getDisplayGrade() {
        return (StringUtils.isNotBlank(getEnteredGrade()) ? getEnteredGrade() : getMappedGrade());
    }
}
