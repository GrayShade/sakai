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
package org.sakaiproject.core.api.grades;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 *  DTO for the {@link org.sakaiproject.gradebook.tool.Permission} to pass to external services. Not persisted.
 */
@Getter @Setter
public class PermissionDefinition implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String userId;
    private String function;
    private Long categoryId;
    private String groupReference;
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PermissionDefinition)) {
            return false;
        }
        
        PermissionDefinition other  = (PermissionDefinition) o;
        
        return new EqualsBuilder()
                // id purposely not included so that we dont get duplicate permissions
                .append(userId, other.userId)
                .append(function, other.function)
                .append(categoryId, other.categoryId)
                .append(groupReference, other.groupReference)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(userId)
                .append(function)
                .append(categoryId)
                .append(groupReference)
                .hashCode();
    }
}
