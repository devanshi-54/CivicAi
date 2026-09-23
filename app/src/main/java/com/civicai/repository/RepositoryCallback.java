package com.civicai.repository;

/**
 * Generic asynchronous callback for data operations across Repositories and Services.
 *
 * @param <T> Result payload type
 */
public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception exception);
}
