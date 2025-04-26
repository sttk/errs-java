package com.github.sttk.errs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.NotSerializableException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

public class ExcTest {
    private ExcTest() {
    }

    /// exception reasons ///

    record IndexOutOfRange(String name, int index, int min, int max) {
    }

    record SerializableReason(String name, int index, int min, int max) implements Serializable {
    }

    @Nested
    class TestConstructor {
        @Test
        void with_reason() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            var reason = IndexOutOfRange.class.cast(exc.getReason());
            assertThat(reason.name()).isEqualTo("data");
            assertThat(reason.index()).isEqualTo(4);
            assertThat(reason.min()).isEqualTo(0);
            assertThat(reason.max()).isEqualTo(3);
            assertThat(exc.getCause()).isNull();

            // exc.printStackTrace();
        }

        @Test
        void with_reason_but_reason_is_null() {
            try {
                new Exc(null);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("reason is null");
            }
        }

        @Test
        void with_reason_and_cause() {
            var cause = new IndexOutOfBoundsException(4);
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3), cause);
            var reason = IndexOutOfRange.class.cast(exc.getReason());
            assertThat(reason.name()).isEqualTo("data");
            assertThat(reason.index()).isEqualTo(4);
            assertThat(reason.min()).isEqualTo(0);
            assertThat(reason.max()).isEqualTo(3);
            assertThat(exc.getCause()).isEqualTo(cause);

            // exc.printStackTrace();
        }

        @Test
        void with_reason_and_cause_but_reason_is_null() {
            var cause = new IndexOutOfBoundsException(4);
            try {
                new Exc(null, cause);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("reason is null");
            }
        }

        @Test
        void with_reason_and_cause_but_cause_is_null() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3), null);
            var reason = IndexOutOfRange.class.cast(exc.getReason());
            assertThat(reason.name()).isEqualTo("data");
            assertThat(reason.index()).isEqualTo(4);
            assertThat(reason.min()).isEqualTo(0);
            assertThat(reason.max()).isEqualTo(3);
            assertThat(exc.getCause()).isNull();

            // exc.printStackTrace();
        }
    }

    @Nested
    class TestThrow {
        @Test
        void identify_reason_with_instanceOf() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            if (exc.getReason() instanceof IndexOutOfRange reason) {
                assertThat(reason.name()).isEqualTo("data");
                assertThat(reason.index()).isEqualTo(4);
                assertThat(reason.min()).isEqualTo(0);
                assertThat(reason.max()).isEqualTo(3);
            }
        }

        @Test
        void identify_reason_with_switch_expression() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            switch (exc.getReason()) {
                case IndexOutOfRange reason -> {
                    assertThat(reason.name()).isEqualTo("data");
                    assertThat(reason.index()).isEqualTo(4);
                    assertThat(reason.min()).isEqualTo(0);
                    assertThat(reason.max()).isEqualTo(3);
                }
                default -> fail();
            }
        }
    }

    @Nested
    class TestGetter {
        @Test
        void getReason() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            assertThat(exc.getReason()).isInstanceOf(IndexOutOfRange.class);

            var reason = IndexOutOfRange.class.cast(exc.getReason());
            assertThat(reason.name()).isEqualTo("data");
            assertThat(reason.index()).isEqualTo(4);
            assertThat(reason.min()).isEqualTo(0);
            assertThat(reason.max()).isEqualTo(3);
        }

        @Test
        void getCause() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            assertThat(exc.getCause()).isNull();

            var cause = new IndexOutOfBoundsException(4);
            exc = new Exc(new IndexOutOfRange("data", 4, 0, 3), cause);
            assertThat(exc.getCause()).isEqualTo(cause);
        }

        @Test
        void getFile() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            assertThat(exc.getFile()).isEqualTo("ExcTest.java");
        }

        @Test
        void getLine() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            assertThat(exc.getLine()).isEqualTo(155);
        }
    }

    @Nested
    class TestGetMessage {
        @Test
        void with_cause() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            assertThat(exc.getMessage())
                    .isEqualTo("com.github.sttk.errs.ExcTest$IndexOutOfRange { name=data, index=4, min=0, max=3 }");
        }

        @Test
        void with_no_cause() {
            var cause = new IndexOutOfBoundsException(4);
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3), cause);
            assertThat(exc.getMessage())
                    .isEqualTo("com.github.sttk.errs.ExcTest$IndexOutOfRange { name=data, index=4, min=0, max=3 }");
        }
    }

    @Nested
    class TestToString {
        @Test
        void with_reason() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            assertThat(exc.toString()).isEqualTo(
                    "com.github.sttk.errs.Exc { reason = com.github.sttk.errs.ExcTest$IndexOutOfRange { name=data, index=4, min=0, max=3 }, file = ExcTest.java, line = 182 }");
        }

        @Test
        void with_reason_and_cause() {
            var cause = new IndexOutOfBoundsException(4);
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3), cause);
            assertThat(exc.toString()).isEqualTo(
                    "com.github.sttk.errs.Exc { reason = com.github.sttk.errs.ExcTest$IndexOutOfRange { name=data, index=4, min=0, max=3 }, file = ExcTest.java, line = 190, cause = java.lang.IndexOutOfBoundsException: Index out of range: 4 }");
        }
    }

    @Nested
    class TestToRuntimeException {
        @Test
        void getMessage() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            var rtExc = exc.toRuntimeException();
            assertThat(rtExc.getMessage())
                    .isEqualTo("com.github.sttk.errs.ExcTest$IndexOutOfRange { name=data, index=4, min=0, max=3 }");
        }

        @Test
        void getCause() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            var rtExc = exc.toRuntimeException();
            assertThat(rtExc.getCause()).isEqualTo(exc);
        }

        @Test
        void printStackTrace() {
            var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
            var rtExc = exc.toRuntimeException();

            var swOfExc = new StringWriter();
            try (var pwOfExc = new PrintWriter(swOfExc)) {
                exc.printStackTrace(pwOfExc);
            }
            var swOfRtExc = new StringWriter();
            try (var pwOfRtExc = new PrintWriter(swOfRtExc)) {
                rtExc.printStackTrace(pwOfRtExc);
            }

            var isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            var prefix = "com.github.sttk.errs.RuntimeExc: " + exc.toString() + "\n";
            if (isWindows) {
                prefix += " ";
            }
            prefix += "Caused by: ";

            assertThat(swOfRtExc.toString()).isEqualTo(prefix + swOfExc.toString());

            // rtExc.printStackTrace();
        }
    }

    @Nested
    class TestSerialize {
        @Test
        void reason_is_serializable_and_has_no_cause() throws Exception {
            var bos = new ByteArrayOutputStream();
            var oos = new ObjectOutputStream(bos);
            try (oos) {
                var exc = new Exc(new SerializableReason("data", 4, 0, 3));
                oos.writeObject(exc);
            }

            var bytes = bos.toByteArray();
            var ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            try (ois) {
                var obj = ois.readObject();
                assertThat(obj).isInstanceOf(Exc.class);

                var exc = Exc.class.cast(obj);
                var cause = exc.getCause();
                assertThat(cause).isNull();

                var robj = exc.getReason();
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
                var exc = new Exc(new SerializableReason("data", 4, 0, 3), cause);
                oos.writeObject(exc);
            }

            var bytes = bos.toByteArray();
            var ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            try (ois) {
                var obj = ois.readObject();
                assertThat(obj).isInstanceOf(Exc.class);

                var exc = Exc.class.cast(obj);
                var cause = exc.getCause();
                assertThat(cause).isInstanceOf(IndexOutOfBoundsException.class);
                assertThat(cause.getMessage()).isEqualTo("Index out of range: 4");

                var robj = exc.getReason();
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
                var exc = new Exc(new IndexOutOfRange("data", 4, 0, 3));
                oos.writeObject(exc);
                fail();
            } catch (NotSerializableException e) {
                assertThat(e.getMessage()).isEqualTo(IndexOutOfRange.class.getName());
            }
        }
    }
}
