# Contributing to the Philter SDK for Java

## Code of Conduct 

In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making participation in our project and our community a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

Please read and understand the [Code of Conduct](CODE_OF_CONDUCT.md).

## GitHub Workflow

We prefer to take contributions as GitHub pull requests. This workflow allows you to create your own copy of the Philter SDK for Java, try out some changes, and then share your changes back to the community, with proper review and feedback from other contributors.

1. Create a fork of philterd/philter-sdk-java
2. Create a feature branch
3. Build and test local changes
4. Commit changes to your feature branch
5. Open a pull request
6. Participate in code review
7. Celebrate your accomplishment

## Building and Testing Changes

### Required Tools

* A JDK, version 11 or later. The project compiles to Java 11 bytecode (`<release>11</release>`), and continuous integration builds with Temurin 11.
* Maven

### Building

The project builds on any platform with a supported JDK and Maven. To compile, run the unit tests, and package the jar:

```
mvn package
```

The unit tests run against a mocked HTTP server and require no external services. The live integration tests in
`PhilterClientTest` are skipped unless the `PHILTER_ENDPOINT` environment variable points at a running Philter
instance; see the [README](README.md#testing) for the full list of configuration variables.

## Continuous Integration

Pushes to `main` and all pull requests are built by GitHub Actions (`.github/workflows/build.yaml`). The pipeline runs
on `ubuntu-latest`, sets up the Temurin JDK 11, and runs:

```
mvn --batch-mode --update-snapshots package
```

A pull request should build cleanly with all tests passing before it is merged.
