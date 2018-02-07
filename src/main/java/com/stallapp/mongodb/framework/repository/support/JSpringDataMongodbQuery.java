package com.stallapp.mongodb.framework.repository.support;

import org.springframework.data.mongodb.core.MongoOperations;

import com.google.common.base.Function;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.querydsl.mongodb.AbstractMongodbQuery;

public class JSpringDataMongodbQuery<T> extends AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> {

	private final MongoOperations operations;

	/**
	 * Creates a new {@link SpringDataMongodbQuery}.
	 * 
	 * @param operations
	 *            must not be {@literal null}.
	 * @param type
	 *            must not be {@literal null}.
	 */
	public JSpringDataMongodbQuery(final MongoOperations operations, final Class<? extends T> type) {
		this(operations, type, operations.getCollectionName(type));
	}

	/**
	 * Creates a new {@link SpringDataMongodbQuery} to query the given
	 * collection.
	 * 
	 * @param operations
	 *            must not be {@literal null}.
	 * @param type
	 *            must not be {@literal null}.
	 * @param collectionName
	 *            must not be {@literal null} or empty.
	 */
	public JSpringDataMongodbQuery(final MongoOperations operations, final Class<? extends T> type,
			String collectionName) {

		super(operations.getCollection(collectionName), new Function<DBObject, T>() {
			public T apply(DBObject input) {
				return operations.getConverter().read(type, input);
			}
		}, new JSpringDataMongodbSerializer(operations.getConverter()));

		this.operations = operations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.querydsl.mongodb.AbstractMongodbQuery#getCollection(java.lang.Class)
	 */
	@Override
	protected DBCollection getCollection(Class<?> type) {
		return operations.getCollection(operations.getCollectionName(type));
	}

}
