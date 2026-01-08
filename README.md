# Kafka Inventory Synchronization System

Real-time inventory synchronization system using Apache Kafka Streams for warehouses, stores, and e-commerce platforms. Event-driven architecture ensuring consistent stock management across all channels.

## 🎯 Overview

This system provides:
- **Real-time inventory updates** from multiple sources (warehouses, POS, e-commerce)
- **Event-driven architecture** using Apache Kafka
- **Kafka Streams** for stateful aggregation and materialized views
- **Strong consistency** per SKU-location with partitioning strategy
- **Scalable architecture** supporting horizontal scaling

## 🏗️ Architecture

### Components

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Warehouse  │────▶│   Kafka     │────▶│   Inventory  │
│     WMS     │     │   Topics    │     │   Streams    │
└─────────────┘     │             │     │   Service    │
                    │ inventory.  │     └──────────────┘
┌─────────────┐     │  events.    │            │
│ Store POS   │────▶│  canonical  │            ▼
└─────────────┘     │             │     ┌──────────────┐
                    │ inventory.  │     │  Materialized│
┌─────────────┐     │  stock-     │     │    Views     │
│ E-commerce  │────▶│  levels     │     │  (KTable)    │
└─────────────┘     └─────────────┘     └──────────────┘
```

### Event Flow

1. **Event Sources** produce inventory events (sales, receipts, adjustments)
2. **Canonical Topic** receives normalized events
3. **Kafka Streams** aggregates events into inventory state per SKU-location
4. **Materialized Views** provide queryable inventory levels
5. **Consumers** (APIs, analytics) subscribe to stock-level updates

## 📋 Event Model

### Canonical Inventory Event

```java
public class InventoryEvent {
    String eventId;
    EventType eventType;  // RECEIPT, SALE, TRANSFER_IN, etc.
    String sku;
    String locationId;    // warehouse-001, store-123, "ONLINE"
    long quantityDelta;   // +100 for receipt, -1 for sale
    String sourceSystem;  // WMS, POS, ECOMMERCE
    Instant timestamp;
    String correlationId;
}
```

### Event Types

**Warehouse Events:**
- `RECEIPT` - Goods received from supplier
- `TRANSFER_IN` - Incoming from another location
- `TRANSFER_OUT` - Outgoing to another location
- `ADJUSTMENT` - Manual adjustment (cycle count, damage)

**Store Events:**
- `SALE` - Item sold
- `RETURN` - Customer return

**E-commerce Events:**
- `RESERVATION` - Cart reservation
- `RELEASE` - Reservation timeout/cancel
- `FULFILLMENT` - Order shipped

## 🔧 Technology Stack

- **Java 17**
- **Spring Boot 3.2.1**
- **Apache Kafka 3.6.1**
- **Kafka Streams**
- **Maven**
- **Lombok**
- **Jackson** (JSON serialization)

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose (for local Kafka)

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/swaroop22/kafka-inventory-sync.git
cd kafka-inventory-sync
```

2. **Start Kafka using Docker Compose**
```bash
docker-compose up -d
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

## 📦 Kafka Topics

### Topic Configuration

| Topic | Partitions | Retention | Compaction |
|-------|------------|-----------|------------|
| `inventory.events.raw.warehouse` | 12 | 7 days | No |
| `inventory.events.raw.store` | 12 | 7 days | No |
| `inventory.events.raw.ecommerce` | 12 | 7 days | No |
| `inventory.events.canonical` | 12 | 30 days | No |
| `inventory.stock-levels` | 12 | Infinite | Yes |

### Partitioning Strategy

Key: `{sku}:{locationId}`

This ensures all events for a SKU-location pair go to the same partition, enabling:
- Single-threaded, ordered processing per SKU-location
- Strong consistency guarantees
- No distributed locking required

## 🔄 Kafka Streams Topology

```java
KStream<String, InventoryEvent> events = 
    builder.stream("inventory.events.canonical");

KTable<String, InventoryAggregate> stockLevels = 
    events.groupByKey()
          .aggregate(
              () -> InventoryAggregate.empty(),
              (key, event, agg) -> agg.apply(event)
          );

stockLevels.toStream()
           .to("inventory.stock-levels");
```

## 🎯 Key Features

### 1. Exactly-Once Semantics
- Kafka Streams exactly-once processing
- Idempotent event handling with `eventId`

### 2. State Management
- Local state stores with changelog topics
- Automatic recovery on failure
- Queryable state via Interactive Queries

### 3. Scalability
- Horizontal scaling by adding instances
- Automatic partition rebalancing
- Stateless event producers

### 4. Observability
- Metrics exposed via JMX
- Structured logging
- Kafka Streams metrics

## 📊 Data Model

### InventoryAggregate

```java
public class InventoryAggregate {
    String sku;
    String locationId;
    long onHand;      // Physical quantity
    long reserved;    // Reserved for orders
    long inTransit;   // Coming from transfers
    Instant lastUpdated;
    long version;     // Optimistic locking
}
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify
```

## 📈 Production Considerations

### Monitoring
- Monitor consumer lag
- Track rebalancing events
- Alert on processing errors

### Scaling
- Number of partitions = max parallelism
- Add instances to handle load
- Use AWS MSK, Confluent Cloud, or self-hosted

### Security
- Enable SSL/TLS for Kafka
- Use SASL authentication
- Implement ACLs for topic access

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see LICENSE file for details.

## 👤 Author

**Swaroop**
- GitHub: [@swaroop22](https://github.com/swaroop22)

## 🙏 Acknowledgments

- Apache Kafka team for the excellent streaming platform
- Spring Kafka for seamless integration
- Inspired by real-world inventory management challenges at scale
