import { useDeferredValue, useEffect, useId, useRef, useState } from "react";
import { listAirports, searchFlights } from "./api/searchFlights.js";
import "./App.css";

const DEFAULT_FORM = {
  from: "New York",
  to: "London",
  date: "",
  passengers: 1,
};

function todayISO() {
  const d = new Date();
  const offset = d.getTimezoneOffset();
  const local = new Date(d.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 10);
}

function money(n) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(n);
}

function AirportField({ label, value, onChange, name }) {
  const id = useId();
  const listId = `${id}-list`;
  const deferred = useDeferredValue(value);
  const suggestions = listAirports(deferred);

  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <input
        id={id}
        name={name}
        list={listId}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="City or code"
        autoComplete="off"
        required
      />
      <datalist id={listId}>
        {suggestions.map((a) => (
          <option key={a.code} value={`${a.city} (${a.code})`}>
            {a.name}
          </option>
        ))}
      </datalist>
    </label>
  );
}

function ResultRow({ flight, index, passengers }) {
  return (
    <article
      className="result"
      style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}
    >
      <div className="result-airline">
        <span className="airline-code">{flight.airline}</span>
        <span className="airline-name">{flight.airlineName}</span>
      </div>
      <div className="result-route">
        <div className="time-block">
          <strong>{flight.departTime}</strong>
          <span>
            {flight.from} · {flight.fromCity}
          </span>
        </div>
        <div className="route-mid">
          <span className="duration">{flight.durationLabel}</span>
          <div className="route-line" aria-hidden="true">
            <i />
          </div>
          <span className="stops">{flight.stopsLabel}</span>
        </div>
        <div className="time-block arrive">
          <strong>{flight.arriveTime}</strong>
          <span>
            {flight.to} · {flight.toCity}
          </span>
        </div>
      </div>
      <div className="result-price">
        <strong>{money(flight.unitPrice)}</strong>
        <span>
          {passengers > 1
            ? `${money(flight.totalPrice)} total · ${flight.cabin}`
            : flight.cabin}
        </span>
        <button type="button" className="select-btn">
          Select
        </button>
      </div>
    </article>
  );
}

export default function App() {
  const [form, setForm] = useState(() => ({
    ...DEFAULT_FORM,
    date: todayISO(),
  }));
  const [sort, setSort] = useState("price");
  const [stops, setStops] = useState("any");
  const [loading, setLoading] = useState(false);
  const [payload, setPayload] = useState(null);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);
  const resultsRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const data = await searchFlights(DEFAULT_FORM);
        if (!cancelled) {
          setPayload(data);
          setSearched(true);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || "Search failed");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    setSearched(true);
    try {
      const data = await searchFlights({
        from: form.from.replace(/\s*\([^)]*\)\s*$/, "").trim(),
        to: form.to.replace(/\s*\([^)]*\)\s*$/, "").trim(),
        date: form.date,
        passengers: Number(form.passengers) || 1,
      });
      setPayload(data);
      requestAnimationFrame(() => {
        resultsRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
    } catch (err) {
      setError(err.message || "Search failed");
    } finally {
      setLoading(false);
    }
  }

  const passengers = payload?.meta?.passengers || Number(form.passengers) || 1;

  let results = payload?.results || [];
  if (stops === "direct") results = results.filter((f) => f.stops === 0);
  if (stops === "1") results = results.filter((f) => f.stops <= 1);

  const sorted = [...results].sort((a, b) => {
    if (sort === "duration") return a.durationMin - b.durationMin;
    if (sort === "depart") return a.departTime.localeCompare(b.departTime);
    return a.unitPrice - b.unitPrice;
  });

  const cheapest = sorted[0]?.unitPrice;

  return (
    <div className="app">
      <div className="sky" aria-hidden="true">
        <div className="sky-photo" />
        <div className="sky-wash" />
        <div className="sky-glow" />
      </div>

      <header className="top">
        <a className="brand" href="/" aria-label="Farlane home">
          <span className="brand-mark" aria-hidden="true" />
          Farlane
        </a>
        <p className="tagline">Compare fares across airlines in one place.</p>
      </header>

      <main>
        <section className="hero">
          <h1>
            Fly farther.
            <br />
            Pay clearer.
          </h1>
          <p className="lede">
            Search routes, sort by price or time, and spot the direct option
            without the clutter.
          </p>

          <form className="search" onSubmit={onSubmit}>
            <div className="search-grid">
              <AirportField
                label="From"
                name="from"
                value={form.from}
                onChange={(from) => setForm((f) => ({ ...f, from }))}
              />
              <button
                type="button"
                className="swap"
                aria-label="Swap airports"
                onClick={() =>
                  setForm((f) => ({ ...f, from: f.to, to: f.from }))
                }
              >
                ⇄
              </button>
              <AirportField
                label="To"
                name="to"
                value={form.to}
                onChange={(to) => setForm((f) => ({ ...f, to }))}
              />
              <label className="field">
                <span>Depart</span>
                <input
                  type="date"
                  name="date"
                  value={form.date}
                  min={todayISO()}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, date: e.target.value }))
                  }
                  required
                />
              </label>
              <label className="field field-narrow">
                <span>Travelers</span>
                <select
                  value={form.passengers}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      passengers: Number(e.target.value),
                    }))
                  }
                >
                  {[1, 2, 3, 4, 5, 6].map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <button className="search-btn" type="submit" disabled={loading}>
              {loading ? "Searching…" : "Search flights"}
            </button>
          </form>
        </section>

        <section className="results-panel" ref={resultsRef}>
          <div className="results-head">
            <div>
              <h2>Fares</h2>
              <p>
                {loading
                  ? "Checking sample inventory…"
                  : searched
                    ? `${sorted.length} option${sorted.length === 1 ? "" : "s"}`
                    : "Run a search to see fares"}
                {cheapest != null && !loading ? ` · from ${money(cheapest)}` : ""}
              </p>
            </div>
            <div className="controls">
              <label>
                Stops
                <select
                  value={stops}
                  onChange={(e) => setStops(e.target.value)}
                  disabled={loading}
                >
                  <option value="any">Any</option>
                  <option value="direct">Direct only</option>
                  <option value="1">1 stop or fewer</option>
                </select>
              </label>
              <label>
                Sort
                <select
                  value={sort}
                  onChange={(e) => setSort(e.target.value)}
                  disabled={loading}
                >
                  <option value="price">Cheapest</option>
                  <option value="duration">Shortest</option>
                  <option value="depart">Departure</option>
                </select>
              </label>
            </div>
          </div>

          {payload?.source === "sample" && (
            <p className="sample-banner" role="note">
              Showing <strong>sample data</strong>. Live fares can come from
              Snowflake, Amadeus, or a carrier API once connected.
            </p>
          )}

          {error && <p className="error">{error}</p>}

          {loading && (
            <div className="skeleton-stack" aria-hidden="true">
              {[0, 1, 2].map((i) => (
                <div key={i} className="skeleton" />
              ))}
            </div>
          )}

          {!loading && searched && sorted.length === 0 && (
            <div className="empty">
              <h3>No flights on this route</h3>
              <p>
                Try New York → London, Delhi → London, LAX → JFK, or Singapore →
                Sydney.
              </p>
            </div>
          )}

          {!loading && (
            <div className="result-list">
              {sorted.map((flight, index) => (
                <ResultRow
                  key={flight.id}
                  flight={flight}
                  index={index}
                  passengers={passengers}
                />
              ))}
            </div>
          )}
        </section>
      </main>

      <footer className="foot">
        <span>Farlane</span>
        <span>Fare comparison prototype</span>
      </footer>
    </div>
  );
}
