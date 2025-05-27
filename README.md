# Middleware Java-based System

A modular and extensible **Message-Oriented Middleware (MOM)** implemented in Java. This project showcases advanced concepts in distributed systems, networking, and message protocol design. It supports a drawing and communication environment where clients (users) interact through structured messages and services.

## 🧠 Project Overview

This project simulates a full-featured middleware platform capable of:
- Managing client-server connections through socket channels with byte buffers.
- Parsing and routing structured messages by a homemade protocol.
- Providing distributed services (e.g., drawing interactions, message repository).
- Abstracting figures as an example of an app, tools, and protocols for extensible applications.

## 🏗️ Architecture

The system is divided into four key modules:

- **`app/`**: Data structures and models related to a drawing app (e.g., `Draw`, `Point`, `Figure`) and figure composition.
- **`network/`**: Classes to handle user roles (`User`, `Admin`) and the underlying network server (`Server`) using a Strategy structure with services (`Srvc`) as policies.
- **`servicies/`**: Service layers such as `SvcMom` and `SvcEditD`, managing communication channels and user actions depending on the policy applied.
- **`protocol/`**: Implementation of the custom communication protocol with message types, headers, permissions, and parsing logic (`Msg`, `Tag`, `Heads`, etc.).

## 💡 Features

- Custom protocol design using tagged and typed messages, used to a better managing.
- Client/server architecture with different roles and services, depending on the app used.
- Decoupled service logic via Strategy and Command architecture.
- Modular and extensible Java codebase, with posibility of mix both different services and extend the app basis applied.
- Strong object-oriented design principles, following Java programming methodology.

## 🧪 Tests

Includes unit tests in:
- `FiguraTest.java`
- `MsgTest.java`
- `UserServerTDraws.java`
- `UserServerTMOM.java`

These demonstrate correctness in geometric figure construction, message serialization/deserialization and service policies applied.

## 🚀 Getting Started

### Requirements

- Java 11 or later.
- A terminal or IDE to run Java applications.
- The following libraries (already included in the `lib/` directory):
  - `junit-4.13.1.jar`
  - `hamcrest-core-1.3.jar`
  - `log4j-core-2.20.0.jar`
  - `log4j-api-2.20.0.jar`

### How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/noelito/middleware-java.git
   cd middleware-java
