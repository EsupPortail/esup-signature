package org.esupportail.esupsignature;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = "app.scheduling.enable=false")
public class JpaTest {

    @PersistenceContext
    transient EntityManager entityManager;

    @Test
    public void testJpaConnexion(){
        entityManager.createNativeQuery("SELECT 1").getSingleResult();
    }

}
