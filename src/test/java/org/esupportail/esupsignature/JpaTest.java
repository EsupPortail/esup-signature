package org.esupportail.esupsignature;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;
import java.util.regex.Pattern;

@ExtendWith(SpringExtension.class)
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
    public void testUpdateDatabase() throws ClassNotFoundException {
        Session session = (Session) entityManager.getDelegate();
        serviceRegistry = new StandardServiceRegistryBuilder().applySettings(session.getSessionFactory().getProperties()).build();
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        final Set<BeanDefinition> beanDefinitions = provider.findCandidateComponents("org.esupportail.esupsignature.entity");
        for(BeanDefinition beanDefinition : beanDefinitions) {
            metadataSources.addAnnotatedClass(Class.forName(beanDefinition.getBeanClassName()));
        }
        Metadata metadata =  metadataSources.buildMetadata();
    }

}