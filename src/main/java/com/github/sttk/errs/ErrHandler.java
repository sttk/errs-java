/*
 * ErrHandler class.
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.errs;

import java.time.OffsetDateTime;

/** {@code ErrHandler} is a handler of an {@link Err} object creation. */
@FunctionalInterface
public interface ErrHandler {

  /**
   * Handles an {@link Err} object creation.
   *
   * @param err The {@link Err} object.
   * @param tm The creation time of the {@link Err} object.
   */
  void handle(Err err, OffsetDateTime tm);
}
