### USAGE
```java -jar simple-benchamrk.jar 10 "java -jar example-app.jar""```

alternatively native image build with GraalVM can be used. To build native image:

```mvn package -Pnative```

then in target directory you will find `simple-benchamrk` executable. To run it, use the following command:

```./simple-benchamrk 10 "java -jar example-app.jar"```
