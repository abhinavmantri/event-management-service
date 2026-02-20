Venue & Event Management

Venue APIs

POST /api/v1/venues (ADMIN/ORGANIZER if you allow)

GET /api/v1/venues

GET /api/v1/venues/{venueId}

POST /api/v1/venues/{venueId}/sections

POST /api/v1/venues/{venueId}/sections/{sectionId}/seats/generate (optional convenience)

Event APIs

POST /api/v1/organizer/events

PATCH /api/v1/organizer/events/{eventId}

POST /api/v1/organizer/events/{eventId}/publish

GET /api/v1/events (public browse)

GET /api/v1/events/{eventId}

Event pricing & inventory init

POST /api/v1/organizer/events/{eventId}/pricing

POST /api/v1/organizer/events/{eventId}/inventory/init
Creates event_seats from venue_seats (or auto on publish)