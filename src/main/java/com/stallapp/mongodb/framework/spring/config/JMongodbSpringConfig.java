package com.stallapp.mongodb.framework.spring.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

@Configuration
@ComponentScan(basePackages = "com.stallapp.mongodb.framework")
public class JMongodbSpringConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() throws IOException {
		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
		propertySourcesPlaceholderConfigurer.setIgnoreResourceNotFound(true); //TODO check this behaviour

		//String propsLocation = System.getProperty(key, def)
		
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		yaml.setResources(new ClassPathResource("application.yml"));
		//yaml.setResources(new ClassPathResource("application.yaml"));
		
		PropertiesFactoryBean props = new PropertiesFactoryBean();
		props.setLocation(new ClassPathResource("application.properties"));
		props.setLocation(new ClassPathResource("application.props"));
		
		Properties[] propsList = new Properties[]{yaml.getObject(), props.getObject()};
		propertySourcesPlaceholderConfigurer.setPropertiesArray(propsList);
		
		return propertySourcesPlaceholderConfigurer;
	}
}
