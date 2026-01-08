package com.inventory.sync.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Aggregate state representing current inventory levels for a SKU at a location.
 * This is the materialized view maintained by Kafka Streams.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class InventoryAggregate {
    
    private String sku;
    private String locationId;
    private long onHand;           // physical quantity available
    private long reserved;         // quantity reserved (cart, orders)
    private long inTransit;        // quantity in transit to this location
    private Instant lastUpdated;
    private long version;          // for optimistic locking
    
    @JsonCreator
    public InventoryAggregate(
            @JsonProperty("sku") String sku,
            @JsonProperty("locationId") String locationId,
            @JsonProperty("onHand") long onHand,
            @JsonProperty("reserved") long reserved,
            @JsonProperty("inTransit") long inTransit,
            @JsonProperty("lastUpdated") Instant lastUpdated,
            @JsonProperty("version") long version) {
        this.sku = sku;
        this.locationId = locationId;
        this.onHand = onHand;
        this.reserved = reserved;
        this.inTransit = inTransit;
        this.lastUpdated = lastUpdated;
        this.version = version;
    }
    
    /**
     * Factory method to create an empty aggregate.
     */
    public static InventoryAggregate empty(String sku, String locationId) {
        return InventoryAggregate.builder()
                .sku(sku)
                .locationId(locationId)
                .onHand(0L)
                .reserved(0L)
                .inTransit(0L)
                .lastUpdated(Instant.now())
                .version(0L)
                .build();
    }
    
    /**
     * Apply an inventory event to update the aggregate state.
     */
    public InventoryAggregate apply(InventoryEvent event) {
        long newOnHand = this.onHand;
        long newReserved = this.reserved;
        long newInTransit = this.inTransit;
        
        switch (event.getEventType()) {
            case RECEIPT:
            case RETURN:
                newOnHand += event.getQuantityDelta();
                break;
                
            case TRANSFER_IN:
                newOnHand += event.getQuantityDelta();
                newInTransit -= event.getQuantityDelta();
                break;
                
            case TRANSFER_OUT:
                newOnHand -= Math.abs(event.getQuantityDelta());
                // Destination location will get TRANSFER_IN
                break;
                
            case ADJUSTMENT:
                newOnHand += event.getQuantityDelta();
                break;
                
            case SALE:
                newOnHand -= Math.abs(event.getQuantityDelta());
                break;
                
            case RESERVATION:
                long reserveQty = Math.abs(event.getQuantityDelta());
                newOnHand -= reserveQty;
                newReserved += reserveQty;
                break;
                
            case RELEASE:
                long releaseQty = Math.abs(event.getQuantityDelta());
                newOnHand += releaseQty;
                newReserved -= releaseQty;
                break;
                
            case FULFILLMENT:
                long fulfillQty = Math.abs(event.getQuantityDelta());
                newReserved -= fulfillQty;
                // Item has left inventory
                break;
        }
        
        return InventoryAggregate.builder()
                .sku(this.sku)
                .locationId(this.locationId)
                .onHand(Math.max(0, newOnHand))
                .reserved(Math.max(0, newReserved))
                .inTransit(Math.max(0, newInTransit))
                .lastUpdated(event.getTimestamp())
                .version(this.version + 1)
                .build();
    }
    
    /**
     * Get available to sell quantity.
     */
    public long getAvailableToSell() {
        return Math.max(0, onHand);
    }
    
    /**
     * Get total quantity (on-hand + in-transit).
     */
    public long getTotalQuantity() {
        return onHand + inTransit;
    }
}
