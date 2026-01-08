package com.inventory.sync.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Canonical inventory event representing any change to inventory levels.
 * All events from warehouses, stores, and e-commerce are normalized to this format.
 */
@Data
@Builder
@Jacksonized
public class InventoryEvent {
    
    private String eventId;
    private EventType eventType;
    private String sku;
    private String locationId;  // warehouse-001, store-123, or "ONLINE"
    private long quantityDelta;  // positive for additions, negative for reductions
    private String sourceSystem;  // "WMS", "POS", "ECOMMERCE"
    private Instant timestamp;
    private String correlationId;  // for tracking related events
    private String userId;  // optional: who performed the action
    private String reason;  // optional: reason for adjustment
    
    @JsonCreator
    public InventoryEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") EventType eventType,
            @JsonProperty("sku") String sku,
            @JsonProperty("locationId") String locationId,
            @JsonProperty("quantityDelta") long quantityDelta,
            @JsonProperty("sourceSystem") String sourceSystem,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("userId") String userId,
            @JsonProperty("reason") String reason) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.sku = sku;
        this.locationId = locationId;
        this.quantityDelta = quantityDelta;
        this.sourceSystem = sourceSystem;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.correlationId = correlationId;
        this.userId = userId;
        this.reason = reason;
    }
    
    public enum EventType {
        // Warehouse events
        RECEIPT,           // goods received from supplier
        TRANSFER_IN,       // incoming from another location
        TRANSFER_OUT,      // outgoing to another location
        ADJUSTMENT,        // manual adjustment (cycle count, damaged goods)
        
        // Store events
        SALE,              // item sold
        RETURN,            // customer return
        
        // E-commerce events
        RESERVATION,       // cart reservation or order placed
        RELEASE,           // reservation released (timeout, cancel)
        FULFILLMENT        // order fulfilled/shipped
    }
}
