package com.stallapp.mongodb.framework.config;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;

@Configuration
public class JMongodbConfiguration extends AbstractMongoConfiguration {

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private MongodbConfigProperties mongodbConfigProperties;

	@Override
	protected String getDatabaseName() {
		return mongodbConfigProperties.getDatabaseName();
	}

	@Override
	public Mongo mongo() throws UnknownHostException {
		return new MongoClient(mongodbConfigProperties.getHost(), mongodbConfigProperties.getPort());
	}

	@Bean
	public MongoDbFactory mongoDbFactory() throws UnknownHostException {
		MongoClient mongoClient = new MongoClient(mongodbConfigProperties.getHost(),
				mongodbConfigProperties.getPort());
		// UserCredentials userCredentials = new UserCredentials("", "");
		return new SimpleMongoDbFactory(mongoClient, getDatabaseName());
	}

	@Bean
	public MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
	}

	@Bean
	public MappingMongoConverter mappingMongoConverter() throws Exception {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
		MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext());
		converter.setCustomConversions(customConversions());
		return converter;
	}
	
	@Bean
	public MongoRepositoryFactory mongoRepositoryFactory() throws Exception {
		MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate());
		return factory;
	}
	
	@Bean
	public MongoMappingContext mongoMappingContext() throws ClassNotFoundException {
		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.setInitialEntitySet(getInitialEntitySet());
		mappingContext.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
		mappingContext.setFieldNamingStrategy(fieldNamingStrategy());
		mappingContext.setApplicationContext(applicationContext);

		return mappingContext;
	}

}
