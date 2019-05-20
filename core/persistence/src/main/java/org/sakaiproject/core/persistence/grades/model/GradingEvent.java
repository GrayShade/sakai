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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * A log of grading activity.  A GradingEvent should be saved any time a grade
 * record is added or modified.  GradingEvents should be added when the entered
 * value of a course grade record is added or modified, but not when the
 * autocalculated value changes.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
@Entity
@Table(name = "GB_GRADING_EVENT_T")
@Getter @Setter
public class GradingEvent implements Comparable<Object>, Serializable {
    
	private static final long serialVersionUID = 1L;
	
    private static final String SEQUENCE_GENERATOR = "grading_grading_event_s";

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_GRADING_EVENT_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
	private Long id;

    @Column(name = "GRADER_ID", nullable = false)
    private String graderId;

    @Column(name = "STUDENT_ID", nullable = false)
    private String studentId;

    @ManyToOne
    @JoinColumn(name = "GRADABLE_OBJECT_ID")
    private GradableObject gradableObject;

    @Column(name = "GRADE")
    private String grade;

    @Column(name = "DATE_GRADED", nullable = false)
    private Date dateGraded;

    public GradingEvent() {
        this.dateGraded = new Date();
    }

    public GradingEvent(GradableObject gradableObject, String graderId, String studentId, Object grade) {

        this.gradableObject = gradableObject;
        this.graderId = graderId;
        this.studentId = studentId;
        if (grade != null) {
        	this.grade = grade.toString();
        }
        this.dateGraded = new Date();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
	public int compareTo(Object o) {
        return this.dateGraded.compareTo(((GradingEvent)o).dateGraded);
    }
}



