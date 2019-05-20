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

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "GB_PROPERTY_T")
@Getter @Setter
public class GradebookProperty implements Serializable, Comparable<Object> {
	
	private static final long serialVersionUID = 1L;
	
    private static final String SEQUENCE_GENERATOR = "grading_property_sequence";

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_PROPERTY_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
	private Long id;

    @Column(name = "VERSION")
	private int version;

    @Column(name = "NAME", nullable = false)
	private String name;

    @Column(name = "VALUE")
	private String value;

	public GradebookProperty() {
	}

	public GradebookProperty(String name) {
		this.name = name;
	}

    @Override
	public int compareTo(Object o) {
        return this.name.compareTo(((GradebookProperty)o).getName());
    }
    @Override
	public String toString() {

        return new ToStringBuilder(this).
            append(this.name).toString();
    }
}
