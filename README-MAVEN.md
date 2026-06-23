Maven build (bootstrap)
========================

This repository includes a small bootstrap that downloads the Maven Wrapper jar and runs it, allowing a full Maven build without a preinstalled `mvn` binary.

Windows (PowerShell):

```
.\mvnw-bootstrap.ps1 -Args '-DskipTests','package'
```

POSIX (bash):

```
./mvnw-bootstrap.sh -DskipTests package
```

Notes:
- The bootstrap downloads `maven-wrapper.jar` (io.takari:maven-wrapper:0.5.6) into `.mvn/wrapper` the first time it is run.
- The `.mvn/wrapper/maven-wrapper.properties` is configured to download Apache Maven 3.8.8.
- After bootstrapping, the wrapper will download the Maven distribution and execute the requested goals.
