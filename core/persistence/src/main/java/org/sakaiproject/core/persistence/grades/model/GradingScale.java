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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.sakaiproject.core.utils.grades.GradingScaleDefinition;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Index;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "GB_GRADING_SCALE_TT")
@Getter @Setter
public class GradingScale implements Serializable, Comparable<Object> {
    
    private static final long serialVersionUID = 1L;
    
    private static final String SEQUENCE_GENERATOR = "grading_grading_scale_sequence";

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_GRADING_SCALE_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
    private Long id;

    @Column(name = "VERSION")
    private int version;

    @Column(name = "SCALE_UID", unique = true, nullable = false)
    private String uid;

    @Column(name = "NAME", nullable = false)
    private String name;

    /**
     * Because the Gradebook now supports non-calculated manual-only grades with
     * no percentage equivalent, it is possible for the list of grades to include
     * codes that are not included in the defaultBottomPercents map. In other
     * words, callers shouldn't expect getDefaultBottomPercents.keySet() to be
     * equivalent to this list.
     * @return list of supported grade codes, ordered from highest to lowest
     */
    /*
    <list name="grades" table="GB_GRADING_SCALE_GRADES_T" cascade="all" lazy="false">
        <key column="GRADING_SCALE_ID"/>
        <index column="GRADE_IDX"/>
        <element column="LETTER_GRADE" type="string"/>
    </list>
    */
    @ElementCollection
    @CollectionTable(name = "GB_GRADING_SCALE_GRADES_T", joinColumns = @JoinColumn(name = "GRADING_SCALE_ID"), indexes = @Index(columnList = "GRADING_SCALE_ID"))
    @OrderColumn(name = "GRADE_IDX")
    @Column(name = "LETTER_GRADE", nullable = false)
    private List<String> grades;

    /*
    <map name="defaultBottomPercents" table="GB_GRADING_SCALE_PERCENTS_T" cascade="all" lazy="false">
        <key column="GRADING_SCALE_ID"/>
        <index column="LETTER_GRADE" type="string"/>
        <element column="PERCENT" type="double"/>
    </map>
    */
    @ElementCollection
    @CollectionTable(name = "GB_GRADING_SCALE_PERCENTS_T", joinColumns = @JoinColumn(name = "GRADING_SCALE_ID"))
    @MapKeyColumn(name = "LETTER_GRADE")
    @Column(name = "PERCENT")
    private Map<String, Double> defaultBottomPercents;  // From grade to percentage

    @Column(name = "UNAVAILABLE")
    private boolean unavailable;

    @Override
    public int compareTo(Object o) {
        return getName().compareTo(((GradingScale)o).getName());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(getUid()).toString();
    }

    /**
     * Convert this GradeingScale instance to a GradingScaleDefinition
     * @return
     */
    public GradingScaleDefinition toGradingScaleDefinition() {

        GradingScaleDefinition scaleDef = new GradingScaleDefinition();
        scaleDef.setUid(this.getUid());
        scaleDef.setName(this.getName());
        
        Map<String, Double> mapBottomPercents = this.getDefaultBottomPercents();
        scaleDef.setDefaultBottomPercents(mapBottomPercents);

        //build the bottom percents as a list as well
        List<Object> listBottomPercents = new ArrayList<>();
        List<String> grades = new ArrayList<>();
        for (Map.Entry<String, Double> pair : mapBottomPercents.entrySet()) {
            listBottomPercents.add(pair.getValue());
            grades.add(pair.getKey());
        }
        scaleDef.setGrades(grades);
        scaleDef.setDefaultBottomPercentsAsList(listBottomPercents);
        
        return scaleDef;
    }
}
