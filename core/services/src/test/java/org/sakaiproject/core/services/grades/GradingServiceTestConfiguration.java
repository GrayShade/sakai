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
package org.sakaiproject.core.services.grades;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;
import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.mockito.Mockito;

import org.sakaiproject.core.api.grades.GradingPersistenceManager;
import org.sakaiproject.core.api.grades.GradingService;
import org.sakaiproject.core.api.grades.SakaiProxy;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.section.api.SectionAwareness;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackages = "org.sakaiproject.core.services.grades")
@EnableTransactionManagement
@EnableJpaRepositories("org.sakaiproject.core.persistence.grades")
public class GradingServiceTestConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public DataSource dataSource() {

        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder.setType(EmbeddedDatabaseType.HSQL).build();
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.sakaiproject.core.persistence.grades");
        factory.setDataSource(dataSource());
        factory.afterPropertiesSet();

        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager() {

        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory());
        return txManager;
    }

    @Bean(name = "org.sakaiproject.tool.api.SessionManager")
    public SessionManager sessionManager() {

        Session session = mock(Session.class);
        when(session.getUserId()).thenReturn("1518418B-4737-498C-8F0B-7016D912F3FB");

        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.getCurrentSession()).thenReturn(session);

        return sessionManager;
    }

    @Bean
    public EventTrackingService eventTrackingService() {
        return mock(EventTrackingService.class);
    }

    @Bean
    public SakaiProxy sakaiProxy() {

        SakaiProxy sakaiProxy = mock(SakaiProxy.class);
        when(sakaiProxy.getCurrentUserId()).thenReturn("1518418B-4737-498C-8F0B-7016D912F3FB");
        return sakaiProxy;
    }

    @Bean
    public SiteService siteService() {

        return mock(SiteService.class);
        //when(siteService.siteReference(gradebookUid)).thenReturn("/site/" + gradebookUid);
    }

    @Bean
    public SecurityService securityService() {
        return mock(SecurityService.class);
    }

    @Bean
    public SectionAwareness sectionAwareness() {
        return mock(SectionAwareness.class);
    }

    @Bean
    public ServerConfigurationService serverConfigurationService() {
        return mock(ServerConfigurationService.class);
    }
}
