/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.common.futures;

import javax.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A super simple Future-like class that can safely notify another Thread when a value is ready.
 * Does not support canceling.
 */
public class SimpleSettableFuture<T> implements Future<T> {
  private final CountDownLatch mReadyLatch = new CountDownLatch(1);
  private @Nullable T mResult;
  private @Nullable Exception mException;

  /**
   * Sets the result. If another thread has called {@link #get}, they will immediately receive the
   * value. set or setException must only be called once.
   */
  public void set(T result) {
    checkNotSet();
    mResult = result;
    mReadyLatch.countDown();
  }

  /**
   * Sets the eception. If another thread has called {@link #get}, they will immediately receive the
   * exception. set or setException must only be called once.
   */
  public void setException(Exception exception) {
    checkNotSet();
    mException = exception;
    mReadyLatch.countDown();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return mReadyLatch.getCount() == 0;
  }

  @Deprecated
  @Override
  public T get() throws InterruptedException, ExecutionException {
    throw new UnsupportedOperationException("Must use a timeout");
  }

  /**
   * Wait up to the timeout time for another Thread to set a value on this future. If a value has
   * already been set, this method will return immediately.
   *
   * NB: For simplicity, we catch and wrap InterruptedException. Do NOT use this class if you
   * are in the 1% of cases where you actually want to handle that.
   */
  @Override
  public @Nullable T get(long timeout, TimeUnit unit) throws
      InterruptedException, ExecutionException, TimeoutException {
    try {
      if (!mReadyLatch.await(timeout, unit)) {
        throw new TimeoutException("Timed out waiting for result");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (mException != null) {
      throw new ExecutionException(mException);
    }

    return mResult;
  }

  private void checkNotSet() {
    if (mReadyLatch.getCount() == 0) {
      throw new RuntimeException("Result has already been set!");
    }
  }
}