# Meow Messaging
### Meow is a lightweight and simple Http and Messaging framework built using Java and Netty. It provides a basic structure for implementing remote method invocations between client and server applications.

# Features
* Bi-directional communication between client and server using Netty.
* Basic Http server implementation using Java HttpServer.
* Basic example showcasing the usage of Meow.

# Getting Started
## Prerequisites
* Java Development Kit (JDK) 17 or above
* Apache Maven (for building the project)
* Netty (dependency will be automatically downloaded via Maven)

# Socket Example
```java
//Creating a DataSerializer to Serialize outgoing and incoming messages.
DataSerializer<String> stringSerializer = new DataSerializer<>() {
    @Override
    public byte[] serialize(String data) {
        return data.getBytes(StandardCharsets.US_ASCII);
    }
    @Override
    public String deserialize(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }
    @Override
    public Class<String> getType() {
        return String.class;
    }
};

//Creating a Server
Meow.Server<Meow.ServerClient<String>, String> server 
        = new Meow.Server<>(stringSerializer, Meow.ServerClient::new);
server.onReceived((client, data) -> client.send(data));
server.start(null, 800);

//Creating a client
Meow.Client<String> client = new Meow.Client<>(stringSerializer);
//Enabling autoreconnect
client.setAutoReconnect(true);
client.beforeReconnect((allow) -> {
    System.out.println("Reconnecting...");
    allow.set(true);
});
//Basic Event handlers
client.onConnected(() -> client.send("Hello Server!"));
client.onDisconnected(() -> System.out.println("Disconnected from server!"));
client.onReceived(System.out::println);
//Connect to the Server
client.connect("localhost", 800, 0);
```
# Http Example
Currently only supports Http and not Https
```java
MeowHttp meowHttp = new MeowHttp(8090); //port 8090
        .addRouteHandler("/hello", new RouteHandler() {
            @Override
            public void handle(HttpExchange httpExchange) {
                HttpUtils.sendResponse(httpExchange, "Hello, world!");
            }
        }).addRouteHandler("/epicroute", new RouteHandler() {
            @Override
            public void handle(HttpExchange httpExchange) {
                HttpUtils.sendResponse(httpExchange, "This is an epic route!");
            }
        }).start();
```
For more Examples or Documentation go visit the Wiki: https://github.com/WorldOfMeow/meow/wiki

# NOTE:
## The Repository server is currently Offline please download the JAR manually while we work on a fix:
### https://github.com/WorldOfMeow/meow/releases/download/1.0.4/meow-1.0.4.jar

# Installation
### Maven
```xml
<repositories>
    <repository>
        <id>meow</id>
        <url>https://getmeow.world/repo</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>world.getmeow</groupId>
        <artifactId>meow</artifactId>
        <version>1.0.5</version>
    </dependency>
</dependencies>
```
### Gradle
```java
repositories {
    maven {
        url "http://getmeow.world/repo/"
    }
}

implementation 'world.getmeow:meow:1.0.5'
```
### Or Clone the Meow Messaging repository:
```bash
git clone https://github.com/WorldOfMeow/meow.git
```
### Build the project using Maven:
```bash 
cd meow
mvn clean install
```

## Contribution
### Contributions to Meow are welcome! If you encounter any issues or have ideas for improvements, feel free to submit a pull request or open an issue on the GitHub repository.

This Project was build on top of Simple-Netty
