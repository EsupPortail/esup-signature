package org.esupportail.esupsignature.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;

@Configuration
@EnableSpringConfigured
@EnableTransactionManagement(mode=AdviceMode.ASPECTJ)
@EnableJpaRepositories(basePackages = "org.esupportail.esupsignature.domain")
public class PersistenceConfig {

	@Value("${database.driverClassName}")
	private String driverClassName;
	@Value("${database.url}")
	private String url;
	@Value("${database.username}")
	private String username;
	@Value("${database.password}")
	private String password;
	
	@Bean
	  public BasicDataSource dataSource() {
	    BasicDataSource basicDataSource = new BasicDataSource();
	    basicDataSource.setDriverClassName(driverClassName);
	    basicDataSource.setUrl(url);
	    basicDataSource.setUsername(username);
	    basicDataSource.setPassword(password);
	    basicDataSource.setTestOnBorrow(true);
	    basicDataSource.setTestOnReturn(true);
	    basicDataSource.setTimeBetweenEvictionRunsMillis(1800000);
	    basicDataSource.setNumTestsPerEvictionRun(3);
	    basicDataSource.setMinEvictableIdleTimeMillis(1800000);
	    basicDataSource.setValidationQuery("SELECT version();");
	    return basicDataSource;
	  }
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactory.setPersistenceUnitName("persistenceUnit");
		entityManagerFactory.setDataSource(dataSource);
		entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		entityManagerFactory.setJpaDialect(new HibernateJpaDialect());
		entityManagerFactory.setPackagesToScan("org.esupportail.esupsignature.domain");
		entityManagerFactory.setJpaPropertyMap(hibernateJpaProperties());
		return entityManagerFactory;
	}

	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory);
		return transactionManager;
	}
	  
	private Map<String, ?> hibernateJpaProperties() {
	    HashMap<String, String> properties = new HashMap<>();
	    properties.put("hibernate.dialect", "org.esupportail.esupsignature.postgres.PgFullTextDialect");
	    properties.put("hibernate.hbm2ddl.auto", "create");
	    properties.put("hibernate.hbm2ddl.import_files", "import.sql");
	    properties.put("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy");
	    properties.put("hibernate.connection.charSet", "UTF-8");
	    return properties;
	  }
	
	@Bean
    public AnnotationTransactionAspect annotationTransactionAspect(JpaTransactionManager transactionManager) {
        AnnotationTransactionAspect annotationTransactionAspect = AnnotationTransactionAspect.aspectOf();
        annotationTransactionAspect.setTransactionManager(transactionManager);
        return annotationTransactionAspect;
    }
}
