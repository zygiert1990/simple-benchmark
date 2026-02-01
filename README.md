### USAGE
By default log which indicates that app has already started is:

```Server started```

and application name is:

```simple-application```

to run measurements with defaults:

```java -jar simple-benchamrk.jar 10 "java -jar example-app.jar"```

it is possible to override those values like this:

```java -jar simple-benchamrk.jar 10 "java -jar example-app.jar" "App started" "example-app"```

alternatively native image build with GraalVM can be used. To build native image:

```mvn package -Pnative```

then in target directory you will find `simple-benchmark` executable. To run it, use the following command:

```./simple-benchmark 10 "java -jar example-app.jar"```
