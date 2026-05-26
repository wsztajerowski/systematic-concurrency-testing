# 06-jcstress

JCStress test definitions for Lamport buffer implementations from:

- `01-lamport-volatile`
- `02-lamport-single-thread`
- `03-lamport-lock`

## Build

```bash
# Build fat jar and run all stress tests
mvn -f demos/pom.xml -pl 06-jcstress -am verify

# Build fat jar only (skips test execution)
mvn -f demos/pom.xml -pl 06-jcstress -am package

# Run the fat jar directly - list available tests
java -jar demos/06-jcstress/target/jcstress.jar -l

# Run the fat jar directly with custom options (e.g. filter by test name)
java -jar demos/06-jcstress/target/jcstress.jar -t TwoProducers
```

jcstress tests live in `src/main/java` and are compiled into a self-contained fat jar
(`target/jcstress.jar`) by `maven-shade-plugin`. The `exec-maven-plugin` runs the jar
during the `integration-test` phase, which is why `verify` (not `test`) is the correct
lifecycle goal.
