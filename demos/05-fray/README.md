# 05-fray

Fray-based concurrency tests for all Lamport buffer implementations:

- `01-lamport-volatile`
- `02-lamport-single-thread`
- `03-lamport-lock`

## Run

```bash
mvn -f demos/pom.xml -pl 05-fray -am test
```

You can also tune Fray execution via system properties, for example:

```bash
mvn -f demos/pom.xml -pl 05-fray -am test -Dfray.enabled=true -Dfray.scheduler=pct -Dfray.iterations=2000
```

