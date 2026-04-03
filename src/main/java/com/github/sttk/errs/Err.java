/*
 * Err class.
 * Copyright (C) 2025-2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.errs;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * Is the exception class with a reason.
 *
 * <p>This class has a field which indicates a reason for this exception. Typically the type of this
 * field is {@link Record}. In this case, the class name of this record represents the reason, and
 * the fields of the record hold the situation where the exception occurred.
 *
 * <p>Optionally, this exception class can notify its instance creation to pre-registered exception
 * handlers. This notification feature can be enabled by specifying the system property {@code
 * -Dgithub.sttk.errs.notify=true} when the JVM is started.
 *
 * <p>The example code of creating and throwing an exception is as follows:
 *
 * <pre>{@code
 * public record FailToDoSomething(String name, int value) {
 * }
 *
 * try {
 *     throw new Err(new FailToDoSomething("abc", 123));
 * } catch (Err e) {
 *     System.out.println(e.getMessage()); // => "FailToDoSomething { name=abc, value=123 }"
 * }
 * }</pre>
 */
public final class Err extends Exception {

  /** The serial version UID. */
  private static final long serialVersionUID = 260427082865587554L;

  /** The reason for this exception. */
  private transient Object reason;

  /** The stack trace for the location of occurrence. */
  private StackTraceElement trace;

  /**
   * Is the constructor which takes an object indicating the reason for this exception.
   *
   * @param reason A reason for this exception.
   */
  public Err(final Object reason) {
    if (reason == null) {
      throw new IllegalArgumentException("reason is null");
    }
    this.reason = reason;

    this.trace = getStackTrace()[0];

    notifyErr(this);
  }

  /**
   * Is the constructor which takes an object indicating the reason and {@link Throwable} object
   * indicating the cause for this exception.
   *
   * @param reason A reason for this exception.
   * @param cause A cause for this exception.
   */
  @SuppressWarnings("this-escape")
  public Err(final Object reason, final Throwable cause) {
    super(cause);

    if (reason == null) {
      throw new IllegalArgumentException("reason is null");
    }
    this.reason = reason;

    this.trace = getStackTrace()[0];

    notifyErr(this);
  }

  /**
   * Gets the reason for this exception. The type of the reason.
   *
   * @return The reason for this exception.
   */
  public Object getReason() {
    return this.reason;
  }

  /**
   * Returns the message of this exception, that is the reason.
   *
   * @return The message of this exception.
   */
  @Override
  public String getMessage() {
    return reason.toString();
  }

  /**
   * Returns the detail message of this exception, that contains the reason, source file name, line
   * number, and the cause if provided.
   *
   * @return The message of this exception.
   */
  @Override
  public String toString() {
    var buf = new StringBuilder(getClass().getName());
    buf.append(" { reason = ")
        .append(reason.getClass().getName())
        .append(" ")
        .append(reason.toString());
    buf.append(", file = ").append(this.trace.getFileName());
    buf.append(", line = ").append(this.trace.getLineNumber());
    if (getCause() != null) {
      buf.append(", cause = ").append(getCause().toString());
    }
    return buf.append(" }").toString();
  }

  /**
   * Returns the name of the source file of this exception occurrence.
   *
   * <p>This method can return null if this information is unavailable.
   *
   * @return The name of the source file of this error occurrence.
   */
  public String getFile() {
    return this.trace.getFileName();
  }

  /**
   * Returns the line number of this exception occurrence in the source file.
   *
   * <p>This method can return a negative number if this information is unavailable.
   *
   * @return The line number of this exception occurrence in the source file.
   */
  public int getLine() {
    return this.trace.getLineNumber();
  }

  /**
   * Creates a {@link RuntimeException} object for methods that cannot throw a {@link Err}.
   *
   * @return A {@link RuntimeException} object.
   */
  public RuntimeException toRuntimeException() {
    return new RuntimeErr(this);
  }

  /**
   * Writes a serial data of this exception to a stream.
   *
   * <p>Since a reason object is not necessarily serializable, this method will throw a {@link
   * NotSerializableException} if the {@code reason} field does not inherit {@link Serializable}.
   *
   * @param out An {@link ObjectOutputStream} to which data is written.
   * @throws IOException if an I/O error occurs.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    if (!(this.reason instanceof Serializable)) {
      throw new NotSerializableException(this.reason.getClass().getName());
    }
    out.defaultWriteObject();
    out.writeObject(this.reason);
  }

  /**
   * Reconstitutes the {@code Err} instance from a stream and initialize the reason and cause
   * properties when deserializing. If the reason by deserialization is null, this method throws
   * {@link InvalidObjectException}.
   *
   * @param in An {@link ObjectInputStream} from which data is read.
   * @throws IOException if an I/O error occurs.
   * @throws ClassNotFoundException if a serialized class cannot be loaded.
   */
  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    this.reason = in.readObject();

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

  private static boolean isHandlersFixed = false;
  private static final List<ErrHandler> syncErrHandlers = new LinkedList<>();
  private static final List<ErrHandler> asyncErrHandlers = new LinkedList<>();

  /**
   * Adds an {@link ErrHandler} object which is executed synchronously just after an {@link Err} is
   * created. Handlers added with this method are executed in the order of addition and stop if one
   * of the handlers throws a {@link RuntimeException} or an {@link Error}. NOTE: This feature is
   * enabled via the system property: {@code github.sttk.errs.notify=true}
   *
   * @param handler An {@link ErrHandler} object.
   */
  public static void addSyncHandler(final ErrHandler handler) {
    if (!useNotification) return;
    if (isHandlersFixed) return;
    syncErrHandlers.add(handler);
  }

  /**
   * Adds an {@link ErrHandler} object which is executed asynchronously just after an {@link Err} is
   * created. Handlers don't stop even if one of the handlers throw a {@link RuntimeException} or an
   * {@link Error}. NOTE: This feature is enabled via the system property: {@code
   * github.sttk.errs.notify=true}
   *
   * @param handler An {@link ErrHandler} object.
   */
  public static void addAsyncHandler(final ErrHandler handler) {
    if (!useNotification) return;
    if (isHandlersFixed) return;
    asyncErrHandlers.add(handler);
  }

  /**
   * Prevents further addition of {@link ErrHandler} objects to synchronous and asynchronous
   * exception handler lists. Before this is called, no {@code Err} is notified to the handlers.
   * After this is called, no new handlers can be added, and {@code Err}(s) is notified to the
   * handlers. NOTE: This feature is enabled via the system property: {@code
   * github.sttk.errs.notify=true}
   */
  public static void fixHandlers() {
    if (!useNotification) return;
    if (isHandlersFixed) return;
    isHandlersFixed = true;
  }

  private static void notifyErr(Err err) {
    if (!useNotification) return;
    if (!isHandlersFixed) return;

    if (syncErrHandlers.isEmpty() && asyncErrHandlers.isEmpty()) {
      return;
    }

    final var tm = OffsetDateTime.now();

    for (var handler : syncErrHandlers) {
      handler.handle(err, tm);
    }

    for (var handler : asyncErrHandlers) {
      Thread.ofVirtual()
          .start(
              () -> {
                handler.handle(err, tm);
              });
    }
  }
}

final class RuntimeErr extends RuntimeException {
  private static final long serialVersionUID = 4664405757902479929L;

  RuntimeErr(Err err) {
    super(err);
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
