package com.cappielloantonio.tempo.util.paging

import com.cappielloantonio.tempo.util.state.RemoteState.Success

/**
 * Используйте эту функцию для изменения состояния обновления (isRefreshing) в экземпляре PagingState,
 * не затрагивая другие данные, находящиеся в объекте RemoteState.Success
 */
fun <T> Success<PagingState<T>>.withRefreshing(
    value: Boolean
): Success<PagingState<T>> = this.copy(data = this.data.copy(isRefreshing = value))

/**
 * Используйте эту функцию для изменения состояния обновления (isNextPageLoading) в экземпляре PagingState,
 * не затрагивая другие данные, находящиеся в объекте RemoteState.Success
 */
fun <T> Success<PagingState<T>>.withNextPageLoading(
    value: Boolean
): Success<PagingState<T>> = this.copy(data = this.data.copy(isNextPageLoading = value))
