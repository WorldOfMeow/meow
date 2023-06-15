# Meow Messaging
### In Development (May not be fully functional)
### Meow is a lightweight and simple RPC and Messaging framework built using Java and Netty. It provides a basic structure for implementing remote method invocations between client and server applications.

# Features
* Bi-directional communication between client and server using Netty.
* Basic server implementation for registering and handling remote procedure calls.
* Basic example showcasing the usage of Meow RPC.

# Getting Started
## Prerequisites
* Java Development Kit (JDK) 17 or above
* Apache Maven (for building the project)
* Netty (dependency will be automatically downloaded via Maven)

# Example
```java
        Meow.DataSerializer<String> stringSerializer = new Meow.DataSerializer<>() {
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

        Meow.Server<Meow.ServerClient<String>, String> server = new Meow.Server<>(stringSerializer, Meow.ServerClient::new);
        server.onReceived((client, data) -> client.send(data));

        server.start(null, 800);

        Meow.Client<String> client = new Meow.Client<>(stringSerializer);
        client.setAutoReconnect(true);
        client.beforeReconnect((allow) -> {
            System.out.println("Reconnecting...");
            allow.set(true);
        });
        client.onConnected(() -> client.send("Hello Server!"));
        client.onDisconnected(() -> System.out.println("Disconnected from server!"));
        client.onReceived(System.out::println);

        client.connect("localhost", 800, 0);
```

# Installation
### Clone the Meow RPC repository:
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
