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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * AbstractGradeRecord is the abstract base class for Grade Records, which are
 * records of instructors (or the application, in the case of autocalculated
 * gradebooks) assigning a grade to a student for a particular GradableObject.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
@Table(name = "GB_GRADE_RECORD_TT", indexes = {@Index(name = "GB_GRADE_RECORD_O_TT_IDX", columnList = "OBJECT_TYPE_ID")})
@DiscriminatorColumn(name = "OBJECT_TYPE_ID", discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue("0")
@MappedSuperclass
@Getter @Setter
public abstract class AbstractGradeRecord /*implements Serializable */{
   
	private static final long serialVersionUID = 1L;

    private static final String SEQUENCE_GENERATOR = "grading_grade_record_sequence";
	
    @Id
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_GRADE_RECORD_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
    @Column(name = "ID")
	private long id;

    @Column(name = "VERSION")
    protected int version;

    @Column(name = "STUDENT_ID")
    protected String studentId;

    @Column(name = "GRADER_ID", nullable = false)
    protected String graderId;

    @ManyToOne
    @JoinColumn(name = "GRADABLE_OBJECT_ID")
    protected GradableObject gradableObject;

    @Column(name = "DATE_RECORDED", nullable = false)
    protected Date dateRecorded;

    public abstract Double getGradeAsPercentage();

    /**
     * @return Whether this is a course grade record
     */
    public abstract boolean isCourseGradeRecord();

    /**
     * @return Returns the pointsEarned
     */
    public abstract Double getPointsEarned();

    @Override
	public String toString() {

        return new ToStringBuilder(this).
		append("id", this.id).
		append("studentId", this.studentId).
        append("graderId", this.graderId).toString();
    }
}



