# [errs][repo-url] [![Maven Central][mvn-img]][mvn-url] [![MVN Repository][mvnrepo-img]][mvnrepo-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]

A library for handling errors with reasons for Java

## Overview

`errs` is an exception handling library for Java designed to focus on the "Reason" behind an error.

### Expressing "Why It Failed" via the Type System

Instead of scattering many small exception subclasses across your codebase, errs uses a single `Err` exception class that carries a “reason” object whose type represents why the failure occurred.

For error reasons, you can use anything from lightweight types like `String` to type-safe definitions using `record`s, all handled flexibly with the same API.
By using a `record` in particular, you can not only express failure factors within the type system but also hold contextual information in its fields, propagating the context and relevant data at the time of the error as-is.
Furthermore, since reasons can be determined in a type-safe manner using pattern-matching `switch` expressions, you can avoid fragile error handling that relies on string comparisons.

### Decentralized Error Definition and Traceability

`errs` encourages defining error reasons close to where they occur.
This eliminates the need to share a massive, monolithic error message management across the entire application, enabling a highly maintainable design while keeping dependencies between classes clean.
Type information is utilized to identify the reason, and the type identifiers required for this determination are resolved statically at compile time. This provides type-safe error handling with minimal runtime overhead.

The core `Err` type of the library inherits `java.lang.Exception`, allowing it to integrate naturally with standard Java exception handling.
It can also retain lower-layer exceptions as causes, enabling you to manage the "Reason" of the upper layer and the "Cause" of the lower layer separately.
Additionally, it automatically records the file name and line number when an error is generated, making log output and failure analysis effortless.

### Powerful Error-Instantiation Notification & Monitoring Ecosystem

Furthermore, `errs` features a mechanism to notify error generation events.
By running with the system property `-Dgithub.sttk.errs.notify=true`, an automatic notification can be sent to registered handlers the exact moment an `Err` is created.
It supports synchronous handlers and asynchronous handlers, and it accommodates registration within functions.
This makes it easy to implement logging, monitoring, metrics collection, and integration with telemetry systems.

While standard Java exceptions often focus primarily on annotating and propagating errors, `errs` emphasizes explicitly defining the reason for failure through types and reliably observing the exact moment it occurs.
This library is ideal for scenarios where you want to tightly manage the semantics of errors within your application while seamlessly integrating with production monitoring and operational infrastructure.

## Install

This package can be installed from [Maven Central Repository][mvn-url].

Examples of declaring that repository and the dependency on this package in Maven `pom.xml` and Gradle `build.gradle` are as follows:

### for Maven

```xml
  <dependencies>
    <dependency>
      <groupId>io.github.sttk</groupId>
      <artifactId>errs</artifactId>
      <version>0.2.1</version>
    </dependency>
  </dependencies>
```

### for Gradle

```gradle
repositories {
  mavenCentral()
}
dependencies {
  implementation 'io.github.sttk:errs:0.2.1'
}
```

## Usage

### Locally Defined Reasons and Instantiate an Err with Them

An `Err` class can be instantiated with any arbitrary error reason.
Typically, a record defined to indicate the cause or context of the error is used as the reason.
This reason does not need to be declared in a centralized file of global errors; rather, it is preferable to define it close to where the error using it as a reason actually occurs.

```java
package sample;

import com.github.sttk.errs.Err;

public class SampleClass {

    record IndexOutOfRange(String name, int index, int min, int max) {}

    public void sampleMethod() throws Err {
        // ...
        throw new Err(new IndexOutOfRange("array", i, 0, array.length));
    }
}
```

An `Err` can also be instantiated with the underlying cause exception along with the reason.

```java
    public void sampleMethod() throws Err {
        try {
            // ...
        } catch (IOException e) {
            throw new Err(new IndexOutOfRange("array", i, 0, array.length), e);
        }
    }
```

### Type-Safe Reason Identification

By using the pattern-matching switch expression, you can extract the error reason as the specified type.

```java
  try {
      sampleMethod();
  } catch (Err e) {
      switch (e.getReason()) {
          case IndexOutOfRange reason -> {
              String name = reason.name();
              int index = reason.index();
              int min = reason.min();
              int max = reason.max();
              ...
          }
          ...
      }
  }
```

### Error Handler Registration

> To enable this feature, you must specify the system property `-Dgithub.sttk.errs.notify=true` at program startup.

This library optionally provides a feature to notify pre-registered error handlers when an `Err` is instantiated.
Multiple error handlers can be registered, and you can choose to receive notifications either synchronously or asynchronously.

To register handlers, you can use the following functions:

```java
package sample;

import com.github.sttk.errs.Err;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class Main {
    static {
        Err.addSyncHandler((err, tm) -> {
          System.out.println(String.format("%s - %s:%d",
            err.getMessage(), err.getFile(), err.getLine());
        });

        Err.addAsyncHandler((err, tm) -> {
          remoteLogger.log(String.format("%s:%s:%d:%s",
              tm.format(ISO_INSTANT), err.getFile(), err.getLine(), err.toString()));
        });

        Err.fixHandlers();
    }

    record IndexOutOfRange(String name, int index, int min, int max) {}

    public static void main(String[] args) {
        try {
            throw new Err(new IndexOutOfRange("array", 11, 0, 10));
        } catch (Err e) {}
    }
}
```

```bash
% java -Dgithub.sttk.errs.notify=true sample.Main
sample.Main$IndexOutOfRange { name=array, index=11, min=0, max=10 } - Main.java:25
```

Error notifications will not occur until the `Err.fixErrHandlers` static method is called.
This static method locks the current set of error handlers, preventing further additions and enabling notification processing.

## Native build

This library supports native build with GraalVM.

See the following pages to setup native build environment on Linux/macOS or Windows.
- [Setup native build environment on Linux/macOS](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Setup native build environment on Windows](https://www.graalvm.org/latest/docs/getting-started/windows/#prerequisites-for-native-image-on-windows)

And see the following pages to build native image with Maven or Gradle.
- [Native image building with Maven plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html)
- [Native image building with Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)

**NOTE:** If serialization for `Err` is used, it is needed to specify the serialization configurations for derived classes of `Record` indicating the reason and derived classes of `Throwable` indicating the causing error classes in the `serialization-config.json` file.

## Supporting JDK versions

This library supports JDK 21 or later.

### Actually checked JDK versions:

- Oracle GraalVM 25.0.1+8.1 (build 25.0.1+8-LTS-jvmci-b01)
- Oracle GraalVM 21.0.9+7.1 (build 21.0.9+7-LTS-jvmci-23.1-b79)

## License

Copyright (C) 2024-2026 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[repo-url]: https://github.com/sttk/errs-java
[mvn-img]: https://img.shields.io/badge/maven_central-0.2.1-276bdd.svg
[mvn-url]: https://central.sonatype.com/artifact/io.github.sttk/errs/0.2.1
[mvnrepo-img]: https://img.shields.io/badge/mvn_repository-0.2.1-498df4.svg
[mvnrepo-url]: https://mvnrepository.com/artifact/io.github.sttk/errs
[io-img]: https://img.shields.io/badge/github.io-Javadoc-4d7a97.svg
[io-url]: https://sttk.github.io/errs-java/
[ci-img]: https://github.com/sttk/errs-java/actions/workflows/java-ci.yml/badge.svg?branch=main
[ci-url]: https://github.com/sttk/errs-java/actions?query=branch%3Amain
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT
