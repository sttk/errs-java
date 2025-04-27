/*
 * ExcHandler class.
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.errs;

import java.time.OffsetDateTime;

/**
 * {@code ExcHandler} is a handler of an {@link Exc} object creation.
 */
@FunctionalInterface
public interface ExcHandler {

    /**
     * Handles an {@link Exc} object creation.
     *
     * @param exc
     *            The {@link Exc} object.
     * @param tm
     *            The creation time of the {@link Exc} object.
     */
    void handle(Exc exc, OffsetDateTime tm);
}
