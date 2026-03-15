# Event Management Service API

## Overview
The **Event Management Service** is responsible for managing events, ticket categories, event schedules, and event discovery metadata for the Ticketmaster-inspired platform.

This service handles:
- event creation and updates
- event publishing/unpublishing
- event retrieval
- ticket type configuration
- event listing and filtering
- inventory metadata exposure for downstream services

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
http://localhost:8081
```

## Gateway
```text
https://api.ticket-platform.com/event-service
```

## Version
```text
/v1
```

Example:
```text
http://localhost:8081/v1/events
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
| ADMIN | Full access to create, update, publish, unpublish, delete events |
| ORGANIZER | Can create and manage own events |
| CUSTOMER | Can only view published events |
| INTERNAL_SERVICE | Internal read/update APIs used by trusted downstream services |

---

# Resource Model

## Event
An event represents a show, concert, play, sports match, or any scheduled occurrence at a venue.

## Ticket Type
Each event can have one or more ticket categories such as:
- VIP
- Premium
- General
- Balcony

## Event Status
Possible states:

- `DRAFT`
- `PUBLISHED`
- `UNPUBLISHED`
- `CANCELLED`
- `COMPLETED`

---

# Event Object

```json
{
  "eventId": "evt_10001",
  "organizerId": "org_501",
  "eventName": "Coldplay Live in Bangalore",
  "description": "Live concert at Bangalore Stadium",
  "category": "MUSIC",
  "language": "ENGLISH",
  "venueId": "ven_301",
  "city": "Bangalore",
  "country": "India",
  "startTime": "2026-06-21T19:00:00Z",
  "endTime": "2026-06-21T23:00:00Z",
  "status": "PUBLISHED",
  "currency": "INR",
  "tags": ["concert", "live", "coldplay"],
  "ticketTypes": [
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
  ],
  "createdAt": "2026-03-15T10:20:00Z",
  "updatedAt": "2026-03-15T11:00:00Z"
}
```

---

# API Conventions

## Common Response Wrapper
```json
{
  "success": true,
  "data": {},
  "meta": {}
}
```

## Common Error Wrapper
```json
{
  "success": false,
  "error": {
    "code": "EVENT_NOT_FOUND",
    "message": "Event does not exist",
    "details": []
  },
  "timestamp": "2026-03-15T11:15:20Z"
}
```

---

# Endpoints

---

# 1. Create Event

Creates a new event in `DRAFT` state.

## Endpoint
```http
POST /v1/events
```

## Access
- `ADMIN`
- `ORGANIZER`

## Request Body
```json
{
  "eventName": "Coldplay Live in Bangalore",
  "description": "Live concert at Bangalore Stadium",
  "category": "MUSIC",
  "language": "ENGLISH",
  "venueId": "ven_301",
  "city": "Bangalore",
  "country": "India",
  "startTime": "2026-06-21T19:00:00Z",
  "endTime": "2026-06-21T23:00:00Z",
  "currency": "INR",
  "tags": ["concert", "live", "coldplay"],
  "ticketTypes": [
    {
      "name": "VIP",
      "price": 7500,
      "totalCapacity": 500,
      "description": "Closest seating with perks"
    },
    {
      "name": "General",
      "price": 2500,
      "totalCapacity": 5000,
      "description": "General admission"
    }
  ]
}
```

## Response
**201 Created**
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "status": "DRAFT"
  }
}
```

## Validation Rules
- `eventName` is required
- `venueId` is required
- `startTime` must be before `endTime`
- at least one ticket type is required
- ticket type names must be unique within the event
- price must be >= 0
- totalCapacity must be > 0

---

# 2. Get Event by ID

Returns complete event details.

## Endpoint
```http
GET /v1/events/{eventId}
```

## Access
- `ADMIN`
- `ORGANIZER` for owned events
- `CUSTOMER` only if event is `PUBLISHED`

## Path Params
| Param | Type | Description |
|------|------|-------------|
| eventId | string | Unique event identifier |

## Response
**200 OK**
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "organizerId": "org_501",
    "eventName": "Coldplay Live in Bangalore",
    "description": "Live concert at Bangalore Stadium",
    "category": "MUSIC",
    "language": "ENGLISH",
    "venueId": "ven_301",
    "city": "Bangalore",
    "country": "India",
    "startTime": "2026-06-21T19:00:00Z",
    "endTime": "2026-06-21T23:00:00Z",
    "status": "PUBLISHED",
    "currency": "INR",
    "tags": ["concert", "live", "coldplay"],
    "ticketTypes": [
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
}
```

---

# 3. List Events

Returns paginated event list with filters.

## Endpoint
```http
GET /v1/events
```

## Access
- Public for published events
- Admin/Organizer can optionally view broader scopes

## Query Params

| Param | Type | Required | Description |
|------|------|----------|-------------|
| city | string | No | Filter by city |
| category | string | No | Filter by category |
| venueId | string | No | Filter by venue |
| status | string | No | Mainly for admin/organizer |
| startDate | string | No | ISO date filter lower bound |
| endDate | string | No | ISO date filter upper bound |
| search | string | No | Search by event name/tags |
| page | int | No | Default 0 |
| size | int | No | Default 20 |
| sortBy | string | No | `startTime`, `createdAt`, `eventName` |
| sortOrder | string | No | `asc`, `desc` |

## Example
```http
GET /v1/events?city=Bangalore&category=MUSIC&page=0&size=10&sortBy=startTime&sortOrder=asc
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
        "venueId": "ven_301",
        "city": "Bangalore",
        "startTime": "2026-06-21T19:00:00Z",
        "endTime": "2026-06-21T23:00:00Z",
        "status": "PUBLISHED",
        "currency": "INR",
        "startingPrice": 2500
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

# 4. Update Event

Updates mutable event fields.

## Endpoint
```http
PUT /v1/events/{eventId}
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Rules
- update allowed only for `DRAFT` and `UNPUBLISHED`
- for `PUBLISHED`, only limited fields may be editable depending on business rules
- start time changes may be blocked once bookings begin

## Request Body
```json
{
  "eventName": "Coldplay Live in Bengaluru",
  "description": "Updated concert description",
  "category": "MUSIC",
  "language": "ENGLISH",
  "startTime": "2026-06-21T19:30:00Z",
  "endTime": "2026-06-21T23:30:00Z",
  "tags": ["concert", "live", "coldplay", "bengaluru"]
}
```

## Response
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "status": "DRAFT",
    "updated": true
  }
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
POST /v1/events/{eventId}/publish
```

## Access
- `ADMIN`
- owning `ORGANIZER`

## Preconditions
- mandatory event fields populated
- venue exists and is active
- at least one valid ticket type configured
- start time must be in future

## Response
```json
{
  "success": true,
  "data": {
    "eventId": "evt_10001",
    "status": "PUBLISHED"
  }
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

1. Organizer calls `POST /v1/events/{eventId}/publish`
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
| POST | `/v1/events` | Create event |
| GET | `/v1/events/{eventId}` | Get event details |
| GET | `/v1/events` | List events |
| PUT | `/v1/events/{eventId}` | Update event |
| DELETE | `/v1/events/{eventId}` | Delete event |
| POST | `/v1/events/{eventId}/publish` | Publish event |
| POST | `/v1/events/{eventId}/unpublish` | Unpublish event |
| POST | `/v1/events/{eventId}/cancel` | Cancel event |
| POST | `/v1/events/{eventId}/ticket-types` | Add ticket type |
| PUT | `/v1/events/{eventId}/ticket-types/{ticketTypeId}` | Update ticket type |
| DELETE | `/v1/events/{eventId}/ticket-types/{ticketTypeId}` | Delete ticket type |
| GET | `/v1/events/{eventId}/ticket-types` | Get ticket types |
| GET | `/v1/events/discovery` | Discovery listing |
| GET | `/v1/events/search` | Search events |
| GET | `/internal/v1/events/{eventId}/inventory-metadata` | Internal inventory metadata |
| POST | `/internal/v1/events/{eventId}/validate-booking` | Internal booking validation |
| POST | `/internal/v1/events/{eventId}/complete` | Mark event completed |

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
