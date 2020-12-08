CULR service SOAP Connector
===========================
Jar library containing helper functions for calling the CULR service

### Usage
In pom.xml add this dependency:

```xml
  <dependency>
    <groupId>dk.dbc</groupId>
    <artifactId>culr-connector</artifactId>
    <version>1.0-SNAPSHOT</version>
  </dependency>
```
In your EJB add the following inject:
```java
    @Inject
    private CulrConnector culrConnector;
```

You must have the following environment variables in your deployment:

    CULR_SERVICE_URL

The following optional environment variables can also be set:

    CULR_CONNECTOR_CONNECT_TIMEOUT_IN_MS (defaults to 2000)
    CULR_CONNECTOR_REQUEST_TIMEOUT_IN_MS (defaults to 10000)

### Development

**Requirements**

To build this project JDK 8  and Apache Maven is required.

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
