# Farlane

Skyscanner-style flight fare comparison UI.

## Features

- Search by city or IATA code
- Sort by price, duration, or departure
- Filter by stops
- Sample inventory for local preview (swap `src/api/searchFlights.js` for a live feed)

## Run

```bash
npm install
npm run dev
```

## Data

Results currently come from `src/data/sampleFlights.js`. Connect Snowflake, Amadeus, or a carrier API through the search module when credentials are available.
