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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A GradeMapping provides a means to convert between an arbitrary set of grades
 * (letter grades, pass / not pass, 4,0 scale) and numeric percentages.
 *
 */
@Entity
@Table(name = "GB_GRADE_MAP_T")
@Setter
public class GradeMapping implements Serializable, Comparable<Object> {

	private static final long serialVersionUID = 1L;

    private static final String SEQUENCE_GENERATOR = "grading_grade_map_sequence";
	
    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR , sequenceName = "GB_GRADE_MAPPING_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
    @Getter
	protected Long id;

    @Column(name = "VERSION")
	protected int version;

    @ManyToOne
    @JoinColumn(name = "GRADEBOOK_ID", nullable = false)
    @Setter
	protected Gradebook gradebook;

    /*
    <map name="gradeMap" table="GB_GRADE_TO_PERCENT_MAPPING_T" cascade="all" lazy="false">
        <key column="GRADE_MAP_ID"/>
        <index column="LETTER_GRADE" type="string"/>
        <element column="PERCENT" type="double"/>
    </map>
    */
    @ElementCollection
    @CollectionTable(name = "GB_GRADE_TO_PERCENT_MAPPING_T", joinColumns = @JoinColumn(name = "GRADE_MAP_ID"))
    @MapKeyColumn(name = "LETTER_GRADE")
    @Column(name = "PERCENT")
    @Getter @Setter
	protected Map<String, Double> gradeMap;

	/**
	 * The GradingScale used to define this mapping, or null if
	 * this is an old Gradebook which uses hard-coded scales
	 */
    @ManyToOne
    @JoinColumn(name = "GB_GRADING_SCALE_T", nullable = false)
    @Getter
	private GradingScale gradingScale;

	public GradeMapping() {
	}

	public GradeMapping(final GradingScale gradingScale) {
		this.gradingScale = gradingScale;
		this.gradeMap = new HashMap<>(gradingScale.getDefaultBottomPercents());
	}

	public String getName() {
		return (this.gradingScale != null) ? this.gradingScale.getName() : null;
	}

	/**
	 * Sets the percentage values for this GradeMapping to their default values.
	 */
	public void setDefaultValues() {
		this.gradeMap = new HashMap<>(getDefaultBottomPercents());
	}

	/**
	 * Backwards-compatible wrapper to get to grading scale.
	 */
	public Map<String, Double> getDefaultBottomPercents() {
		final GradingScale scale = this.gradingScale;
		if (scale != null) {
			return scale.getDefaultBottomPercents();
		} else {
			final Map<String, Double> defaultBottomPercents = new HashMap<String, Double>();
			final Iterator<String> gradesIter = getGrades().iterator();
			final Iterator<Double> defaultValuesIter = getDefaultValues().iterator();
			while (gradesIter.hasNext()) {
				final String grade = gradesIter.next();
				final Double value = defaultValuesIter.next();
				defaultBottomPercents.put(grade, value);
			}
			return defaultBottomPercents;
		}
	}

	/**
	 *
	 * @return An (ordered) collection of the available grade values
	 */
	public Collection<String> getGrades() {
		return (this.gradingScale != null) ? this.gradingScale.getGrades() : Collections.emptyList();
	}

	/**
	 *
	 * @return A List of the default grade values. Only used for backward
	 * compatibility to pre-grading-scale mappings.
	 *
	 * @deprecated
	 */
	@Deprecated
	public List<Double> getDefaultValues() {
		throw new UnsupportedOperationException("getDefaultValues called for GradeMapping " + getName() + " in Gradebook " + this.gradebook);
    }

	/**
	 * Gets the percentage mapped to a particular grade.
	 */
	public Double getValue(final String grade) {
		return this.gradeMap.get(grade);
	}

	/**
	 * Get the mapped grade based on the persistent grade mappings
	 *
	 */
	public String getMappedGrade(final Double value) {
		return getMappedGrade(this.gradeMap, value);
	}

	/**
	 * Get the mapped grade based on the passed in grade mappings.
	 *
	 * NOTE: The gradeMap MUST be sorted!
	 */
	public static String getMappedGrade(final Map<String, Double> gradeMap, final Double value) {
		if(value == null) {
            return null;
        }

		for (final Map.Entry<String, Double> entry : sortGradeMapping(gradeMap).entrySet()) {
			final String grade = entry.getKey();
			final Double mapVal = entry.getValue();

			// If the value in the map is less than the value passed, then the
			// map value is the letter grade for this value
			if (mapVal != null && mapVal.compareTo(value) <= 0) {
				return grade;
			}
		}
		// As long as 'F' is zero, this should never happen.
		return null;
	}

	/**
	 * Handles the sorting of the grade mapping.
	 *
	 * @param gradeMap
	 * @return
	 */
	public static Map<String, Double> sortGradeMapping(final Map<String, Double> gradeMap) {

		// we only ever order by bottom percents now
		final DoubleComparator doubleComparator = new DoubleComparator(gradeMap);
		final Map<String, Double> rval = new TreeMap<>(doubleComparator);
		rval.putAll(gradeMap);

		return rval;
	}

 	@Override
	public int compareTo(final Object o) {
		final GradeMapping other = (GradeMapping) o;
		return new CompareToBuilder().append(getName(), other.getName()).toComparison();
    }

	@Override
	public boolean equals(final Object o) {
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		final GradeMapping other = (GradeMapping) o;
		return new EqualsBuilder().append(getName(), other.getName()).isEquals();
	}

	@Override
	public int hashCode() {
		if (this.gradingScale == null || getName() == null) {
			return 0;
		}
		return new HashCodeBuilder().append(getName()).toHashCode();

	}

    @Override
	public String toString() {
        return new ToStringBuilder(this).
            append(getName()).
            append(this.id).toString();
    }

    /**
     * Enable any-case input of grades (typically lowercase input
     * for uppercase grades). Look for a case-insensitive match
     * to the input text and if it's found, return the official
     * version.
     *
     * @return The normalized version of the grade, or null if not found.
     */
    public String standardizeInputGrade(String inputGrade) {

    	String standardizedGrade = null;
    	for (String grade: getGrades()) {
    		if (grade.equalsIgnoreCase(inputGrade)) {
    			standardizedGrade = grade;
    			break;
    		}
    	}
    	return standardizedGrade;
    }
}
