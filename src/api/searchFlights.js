/**
 * Flight search data access.
 * Today: sample inventory (no credentials).
 * Tomorrow: point this at Snowflake, Amadeus, or another fare feed
 * by reading process.env on a server route and keeping this signature.
 */

import {
  AIRLINE_NAMES,
  AIRPORTS,
  FLIGHTS,
} from "../data/sampleFlights.js";

const CITY_CODES = AIRPORTS.reduce((map, airport) => {
  const key = airport.city.toLowerCase();
  if (!map[key]) map[key] = [];
  map[key].push(airport.code);
  return map;
}, {});

function resolveCodes(query) {
  if (!query) return [];
  const q = query.trim().toUpperCase();
  const byCode = AIRPORTS.filter((a) => a.code === q).map((a) => a.code);
  if (byCode.length) return byCode;

  const city = query.trim().toLowerCase();
  if (CITY_CODES[city]) return CITY_CODES[city];

  return AIRPORTS.filter(
    (a) =>
      a.city.toLowerCase().includes(city) ||
      a.name.toLowerCase().includes(city) ||
      a.code.includes(q),
  ).map((a) => a.code);
}

function formatDuration(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m.toString().padStart(2, "0")}m`;
}

function enrich(flight) {
  const from = AIRPORTS.find((a) => a.code === flight.from);
  const to = AIRPORTS.find((a) => a.code === flight.to);
  return {
    ...flight,
    airlineName: AIRLINE_NAMES[flight.airline] || flight.airline,
    fromCity: from?.city || flight.from,
    toCity: to?.city || flight.to,
    durationLabel: formatDuration(flight.durationMin),
    stopsLabel:
      flight.stops === 0
        ? "Direct"
        : flight.stops === 1
          ? "1 stop"
          : `${flight.stops} stops`,
  };
}

/**
 * @param {{ from: string, to: string, date?: string, passengers?: number }} query
 * @returns {Promise<{ results: object[], source: 'sample', meta: object }>}
 */
export async function searchFlights(query) {
  // Simulate network latency so the UI can show a loading state.
  await new Promise((r) => setTimeout(r, 420 + Math.random() * 280));

  const fromCodes = resolveCodes(query.from);
  const toCodes = resolveCodes(query.to);

  if (!fromCodes.length || !toCodes.length) {
    return {
      results: [],
      source: "sample",
      meta: {
        fromCodes,
        toCodes,
        passengers: query.passengers || 1,
        date: query.date || null,
        message: "No matching airports. Try a city or IATA code.",
      },
    };
  }

  const matches = FLIGHTS.filter(
    (f) => fromCodes.includes(f.from) && toCodes.includes(f.to),
  ).map(enrich);

  // Light day-of-week price wobble so dates feel meaningful with sample data.
  const dayFactor = query.date
    ? 1 + ((new Date(query.date + "T12:00:00").getDay() % 5) - 2) * 0.035
    : 1;

  const passengers = Math.max(1, Number(query.passengers) || 1);
  const results = matches.map((f) => ({
    ...f,
    unitPrice: Math.round(f.price * dayFactor),
    totalPrice: Math.round(f.price * dayFactor) * passengers,
  }));

  results.sort((a, b) => a.unitPrice - b.unitPrice);

  return {
    results,
    source: "sample",
    meta: {
      fromCodes,
      toCodes,
      passengers,
      date: query.date || null,
      count: results.length,
    },
  };
}

export function listAirports(filter = "") {
  const q = filter.trim().toLowerCase();
  if (!q) return AIRPORTS.slice(0, 12);
  return AIRPORTS.filter(
    (a) =>
      a.code.toLowerCase().includes(q) ||
      a.city.toLowerCase().includes(q) ||
      a.name.toLowerCase().includes(q),
  ).slice(0, 12);
}

export { AIRPORTS };
