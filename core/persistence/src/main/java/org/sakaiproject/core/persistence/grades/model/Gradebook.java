/**
 * Copyright (c) 2003-2019 The Apereo Foundation
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
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A Gradebook is the top-level object in the Sakai Gradebook tool.  Only one
 * Gradebook should be associated with any particular course (or site, as they
 * exist in Sakai 1.5) for any given academic term.  How courses and terms are
 * determined will likely depend on the particular Sakai installation.
 */
@Entity
@Table(name = "GB_GRADEBOOK_T")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Gradebook implements Serializable {

	private static final long serialVersionUID = 1L;

    private static final String SEQUENCE_GENERATOR = "grading_gradebook_sequence";
	
    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(name = SEQUENCE_GENERATOR , sequenceName = "GB_GRADEBOOK_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
	@ToString.Include
    private Long id;

	@ToString.Include
	@EqualsAndHashCode.Include
    @Column(name = "GRADEBOOK_UID", unique = true, nullable = false)
    private String uid;

    @Column(name = "VERSION")
    private int version;

	@ToString.Include
    @Column(name = "NAME", nullable = false)
    private String name;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "SELECTED_GRADE_MAPPING_ID")
    private GradeMapping selectedGradeMapping;

    @OneToMany(mappedBy = "gradebook", cascade = CascadeType.ALL)
    private Set<GradeMapping> gradeMappings;

    @Column(name = "ASSIGNMENTS_DISPLAYED", nullable = false)
    private boolean assignmentsDisplayed;

    @Column(name = "COURSE_GRADE_DISPLAYED", nullable = false)
    private boolean courseGradeDisplayed;

    @Column(name = "COURSE_LETTER_GRADE_DISPLAYED", nullable = false)
    private boolean courseLetterGradeDisplayed;

    @Column(name = "COURSE_POINTS_DISPLAYED", nullable = false)
    private boolean coursePointsDisplayed;

    @Column(name = "TOTAL_POINTS_DISPLAYED", nullable = false)
    private boolean totalPointsDisplayed;

    @Column(name = "COURSE_AVERAGE_DISPLAYED", nullable = false)
    private boolean courseAverageDisplayed;

    @Column(name = "ALL_ASSIGNMENTS_ENTERED", nullable = false)
    private boolean allAssignmentsEntered;

    @Column(name = "LOCKED", nullable = false)
    private boolean locked;

    @Column(name = "GRADE_TYPE", nullable = false)
    private int grade_type;

    @Column(name = "CATEGORY_TYPE", nullable = false)
    private int category_type;

    @Column(name = "IS_EQUAL_WEIGHT_CATS")
    private Boolean equalWeightCategories;

    @Column(name = "IS_SCALED_EXTRA_CREDIT")
    private Boolean scaledExtraCredit;

    @Column(name = "DO_SHOW_MEAN")
    private Boolean showMean;

    @Column(name = "DO_SHOW_MEDIAN")
    private Boolean showMedian;

    @Column(name = "DO_SHOW_MODE")
    private Boolean showMode;

    @Column(name = "DO_SHOW_RANK")
    private Boolean showRank;

    @Column(name = "DO_SHOW_ITEM_STATS")
    private Boolean showItemStatistics;

    @Column(name = "DO_SHOW_STATISTICS_CHART")
    private Boolean showStatisticsChart;

    @Column(name = "ASSIGNMENT_STATS_DISPLAYED")
	private boolean assignmentStatsDisplayed;

    @Column(name = "COURSE_GRADE_STATS_DISPLAYED")
	private boolean courseGradeStatsDisplayed;
}
