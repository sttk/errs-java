# [errs-java][repo-url] [![Maven Central][mvn-img]][mvn-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]

A library for handling errors with reasons.

In Java programming, it is cumbersome to implement a separate exception class for each exception case.
However, trying to handle multiple exception cases with a single exception class makes it difficult to distinguish between them.

The error class `Err` provided by this library solves this problem by accepting an object that represents the reason for the error.
Typically, the type of this reason object is `Record`.
Since a `Record` object can have any fields, it can store information about the situation at the time the error occurred.
The type of the reason can be determined and cast using a switch statement, making it easy to write handling logic for each error case.

Optionally, when an `Err` object is instantiated, pre-registered error handlers can receive notifications either synchronously or asynchronously.
However, to enable this feature, the system property `-Dgithub.sttk.errs.notify=true` must be specified at program startup.

## Install

This package can be installed from [Maven Central Repository][mvn-url].

The examples of declaring that repository and the dependency on this package in Maven `pom.xml` and Gradle `build.gradle` are as follows:

### for Maven

```
  <dependencies>
    <dependency>
      <groupId>io.github.sttk</groupId>
      <artifactId>errs</artifactId>
      <version>0.2.0</version>
    </dependency>
  </dependencies>
```

### for Gradle

```
repositories {
  mavenCentral()
}
dependencies {
  implementation 'io.github.sttk:errs:0.2.0'
}
```

## Usage

### Err instantiation and identification of a reason

The following code instantiates an `Err` and throws it.

```java
package sample;

import com.github.sttk.errs.Err;

public class SampleClass {

    record IndexOutOfRange(String name, int index, int min, int max) {}

    public void sampleMethod() throws Err {
        ...
        throw new Err(new IndexOutOfRange("array", i, 0, array.length));
    }
}
```

And the following code catches the error and identifies the reason with a switch expression.

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

### Error notification (Optional)

> To enable this feature, you must specify the system property `-Dgithub.sttk.errs.notify=true` at program startup.

This library optionally provides a feature to notify pre-registered error handlers when an `Err` is instantiated.
Multiple error handlers can be registered, and you can choose to receive notifications either synchronously or asynchronously.
To register error handlers that receive notifications synchronously, use the `Err.addSyncHandler` static method.
For asynchronous notifications, use the `Err.addAsyncHandler` static method.

Error notifications will not occur until the `Err.fixHandlers` static method is called.
This static method locks the current set of error handlers, preventing further additions and enabling notification processing.

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
          removeLogger.log(String.format("%s:%s:%d:%s",
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

```sh
% java -Dgithub.sttk.errs.notify=true sample.Main
sample.Main$IndexOutOfRange { name=array, index=11, min=0, max=10 } - Main.java:25
```

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
- Oracle GraalVM 24.0.2+11.1 (build 24.0.2+11-jvmci-b01)
- Oracle GraalVM 23.0.2+7.1 (build 23.0.2+7-jvmci-b01)
- Oracle GraalVM 21.0.9+7.1 (build 21.0.9+7-LTS-jvmci-23.1-b79)

## License

Copyright (C) 2024-2026 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[repo-url]: https://github.com/sttk/errs-java
[mvn-img]: https://img.shields.io/badge/maven_central-0.2.0-276bdd.svg
[mvn-url]: https://central.sonatype.com/artifact/io.github.sttk/errs/0.2.0
[io-img]: https://img.shields.io/badge/github.io-Javadoc-4d7a97.svg
[io-url]: https://sttk.github.io/errs-java/
[ci-img]: https://github.com/sttk/errs-java/actions/workflows/java-ci.yml/badge.svg?branch=main
[ci-url]: https://github.com/sttk/errs-java/actions?query=branch%3Amain
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT
