package com.stallapp.mongodb.framework.document;

import org.springframework.data.annotation.Id;

import com.querydsl.core.annotations.QueryEntity;

@QueryEntity
public abstract class BaseDocument<ID> {

	@Id
	private ID id;

	public ID getId() {
		return id;
	}

	public void setId(ID id) {
		this.id = id;
	}
}
