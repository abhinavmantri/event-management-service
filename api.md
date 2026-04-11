# Event Management Service API

## Overview
The **Event Management Service** is responsible for managing events, ticket categories, event schedules, and event discovery metadata for the Ticketmaster-inspired platform.

This service handles:
- event creation and updates
- event publishing
- event retrieval
- section-level pricing configuration
- event listing and filtering
- venue management
- venue seat generation
- event inventory initialization

It does **not** handle:
- seat locking
- booking creation
- payment processing
- ticket issuance

Those responsibilities belong to Booking/Ticket Service and Payment Service.

---

# Base URL

## Local
```text
http://localhost:8081/event-management-service/v1
```

## Context Path
```text
/event-management-service/v1
```

Example:
```text
http://localhost:8081/event-management-service/v1/events
```

---

# Authentication

All external APIs require JWT authentication.

## Headers
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

---

# Authorization Rules

| Role | Access |
|------|--------|
| ORGANISER | Can create and manage own events on organiser routes |
| PUBLIC | Can browse published events and fetch published event details |

---

# Resource Model

## Event
An event represents a show, concert, play, sports match, or any scheduled occurrence at a venue.

## Event Status
Possible states:

- `DRAFT`
- `PUBLISHED`
- `CANCELLED`

---

# Event Object

```json
{
  "id": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6",
  "organiserId": "8db2c7d7-e3cb-4f5a-89d8-a67f6de62f26",
  "organiserEmail": "organiser@example.com",
  "venue": {
    "id": "5d06b702-4ff8-4f4b-92c7-22d90c1da4bb"
  },
  "title": "Coldplay Live in Bangalore",
  "description": "Live concert at Bangalore Stadium",
  "category": "MUSIC",
  "startsAt": "2026-06-21T19:00:00Z",
  "endsAt": "2026-06-21T23:00:00Z",
  "status": "PUBLISHED",
  "createdAt": "2026-03-15T10:20:00Z",
  "updatedAt": "2026-03-15T11:00:00Z"
}
```

---

# API Conventions

## Common Response Wrapper
```json
{
  "responseStatus": "SUCCESS",
  "message": "Operation completed successfully",
  "...endpointSpecificFields": {}
}
```

## Common Error Wrapper
```json
{
  "responseStatus": "FAILURE",
  "message": "Event not found"
}
```

## Current Response Shapes
- All current controller responses extend `ApiResponse`
- Common fields:
  - `responseStatus`: `SUCCESS` or `FAILURE`
  - `message`: human-readable status message
- Event create/get/update responses include `event`
- Event list responses include `events`, `totalElements`, `totalPages`, `pageNumber`, `pageSize`
- Publish responses include `eventId`
- Pricing responses include `pricings`
- Inventory initialization responses include `eventId`, `createdSeats`, `alreadyInitialized`

---

# Endpoints

> Current implementation note:
> The implemented routes are the ones documented with current path examples in this file and in the endpoint summary near the end of this file.
> Later sections that describe delete, unpublish, cancel, ticket-type APIs, discovery/search, and internal APIs are future/planned APIs and are not aligned to the current codebase.

---

# 1. Create Event

Creates a new event in `DRAFT` state.

## Endpoint
```http
POST /organiser/events
```

## Access
- `ORGANISER`

## Request Body
```json
{
  "venueId": "5d06b702-4ff8-4f4b-92c7-22d90c1da4bb",
  "title": "Coldplay Live in Bangalore",
  "description": "Live concert at Bangalore Stadium",
  "category": "MUSIC",
  "startsAt": "2026-06-21T19:00:00Z",
  "endsAt": "2026-06-21T23:00:00Z"
}
```

## Response
**201 Created**
```json
{
  "responseStatus": "SUCCESS",
  "message": "Event created successfully",
  "event": {
    "id": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6",
    "organiserId": "8db2c7d7-e3cb-4f5a-89d8-a67f6de62f26",
    "organiserEmail": "organiser@example.com",
    "title": "Coldplay Live in Bangalore",
    "description": "Live concert at Bangalore Stadium",
    "category": "MUSIC",
    "startsAt": "2026-06-21T19:00:00Z",
    "endsAt": "2026-06-21T23:00:00Z",
    "status": "DRAFT",
    "createdAt": "2026-03-15T10:20:00Z",
    "updatedAt": "2026-03-15T10:20:00Z"
  }
}
```

## Validation Rules
- `title` is required
- `venueId` is required
- `startsAt` is required
- `startsAt` must be a future date-time
- if provided, `endsAt` must be a future date-time
- if both are provided, `endsAt` must be greater than or equal to `startsAt`

---

# 2. Get Event by ID

Returns complete event details.

## Endpoint
```http
GET /events/{eventId}
```

## Access
- Public access
- Only published events are returned

## Path Params
| Param | Type | Description |
|------|------|-------------|
| eventId | string | Unique event identifier |

## Response
**200 OK**
```json
{
  "responseStatus": "SUCCESS",
  "message": "Event fetched successfully",
  "event": {
    "id": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6",
    "organiserId": "8db2c7d7-e3cb-4f5a-89d8-a67f6de62f26",
    "organiserEmail": "organiser@example.com",
    "title": "Coldplay Live in Bangalore",
    "description": "Live concert at Bangalore Stadium",
    "category": "MUSIC",
    "startsAt": "2026-06-21T19:00:00Z",
    "endsAt": "2026-06-21T23:00:00Z",
    "status": "PUBLISHED"
  }
}
```

---

# 3. List Events

Returns paginated event list with filters.

## Endpoint
```http
GET /events
```

## Access
- Public for published events

## Query Params

| Param | Type | Required | Description |
|------|------|----------|-------------|
| query | string | No | Search term for title |
| city | string | No | Filter by city |
| category | string | No | Filter by category |
| startDate | string | No | ISO date-time lower bound |
| endDate | string | No | ISO date-time upper bound |
| page | int | No | Default 0 |
| size | int | No | Default 20 |
| sort | string | No | Spring pageable sort parameter |

## Example
```http
GET /events?query=coldplay&city=Bangalore&category=MUSIC&page=0&size=10
```

## Response
```json
{
  "responseStatus": "SUCCESS",
  "message": "Events fetched successfully",
  "events": [
    {
      "id": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6",
      "title": "Coldplay Live in Bangalore",
      "category": "MUSIC",
      "startsAt": "2026-06-21T19:00:00Z",
      "endsAt": "2026-06-21T23:00:00Z",
      "status": "PUBLISHED"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

# 4. Update Event

Updates mutable event fields.

## Endpoint
```http
PATCH /organiser/events/{eventId}
```

## Access
- owning `ORGANISER`

## Rules
- current implementation validates organiser ownership by JWT claim
- current implementation does not restrict updates by event status
- `endsAt` must not be before `startsAt`

## Request Body
```json
{
  "title": "Coldplay Live in Bengaluru",
  "description": "Updated concert description",
  "category": "MUSIC",
  "startsAt": "2026-06-21T19:30:00Z",
  "endsAt": "2026-06-21T23:30:00Z"
}
```

## Response
```json
{
  "responseStatus": "SUCCESS",
  "message": "Event updated successfully",
  "event": {
    "id": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6",
    "title": "Coldplay Live in Bengaluru",
    "description": "Updated concert description",
    "category": "MUSIC",
    "startsAt": "2026-06-21T19:30:00Z",
    "endsAt": "2026-06-21T23:30:00Z",
    "status": "DRAFT"
  }
}
```

---

# Current Implemented Endpoint: Configure Event Pricing

Configures section-level pricing for an event.

## Endpoint
```http
POST /organiser/events/{eventId}/pricing
```

## Request Body
```json
{
  "currency": "INR",
  "prices": [
    {
      "sectionId": "2ac22535-b6cd-4294-8295-45b7d5146626",
      "priceCents": 750000
    },
    {
      "sectionId": "cf5d375a-95ce-43ce-a04b-b395c7d8ab6c",
      "priceCents": 250000
    }
  ]
}
```

## Validation Rules
- `currency` is required
- `prices` must not be empty
- each price item requires `sectionId`
- each price item requires `priceCents`
- `priceCents` must be greater than `0`

## Response
```json
{
  "responseStatus": "SUCCESS",
  "message": "Event pricing configured successfully",
  "pricings": [
    {
      "id": "38af9893-e6b6-4257-b942-9907e9640b8a",
      "priceCents": 750000,
      "currency": "INR"
    }
  ]
}
```

---

# 5. Delete Event

Soft deletes or hard deletes an event depending on business policy.

## Endpoint
```http
DELETE /v1/events/{eventId}
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Rules
- deletion allowed only when no confirmed bookings exist
- usually implemented as soft delete / archival

## Response
**204 No Content**

> **Note:** This is an advanced feature for future consideration. Current implementation focuses on core event management workflow.

---

# 6. Publish Event

Moves event from `DRAFT` or `UNPUBLISHED` to `PUBLISHED`.

## Endpoint
```http
POST /organiser/events/{eventId}/publish
```

## Access
- owning `ORGANISER`

## Preconditions
- event must belong to the authenticated organiser
- event status must be `DRAFT`
- pricing must already be configured

## Response
```json
{
  "responseStatus": "SUCCESS",
  "message": "Event published successfully",
  "eventId": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6"
}
```

---

# Current Implemented Endpoint: Initialize Event Inventory

Creates event seat inventory for the event from venue seats.

## Endpoint
```http
POST /organiser/events/{eventId}/inventory/init
```

## Request Body

No request body.

## Response
```json
{
  "responseStatus": "SUCCESS",
  "message": "Event inventory initialized successfully",
  "eventId": "f5cf6f1c-0bd0-4cb8-bf75-9e5645f914d6",
  "createdSeats": 120,
  "alreadyInitialized": false
}
```

---

# 7. Unpublish Event

Removes event from public discovery.

## Endpoint
```http
POST /v1/events/{eventId}/unpublish
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Response
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "status": "UNPUBLISHED"
  }
}
```

> **Note:** This is an advanced feature for future consideration. Current implementation focuses on core event management workflow.

---

# 8. Cancel Event

Marks event as cancelled.

## Endpoint
```http
POST /v1/events/{eventId}/cancel
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Notes
This should trigger downstream processes asynchronously:
- booking cancellation workflow
- refund workflow
- customer notification workflow

## Request Body
```json
{
  "reason": "Artist unavailable due to emergency"
}
```

## Response
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "status": "CANCELLED"
  }
}
```

> **Note:** This is an advanced feature for future consideration. Current implementation focuses on core event management workflow.

---

# 9. Add Ticket Type

Adds a new ticket category for an event.

## Endpoint
```http
POST /v1/events/{eventId}/ticket-types
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Request Body
```json
{
  "name": "Balcony",
  "price": 1800,
  "totalCapacity": 800,
  "description": "Upper tier balcony seating"
}
```

## Response
```json
{
  "success": true,
  "data": {
    "ticketTypeId": "tt_13",
    "eventId": "evt_10001"
  }
}
```

> **Note:** This is an advanced feature for future consideration. Current implementation uses section-based pricing instead of ticket types.

---

# 10. Update Ticket Type

## Endpoint
```http
PUT /v1/events/{eventId}/ticket-types/{ticketTypeId}
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Request Body
```json
{
  "name": "VIP Plus",
  "price": 8500,
  "description": "VIP with lounge access"
}
```

## Response
```json
{
  "success": true,
  "data": {
    "ticketTypeId": "tt_11",
    "updated": true
  }
}
```

## Rules
- reducing `totalCapacity` below already sold/reserved inventory is not allowed
- price update may need business guardrails after publishing

> **Note:** This is an advanced feature for future consideration. Current implementation uses section-based pricing instead of ticket types.

---

# 11. Delete Ticket Type

## Endpoint
```http
DELETE /v1/events/{eventId}/ticket-types/{ticketTypeId}
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Rules
- cannot delete if bookings already exist for that ticket type

## Response
**204 No Content**

> **Note:** This is an advanced feature for future consideration. Current implementation uses section-based pricing instead of ticket types.

---

# 12. Get Ticket Types for Event

## Endpoint
```http
GET /v1/events/{eventId}/ticket-types
```

## Response
```json
{
  "success": true,
  "data": [
    {
      "ticketTypeId": "tt_11",
      "name": "VIP",
      "price": 7500,
      "totalCapacity": 500,
      "description": "Closest seating with perks"
    },
    {
      "ticketTypeId": "tt_12",
      "name": "General",
      "price": 2500,
      "totalCapacity": 5000,
      "description": "General admission"
    }
  ]
}
```

> **Note:** This is an advanced feature for future consideration. Current implementation uses section-based pricing instead of ticket types.

---

# 13. Get Event Summary for Discovery

A lightweight API for UI listing pages.

## Endpoint
```http
GET /v1/events/discovery
```

## Query Params
| Param | Type | Description |
|------|------|-------------|
| city | string | City filter |
| category | string | Category filter |
| date | string | Event date |
| page | int | Page number |
| size | int | Page size |

## Response
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "eventId": "evt_10001",
        "eventName": "Coldplay Live in Bangalore",
        "category": "MUSIC",
        "city": "Bangalore",
        "venueName": "Bangalore Stadium",
        "startTime": "2026-06-21T19:00:00Z",
        "startingPrice": 2500,
        "thumbnailUrl": "https://cdn.ticket-platform.com/events/evt_10001/banner.jpg"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

> **Note:** This is an advanced feature for future consideration. Current implementation provides basic event listing with filters.

---

# 14. Search Events

Full text or partial search on event name, performer, tags.

## Endpoint
```http
GET /v1/events/search
```

## Query Params
| Param | Type | Required | Description |
|------|------|----------|-------------|
| q | string | Yes | Search keyword |
| city | string | No | Optional city filter |
| page | int | No | Default 0 |
| size | int | No | Default 20 |

## Example
```http
GET /v1/events/search?q=coldplay&city=Bangalore
```

## Response
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "eventId": "evt_10001",
        "eventName": "Coldplay Live in Bangalore",
        "category": "MUSIC",
        "city": "Bangalore",
        "startTime": "2026-06-21T19:00:00Z",
        "status": "PUBLISHED"
      }
    ]
  }
}
```

> **Note:** This is an advanced feature for future consideration. Current implementation provides basic event listing with filters.

---

# Internal APIs

These endpoints are meant only for trusted services behind gateway/service mesh.  
They should be protected with:
- mTLS or service identity
- internal auth token
- network allowlisting
- gateway/internal route segregation

> **Note:** These are advanced features for future consideration. Current implementation focuses on core event management workflow and basic venue seat layout APIs.

---

# 15. Internal Get Event Inventory Metadata

Used by Booking Service to understand event + ticket type metadata.

## Endpoint
```http
GET /internal/v1/events/{eventId}/inventory-metadata
```

## Access
- `INTERNAL_SERVICE`

## Response
```json
{
  "eventId": "evt_10001",
  "status": "PUBLISHED",
  "venueId": "ven_301",
  "currency": "INR",
  "ticketTypes": [
    {
      "ticketTypeId": "tt_11",
      "name": "VIP",
      "price": 7500,
      "totalCapacity": 500
    },
    {
      "ticketTypeId": "tt_12",
      "name": "General",
      "price": 2500,
      "totalCapacity": 5000
    }
  ]
}
```

---

# 16. Internal Validate Event Eligibility

Used before booking initiation.

## Endpoint
```http
POST /internal/v1/events/{eventId}/validate-booking
```

## Request Body
```json
{
  "ticketRequests": [
    {
      "ticketTypeId": "tt_11",
      "quantity": 2
    }
  ]
}
```

## Response
```json
{
  "eligible": true,
  "reason": null
}
```

Possible reasons:
- `EVENT_NOT_PUBLISHED`
- `EVENT_CANCELLED`
- `EVENT_EXPIRED`
- `INVALID_TICKET_TYPE`

---

# 17. Internal Mark Event Completed

Used by scheduled job or internal workflow after end time.

## Endpoint
```http
POST /internal/v1/events/{eventId}/complete
```

## Response
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "status": "COMPLETED"
  }
}
```

---

# Request/Response Headers

## Standard Headers
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
X-Request-Id: <uuid>
X-Correlation-Id: <uuid>
X-Idempotency-Key: <unique-key>
```

## Notes
- `X-Request-Id` used for traceability
- `X-Correlation-Id` propagated across services
- `X-Idempotency-Key` recommended for create/update publish operations

---

# Status Codes

| HTTP Code | Meaning |
|----------|---------|
| 200 | Success |
| 201 | Resource created |
| 204 | No content |
| 400 | Bad request / validation failure |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Resource not found |
| 409 | Conflict |
| 422 | Business rule violation |
| 500 | Internal server error |

---

# Error Codes

| Error Code | Description |
|-----------|-------------|
| EVENT_NOT_FOUND | Event does not exist |
| VENUE_NOT_FOUND | Venue does not exist |
| INVALID_EVENT_STATE | Invalid operation for current state |
| EVENT_ALREADY_PUBLISHED | Event already published |
| EVENT_ALREADY_CANCELLED | Event already cancelled |
| DUPLICATE_TICKET_TYPE_NAME | Duplicate ticket type name |
| INVALID_DATE_RANGE | Start time greater than or equal to end time |
| INVALID_TICKET_CONFIGURATION | Ticket configuration invalid |
| TICKET_TYPE_NOT_FOUND | Ticket type does not exist |
| EVENT_HAS_ACTIVE_BOOKINGS | Event cannot be modified/deleted because bookings exist |
| CAPACITY_REDUCTION_NOT_ALLOWED | New capacity lower than sold/reserved count |

---

# Idempotency

Recommended for:
- create event
- publish event
- cancel event
- add ticket type

## Example Header
```http
X-Idempotency-Key: 7b1d2c64-11b1-4f89-96f1-11a4b7d0e001
```

Server behavior:
- same key + same request body → returns original response
- same key + different request body → returns conflict error

---

# Pagination Standard

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

---

# Security Considerations

For external APIs:
- JWT-based auth
- RBAC at gateway/service layer
- request validation
- rate limiting
- audit logging for create/update/delete/publish/cancel actions

For internal APIs:
- service-to-service auth
- internal route isolation
- no public exposure
- correlation-id propagation

---

# Sync vs Async Notes

## Synchronous usage
Used for critical request path:
- UI fetch event details
- Booking service validates event before booking
- UI lists events

## Asynchronous downstream events
Event Management Service should publish domain events for non-critical side effects:
- `EVENT_CREATED`
- `EVENT_PUBLISHED`
- `EVENT_UNPUBLISHED`
- `EVENT_CANCELLED`
- `EVENT_UPDATED`

Consumers may include:
- Search indexing service
- Notification service
- Analytics service
- Recommendation service
- Audit pipeline

---

# Example Sequence: Publish Event

1. Organizer calls `POST /organiser/events/{eventId}/publish`
2. Event service validates event completeness
3. Event state changes to `PUBLISHED`
4. Service emits `EVENT_PUBLISHED`
5. Search/discovery systems update asynchronously

---

# Example Sequence: Booking Validation

1. Booking Service calls `POST /internal/v1/events/{eventId}/validate-booking`
2. Event service checks:
   - event exists
   - event published
   - event not cancelled
   - event not expired
   - ticket types valid
3. Response returned synchronously
4. Booking service proceeds to seat lock / reservation flow

---

# Sample OpenAPI-style Endpoint Summary

| Method | Endpoint | Description |
|-------|----------|-------------|
| GET | `/events` | Browse public events |
| GET | `/events/{eventId}` | Get event details |
| POST | `/organiser/events` | Create event |
| PATCH | `/organiser/events/{eventId}` | Update event |
| POST | `/organiser/events/{eventId}/publish` | Publish event |
| POST | `/organiser/events/{eventId}/pricing` | Configure section pricing |
| POST | `/organiser/events/{eventId}/inventory/init` | Initialize event inventory |

All routes above are served under the application context path:

```text
/event-management-service/v1
```

---

# Future Enhancements

Possible additions:
- artist/performer APIs
- event image/banner upload APIs
- recurring events support
- multi-day event support
- seating map integration
- dynamic pricing support
- event approval workflow
- organizer onboarding APIs
