# Building Legacy Item DFU for Minecraft 26.3

This port starts from [AzureAaron/legacy-item-dfu master 1f0eaed1a4520983b49facdb3071e80730ef4d43](https://github.com/AzureAaron/legacy-item-dfu/tree/1f0eaed1a4520983b49facdb3071e80730ef4d43). The original Apache 2.0 license and source API remain. The genuine 26.3 dependency is DataFixerUpper 10.0.21, and this plain Java library compiles with Java 25.

Choose `JDK25` and `PORT_DEPENDENCIES` outside this checkout. From its own root, use the committed wrapper:

```sh
export JAVA_HOME="$JDK25"
./gradlew --no-daemon --max-workers=2 build \
  publishMavenJavaPublicationToPortWorkspaceRepository \
  -PportMavenRepository="$PORT_DEPENDENCIES"
```

`portMavenRepository` is optional and adds the explicitly selected local publication destination `portWorkspace`. A relative path resolves against the checkout; an external absolute directory or file URI avoids a fixed source layout. The exact task above only publishes to that directory. Existing dependency repositories remain the defaults.

The coordinate is `net.azureaaron:legacy-item-dfu:1.0.4+26.3`; the generated POM and sources JAR accompany the unclassified runtime library. Java 25 production compilation, all eight existing JUnit cases and local publication passed. No Fabric client, game account, owner config, DevAuth or Minecraft launch is needed to build/test this library. Native game-consumer and full-pack behavior require their separate validation; passing library tests alone does not establish that behavior.
