package com.stallapp.mongodb.framework.respository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;

import com.stallapp.mongodb.framework.document.BaseDocument;

public interface IJMongoRepository<T extends BaseDocument<ID>, ID extends Serializable> {

	public boolean bulkUpdate(List<T> entities, BulkMode mode) throws BulkOperationException;

	public boolean bulkUpdate(List<T> entities) throws BulkOperationException;
}
