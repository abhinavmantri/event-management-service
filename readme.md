# Event Management Service

Spring Boot microservice for venue setup, event lifecycle management, section-level pricing, event inventory initialization, and public event discovery in the Ticketmaster-style backend platform.

This service is the upstream source of truth for event metadata and event seat inventory creation. It models venues and physical seats, creates event-specific seat inventory from venue layouts, and publishes inventory data to Kafka when an event becomes available for customers.

## Where It Fits

```text
organiser / admin
      |
      | venue, section, seat layout, event, pricing
      v
event-management-service
      |
      | publish event after inventory is initialized
      v
Kafka topic: inventory-init.v1
      |
      v
seats-allocation-service
      |
      | checkout-time lock / confirm / release
      v
booking + payment flow
```

Kafka interaction is intentionally narrow: this service publishes event inventory to `inventory-init.v1`, and seats-allocation-service consumes it. Checkout-time locking, payment, booking, and ticket issuance are handled by downstream services.

## What It Does Today

- Creates venues and keeps venue names unique within a city.
- Creates venue sections and keeps section names unique within a venue.
- Generates physical venue seats for a section using row/seat layout input.
- Prevents new seat generation for venues already used by published events.
- Creates organiser-owned events in `DRAFT` state.
- Updates organiser-owned event metadata.
- Configures section-level event pricing while the event is still draft.
- Initializes event-specific seat inventory from venue seats and pricing.
- Requires pricing and initialized inventory before publishing an event.
- Publishes an `EVENT_PUBLISHED` Kafka message after the publish transaction commits.
- Exposes public browse/detail APIs for published upcoming events.

## Core Components

| Component | Responsibility |
| --- | --- |
| `VenueController` | Venue create, list, search, and detail APIs |
| `VenueSectionController` | Section creation and physical seat generation |
| `OrganiserEventController` | Organiser event create/update/pricing/inventory/publish APIs |
| `PublicEventController` | Customer event browse and detail APIs |
| `VenueService` | Venue ownership and uniqueness rules |
| `VenueSectionService` | Section creation and deterministic venue seat generation |
| `OrganiserEventService` | Event lifecycle, pricing, inventory initialization, and publish workflow |
| `EventService` | Published event search and detail lookup |
| `EventPublishedKafkaPublisher` | After-commit Kafka publication for event inventory |
| `OrganiserAuthorizationFilter` | JWT role enforcement for organiser APIs |
| `VenueAuthorizationFilter` | JWT role enforcement for venue creation |

## Event Lifecycle

```text
DRAFT
  |
  | configure pricing
  v
DRAFT + pricing
  |
  | initialize inventory from venue seats
  v
DRAFT + event_seats
  |
  | publish
  v
PUBLISHED + Kafka inventory-init.v1 message
```

Current status model:

- `DRAFT`
- `PUBLISHED`
- `CANCELLED`

Publishing is allowed only from `DRAFT`. The current implementation requires both section pricing and event seat inventory before publish.

## Inventory Initialization

Inventory initialization creates event-specific seats from the reusable venue layout:

1. Organiser creates a venue.
2. Organiser adds venue sections.
3. Organiser generates physical venue seats for each section.
4. Organiser creates an event for the venue.
5. Organiser configures section-level pricing for the event.
6. Organiser initializes event inventory.
7. Service creates `event_seats` with `AVAILABLE` status, price, currency, section, and venue-seat reference.

If inventory already exists, the endpoint returns success with `createdSeats = 0`.

## Kafka Publication

When an event is published, `OrganiserEventService` publishes an internal domain event. `EventPublishedKafkaPublisher` listens after transaction commit and sends a Kafka message.

Configured topic:

```text
inventory-init.v1
```

Message includes:

- event id
- venue id
- organiser id and email snapshot
- title and category
- start/end time
- published time
- section prices
- event seat inventory including event seat id, venue seat id, section id, seat code, row label, seat number, price, and currency

This makes seats-allocation-service independent of event-management database reads during checkout.

## API Surface

Configured base path:

```text
/event-management-service/v1
```

Primary endpoints:

| Endpoint | Purpose |
| --- | --- |
| `POST /venues` | Create venue |
| `GET /venues` | List/search venues |
| `GET /venues/{venueId}` | Get venue detail |
| `POST /venues/{venueId}/sections` | Add venue section |
| `POST /venues/{venueId}/sections/{sectionId}/seats/generate` | Generate physical seats for a section |
| `POST /organiser/events` | Create draft event |
| `PATCH /organiser/events/{eventId}` | Update organiser-owned event |
| `POST /organiser/events/{eventId}/pricing` | Configure section pricing |
| `POST /organiser/events/{eventId}/inventory/init` | Create event seat inventory |
| `POST /organiser/events/{eventId}/publish` | Publish event and emit inventory message |
| `GET /api/v1/events` | Browse published upcoming events |
| `GET /api/v1/events/{eventId}` | Get published event detail |

Detailed examples are in [api.md](api.md).

## Reliability And Consistency Choices

- **Clear ownership boundary:** event-management owns event metadata and event inventory creation, not checkout locks.
- **Publish after commit:** Kafka publication runs through `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- **Inventory required before publish:** published events cannot be exposed without generated event seats.
- **Pricing required before inventory:** every generated event seat receives price and currency from section pricing.
- **Venue layout protection:** seat generation is blocked once a venue is used by a published event.
- **Organiser scoping:** organiser mutations use JWT claims and query by `eventId + organiserId`.
- **Public browse isolation:** public APIs return only published upcoming events.

## Data Model

Core tables:

- `venues`
- `venue_sections`
- `venue_seats`
- `events`
- `event_section_pricing`
- `event_seats`

Important constraints:

- unique venue by lowercase city and name
- unique section name within a venue
- unique seat code within a venue
- unique pricing row per event and section
- unique event seat per event and venue seat
- event seat status restricted to `AVAILABLE`, `LOCKED`, `BOOKED`

Schema: [scripts/db.sql](scripts/db.sql)

## Authentication

Organiser APIs require JWT with organiser role:

```http
Authorization: Bearer <jwt>
```

Accepted organiser role values:

- `ORGANISER`
- `ORGANIZER`

Venue creation requires `ADMIN`, `ORGANISER`, or `ORGANIZER`. Public browse/detail APIs are not protected by these filters.

## Configuration

Primary settings:

| Property | Purpose |
| --- | --- |
| `api.prefix` | Servlet context path |
| `spring.datasource.*` | PostgreSQL connection |
| `security.jwt.secret` | JWT validation secret |
| `spring.kafka.bootstrap-servers` | Kafka broker list |
| `spring.kafka.producer.client-id` | Kafka producer client id |
| `app.kafka.topics.event-published` | Topic used for inventory initialization |

## Running Locally

Prerequisites:

- Java 21
- PostgreSQL
- Kafka for publish-flow verification
- event schema from [scripts/db.sql](scripts/db.sql)

Run:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Run tests:

```powershell
.\mvnw.cmd test
```

## Current Limitations

- Kafka publish uses after-commit listener but not a durable outbox table yet.
- Public event detail currently returns HTTP `200` with failure status when a published event is not found.
- Venue seat generation checks published-event usage through repository loading rather than a dedicated indexed existence query.
- Update event currently allows updates while draft/published state rules are not fully hardened.
- Cancellation workflow is represented in the status model but not fully exposed as a production workflow.
- No OpenAPI contract is generated yet.

## Production Hardening Roadmap

- Add outbox pattern for guaranteed Kafka publication and retry.
- Add dead-letter/retry topic strategy for downstream inventory propagation.
- Add cancellation workflow and downstream event notifications.
- Add stricter event update rules after inventory initialization and publication.
- Add ownership and role-policy tests around every organiser/admin endpoint.
- Add Redis/search index for public browse if read volume grows.
- Add OpenAPI generation and contract tests for seats-allocation event payload compatibility.
