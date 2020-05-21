package org.esupportail.esupsignature;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.User;
import org.h2.util.Task;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.hbm2ddl.Target;
import org.hibernate.tool.schema.TargetType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class JpaTest {

    @PersistenceContext
    transient EntityManager entityManager;

    private StandardServiceRegistry serviceRegistry;

    @Test
    public void testJpaConnexion() {
        entityManager.createNativeQuery("SELECT 1").getSingleResult();
    }

    @Test
    public void testUpdateDatabase() {
        Session session = (Session) entityManager.getDelegate();
        serviceRegistry = new StandardServiceRegistryBuilder().applySettings(session.getSessionFactory().getProperties()).build();
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        Reflections reflections = new Reflections("org.esupportail.esupsignature.entity");
        Set<Class<?>> allClasses = reflections.getTypesAnnotatedWith(Entity.class);
        for(Class<?> eClass : allClasses) {
            metadataSources.addAnnotatedClass(eClass);
        }
        Metadata metadata =  metadataSources.buildMetadata();
        new SchemaValidator().validate(metadata);

    }

}