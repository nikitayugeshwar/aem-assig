package com.ifb.aem.core.core.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ifb.aem.core.core.services.WeatherReport;

class WeatherServiceImplTest {

    private static final String OWM_PUNE = "{\"name\":\"Pune\",\"sys\":{\"country\":\"IN\"},"
            + "\"main\":{\"temp\":27.6,\"feels_like\":29.1,\"humidity\":70},\"wind\":{\"speed\":2.5},"
            + "\"weather\":[{\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10n\"}]}";
    private static final String GEO_PUNE = "{\"results\":[{\"name\":\"Pune\",\"country_code\":\"IN\","
            + "\"latitude\":18.52,\"longitude\":73.86}]}";
    private static final String FORECAST = "{\"current\":{\"temperature_2m\":24.4,\"apparent_temperature\":25.0,"
            + "\"relative_humidity_2m\":61,\"wind_speed_10m\":9.7,\"weather_code\":2,\"is_day\":1}}";

    /** Serves canned JSON per URL prefix instead of calling the real APIs. */
    private static class StubWeatherService extends WeatherServiceImpl {
        private final Map<String, String> responses = new HashMap<>();
        private final List<String> calls = new ArrayList<>();
        private long now = 1_000_000L;
        private boolean failing;

        StubWeatherService respond(String urlPrefix, String json) {
            responses.put(urlPrefix, json);
            return this;
        }

        @Override
        protected JsonObject getJson(String url) throws IOException {
            calls.add(url);
            if (failing) {
                throw new IOException("provider down");
            }
            return responses.entrySet().stream()
                    .filter(entry -> url.startsWith(entry.getKey()))
                    .map(entry -> JsonParser.parseString(entry.getValue()).getAsJsonObject())
                    .findFirst()
                    .orElse(null);
        }

        @Override
        protected long now() {
            return now;
        }
    }

    private static WeatherServiceImpl.Config config(String apiKey, boolean fallback) {
        return new WeatherServiceImpl.Config() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return WeatherServiceImpl.Config.class;
            }

            @Override
            public String apiKey() {
                return apiKey;
            }

            @Override
            public boolean fallbackEnabled() {
                return fallback;
            }

            @Override
            public int cacheMinutes() {
                return 10;
            }

            @Override
            public int timeoutMillis() {
                return 1000;
            }
        };
    }

    @Test
    void usesOpenWeatherMapWhenApiKeyIsSet() {
        StubWeatherService service = new StubWeatherService().respond("https://api.openweathermap.org/", OWM_PUNE);
        service.activate(config("secret-key", true));

        WeatherReport report = service.getCurrentWeather("  Pune ").orElseThrow();

        assertEquals("Pune", report.getCity());
        assertEquals("IN", report.getCountry());
        assertEquals(27.6, report.getTemperature());
        assertEquals(70, report.getHumidity());
        assertEquals(9.0, report.getWindSpeed(), 0.001); // 2.5 m/s in km/h
        assertEquals("Light rain", report.getDescription());
        assertEquals(WeatherReport.RAIN, report.getCondition());
        assertFalse(report.isDay());
        assertEquals(WeatherServiceImpl.OPENWEATHERMAP, report.getProvider());
        assertTrue(service.calls.get(0).contains("q=Pune&appid=secret-key"));
    }

    @Test
    void fallsBackToOpenMeteoWithoutApiKey() {
        StubWeatherService service = new StubWeatherService()
                .respond("https://geocoding-api.open-meteo.com/", GEO_PUNE)
                .respond("https://api.open-meteo.com/", FORECAST);
        service.activate(config("", true));

        WeatherReport report = service.getCurrentWeather("Pune,IN").orElseThrow();

        assertEquals("Pune", report.getCity());
        assertEquals(24.4, report.getTemperature());
        assertEquals("Partly cloudy", report.getDescription());
        assertEquals(WeatherReport.CLOUDS, report.getCondition());
        assertTrue(report.isDay());
        assertEquals(WeatherServiceImpl.OPEN_METEO, report.getProvider());
        assertTrue(service.calls.get(0).endsWith("name=Pune"), "country suffix is dropped for geocoding");
    }

    @Test
    void unknownCityOrNoProviderGivesEmpty() {
        StubWeatherService service = new StubWeatherService();
        service.activate(config("", true));
        assertEquals(Optional.empty(), service.getCurrentWeather("Atlantis"));
        assertEquals(Optional.empty(), service.getCurrentWeather(" "));

        service.activate(config("", false));
        assertEquals(Optional.empty(), service.getCurrentWeather("Pune"));
        assertEquals(1, service.calls.size(), "no HTTP call without a provider");
    }

    @Test
    void cachesPerCityAndServesStaleDataWhenProviderFails() {
        StubWeatherService service = new StubWeatherService().respond("https://api.openweathermap.org/", OWM_PUNE);
        service.activate(config("key", true));

        service.getCurrentWeather("Pune");
        service.getCurrentWeather("PUNE");
        assertEquals(1, service.calls.size());

        service.now += 11 * 60_000L;
        service.failing = true;
        assertTrue(service.getCurrentWeather("pune").isPresent());
        assertEquals(2, service.calls.size());
    }

    @Test
    void mapsWmoCodes() {
        assertEquals(WeatherReport.CLEAR, WeatherServiceImpl.wmoCondition(0));
        assertEquals(WeatherReport.FOG, WeatherServiceImpl.wmoCondition(45));
        assertEquals(WeatherReport.DRIZZLE, WeatherServiceImpl.wmoCondition(53));
        assertEquals(WeatherReport.RAIN, WeatherServiceImpl.wmoCondition(81));
        assertEquals(WeatherReport.SNOW, WeatherServiceImpl.wmoCondition(73));
        assertEquals(WeatherReport.STORM, WeatherServiceImpl.wmoCondition(95));
        assertEquals("Thunderstorm with hail", WeatherServiceImpl.wmoDescription(99));
    }
}
