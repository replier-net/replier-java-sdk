# replier-java

Java SDK for the [Replier.net](https://www.replier.net) WhatsApp API. Java 11+ (uses `java.net.http.HttpClient`).

## Install

Maven:

```xml
<dependency>
  <groupId>net.replier</groupId>
  <artifactId>replier-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'net.replier:replier-java:1.0.0'
```

## Usage

```java
import net.replier.Replier;
import net.replier.ReplierException;
import java.util.List;
import java.util.Map;

Replier replier = Replier.builder()
        .instanceId("12345")
        .token("your-instance-token")
        // only for listInstances / newInstance / renewInstance:
        .email("you@example.com")
        .apiKey("your-account-api-key")
        .build();

try {
    Map<String, Object> sent = replier.sendMessage("15551234567", "Hello!");
    System.out.println(sent.get("id"));

    replier.sendFile("15551234567", "https://example.com/invoice.pdf", "invoice.pdf", "Your invoice");
    replier.createGroup("Team", List.of("15551234567", "15557654321"));
    replier.setWebhook("https://example.com/replier/webhook");
} catch (ReplierException e) {
    System.err.println(e.getErrorCode() + " " + e.getMessage());
}
```

Optional arguments may be passed as `null`. Call an endpoint that is not wrapped yet with
`replier.call("someNewEndpoint", Map.of("param", "value"))`.
See the [method reference](../README.md#method-reference).

## Build & publish

```bash
mvn package
mvn deploy   # after configuring Maven Central (Sonatype) credentials and signing
```

## License

MIT
