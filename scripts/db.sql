BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

SET search_path TO event_db;

-- -------------------------
-- VENUES
-- -------------------------
CREATE TABLE IF NOT EXISTS venues (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        VARCHAR(200) NOT NULL,
  city        VARCHAR(120) NOT NULL,
  address     TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_venues_city_name_ci
ON venues (lower(city), lower(name));

CREATE TABLE IF NOT EXISTS venue_sections (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id    UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
  name        VARCHAR(80) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (venue_id, name)
);

CREATE TABLE IF NOT EXISTS venue_seats (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id    UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
  section_id  UUID NOT NULL REFERENCES venue_sections(id) ON DELETE CASCADE,
  seat_code   VARCHAR(40) NOT NULL,
  row_label   VARCHAR(20),
  seat_number INT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (venue_id, seat_code)
);

CREATE INDEX IF NOT EXISTS idx_venue_seats_venue   ON venue_seats(venue_id);
CREATE INDEX IF NOT EXISTS idx_venue_seats_section ON venue_seats(section_id);

-- -------------------------
-- EVENTS (NO FK to users)
-- -------------------------
CREATE TABLE IF NOT EXISTS events (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- references user-service user id; no foreign key
  organiser_id   UUID NOT NULL,
  organiser_email VARCHAR(320),    -- optional snapshot

  venue_id       UUID NOT NULL REFERENCES venues(id),

  title          VARCHAR(200) NOT NULL,
  description    TEXT,
  category       VARCHAR(60),
  starts_at      TIMESTAMPTZ NOT NULL,
  ends_at        TIMESTAMPTZ,
  status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, CANCELLED

  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_event_status CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_events_organiser     ON events(organiser_id);
CREATE INDEX IF NOT EXISTS idx_events_venue_starts  ON events(venue_id, starts_at);
CREATE INDEX IF NOT EXISTS idx_events_status_starts ON events(status, starts_at);

-- Section pricing per event
CREATE TABLE IF NOT EXISTS event_section_pricing (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  section_id  UUID NOT NULL REFERENCES venue_sections(id),
  price_cents INT NOT NULL CHECK (price_cents > 0),
  currency    VARCHAR(3) NOT NULL DEFAULT 'INR',
  UNIQUE (event_id, section_id)
);

-- Seat inventory per event
CREATE TABLE IF NOT EXISTS event_seats (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id      UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  venue_seat_id UUID NOT NULL REFERENCES venue_seats(id),
  section_id    UUID NOT NULL REFERENCES venue_sections(id),
  price_cents   INT NOT NULL CHECK (price_cents > 0),
  currency      VARCHAR(3) NOT NULL DEFAULT 'INR',
  status        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  version       INT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (event_id, venue_seat_id),
  CONSTRAINT chk_event_seat_status CHECK (status IN ('AVAILABLE','LOCKED','BOOKED'))
);

CREATE INDEX IF NOT EXISTS idx_event_seats_event_status ON event_seats(event_id, status);
CREATE INDEX IF NOT EXISTS idx_event_seats_event_section ON event_seats(event_id, section_id);

COMMIT;