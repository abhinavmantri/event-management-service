# Event Management Service

## Overview
`event-management-service` is responsible for managing event and venue domain data for the ticketing platform.

It owns:
- venues
- venue sections
- venue seats
- events
- event pricing
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
- initialize inventory through seat-allocation-service
- publish / cancel events
- provide public browse and event detail APIs
- provide internal venue seat layout APIs for downstream services

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

### Internal Downstream
- seat-allocation-service

---

## Data Ownership

### This service database owns
- `venues`
- `venue_sections`
- `venue_seats`
- `events`
- `event_section_pricing`

### This service does not store
- `event_seats`
- lock state
- booking state
- payment state

`event_seats` is owned by **seat-allocation-service**.

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
- `organizerId`
- `venueId`
- `title`
- `description`
- `category`
- `currency`
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
- `organizerId` is an external reference from `user-service`
- no foreign key to users table is maintained in this service

---

### EventSectionPricing
Represents section-level price configuration for an event.

Fields:
- `id`
- `eventId`
- `sectionId`
- `priceMinor`

Notes:
- currency is owned at event level
- pricing is configured before inventory initialization
- pricing updates after inventory initialization are not allowed in MVP

---

## Currency Model

- Currency is stored at **event level**
- All seats of an event use the same currency
- Prices are stored in **minor units**
  - INR: paise
  - USD: cents
- Example:
  - ₹1500 => `150000`

This service stores:
- `events.currency`
- `event_section_pricing.price_minor`

---

## Public APIs

## Browse Events
Returns paginated published events for customers.

`GET /api/v1/events`

### Supported filters
- `q`
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

`GET /api/v1/events/{eventId}`

---

## Venue Read APIs
Optional public or restricted read APIs depending on product needs.

`GET /api/v1/venues`
`GET /api/v1/venues/{venueId}`

---

## Organizer APIs

## Create Venue
Creates a new venue.

`POST /api/v1/organizer/venues`

---

## Add Venue Section
Creates a section under a venue.

`POST /api/v1/organizer/venues/{venueId}/sections`

---

## Generate Venue Seats
Generates venue seats for a section based on layout input.

`POST /api/v1/organizer/venues/{venueId}/sections/{sectionId}/seats/generate`

### Business Logic
- validates that section belongs to venue
- generates seat rows and seat numbers
- creates unique seat codes like `VIP-R01-S01`
- rejects duplicate seat generation for same section in MVP

---

## Create Event
Creates an event in `DRAFT` state.

`POST /api/v1/organizer/events`

### Notes
- `organizerId` is derived from JWT
- organizer information is not accepted from request body

---

## Update Event
Updates an existing event.

`PATCH /api/v1/organizer/events/{eventId}`

### Rules
- `DRAFT` events are fully editable
- `PUBLISHED` events allow only safe/non-breaking fields in future expansion
- MVP recommendation:
  - allow updates only in `DRAFT`
  - allow cancel separately

---

## Set Event Pricing
Configures full section pricing for the event.

`PUT /api/v1/organizer/events/{eventId}/pricing`

### Notes
- request must include complete pricing for all required sections
- pricing is section-based
- pricing is stored in minor units
- partial pricing should be rejected for inventory initialization path

---

## Initialize Inventory
Triggers creation of event seat inventory in seat-allocation-service.

`POST /api/v1/organizer/events/{eventId}/inventory/init`

### Flow
1. validate organizer ownership
2. validate event status is `DRAFT`
3. validate pricing is configured
4. internally call seat-allocation-service
5. allocation service creates `event_seats` in its own database

### Notes
- this service does **not** store `event_seats`
- this endpoint is synchronous because inventory creation is correctness-critical

---

## Publish Event
Publishes event for customer visibility.

`POST /api/v1/organizer/events/{eventId}/publish`

### Rules
- inventory must be initialized before publish
- pricing must already exist
- only organizer owner or admin may publish

---

## Cancel Event
Cancels an event.

`POST /api/v1/organizer/events/{eventId}/cancel`

---

## Internal APIs

## Get Venue Seats
Returns venue seat layout for internal service consumption.

`GET /internal/v1/venues/{venueId}/seats`

### Used by
- seat-allocation-service during inventory initialization

### Security
- internal-only
- not exposed publicly through gateway
- protected by service-to-service auth

---

## Inter-Service Communication

### Synchronous Calls
This service uses synchronous communication only for correctness-critical paths.

#### Event Management -> Seat Allocation
- inventory initialization

Reason:
- event publish / inventory setup requires immediate success or failure

### Asynchronous Communication
Not required for current core event setup path.
Can be added later for:
- search projections
- analytics
- audit trail

---

## Security Model

### Public/Organizer APIs
- authenticated through JWT validated at gateway/resource server layer
- organizer ownership enforced in service logic
- organizer id is taken from JWT claims

### Internal APIs
- `/internal/**` endpoints are not publicly exposed
- protected using service-to-service authentication such as shared internal auth header
- should also be network-restricted

---

## Business Rules

- user data is not stored in this service database
- `organizerId` is an external reference
- venue names are unique within a city
- section names are unique within a venue
- seat codes are unique within a venue
- currency is event-level
- prices are stored in minor units
- inventory state is owned by seat-allocation-service
- pricing must be configured before inventory initialization
- inventory must be initialized before publish
- published events are served through public browse APIs only

---

## Status Rules

### Event Lifecycle
- `DRAFT` -> `PUBLISHED`
- `DRAFT` -> `CANCELLED`
- `PUBLISHED` -> `CANCELLED`

### Pricing Rules
- pricing is configured before inventory init
- no pricing updates after inventory init in MVP

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

POST /api/v1/venues (ADMIN/ORGANISER if you allow)

GET /api/v1/venues

GET /api/v1/venues/{venueId}

POST /api/v1/venues/{venueId}/sections

POST /api/v1/venues/{venueId}/sections/{sectionId}/seats/generate (optional convenience)

Event APIs

POST /api/v1/organiser/events

PATCH /api/v1/organiser/events/{eventId}

POST /api/v1/organiser/events/{eventId}/publish

GET /api/v1/events (public browse)

GET /api/v1/events/{eventId}

Event pricing & inventory init

POST /api/v1/organiser/events/{eventId}/pricing

POST /api/v1/organiser/events/{eventId}/inventory/init
Creates event_seats from venue_seats (or auto on publish)
