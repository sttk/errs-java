package com.github.sttk.errs;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExcHandlerTest {
  private ExcHandlerTest() {}

  @BeforeEach
  void reset() throws Exception {
    var f = Exc.class.getDeclaredField("isHandlersFixed");
    f.setAccessible(true);
    f.setBoolean(null, false);

    f = Exc.class.getDeclaredField("syncExcHandlers");
    f.setAccessible(true);
    var o = f.get(null);
    var m = LinkedList.class.getMethod("clear");
    m.invoke(o);

    f = Exc.class.getDeclaredField("asyncExcHandlers");
    f.setAccessible(true);
    o = f.get(null);
    m = LinkedList.class.getMethod("clear");
    m.invoke(o);
  }

  @SuppressWarnings("unchecked")
  List<ExcHandler> getSyncExcHandlers() throws Exception {
    var f = Exc.class.getDeclaredField("syncExcHandlers");
    f.setAccessible(true);
    var o = f.get(null);
    return (List<ExcHandler>) o;
  }

  @SuppressWarnings("unchecked")
  List<ExcHandler> getAsyncExcHandlers() throws Exception {
    var f = Exc.class.getDeclaredField("asyncExcHandlers");
    f.setAccessible(true);
    var o = f.get(null);
    return (List<ExcHandler>) o;
  }

  @Test
  void should_add_sync_handlers_and_fix() throws Exception {
    var handlers = getSyncExcHandlers();
    assertThat(handlers).isEmpty();

    ExcHandler handler1 = (exc, tm) -> {};
    Exc.addSyncHandler(handler1);

    handlers = getSyncExcHandlers();
    assertThat(handlers).containsExactly(handler1);

    ExcHandler handler2 = (exc, tm) -> {};
    Exc.addSyncHandler(handler2);

    handlers = getSyncExcHandlers();
    assertThat(handlers).containsExactly(handler1, handler2);

    Exc.fixHandlers();

    ExcHandler handler3 = (exc, tm) -> {};
    Exc.addSyncHandler(handler3);

    handlers = getSyncExcHandlers();
    assertThat(handlers).containsExactly(handler1, handler2);
  }

  @Test
  void should_add_async_handlers_and_fix() throws Exception {
    var handlers = getAsyncExcHandlers();
    assertThat(handlers).isEmpty();

    ExcHandler handler1 = (exc, tm) -> {};
    Exc.addAsyncHandler(handler1);

    handlers = getAsyncExcHandlers();
    assertThat(handlers).containsExactly(handler1);

    ExcHandler handler2 = (exc, tm) -> {};
    Exc.addAsyncHandler(handler2);

    handlers = getAsyncExcHandlers();
    assertThat(handlers).containsExactly(handler1, handler2);

    Exc.fixHandlers();

    ExcHandler handler3 = (exc, tm) -> {};
    Exc.addAsyncHandler(handler3);

    handlers = getAsyncExcHandlers();
    assertThat(handlers).containsExactly(handler1, handler2);
  }

  @Test
  void should_notify_exception() throws Exception {
    final List<String> syncLogs = new LinkedList<>();
    final List<String> asyncLogs = new LinkedList<>();

    Exc.addSyncHandler(
        (exc, tm) -> {
          syncLogs.add(
              String.format(
                  "%s:%s(%d):%s",
                  tm.format(ISO_INSTANT),
                  exc.getFile(),
                  exc.getLine(),
                  exc.getReason().toString()));
        });
    Exc.addAsyncHandler(
        (exc, tm) -> {
          asyncLogs.add(
              String.format(
                  "%s:%s(%d):%s",
                  tm.format(ISO_INSTANT),
                  exc.getFile(),
                  exc.getLine(),
                  exc.getReason().toString()));
        });

    record FailToDoSomething(String name) {}

    new Exc(new FailToDoSomething("abc"));

    assertThat(syncLogs).isEmpty();
    assertThat(asyncLogs).isEmpty();

    Exc.fixHandlers();

    new Exc(new FailToDoSomething("abc"));
    assertThat(syncLogs.get(0)).endsWith(":ExcHandlerTest.java(136):FailToDoSomething[name=abc]");

    Thread.sleep(100);
    assertThat(asyncLogs.get(0)).endsWith(":ExcHandlerTest.java(136):FailToDoSomething[name=abc]");
  }
}
