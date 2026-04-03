package com.github.sttk.errs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ErrTest {
  private ErrTest() {}

  /// exception reasons ///

  record IndexOutOfRange(String name, int index, int min, int max) {}

  record SerializableReason(String name, int index, int min, int max) implements Serializable {}

  @Nested
  class TestConstructor {
    @Test
    void with_Record_reason() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      var reason = IndexOutOfRange.class.cast(err.getReason());
      assertThat(reason.name()).isEqualTo("data");
      assertThat(reason.index()).isEqualTo(4);
      assertThat(reason.min()).isEqualTo(0);
      assertThat(reason.max()).isEqualTo(3);
      assertThat(err.getCause()).isNull();

      // err.printStackTrace();
    }

    @Test
    void with_enum_reason() {
      enum Reasons {
        FailToDoSomething,
      }

      var err = new Err(Reasons.FailToDoSomething);
      var reason = Reasons.class.cast(err.getReason());
      assertThat(reason.name()).isEqualTo("FailToDoSomething");
      assertThat(err.getCause()).isNull();

      // err.printStackTrace();
    }

    @Test
    void with_String_reason() {
      var err = new Err("FailToDoSomething");
      var reason = String.class.cast(err.getReason());
      assertThat(reason).isEqualTo("FailToDoSomething");
      assertThat(err.getCause()).isNull();
    }

    @Test
    void with_reason_but_reason_is_null() {
      try {
        new Err(null);
      } catch (IllegalArgumentException e) {
        assertThat(e.getMessage()).isEqualTo("reason is null");
      }
    }

    @Test
    void with_reason_and_cause() {
      var cause = new IndexOutOfBoundsException(4);
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3), cause);
      var reason = IndexOutOfRange.class.cast(err.getReason());
      assertThat(reason.name()).isEqualTo("data");
      assertThat(reason.index()).isEqualTo(4);
      assertThat(reason.min()).isEqualTo(0);
      assertThat(reason.max()).isEqualTo(3);
      assertThat(err.getCause()).isEqualTo(cause);

      // err.printStackTrace();
    }

    @Test
    void with_reason_and_cause_but_reason_is_null() {
      var cause = new IndexOutOfBoundsException(4);
      try {
        new Err(null, cause);
      } catch (IllegalArgumentException e) {
        assertThat(e.getMessage()).isEqualTo("reason is null");
      }
    }

    @Test
    void with_reason_and_cause_but_cause_is_null() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3), null);
      var reason = IndexOutOfRange.class.cast(err.getReason());
      assertThat(reason.name()).isEqualTo("data");
      assertThat(reason.index()).isEqualTo(4);
      assertThat(reason.min()).isEqualTo(0);
      assertThat(reason.max()).isEqualTo(3);
      assertThat(err.getCause()).isNull();

      // err.printStackTrace();
    }
  }

  @Nested
  class TestThrow {
    @Test
    void identify_reason_with_instanceOf() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      if (err.getReason() instanceof IndexOutOfRange reason) {
        assertThat(reason.name()).isEqualTo("data");
        assertThat(reason.index()).isEqualTo(4);
        assertThat(reason.min()).isEqualTo(0);
        assertThat(reason.max()).isEqualTo(3);
      }
    }

    @Test
    void identify_Record_reason_with_switch_expression() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      switch (err.getReason()) {
        case IndexOutOfRange reason -> {
          assertThat(reason.name()).isEqualTo("data");
          assertThat(reason.index()).isEqualTo(4);
          assertThat(reason.min()).isEqualTo(0);
          assertThat(reason.max()).isEqualTo(3);
        }
        default -> fail();
      }
    }

    @Test
    void identify_Enum_reason_with_switch_expression() {
      enum Reasons {
        FailToDoSomething,
        InvalidValue,
      }

      var err = new Err(Reasons.FailToDoSomething);

      var s =
          switch (err.getReason()) {
            case Reasons enm ->
                switch (enm) {
                  case FailToDoSomething -> "fail to do something";
                  case InvalidValue -> "invalid value";
                };
            default -> "unknown";
          };
      assertThat(s).isEqualTo("fail to do something");
    }
  }

  @Nested
  class TestGetter {
    @Test
    void getReason() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      assertThat(err.getReason()).isInstanceOf(IndexOutOfRange.class);

      var reason = IndexOutOfRange.class.cast(err.getReason());
      assertThat(reason.name()).isEqualTo("data");
      assertThat(reason.index()).isEqualTo(4);
      assertThat(reason.min()).isEqualTo(0);
      assertThat(reason.max()).isEqualTo(3);
    }

    @Test
    void getCause() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      assertThat(err.getCause()).isNull();

      var cause = new IndexOutOfBoundsException(4);
      err = new Err(new IndexOutOfRange("data", 4, 0, 3), cause);
      assertThat(err.getCause()).isEqualTo(cause);
    }

    @Test
    void getFile() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      assertThat(err.getFile()).isEqualTo("ErrTest.java");
    }

    @Test
    void getLine() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      assertThat(err.getLine()).isEqualTo(191);
    }
  }

  @Nested
  class TestGetMessage {
    @Test
    void with_cause() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      assertThat(err.getMessage()).isEqualTo("IndexOutOfRange[name=data, index=4, min=0, max=3]");
    }

    @Test
    void with_no_cause() {
      var cause = new IndexOutOfBoundsException(4);
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3), cause);
      assertThat(err.getMessage()).isEqualTo("IndexOutOfRange[name=data, index=4, min=0, max=3]");
    }
  }

  @Nested
  class TestToString {
    @Test
    void with_reason() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      assertThat(err.toString())
          .isEqualTo(
              "com.github.sttk.errs.Err { reason = com.github.sttk.errs.ErrTest$IndexOutOfRange IndexOutOfRange[name=data, index=4, min=0, max=3], file = ErrTest.java, line = 216 }");
    }

    @Test
    void with_reason_and_cause() {
      var cause = new IndexOutOfBoundsException(4);
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3), cause);
      assertThat(err.toString())
          .isEqualTo(
              "com.github.sttk.errs.Err { reason = com.github.sttk.errs.ErrTest$IndexOutOfRange IndexOutOfRange[name=data, index=4, min=0, max=3], file = ErrTest.java, line = 225, cause = java.lang.IndexOutOfBoundsException: Index out of range: 4 }");
    }
  }

  @Nested
  class TestToRuntimeException {
    @Test
    void getMessage() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      var rtErr = err.toRuntimeException();
      assertThat(rtErr.getMessage()).isEqualTo("IndexOutOfRange[name=data, index=4, min=0, max=3]");
    }

    @Test
    void getCause() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      var rtErr = err.toRuntimeException();
      assertThat(rtErr.getCause()).isEqualTo(err);
    }

    @Test
    void printStackTrace() {
      var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
      var rtErr = err.toRuntimeException();

      var swOfErr = new StringWriter();
      try (var pwOfErr = new PrintWriter(swOfErr)) {
        err.printStackTrace(pwOfErr);
      }
      var swOfRtErr = new StringWriter();
      try (var pwOfRtErr = new PrintWriter(swOfRtErr)) {
        rtErr.printStackTrace(pwOfRtErr);
      }

      var isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
      var prefix = "com.github.sttk.errs.RuntimeErr: " + err.toString();
      if (isWindows) {
        prefix += System.lineSeparator();
      } else {
        prefix += System.lineSeparator();
      }
      prefix += "Caused by: ";

      assertThat(swOfRtErr.toString()).isEqualTo(prefix + swOfErr.toString());

      // rtErr.printStackTrace();
    }
  }

  @Nested
  class TestSerialize {
    @Test
    void reason_is_serializable_and_has_no_cause() throws Exception {
      var bos = new ByteArrayOutputStream();
      var oos = new ObjectOutputStream(bos);
      try (oos) {
        var err = new Err(new SerializableReason("data", 4, 0, 3));
        oos.writeObject(err);
      }

      var bytes = bos.toByteArray();
      var ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
      try (ois) {
        var obj = ois.readObject();
        assertThat(obj).isInstanceOf(Err.class);

        var err = Err.class.cast(obj);
        var cause = err.getCause();
        assertThat(cause).isNull();

        var robj = err.getReason();
        assertThat(robj).isInstanceOf(SerializableReason.class);
        var reason = SerializableReason.class.cast(robj);
        assertThat(reason.name()).isEqualTo("data");
        assertThat(reason.index()).isEqualTo(4);
        assertThat(reason.min()).isEqualTo(0);
        assertThat(reason.max()).isEqualTo(3);
      }
    }

    @Test
    void reason_is_serializable_and_has_cause() throws Exception {
      var bos = new ByteArrayOutputStream();
      var oos = new ObjectOutputStream(bos);
      try (oos) {
        var cause = new IndexOutOfBoundsException(4);
        var err = new Err(new SerializableReason("data", 4, 0, 3), cause);
        oos.writeObject(err);
      }

      var bytes = bos.toByteArray();
      var ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
      try (ois) {
        var obj = ois.readObject();
        assertThat(obj).isInstanceOf(Err.class);

        var err = Err.class.cast(obj);
        var cause = err.getCause();
        assertThat(cause).isInstanceOf(IndexOutOfBoundsException.class);
        assertThat(cause.getMessage()).isEqualTo("Index out of range: 4");

        var robj = err.getReason();
        assertThat(robj).isInstanceOf(SerializableReason.class);
        var reason = SerializableReason.class.cast(robj);
        assertThat(reason.name()).isEqualTo("data");
        assertThat(reason.index()).isEqualTo(4);
        assertThat(reason.min()).isEqualTo(0);
        assertThat(reason.max()).isEqualTo(3);
      }
    }

    @Test
    void reason_is_not_serializable() throws Exception {
      var bos = new ByteArrayOutputStream();
      var oos = new ObjectOutputStream(bos);
      try (oos) {
        var err = new Err(new IndexOutOfRange("data", 4, 0, 3));
        oos.writeObject(err);
        fail();
      } catch (NotSerializableException e) {
        assertThat(e.getMessage()).isEqualTo(IndexOutOfRange.class.getName());
      }
    }
  }
}
