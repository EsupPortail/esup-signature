package org.esupportail.esupsignature;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class JpaTest {

    @PersistenceContext
    private EntityManager entityManager;

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