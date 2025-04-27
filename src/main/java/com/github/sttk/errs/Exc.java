/*
 * Exc class.
 * Copyright (C) 2025 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.errs;

import java.lang.management.ManagementFactory;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.InvalidObjectException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedList;

/**
 * Is the exception class with a reason.
 * <p>
 * This class has a record field which indicates a reason for this exception. The class name of the reason record
 * represents the type of reason, and the fields of the reason record hold the situation where the exception occurred.
 * <p>
 * Optionally, this exception class can notify its instance creation to pre-registered exception handlers. This
 * notification feature can be enabled by specifying the system property {@code -Dgithub.sttk.errs.notify=true} when the
 * JVM is started.
 * <p>
 * The example code of creating and throwing an excepton is as follows:
 *
 * <pre>{@code
 * public record FailToDoSomething(String name, int value) {
 * }
 *
 * try {
 *     throw new Exc(new FailToDoSomething("abc", 123));
 * } catch (Exc e) {
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
     * Is the constructor which takes a {@link Record} object indicating the reason for this exception.
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

        notifyExc(this);
    }

    /**
     * Is the constructor which takes a {@link Record} object indicating the reason and {@link Throwable} object
     * indicating the cause for this exception.
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

        notifyExc(this);
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

    //// Notification ////

    private static final boolean useNotification;

    static {
        boolean b = false;
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if ("-Dgithub.sttk.errs.notify=true".equals(arg)) {
                b = true;
                break;
            }
        }
        useNotification = b;
    }

    private static boolean isFixed = false;
    private static final List<ExcHandler> syncExcHandlers = new LinkedList<>();
    private static final List<ExcHandler> asyncExcHandlers = new LinkedList<>();

    /**
     * Adds an {@link ExcHandler} object which is executed synchronously just after an {@link Exc} is created. Handlers
     * added with this method are executed in the order of addition and stop if one of the handlers throws a
     * {@link RuntimeException} or an {@link Error}. NOTE: This feature is enabled via the system property:
     * {@code github.sttk.errs.notify=true}
     *
     * @param handler
     *            An {@link ExcHandler} object.
     */
    public static void addSyncHandler(final ExcHandler handler) {
        if (!useNotification)
            return;
        if (isFixed)
            return;
        syncExcHandlers.add(handler);
    }

    /**
     * Adds an {@link ExcHandler} object which is executed asynchronously just after an {@link Exc} is created. Handlers
     * don't stop even if one of the handlers throw a {@link RuntimeException} or an {@link Error}. NOTE: This feature
     * is enabled via the system property: {@code github.sttk.errs.notify=true}
     *
     * @param handler
     *            An {@link ExcHandler} object.
     */
    public static void addAsyncHandler(final ExcHandler handler) {
        if (!useNotification)
            return;
        if (isFixed)
            return;
        asyncExcHandlers.add(handler);
    }

    /**
     * Prevents further addition of {@link ExcHandler} objects to synchronous and asynchronous exception handler lists.
     * Before this is called, no {@code Exc} is notified to the handlers. After this is called, no new handlers can be
     * added, and {@code Exc}(s) is notified to the handlers. NOTE: This feature is enabled via the system property:
     * {@code github.sttk.errs.notify=true}
     */
    public static void fixHandlers() {
        if (!useNotification)
            return;
        if (isFixed)
            return;
        isFixed = true;
    }

    private static void notifyExc(Exc exc) {
        if (!useNotification)
            return;
        if (!isFixed)
            return;

        if (syncExcHandlers.isEmpty() && asyncExcHandlers.isEmpty()) {
            return;
        }

        final var tm = OffsetDateTime.now();

        for (var handler : syncExcHandlers) {
            handler.handle(exc, tm);
        }

        for (var handler : asyncExcHandlers) {
            Thread.ofVirtual().start(() -> {
                handler.handle(exc, tm);
            });
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
