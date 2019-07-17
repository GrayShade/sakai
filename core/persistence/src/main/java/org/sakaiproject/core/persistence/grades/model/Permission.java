/**
 * Copyright (c) 2003-2009 The Apereo Foundation
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
@Table(name = "GB_PERMISSION_TT")
@Getter @Setter
public class Permission implements Serializable {

    private static final String SEQUENCE_GENERATOR = "grading_permission_sequence";

    @Id
    @Column(name = "GB_PERMISSION_ID")
    @SequenceGenerator(name = SEQUENCE_GENERATOR, sequenceName = "GB_PERMISSION_S")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = SEQUENCE_GENERATOR)
	private Long id;

    @Column(name = "VERSION")
	private int version;

    @Column(name = "GRADEBOOK_ID", nullable = false)
	private Long gradebookId;

    @Column(name = "USER_ID", nullable = false)
	private String userId;

    @Column(name = "FUNCTION_NAME", nullable = false)
	private String function;

    @Column(name = "CATEGORY_ID")
	private Long categoryId;

    @Column(name = "GROUP_ID")
	private String groupId;
}
