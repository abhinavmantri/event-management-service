# Event Management Service

## Overview
`event-management-service` is responsible for managing event and venue domain data for the ticketing platform.

It owns:
- venues
- venue sections
- venue seats
- events
- event pricing
- event seat inventory
- public event browse/search APIs

It does **not** own:
- user identities
- seat lock state
- booked seat state
- payment lifecycle
- booking/ticket fulfillment

---

## Responsibilities

### Owned by this service
- create and manage venues
- create venue sections and venue seats
- create and manage events
- configure section-level pricing for events
- initialize event seat inventory from venue seats
- publish events
- provide public browse and event detail APIs

### Not owned by this service
- authentication and user profile storage
- seat locking and booking state
- payment provider integration
- booking and ticket generation

---

## Service Dependencies

### Upstream
- API Gateway
- user-service (JWT issued upstream)

## Data Ownership

### This service database owns
- `venues`
- `venue_sections`
- `venue_seats`
- `events`
- `event_section_pricing`
- `event_seats`

### This service does not store
- lock state
- booking state
- payment state

---

## Core Domain Model

### Venue
Represents a physical place where events are held.

Fields:
- `id`
- `name`
- `city`
- `address`
- `createdAt`
- `updatedAt`

Constraint:
- unique by `(city, name)` case-insensitively

---

### VenueSection
Represents a logical section inside a venue such as VIP, A, B.

Fields:
- `id`
- `venueId`
- `name`
- `sortOrder`
- `createdAt`

Constraint:
- unique by `(venueId, name)`

---

### VenueSeat
Represents a physical seat in a venue.

Fields:
- `id`
- `venueId`
- `sectionId`
- `seatCode`
- `rowLabel`
- `seatNumber`
- `createdAt`

Constraint:
- unique by `(venueId, seatCode)`

---

### Event
Represents a scheduled program at a venue.

Fields:
- `id`
- `organiserId`
- `organiserEmail`
- `venue`
- `title`
- `description`
- `category`
- `startsAt`
- `endsAt`
- `status`
- `createdAt`
- `updatedAt`

### Event Status
- `DRAFT`
- `PUBLISHED`
- `CANCELLED`

Notes:
- `organiserId` is an external reference from `user-service`
- no foreign key to users table is maintained in this service

---

### EventSectionPricing
Represents section-level price configuration for an event.

Fields:
- `id`
- `event`
- `section`
- `priceCents`
- `currency`

Notes:
- currency is stored on each pricing row
- prices are stored in integer minor units as `priceCents`
- pricing is configured before inventory initialization

---

## Public APIs

All routes below are served under the application context path:

`/event-management-service/v1`

## Browse Events
Returns paginated published events for customers.

`GET /events`

### Supported filters
- `query`
- `category`
- `city`
- `startDate`
- `endDate`
- `page`
- `size`
- `sort`

### Notes
- only `PUBLISHED` events are visible publicly
- browse path should be read-optimized
- Redis caching can be used for event detail and search response caching

---

## Get Event Detail
Returns event details for a single published event.

`GET /events/{eventId}`

---

## Venue Read APIs
Optional public or restricted read APIs depending on product needs.

`GET /venues`
`GET /venues/{venueId}`

---

## Organizer APIs

## Create Venue
Creates a new venue.

`POST /venues`

---

## Add Venue Section
Creates a section under a venue.

`POST /venues/{venueId}/sections`

---

## Generate Venue Seats
Generates venue seats for a section based on layout input.

`POST /venues/{venueId}/sections/{sectionId}/seats/generate`

### Business Logic
- validates that section belongs to venue
- generates seat rows and seat numbers
- creates unique seat codes like `VIP-R01-S01`
- rejects duplicate seat generation for same section in MVP

---

## Create Event
Creates an event in `DRAFT` state.

`POST /organiser/events`

### Notes
- `organiserId` is derived from JWT
- organiser information is not accepted from request body

---

## Update Event
Updates an existing event.

`PATCH /organiser/events/{eventId}`

### Rules
- current implementation validates organiser ownership by JWT claim
- current implementation allows updates regardless of status
- request validation still enforces valid date ordering

---

## Set Event Pricing
Configures full section pricing for the event.

`POST /organiser/events/{eventId}/pricing`

### Notes
- pricing is section-based
- pricing is stored in integer minor units as `priceCents`
- duplicate section ids are rejected
- section ids must belong to the event venue

---

## Initialize Inventory
Creates local `event_seats` from venue seats for the event.

`POST /organiser/events/{eventId}/inventory/init`

### Flow
1. validate organizer ownership
2. validate event status is `DRAFT`
3. validate pricing is configured
4. load venue seats for the event venue
5. create `event_seats` in this service database

### Notes
- this service stores `event_seats`
- this endpoint is synchronous because inventory creation is correctness-critical

---

## Publish Event
Publishes event for customer visibility.

`POST /organiser/events/{eventId}/publish`

### Rules
- current implementation requires pricing before publish
- pricing must already exist
- only the owning organiser can publish

---

## Inter-Service Communication

### Synchronous Calls
This service uses synchronous in-process persistence for correctness-critical paths.

### Asynchronous Communication
The current implementation publishes an `EVENT_PUBLISHED` style domain event to Kafka after publish.

---

## Security Model

### Public/Organizer APIs
- authenticated through JWT validated at gateway/resource server layer
- organiser ownership is enforced for organiser mutation operations
- organiser id is taken from JWT claims

---

## Business Rules

- user data is not stored in this service database
- `organiserId` is an external reference
- venue names are unique within a city
- section names are unique within a venue
- seat codes are unique within a venue
- prices are stored in minor units as `priceCents`
- inventory state is stored locally in `event_seats`
- pricing must be configured before inventory initialization
- current implementation does not require inventory initialization before publish
- published events are served through public browse APIs only

---

## Status Rules

### Event Lifecycle
- `DRAFT` -> `PUBLISHED`
- `DRAFT` -> `CANCELLED`
- `PUBLISHED` -> `CANCELLED`

### Pricing Rules
- pricing is configured before inventory init
- pricing is allowed only while the event is `DRAFT`

### Venue Rules
- venue seat generation is allowed before event publication
- major venue structure changes should be restricted once active events depend on that venue

---

## Performance / Read Path

Public browse APIs are read-heavy and should be optimized for low-latency lookups.

Recommended approach:
- use PostgreSQL indexes for browse filters
- add Redis cache for:
  - event detail
  - venue detail
  - browse/search response pages
- avoid joining seat allocation state in event browse path
- keep inventory state out of this service

---

Venue & Event Management

Venue APIs

POST /event-management-service/v1/venues

GET /event-management-service/v1/venues

GET /event-management-service/v1/venues/{venueId}

POST /event-management-service/v1/venues/{venueId}/sections

POST /event-management-service/v1/venues/{venueId}/sections/{sectionId}/seats/generate (optional convenience)

Event APIs

POST /event-management-service/v1/organiser/events

PATCH /event-management-service/v1/organiser/events/{eventId}

POST /event-management-service/v1/organiser/events/{eventId}/publish

GET /event-management-service/v1/events (public browse)

GET /event-management-service/v1/events/{eventId}

Event pricing & inventory init

POST /event-management-service/v1/organiser/events/{eventId}/pricing

POST /event-management-service/v1/organiser/events/{eventId}/inventory/init
Creates local event_seats from venue_seats
