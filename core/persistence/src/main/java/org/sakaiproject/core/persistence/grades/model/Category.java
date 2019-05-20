/**
 * Copyright (c) 2003-2014 The Apereo Foundation
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.core.utils.grades.GradingConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "GB_CATEGORY_T")
@Getter @Setter
public class Category implements Serializable {

	private static final long serialVersionUID = 2609776646548825870L;

    private static final String SEQUENCE_GENERATOR = "grading_category_sequence";

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_CATEGORY_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
	private Long id;

    @Column(name = "VERSION")
	private int version;

    @ManyToOne
    @JoinColumn(name = "GRADEBOOK_ID", nullable = false)
	private Gradebook gradebook;

    @Column(name = "NAME", nullable = false)
	private String name;

    @Column(name = "WEIGHT")
	private Double weight;

    @Column(name = "DROP_LOWEST")
	private Integer dropLowest;

    @Column(name = "DROP_HIGHEST")
	private Integer dropHighest;

    @Column(name = "KEEP_HIGHEST")
	private Integer keepHighest;

    @Column(name = "REMOVED")
	private boolean removed;

    @Column(name = "IS_EXTRA_CREDIT")
	private Boolean extraCredit = false;

    @Column(name = "IS_UNWEIGHTED")
	private Boolean unweighted;

    @Column(name = "IS_EQUAL_WEIGHT_ASSNS")
	private Boolean equalWeightAssignments;

    @Column(name = "CATEGORY_ORDER")
	private Integer categoryOrder;

    @Transient
	private Double averageTotalPoints; // average total points possible for this category
    @Transient
	private Double averageScore; // average scores that students got for this category
    @Transient
	private Double mean; // mean value of percentage for this category
    @Transient
	private Double totalPointsEarned; // scores that students got for this category
	/**
	 * Fix for Category NPE reported in SAK-14519 Since category uses "totalPointsPossible" property instead of "pointsPossible" property,
	 * as in Assignments
	 */
    @Transient
	private Double totalPointsPossible; // total points possible for this category
    @Transient
	private List assignmentList;
    @Transient
	private int assignmentCount;
    @Transient
	private Boolean enforcePointWeighting;

	public static final Comparator nameComparator;
	public static final Comparator averageScoreComparator;
	public static final Comparator weightComparator;

	public static String SORT_BY_NAME = "name";
	public static String SORT_BY_AVERAGE_SCORE = "averageScore";
	public static String SORT_BY_WEIGHT = "weight";

	static {
		nameComparator = new Comparator() {
			@Override
			public int compare(final Object o1, final Object o2) {
				return ((Category) o1).getName().toLowerCase().compareTo(((Category) o2).getName().toLowerCase());
			}
		};
		averageScoreComparator = new Comparator() {
			@Override
			public int compare(final Object o1, final Object o2) {
				final Category one = (Category) o1;
				final Category two = (Category) o2;

				if (one.getAverageScore() == null && two.getAverageScore() == null) {
					return one.getName().compareTo(two.getName());
				}

				if (one.getAverageScore() == null) {
					return -1;
				}
				if (two.getAverageScore() == null) {
					return 1;
				}

				final int comp = (one.getAverageScore().compareTo(two.getAverageScore()));
				if (comp == 0) {
					return one.getName().compareTo(two.getName());
				} else {
					return comp;
				}
			}
		};
		weightComparator = new Comparator() {
			@Override
			public int compare(final Object o1, final Object o2) {
				final Category one = (Category) o1;
				final Category two = (Category) o2;

				if (one.getWeight() == null && two.getWeight() == null) {
					return one.getName().compareTo(two.getName());
				}

				if (one.getWeight() == null) {
					return -1;
				}
				if (two.getWeight() == null) {
					return 1;
				}

				final int comp = (one.getWeight().compareTo(two.getWeight()));
				if (comp == 0) {
					return one.getName().compareTo(two.getName());
				} else {
					return comp;
				}
			}
		};
	}

	public Integer getDropHighest() {
		return this.dropHighest == null ? 0 : this.dropHighest;
	}

	public Integer getKeepHighest() {
		return this.keepHighest == null ? 0 : this.keepHighest;
	}

	/*
	 * returns true if this category drops any scores
	 */
	public boolean isDropScores() {
		return getDropLowest() > 0 || getDropHighest() > 0 || getKeepHighest() > 0;
	}

	public Double getItemValue() {
		if (isAssignmentsEqual()) {
			Double returnVal = 0.0;
			final List assignments = getAssignmentList();
			if (assignments != null) {
				for (final Object obj : assignments) {
					if (obj instanceof GradebookAssignment) {
						final GradebookAssignment assignment = (GradebookAssignment) obj;
						if (!GradebookAssignment.item_type_adjustment.equals(assignment.getItemType())) {// ignore adjustment items
							returnVal = assignment.getPointsPossible();
							return returnVal;
						}
					}
				}
			}
			// didn't find any, so return 0.0
			return returnVal;
		} else {
			return 0.0;
		}
	}

	public Integer getDropLowest() {
		return this.dropLowest != null ? this.dropLowest : 0;
	}

	public void setName(final String name) {
		// SAK-20071 - names over 255 chars cause DB insert failure
		this.name = StringUtils.substring(name, 0, 249);
	}

	public void calculateStatistics(final List<GradebookAssignment> assignmentsWithStats) {
		int numScored = 0;
		int numOfAssignments = 0;
		BigDecimal total = new BigDecimal("0");
		BigDecimal totalPossible = new BigDecimal("0");

		for (final GradebookAssignment assign : assignmentsWithStats) {
			final Double score = assign.getAverageTotal();

			if (assign.isCounted() && !assign.getUngraded() && assign.getPointsPossible() != null
					&& assign.getPointsPossible().doubleValue() > 0.0) {
				if (score != null) {
					total = total.add(new BigDecimal(score.toString()));
					if (assign.getPointsPossible() != null && !assign.isExtraCredit()) {
						totalPossible = totalPossible.add(new BigDecimal(assign.getPointsPossible().toString()));
						numOfAssignments++;
					}
					if (!assign.isExtraCredit()) {
						numScored++;
					}
				}
			}
		}

		if (numScored == 0 || numOfAssignments == 0) {
			this.averageScore = null;
			this.averageTotalPoints = null;
			this.mean = null;
			this.totalPointsEarned = null;
			this.totalPointsPossible = null;
		} else {
			final BigDecimal bdNumScored = new BigDecimal(numScored);
			final BigDecimal bdNumAssign = new BigDecimal(numOfAssignments);
			this.averageScore = Double.valueOf(total.divide(bdNumScored, GradingConstants.MATH_CONTEXT).doubleValue());
			this.averageTotalPoints = Double.valueOf(totalPossible.divide(bdNumAssign, GradingConstants.MATH_CONTEXT).doubleValue());
			final BigDecimal value = total.divide(bdNumScored, GradingConstants.MATH_CONTEXT)
					.divide(new BigDecimal(this.averageTotalPoints.doubleValue()), GradingConstants.MATH_CONTEXT)
					.multiply(new BigDecimal("100"));
			this.mean = Double.valueOf(value.doubleValue());
		}
	}

	public void calculateStatisticsPerStudent(final List<AssignmentGradeRecord> gradeRecords, final String studentUid) {
		getGradebook().getGrade_type();
		int numScored = 0;
		int numOfAssignments = 0;
		BigDecimal total = new BigDecimal("0");
		BigDecimal totalPossible = new BigDecimal("0");

		if (gradeRecords == null) {
			setAverageScore(null);
			setAverageTotalPoints(null);
			setMean(null);
			setTotalPointsEarned(null);
			setTotalPointsPossible(null);
			return;
		}

		for (final AssignmentGradeRecord gradeRecord : gradeRecords) {
			if (gradeRecord != null && gradeRecord.getStudentId().equals(studentUid)) {
				final GradebookAssignment assignment = gradeRecord.getAssignment();
				if (assignment.isCounted() && !assignment.getUngraded() && assignment.getPointsPossible().doubleValue() > 0.0
						&& !gradeRecord.getDroppedFromGrade()) {

					final Category assignCategory = assignment.getCategory();
					if (assignCategory != null && assignCategory.getId().equals(this.id)) {
						final Double score = gradeRecord.getPointsEarned();
						if (score != null) {
							final BigDecimal bdScore = new BigDecimal(score.toString());
							total = total.add(bdScore);
							if (assignment.getPointsPossible() != null && !assignment.isExtraCredit()) {
								final BigDecimal bdPointsPossible = new BigDecimal(assignment.getPointsPossible().toString());
								totalPossible = totalPossible.add(bdPointsPossible);
								numOfAssignments++;
							}
							if (!assignment.isExtraCredit()) {
								numScored++;
							}
						}
					}
				}
			}
		}

		// if totalPossible is 0, this prevents a division by zero scenario likely from
		// an adjustment item being the only thing graded.
		if (numScored == 0 || numOfAssignments == 0 || totalPossible.doubleValue() == 0) {
			this.averageScore = null;
			this.averageTotalPoints = null;
			this.mean = null;
			this.totalPointsEarned = null;
			this.totalPointsPossible = null;
		} else {
			final BigDecimal bdNumScored = new BigDecimal(numScored);
			final BigDecimal bdNumAssign = new BigDecimal(numOfAssignments);
			this.averageScore = Double.valueOf(total.divide(bdNumScored, GradingConstants.MATH_CONTEXT).doubleValue());
			this.averageTotalPoints = Double.valueOf(totalPossible.divide(bdNumAssign, GradingConstants.MATH_CONTEXT).doubleValue());
			final BigDecimal value = total.divide(bdNumScored, GradingConstants.MATH_CONTEXT)
					.divide((totalPossible.divide(bdNumAssign, GradingConstants.MATH_CONTEXT)), GradingConstants.MATH_CONTEXT)
					.multiply(new BigDecimal("100"));

			this.mean = Double.valueOf(value.doubleValue());
		}
	}

	/*
	 * The methods below are used with the GradableObjects because all three are displayed in a dataTable together
	 */
	public boolean getIsCategory() {
		return true;
	}

	public boolean isCourseGrade() {
		return false;
	}

	public boolean isAssignment() {
		return false;
	}

	// these two functions are needed to keep the old API and help JSF and RSF play nicely together. Since isExtraCredit already exists and
	// we can't remove it
	// and JSF expects Boolean values to be "getExtraCredit", this had to be added for JSF. Also, since the external GB create item page is
	// in
	// RSF, you can't name it getExtraCredit and keep isExtraCredit b/c of SAK-14589
	public Boolean getIsExtraCredit() {
		return isExtraCredit();
	}

	public void setIsExtraCredit(final Boolean isExtraCredit) {
		setExtraCredit(isExtraCredit);
	}

	public Boolean isExtraCredit() {
		return this.extraCredit;
	}

	public boolean isAssignmentsEqual() {
		boolean isEqual = true;
		Double pointsPossible = null;
		final List assignments = getAssignmentList();
		if (assignments == null) {
			return isEqual;
		} else {
			for (final Object obj : assignments) {
				if (obj instanceof GradebookAssignment) {
					final GradebookAssignment assignment = (GradebookAssignment) obj;
					if (pointsPossible == null) {
						if (!GradebookAssignment.item_type_adjustment.equals(assignment.getItemType())) {// ignore adjustment items
							pointsPossible = assignment.getPointsPossible();
						}
					} else {
						if (assignment.getPointsPossible() != null
								&& !GradebookAssignment.item_type_adjustment.equals(assignment.getItemType()) // ignore adjustment items
																												// that are not equal
								&& !pointsPossible.equals(assignment.getPointsPossible())) {
							isEqual = false;
							return isEqual;
						}
					}
				}
			}
		}
		return isEqual;
	}
}
