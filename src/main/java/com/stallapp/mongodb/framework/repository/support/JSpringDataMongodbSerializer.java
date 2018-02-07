package com.stallapp.mongodb.framework.repository.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.util.Assert;

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathType;
import com.querydsl.mongodb.MongodbSerializer;

public class JSpringDataMongodbSerializer extends MongodbSerializer {
	private static final String ID_KEY = "_id";
	private static final Set<PathType> PATH_TYPES;

	static {

		Set<PathType> pathTypes = new HashSet<PathType>();
		pathTypes.add(PathType.VARIABLE);
		pathTypes.add(PathType.PROPERTY);

		PATH_TYPES = Collections.unmodifiableSet(pathTypes);
	}

	private final MongoConverter converter;
	private final MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext;
	private final QueryMapper mapper;

	/**
	 * Creates a new {@link SpringDataMongodbSerializer} for the given {@link MappingContext}.
	 * 
	 * @param mappingContext must not be {@literal null}.
	 */
	public JSpringDataMongodbSerializer(MongoConverter converter) {

		Assert.notNull(converter, "MongoConverter must not be null!");

		this.mappingContext = converter.getMappingContext();
		this.converter = converter;
		this.mapper = new QueryMapper(converter);
	}

	/*
	 * (non-Javadoc)
	 * @see com.querydsl.mongodb.MongodbSerializer#getKeyForPath(com.querydsl.core.types.Path, com.querydsl.core.types.PathMetadata)
	 */
	@Override
	protected String getKeyForPath(Path<?> expr, PathMetadata metadata) {

		if (!metadata.getPathType().equals(PathType.PROPERTY)) {
			return super.getKeyForPath(expr, metadata);
		}

		Path<?> parent = metadata.getParent();
		MongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(parent.getType());
		MongoPersistentProperty property = entity.getPersistentProperty(metadata.getName());

		return property == null ? super.getKeyForPath(expr, metadata) : property.getFieldName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.querydsl.mongodb.MongodbSerializer#asDBObject(java.lang.String, java.lang.Object)
	 */
	@Override
	protected DBObject asDBObject(String key, Object value) {

		if (ID_KEY.equals(key)) {
			return mapper.getMappedObject(super.asDBObject(key, value), null);
		}

		return super.asDBObject(key, value instanceof Pattern ? value : converter.convertToMongoType(value));
	}

	/*
	 * (non-Javadoc)
	 * @see com.querydsl.mongodb.MongodbSerializer#isReference(com.querydsl.core.types.Path)
	 */
	@Override
	protected boolean isReference(Path<?> path) {

		MongoPersistentProperty property = getPropertyFor(path);
		return property == null ? false : property.isAssociation();
	}

	/*
	 * (non-Javadoc)
	 * @see com.querydsl.mongodb.MongodbSerializer#asReference(java.lang.Object)
	 */
	@Override
	protected DBRef asReference(Object constant) {
		return converter.toDBRef(constant, null);
	}

	private MongoPersistentProperty getPropertyFor(Path<?> path) {

		Path<?> parent = path.getMetadata().getParent();

		if (parent == null || !PATH_TYPES.contains(path.getMetadata().getPathType())) {
			return null;
		}

		MongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(parent.getType());
		return entity != null ? entity.getPersistentProperty(path.getMetadata().getName()) : null;
	}
}
