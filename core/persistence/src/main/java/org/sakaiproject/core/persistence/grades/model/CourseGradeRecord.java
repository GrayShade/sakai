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

package org.sakaiproject.core.persistence.grades.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.core.utils.grades.GradingConstants;

import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

/**
 * A CourseGradeRecord is a grade record that can be associated with a CourseGrade.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
@Entity
@DiscriminatorValue("2")
@Getter @Setter
public class CourseGradeRecord extends AbstractGradeRecord {

	private static final long serialVersionUID = 1L;

    @Column(name = "ENTERED_GRADE")
	private String enteredGrade;

    @Transient
	private Double autoCalculatedGrade; // Not persisted

    @Transient
	private Double calculatedPointsEarned; // Not persisted

    @Transient
	private Double totalPointsPossible; // Not persisted

	public static Comparator<CourseGradeRecord> calcComparator;

	static {
		calcComparator = new Comparator<CourseGradeRecord>() {
			@Override
			public int compare(final CourseGradeRecord cgr1, final CourseGradeRecord cgr2) {
				if ((cgr1 == null || cgr2 == null) || (cgr1.getGradeAsPercentage() == null && cgr2.getGradeAsPercentage() == null)) {
					return 0;
				}
				if (cgr1.getGradeAsPercentage() == null) {
					return -1;
				}
				if (cgr2.getGradeAsPercentage() == null) {
					return 1;
				}
				// SAK-12017 - Commented out as getPointsEarned is no longer an accurate comparator
				// due to nulls no longer being calculated in to the Course Grade
				// return cgr1.getPointsEarned().compareTo(cgr2.getPointsEarned());
				// Better to use getGradeAsPercentage
				return cgr1.getGradeAsPercentage().compareTo(cgr2.getGradeAsPercentage());
			}
		};
	}

	public static Comparator<CourseGradeRecord> getOverrideComparator(final GradeMapping mapping) {
		return new Comparator<CourseGradeRecord>() {
			@Override
			public int compare(final CourseGradeRecord cgr1, final CourseGradeRecord cgr2) {

				if (cgr1 == null && cgr2 == null) {
					return 0;
				}
				if (cgr1 == null) {
					return -1;
				}
				if (cgr2 == null) {
					return 1;
				}

				final String enteredGrade1 = StringUtils.trimToEmpty(cgr1.getEnteredGrade());
				final String enteredGrade2 = StringUtils.trimToEmpty(cgr2.getEnteredGrade());

				// Grading scales are always defined in descending order.
				final List<String> grades = mapping.getGradingScale().getGrades();
				int gradePos1 = -1;
				int gradePos2 = -1;
				for (int i = 0; (i < grades.size()) && ((gradePos1 == -1) || (gradePos2 == -1)); i++) {
					final String grade = grades.get(i);
					if (grade.equals(enteredGrade1)) {
						gradePos1 = i;
					}
					if (grade.equals(enteredGrade2)) {
						gradePos2 = i;
					}
				}
				return gradePos2 - gradePos1;
			}
		};

	}

	/**
	 * The graderId and dateRecorded properties will be set explicitly by the grade manager before the database is updated.
	 *
	 * @param courseGrade
	 * @param studentId
	 */
	public CourseGradeRecord(final CourseGrade courseGrade, final String studentId) {
		this.gradableObject = courseGrade;
		this.studentId = studentId;
	}

	/**
	 * Default no-arg constructor
	 */
	public CourseGradeRecord() {
		super();
	}

	/**
	 * This method will fail unless this course grade was fetched "with statistics", since it relies on having the total number of points
	 * possible available to calculate the percentage.
	 *
	 * @see org.sakaiproject.tool.gradebook.AbstractGradeRecord#getGradeAsPercentage()
	 */
	@Override
	public Double getGradeAsPercentage() {
		if (this.enteredGrade == null) {
			return this.autoCalculatedGrade;
		} else {
			return getCourseGrade().getGradebook().getSelectedGradeMapping().getValue(this.enteredGrade);
		}
	}

	/**
	 * Convenience method to get the correctly cast CourseGrade that this CourseGradeRecord references.
	 *
	 * @return CourseGrade referenced by this GradableObject
	 */
	public CourseGrade getCourseGrade() {
		return (CourseGrade) super.getGradableObject();
	}

	@Override
	public Double getPointsEarned() {
		return this.calculatedPointsEarned;
	}

	/**
	 * @return Returns the displayGrade.
	 */
	public String getDisplayGrade() {
		if (this.enteredGrade != null) {
			return this.enteredGrade;
		} else {
			return getCourseGrade().getGradebook().getSelectedGradeMapping().getMappedGrade(this.autoCalculatedGrade);
		}
	}

	/**
	 * @see org.sakaiproject.tool.gradebook.AbstractGradeRecord#isCourseGradeRecord()
	 */
	@Override
	public boolean isCourseGradeRecord() {
		return true;
	}

	/**
	 * For use by the Course Grade UI.
	 */
	public Double getNonNullAutoCalculatedGrade() {
		Double percent = getAutoCalculatedGrade();
		if (percent == null) {
			percent = Double.valueOf(0);
		}
		return percent;
	}

	public void initNonpersistentFields(final double totalPointsPossible, final double totalPointsEarned) {
		Double percentageEarned;
		this.totalPointsPossible = totalPointsPossible;
		this.calculatedPointsEarned = totalPointsEarned;
		final BigDecimal bdTotalPointsPossible = new BigDecimal(totalPointsPossible);
		final BigDecimal bdTotalPointsEarned = new BigDecimal(totalPointsEarned);
		if (totalPointsPossible == 0.0) {
			percentageEarned = null;
		} else {
			percentageEarned = Double.valueOf(bdTotalPointsEarned.divide(bdTotalPointsPossible, GradingConstants.MATH_CONTEXT)
					.multiply(new BigDecimal("100")).doubleValue());
		}
		this.autoCalculatedGrade = percentageEarned;
	}

	// Added by -Qu for totalPoints implementation in GB2 bugid:4371 9/2011
	public void setCalculatedPointsEarned(final double literalTotalPointsEarned) {
		this.calculatedPointsEarned = literalTotalPointsEarned;
	}

	public void initNonpersistentFields(final double totalPointsPossible, final double totalPointsEarned,
			final double literalTotalPointsEarned) {
		Double percentageEarned;
		this.calculatedPointsEarned = literalTotalPointsEarned;
		this.totalPointsPossible = totalPointsPossible;
		final BigDecimal bdTotalPointsPossible = new BigDecimal(totalPointsPossible);
		final BigDecimal bdTotalPointsEarned = new BigDecimal(totalPointsEarned);

		if (totalPointsPossible <= 0.0) {
			percentageEarned = null;
		} else {
			percentageEarned = Double.valueOf(bdTotalPointsEarned.divide(bdTotalPointsPossible, GradingConstants.MATH_CONTEXT)
					.multiply(new BigDecimal("100")).doubleValue());
		}
		this.autoCalculatedGrade = percentageEarned;
	}
}
