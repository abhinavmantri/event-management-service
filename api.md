# Event Management Service API

Current HTTP contract for `event-management-service`.

Base path:

```text
/event-management-service/v1
```

## Conventions

Organiser and venue mutation APIs use:

```http
Authorization: Bearer <jwt>
```

The organiser filter accepts JWT role value `ORGANIZER`. Venue creation accepts `ADMIN` or `ORGANIZER`.

Most responses use:

- `responseStatus: SUCCESS`
- `responseStatus: FAILURE`

## POST `/venues`

Creates a venue.

### Request

```json
{
  "name": "Dubai Arena",
  "city": "Dubai",
  "address": "Downtown Dubai"
}
```

### Success Response

`201 Created`

```json
{
  "message": "Venue created successfully",
  "responseStatus": "SUCCESS",
  "venue": {
    "id": "11111111-1111-1111-1111-111111111111",
    "name": "Dubai Arena",
    "city": "Dubai",
    "address": "Downtown Dubai"
  }
}
```

### Behavior Notes

- Venue uniqueness is enforced by lowercase city and name.
- Create requires JWT role `ADMIN` or `ORGANIZER`.

## GET `/venues`

Lists/searches venues.

### Query Params

| Param | Purpose |
| --- | --- |
| `query` | Optional venue search text |
| `page` | Page number |
| `size` | Page size |

## GET `/venues/{venueId}`

Returns venue detail.

## POST `/venues/{venueId}/sections`

Creates a section inside a venue.

### Request

```json
{
  "name": "VIP",
  "sortOrder": 1
}
```

### Behavior Notes

- Section name is unique within a venue.
- Section belongs to the venue layout used later for event inventory generation.

## POST `/venues/{venueId}/sections/{sectionId}/seats/generate`

Generates physical seats for a section.

### Request

```json
{
  "rowCount": 10,
  "seatsPerRow": 20,
  "rowLabelType": 1
}
```

### Behavior Notes

- `rowLabelType = 0` generates numeric row labels like `R01`.
- `rowLabelType = 1` generates alphabetic row labels like `A`, `B`.
- Seat codes are generated from section name, row label, and seat number.
- Generation is blocked if the venue is already used by a published event.
- Duplicate generation for the same section is rejected.

## POST `/organiser/events`

Creates an event in `DRAFT` state.

### Request

```json
{
  "venueId": "11111111-1111-1111-1111-111111111111",
  "title": "Backend Architecture Summit",
  "description": "Engineering leadership and distributed systems sessions",
  "category": "Technology",
  "startsAt": "2026-08-10T14:00:00Z",
  "endsAt": "2026-08-10T18:00:00Z"
}
```

### Success Response

`201 Created`

```json
{
  "message": "Event created successfully",
  "responseStatus": "SUCCESS",
  "event": {
    "id": "22222222-2222-2222-2222-222222222222",
    "title": "Backend Architecture Summit",
    "status": "DRAFT"
  }
}
```

### Behavior Notes

- `organiserId` and `organiserEmail` are derived from JWT claims.
- Duplicate event is rejected for same organiser, venue, title, and start time.

## PATCH `/organiser/events/{eventId}`

Updates an organiser-owned event.

### Request

```json
{
  "title": "Backend Architecture Summit 2026",
  "description": "Updated event description",
  "category": "Technology",
  "startsAt": "2026-08-10T14:00:00Z",
  "endsAt": "2026-08-10T18:30:00Z"
}
```

### Behavior Notes

- Lookup is scoped by `eventId` and organiser id from JWT.
- `endsAt` must be greater than or equal to `startsAt`.
- Current implementation does not fully restrict update by event status.

## POST `/organiser/events/{eventId}/pricing`

Configures section-level pricing for an event.

### Request

```json
{
  "currency": "INR",
  "prices": [
    {
      "sectionId": "33333333-3333-3333-3333-333333333333",
      "priceCents": 250000
    },
    {
      "sectionId": "44444444-4444-4444-4444-444444444444",
      "priceCents": 150000
    }
  ]
}
```

### Behavior Notes

- Pricing is allowed only while the event is `DRAFT`.
- Currency defaults to `INR` if omitted or blank.
- Currency must be a 3-letter code.
- Duplicate section ids are rejected.
- Section ids must belong to the event venue.
- Price must be greater than zero.

## POST `/organiser/events/{eventId}/inventory/init`

Creates event-specific seat inventory from the venue seat layout.

### Success Response

`200 OK`

```json
{
  "message": "Event inventory initialized successfully",
  "responseStatus": "SUCCESS",
  "eventId": "22222222-2222-2222-2222-222222222222",
  "createdSeats": 200,
  "alreadyInitialized": false
}
```

### Already Initialized Response

`200 OK`

```json
{
  "message": "Event inventory already initialized",
  "responseStatus": "SUCCESS",
  "eventId": "22222222-2222-2222-2222-222222222222",
  "createdSeats": 0,
  "alreadyInitialized": true
}
```

### Behavior Notes

- Event must be organiser-owned.
- Event must be `DRAFT`.
- Pricing must exist before inventory initialization.
- Every venue seat must have pricing for its section.
- Created event seats start as `AVAILABLE`.

## POST `/organiser/events/{eventId}/publish`

Publishes an event and emits the inventory message to Kafka after commit.

### Success Response

`200 OK`

```json
{
  "message": "Event published successfully",
  "responseStatus": "SUCCESS",
  "eventId": "22222222-2222-2222-2222-222222222222"
}
```

### Behavior Notes

- Event must be organiser-owned.
- Event must be `DRAFT`.
- Section pricing must be configured.
- Event inventory must be initialized.
- After the transaction commits, the service publishes `EVENT_PUBLISHED` to `inventory-init.v1`.

## Kafka Message: `inventory-init.v1`

Published after event publish.

```json
{
  "eventType": "EVENT_PUBLISHED",
  "eventId": "22222222-2222-2222-2222-222222222222",
  "venueId": "11111111-1111-1111-1111-111111111111",
  "organiserId": "55555555-5555-5555-5555-555555555555",
  "organiserEmail": "organiser@example.com",
  "title": "Backend Architecture Summit",
  "category": "Technology",
  "startsAt": "2026-08-10T14:00:00Z",
  "endsAt": "2026-08-10T18:00:00Z",
  "publishedAt": "2026-07-01T10:00:00Z",
  "sectionPrices": [
    {
      "sectionId": "33333333-3333-3333-3333-333333333333",
      "sectionName": "VIP",
      "sortOrder": 1,
      "priceCents": 250000,
      "currency": "INR"
    }
  ],
  "seats": [
    {
      "eventSeatId": "66666666-6666-6666-6666-666666666666",
      "venueSeatId": "77777777-7777-7777-7777-777777777777",
      "sectionId": "33333333-3333-3333-3333-333333333333",
      "seatCode": "VIP-A-S01",
      "rowLabel": "A",
      "seatNumber": 1,
      "priceCents": 250000,
      "currency": "INR"
    }
  ]
}
```

## GET `/api/v1/events`

Browses published upcoming events.

### Query Params

| Param | Purpose |
| --- | --- |
| `query` | Optional text search |
| `city` | Optional venue city filter |
| `category` | Optional event category filter |
| `startDate` | Optional ISO date-time lower bound |
| `endDate` | Optional ISO date-time upper bound |
| `page` | Page number |
| `size` | Page size |

### Behavior Notes

- Only `PUBLISHED` events are returned.
- Only events with `startsAt >= now` are returned.
- Results are paginated.

## GET `/api/v1/events/{eventId}`

Returns a published event by id.

### Behavior Notes

- Only `PUBLISHED` events are returned.
- Current implementation returns HTTP `200` with `responseStatus = FAILURE` when the event is not found.

## Common Errors

- `400 Bad Request` for invalid event state or malformed request.
- `401 Unauthorized` for missing/invalid JWT on protected APIs.
- `403 Forbidden` for insufficient role.
- `404 Not Found` for organiser-owned resources that do not exist or do not belong to the organiser.
- `409 Conflict` for duplicate venue/event/section creation.
