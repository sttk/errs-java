/*
 * Exc class.
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.errs;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.InvalidObjectException;

/**
 * Is the exception class with a reason.
 * <p>
 * This class has a record field which indicates a reason for this exception. The class name of the reason record
 * represents the type of reason, and the fields of the reason record hold the situation where the exception occurred.
 * <p>
 * Optionally, this exception class can notify its instance creation to pre-registered exception handlers.
 * <p>
 * The example code of creating and throwing an excepton is as follows:
 *
 * <pre>{@code
 * public record FailToDoSomething(String name, int value) {
 * }
 *
 * try {
 *     throw new Exc(new FailToDoSomething("abc", 123));
 * } catch (Err e) {
 *     System.out.println(e.getMessage()); // => "FailToDoSomething { name=abc, value=123 }"
 * }
 * }</pre>
 */
public final class Exc extends Exception {

    /** The serial version UID. */
    private static final long serialVersionUID = 260427082865587554L;

    /** The reason for this exception. */
    private transient Record reason;

    /** The stack trace for the location of occurrence. */
    private StackTraceElement trace;

    /**
     * Is the constructor which takes a {@link Record} object indicating the reason for this excpetion.
     *
     * @param reason
     *            A reason for this exception.
     */
    public Exc(final Record reason) {
        if (reason == null) {
            throw new IllegalArgumentException("reason is null");
        }
        this.reason = reason;

        this.trace = getStackTrace()[0];
    }

    /**
     * Is the constructor which takes a {@link Record} object indicating the reason and {@link Throwable} object
     * indicating the cause for this excpetion.
     *
     * @param reason
     *            A reason for this exception.
     * @param cause
     *            A cause for this exception.
     */
    @SuppressWarnings("this-escape")
    public Exc(final Record reason, final Throwable cause) {
        super(cause);

        if (reason == null) {
            throw new IllegalArgumentException("reason is null");
        }
        this.reason = reason;

        this.trace = getStackTrace()[0];
    }

    /**
     * Gets the reason for this exception. The type of the reason.
     *
     * @return The reason for this exception.
     */
    public Record getReason() {
        return this.reason;
    }

    /**
     * Returns the message of this exception, that is the reason.
     *
     * @return The message of this exception.
     */
    @Override
    public String getMessage() {
        var rsn = this.reason.toString();
        var rname = this.reason.getClass().getSimpleName();
        rsn = rsn.substring(rname.length() + 1, rsn.length() - 1);

        var buf = new StringBuilder(this.reason.getClass().getName());
        buf.append(" { ").append(rsn).append(" }");
        return buf.toString();
    }

    /**
     * Returns the detail message of this exception, that contains the reason, source file name, line number, and the
     * cause if provided.
     *
     * @return The message of this exception.
     */
    @Override
    public String toString() {
        var buf = new StringBuilder(getClass().getName());
        buf.append(" { reason = ").append(getMessage());
        buf.append(", file = ").append(this.trace.getFileName());
        buf.append(", line = ").append(this.trace.getLineNumber());
        if (getCause() != null) {
            buf.append(", cause = ").append(getCause().toString());
        }
        return buf.append(" }").toString();
    }

    /**
     * Returns the name of the source file of this exception occurrance.
     * <p>
     * This method can return null if this information is unavailable.
     *
     * @return The name of the source file of this error occurrence.
     */
    public String getFile() {
        return this.trace.getFileName();
    }

    /**
     * Returns the line number of this exception occurrance in the source file.
     * <p>
     * This method can return a negative number if this information is unavailable.
     *
     * @return The line number of this exception occurrance in the source file.
     */
    public int getLine() {
        return this.trace.getLineNumber();
    }

    /**
     * Creates a {@link RuntimeException} object for methods that cannot throw a {@link Exc}.
     *
     * @return A {@link RuntimeException} object.
     */
    public RuntimeException toRuntimeException() {
        return new RuntimeExc(this);
    }

    /**
     * Writes a serial data of this exception to a stream.
     * <p>
     * Since a {@link Record} object is not necessarily serializable, this method will throw a
     * {@link NotSerializableException} if the {@code reason} field does not inherit {@link Serializable}.
     *
     * @param out
     *            An {@link ObjectOutputStream} to which data is written.
     *
     * @throws IOException
     *             if an I/O error occurs.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        if (!(this.reason instanceof Serializable)) {
            throw new NotSerializableException(this.reason.getClass().getName());
        }
        out.defaultWriteObject();
        out.writeObject(this.reason);
    }

    /**
     * Reconstitutes the {@code Exc} instance from a stream and initialize the reason and cause properties when
     * deserializing. If the reason by deserialization is null or invalid, this method throws
     * {@link InvalidObjectException}.
     *
     * @param in
     *            An {@link ObjectInputStream} from which data is read.
     *
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a serialized class cannot be loaded.
     */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.reason = Record.class.cast(in.readObject());

        if (this.reason == null) {
            throw new InvalidObjectException("reason is null or invalid.");
        }
    }
}

final class RuntimeExc extends RuntimeException {
    private static final long serialVersionUID = 4664405757902479929L;

    RuntimeExc(Exc exc) {
        super(exc);
    }

    @Override
    public String getMessage() {
        return getCause().getMessage();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getCause().toString();
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
