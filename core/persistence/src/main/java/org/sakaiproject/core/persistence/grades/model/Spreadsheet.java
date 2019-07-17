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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "GB_SPREADSHEET_TT")
@Getter @Setter
public class Spreadsheet implements Serializable {
   
	private static final long serialVersionUID = 1L;
	
    private static final String SEQUENCE_GENERATOR = "grading_spreadsheet_sequence";

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_SPREADSHEET_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
    protected Long id;

    @ManyToOne
    @JoinColumn(name = "GRADEBOOK_ID")
	protected Gradebook gradebook;

    @Column(name = "VERSION")
    protected int version;

    @Lob
    @Column(name = "CONTENT", length = 16777215, nullable = false)
    protected String content;

    @Column(name = "CREATOR", nullable = false)
    protected String creator;

    @Column(name = "NAME", nullable = false)
    protected String name;

    @Column(name = "DATE_CREATED", nullable = false)
    protected Date dateCreated;

    public Spreadsheet() {
    }

    public Spreadsheet(Gradebook gradebook, String content, String creator, String name, Date dateCreated) {

        this.gradebook = gradebook;
        this.content = content;
        this.creator = creator;
        this.name = name;
        this.dateCreated = dateCreated;
    }

    @Override
	public boolean equals(Object other) {

        if (!(other instanceof Spreadsheet)) {
        	return false;
        }
        Spreadsheet sp = (Spreadsheet) other;
        return new EqualsBuilder()
            .append(this.gradebook, sp.getGradebook())
            .append(this.id, sp.getId())
            .append(this.name, sp.getName()).isEquals();
    }

    @Override
	public int hashCode() {

        return new HashCodeBuilder().
            append(gradebook).
            append(id).
            append(name).
            toHashCode();
	}

    @Override
	public String toString() {

        return new ToStringBuilder(this).
            append("id", id).
            append("name", name).toString();
    }
}
