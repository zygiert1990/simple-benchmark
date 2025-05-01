### USAGE
```java -jar simple-benchamrk.jar 10 "java -jar example-app.jar""```

alternatively native image build with GraalVM can be used. To build native image:

```mvn package -Pnative```

then in target directory you will find `simple-benchmark` executable. To run it, use the following command:

```./simple-benchmark 10 "java -jar example-app.jar"```
