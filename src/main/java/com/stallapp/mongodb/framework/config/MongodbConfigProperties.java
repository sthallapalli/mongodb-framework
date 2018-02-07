package com.stallapp.mongodb.framework.config;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class MongodbConfigProperties {

	@NotNull(message = "Hostname can not be null")
	@Value("${stallapp.mongo.host}")
	private String host;

	@NotNull(message = "Port can not be null")
	@Min(value = 1025, message = "Minimum value should not be less than 1025")
	@Max(value = 65535, message = "Maximum value should not be greater than 65535")
	@Value("${stallapp.mongo.port}")
	private int port;

	@NotNull(message = "Database name can not be null")
	@Value("${stallapp.mongo.databaseName}")
	private String databaseName;

	@Value("${stallapp.mongo.userName}")
	private String userName;

	@Value("${stallapp.mongo.password}")
	private String password;

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return "MongodbConfigProperties [host=" + host + ", port=" + port + ", databaseName=" + databaseName
				+ ", userName=" + userName + ", password=" + password + "]";
	}
}
