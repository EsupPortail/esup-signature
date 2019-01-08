package org.esupportail.esupsignature.config;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableSpringConfigured
@EnableTransactionManagement(mode=AdviceMode.ASPECTJ)
public class Config {

	@Resource
	private DataSource dataSource;
	
	@Bean
	  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
	      LocalContainerEntityManagerFactoryBean thing = new LocalContainerEntityManagerFactoryBean();
	      thing.setPersistenceUnitName("persistenceUnit");
	      thing.setDataSource(dataSource);
	      return thing;
	  }

	  @Bean
	  public JpaTransactionManager transactionManager() {
	      JpaTransactionManager transactionManager = new JpaTransactionManager();
	      transactionManager.setEntityManagerFactory(entityManagerFactory().getNativeEntityManagerFactory());
	      return transactionManager;
	  } 
	
}
