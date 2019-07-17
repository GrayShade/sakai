/*******************************************************************************
 * Copyright (c) 2006, 2008 The Sakai Foundation, The MIT Corporation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "GB_COMMENT_TT", uniqueConstraints = @UniqueConstraint(name = "G_O_STUDENT", columnNames = {"STUDENT_ID", "GRADABLE_OBJECT_ID"}))
@Getter @Setter
public class Comment implements Serializable {

    private static final String SEQUENCE_GENERATOR = "grading_comment_sequence";

    @Id
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_COMMENT_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
    @Column(name = "ID")
	private Long id;

    @Column(name = "STUDENT_ID", nullable = false)
    private String studentId;

    @Column(name = "GRADER_ID", nullable = false)
    private String graderId;

    @Column(name = "VERSION")
    private int version;

    @Column(name = "DATE_RECORDED")
    private Date dateRecorded;

    @Column(name = "COMMENT_TEXT")
    private String commentText;

    @ManyToOne
    @JoinColumn(name = "GRADABLE_OBJECT_ID")
    private GradableObject gradableObject;

    public Comment() {
    }

    public Comment(String studentId, String comment, GradableObject gradableObject) {

        this.gradableObject = gradableObject;
        this.studentId = studentId;
        this.commentText = comment;
    }

    @Override
	public String toString() {

        return new ToStringBuilder(this).
                append("id", id).
                append("grader", graderId).
                append("comment",commentText).
                append("studentid",studentId).toString();

    }

    @Override
	public boolean equals(Object other) {

        if (!(other instanceof Comment)) {
            return false;
        }
        Comment comment = (Comment)other;
        return new EqualsBuilder()
            .append(this.gradableObject, comment.getGradableObject())
            .append(this.id, comment.getId())
            .append(this.commentText, comment.getCommentText()).isEquals();
    }

    @Override
	public int hashCode() {

        return new HashCodeBuilder().
          append(this.gradableObject).
          append(this.id).
          append(this.commentText).
          toHashCode();
	}
}

