package com.stallapp.mongodb.framework.respository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.querydsl.QSort;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.util.Assert;

import com.mongodb.BasicDBObject;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.mongodb.AbstractMongodbQuery;
import com.stallapp.mongodb.framework.document.BaseDocument;
import com.stallapp.mongodb.framework.repository.support.JSpringDataMongodbQuery;

public abstract class AbstractJMongoRespository<T extends BaseDocument<ID>, ID extends Serializable>
		implements IJMongoRepository<T, ID>, MongoRepository<T, ID>, QueryDslPredicateExecutor<T> {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private MongoRepositoryFactory mongoRepositoryFactory;

	private Class<T> entityClass;

	private PathBuilder<T> builder;

	protected AbstractJMongoRespository(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	@PostConstruct
	private void init() {
		EntityPath<T> path = SimpleEntityPathResolver.INSTANCE.createPath(entityInformation().getJavaType());
		this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
	}

	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be null!");

		if (entityInformation().isNew(entity)) {
			mongoTemplate.insert(entity, entityInformation().getCollectionName());
		} else {
			mongoTemplate.save(entity, entityInformation().getCollectionName());
		}

		return entity;
	}

	
	public <S extends T> List<S> save(Iterable<S> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");

		List<S> result = convertIterableToList(entities);
		boolean allNew = true;

		for (S entity : entities) {
			if (allNew && !entityInformation().isNew(entity)) {
				allNew = false;
			}
		}

		if (allNew) {
			mongoTemplate.insertAll(result);
		} else {

			result.forEach(entity -> save(entity));
			/*for (S entity : result) {
				save(entity);
			}*/
		}

		return result;
	}

	public T findOne(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return mongoTemplate.findById(id, entityInformation().getJavaType(), entityInformation().getCollectionName());
	}

	private Query getIdQuery(Object id) {
		return new Query(getIdCriteria(id));
	}

	private Criteria getIdCriteria(Object id) {
		return where(entityInformation().getIdAttribute()).is(id);
	}


	public boolean exists(ID id) {

		Assert.notNull(id, "The given id must not be null!");
		return mongoTemplate.exists(getIdQuery(id), entityInformation().getJavaType(),
				entityInformation().getCollectionName());
	}

	
	public long count() {
		return mongoTemplate.getCollection(entityInformation().getCollectionName()).count();
	}


	public void delete(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		mongoTemplate.remove(getIdQuery(id), entityInformation().getJavaType(),
				entityInformation().getCollectionName());
	}

	public void delete(T entity) {
		Assert.notNull(entity, "The given entity must not be null!");
		delete(entityInformation().getId(entity));
	}

	public void delete(Iterable<? extends T> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");

		entities.forEach(entity -> delete(entity));
		
		/*for (T entity : entities) {
			delete(entity);
		}*/
	}

	public void deleteAll() {
		mongoTemplate.remove(new Query(), entityInformation().getCollectionName());
	}

	public List<T> findAll() {
		return findAll(new Query());
	}

	public Iterable<T> findAll(Iterable<ID> ids) {

		Set<ID> parameters = new HashSet<ID>(tryDetermineRealSizeOrReturn(ids, 10));
		ids.forEach(id -> parameters.add(id));
		
		/*for (ID id : ids) {
			parameters.add(id);
		}*/

		return findAll(new Query(new Criteria(entityInformation().getIdAttribute()).in(parameters)));
	}

	public Page<T> findAll(final Pageable pageable) {

		Long count = count();
		List<T> list = findAll(new Query().with(pageable));

		return new PageImpl<T>(list, pageable, count);
	}

	public List<T> findAll(Sort sort) {
		return findAll(new Query().with(sort));
	}

	@Override
	public <S extends T> S insert(S entity) {

		Assert.notNull(entity, "Entity must not be null!");

		mongoTemplate.insert(entity, entityInformation().getCollectionName());
		return entity;
	}

	@Override
	public <S extends T> List<S> insert(Iterable<S> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");

		List<S> list = convertIterableToList(entities);

		if (list.isEmpty()) {
			return list;
		}

		mongoTemplate.insertAll(list);
		return list;
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {

		Assert.notNull(example, "Sample must not be null!");

		Query q = new Query(new Criteria().alike(example)).with(pageable);

		long count = mongoTemplate.count(q, example.getProbeType(), entityInformation().getCollectionName());

		if (count == 0) {
			return new PageImpl<S>(Collections.<S>emptyList());
		}

		return new PageImpl<S>(mongoTemplate.find(q, example.getProbeType(), entityInformation().getCollectionName()),
				pageable, count);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {

		Assert.notNull(example, "Sample must not be null!");

		Query q = new Query(new Criteria().alike(example));

		if (sort != null) {
			q.with(sort);
		}

		return mongoTemplate.find(q, example.getProbeType(), entityInformation().getCollectionName());
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		return findAll(example, (Sort) null);
	}

	@Override
	public <S extends T> S findOne(Example<S> example) {

		Assert.notNull(example, "Sample must not be null!");

		Query q = new Query(new Criteria().alike(example));
		return mongoTemplate.findOne(q, example.getProbeType(), entityInformation().getCollectionName());
	}

	@Override
	public <S extends T> long count(Example<S> example) {

		Assert.notNull(example, "Sample must not be null!");

		Query q = new Query(new Criteria().alike(example));
		return mongoTemplate.count(q, example.getProbeType(), entityInformation().getCollectionName());
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {

		Assert.notNull(example, "Sample must not be null!");

		Query q = new Query(new Criteria().alike(example));
		return mongoTemplate.exists(q, example.getProbeType(), entityInformation().getCollectionName());
	}

	private List<T> findAll(Query query) {

		if (query == null) {
			return Collections.emptyList();
		}

		return mongoTemplate.find(query, entityInformation().getJavaType(), entityInformation().getCollectionName());
	}

	private static <T> List<T> convertIterableToList(Iterable<T> entities) {

		if (entities instanceof List) {
			return (List<T>) entities;
		}

		int capacity = tryDetermineRealSizeOrReturn(entities, 10);

		if (capacity == 0 || entities == null) {
			return Collections.<T>emptyList();
		}

		List<T> list = new ArrayList<T>(capacity);
		entities.forEach(entity -> list.add(entity));
		
		/*for (T entity : entities) {
			list.add(entity);
		}*/

		return list;
	}

	private static int tryDetermineRealSizeOrReturn(Iterable<?> iterable, int defaultSize) {
		return iterable == null ? 0
				: (iterable instanceof Collection) ? ((Collection<?>) iterable).size() : defaultSize;
	}

	private MongoEntityInformation<T, ID> entityInformation() {
		@SuppressWarnings("unchecked")
		MongoEntityInformation<T, ID> entityInformation = (MongoEntityInformation<T, ID>) mongoRepositoryFactory
				.getEntityInformation(entityClass);
		return (MongoEntityInformation<T, ID>) entityInformation;
	}

	@Override
	public T findOne(Predicate predicate) {
		return createQueryFor(predicate).fetchOne();
	}

	@Override
	public List<T> findAll(Predicate predicate) {
		return createQueryFor(predicate).fetchResults().getResults();
	}

	@Override
	public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		return createQueryFor(predicate).orderBy(orders).fetchResults().getResults();
	}


	@Override
	public List<T> findAll(Predicate predicate, Sort sort) {
		return applySorting(createQueryFor(predicate), sort).fetchResults().getResults();
	}

	@Override
	public Iterable<T> findAll(OrderSpecifier<?>... orders) {
		return createQuery().orderBy(orders).fetchResults().getResults();
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> countQuery = createQueryFor(predicate);
		AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> query = createQueryFor(predicate);

		return new PageImpl<T>(applyPagination(query, pageable).fetchResults().getResults(), pageable,
				countQuery.fetchCount());
	}

	@Override
	public long count(Predicate predicate) {
		return createQueryFor(predicate).fetchCount();
	}

	@Override
	public boolean exists(Predicate predicate) {
		return createQueryFor(predicate).fetchCount() > 0;
	}

	/**
	 * Creates a {@link MongodbQuery} for the given {@link Predicate}.
	 * 
	 * @param predicate
	 * @return
	 */
	private AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> createQueryFor(Predicate predicate) {
		return createQuery().where(predicate);
	}

	/**
	 * Creates a {@link MongodbQuery}.
	 * 
	 * @return
	 */
	private AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> createQuery() {
		return new JSpringDataMongodbQuery<T>(mongoTemplate, entityInformation().getJavaType());
	}

	/**
	 * Applies the given {@link Pageable} to the given {@link MongodbQuery}.
	 * 
	 * @param query
	 * @param pageable
	 * @return
	 */
	private AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> applyPagination(
			AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> query, Pageable pageable) {

		if (pageable == null) {
			return query;
		}

		query = query.offset(pageable.getOffset()).limit(pageable.getPageSize());
		return applySorting(query, pageable.getSort());
	}

	/**
	 * Applies the given {@link Sort} to the given {@link MongodbQuery}.
	 * 
	 * @param query
	 * @param sort
	 * @return
	 */
	private AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> applySorting(
			AbstractMongodbQuery<T, JSpringDataMongodbQuery<T>> query, Sort sort) {

		if (sort == null) {
			return query;
		}

		// TODO: find better solution than instanceof check
		if (sort instanceof QSort) {

			List<OrderSpecifier<?>> orderSpecifiers = ((QSort) sort).getOrderSpecifiers();
			query.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[orderSpecifiers.size()]));

			return query;
		}

		sort.forEach(order -> query.orderBy(toOrder(order)));
		
		/*for (Order order : sort) {
			query.orderBy(toOrder(order));
		}*/

		return query;
	}

	/**
	 * Transforms a plain {@link Order} into a QueryDsl specific
	 * {@link OrderSpecifier}.
	 * 
	 * @param order
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OrderSpecifier<?> toOrder(Order order) {

		Expression<Object> property = builder.get(order.getProperty());

		return new OrderSpecifier(
				order.isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC, property);
	}
	
	public boolean bulkUpdate(List<T> entities) throws BulkOperationException {
		return bulkUpdate(entities, BulkOperations.BulkMode.ORDERED);
	}
	
	public boolean bulkUpdate(List<T> entities, BulkMode mode) throws BulkOperationException {
		MongoConverter converter = mongoTemplate.getConverter();
		// ConversionService conversionService =
		// converter.getConversionService();
		com.mongodb.DBObject dbObject;
		BulkOperations bulkOps = mongoTemplate.bulkOps(mode,
				entityInformation().getJavaType());
		for (T entity : entities) {
			if (entityInformation().isNew(entity)) { // --- if NEW entity, then
														// generate id and
														// INSERT ---
				bulkOps.insert(entity);

			} else {
				// --- if EXISTING entity, then UPSERT ---
				// convert entity to mongo DBObject
				dbObject = new BasicDBObject();
				// NULL fields will NOT BE UPDATED - will be ignored when
				// converting an entity to a {@link com.mongodb.DBObject}
				// and thus they will not be added to the {@link Update}
				// statement.
				converter.write(entity, dbObject);
				// upsert
				bulkOps.upsert(new Query(Criteria.where("_id").is(dbObject.get("_id"))),
						Update.fromDBObject(new BasicDBObject("$set", dbObject)));
			}
		}
		bulkOps.execute();
		return true;
	}
	
	
}
