package com.ifb.aem.core.core.services.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ifb.aem.core.core.services.WeatherReport;
import com.ifb.aem.core.core.services.WeatherService;

/**
 * Fetches current weather from OpenWeatherMap (https://openweathermap.org/current) when an API key is
 * configured. Without a key it falls back to the keyless Open-Meteo API, so the component also works on
 * a fresh local SDK. Results are cached in memory per city to stay well inside the providers' rate limits.
 */
@Component(service = WeatherService.class)
@Designate(ocd = WeatherServiceImpl.Config.class)
public class WeatherServiceImpl implements WeatherService {

    @ObjectClassDefinition(name = "IFB - Weather Service",
            description = "Current weather lookup used by the City Weather component")
    public @interface Config {

        @AttributeDefinition(name = "OpenWeatherMap API key",
                description = "Key from https://home.openweathermap.org/api_keys. On AEM as a Cloud Service, "
                        + "set it as the secret environment variable OPENWEATHER_API_KEY.",
                type = AttributeType.PASSWORD)
        String apiKey() default "";

        @AttributeDefinition(name = "Use Open-Meteo without API key",
                description = "Fall back to the keyless Open-Meteo API when no OpenWeatherMap key is set")
        boolean fallbackEnabled() default true;

        @AttributeDefinition(name = "Cache duration (minutes)")
        int cacheMinutes() default 10;

        @AttributeDefinition(name = "HTTP timeout (milliseconds)")
        int timeoutMillis() default 4000;
    }

    static final String OPENWEATHERMAP = "OpenWeatherMap";
    static final String OPEN_METEO = "Open-Meteo";

    private static final Logger LOG = LoggerFactory.getLogger(WeatherServiceImpl.class);
    private static final int MAX_CACHE_ENTRIES = 500;
    private static final String OWM_URL = "https://api.openweathermap.org/data/2.5/weather?units=metric&q=%s&appid=%s";
    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&format=json&name=%s";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s"
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day";

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private HttpClient httpClient;
    private String apiKey;
    private boolean fallbackEnabled;
    private long cacheMillis;
    private Duration timeout;

    @Activate
    @Modified
    protected void activate(Config config) {
        apiKey = StringUtils.trimToEmpty(config.apiKey());
        fallbackEnabled = config.fallbackEnabled();
        cacheMillis = Math.max(0, config.cacheMinutes()) * 60_000L;
        timeout = Duration.ofMillis(Math.max(500, config.timeoutMillis()));
        httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        cache.clear();
        LOG.info("Weather service active, provider: {}", StringUtils.isNotEmpty(apiKey) ? OPENWEATHERMAP
                : fallbackEnabled ? OPEN_METEO : "none (no OpenWeatherMap API key)");
    }

    @Override
    public Optional<WeatherReport> getCurrentWeather(String city) {
        String query = StringUtils.normalizeSpace(city);
        if (StringUtils.isEmpty(query)) {
            return Optional.empty();
        }
        String key = query.toLowerCase(Locale.ROOT);
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.isFresh(now(), cacheMillis)) {
            return Optional.of(cached.report);
        }
        try {
            WeatherReport report = StringUtils.isNotEmpty(apiKey) ? fromOpenWeatherMap(query)
                    : fallbackEnabled ? fromOpenMeteo(query) : null;
            if (report != null) {
                if (cache.size() >= MAX_CACHE_ENTRIES) {
                    cache.clear();
                }
                cache.put(key, new CacheEntry(report));
            }
            return Optional.ofNullable(report);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not fetch the weather for '{}': {}", query, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Better slightly stale than nothing when the provider has a hiccup.
        return cached != null ? Optional.of(cached.report) : Optional.empty();
    }

    private WeatherReport fromOpenWeatherMap(String city) throws IOException, InterruptedException {
        JsonObject json = getJson(String.format(OWM_URL, encode(city), encode(apiKey)));
        if (json == null) {
            return null;
        }
        JsonObject main = json.getAsJsonObject("main");
        JsonObject weather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
        String icon = weather.get("icon").getAsString();
        return new WeatherReport(
                json.get("name").getAsString(),
                json.getAsJsonObject("sys").get("country").getAsString(),
                main.get("temp").getAsDouble(),
                main.get("feels_like").getAsDouble(),
                main.get("humidity").getAsInt(),
                json.getAsJsonObject("wind").get("speed").getAsDouble() * 3.6,
                StringUtils.capitalize(weather.get("description").getAsString()),
                owmCondition(weather.get("main").getAsString()),
                !icon.endsWith("n"),
                OPENWEATHERMAP,
                now());
    }

    private WeatherReport fromOpenMeteo(String city) throws IOException, InterruptedException {
        // Open-Meteo geocoding only understands the bare city name, so drop a ",CountryCode" suffix.
        JsonObject geo = getJson(String.format(GEOCODING_URL, encode(StringUtils.substringBefore(city, ",").trim())));
        JsonArray results = geo != null ? geo.getAsJsonArray("results") : null;
        if (results == null || results.size() == 0) {
            return null;
        }
        JsonObject place = results.get(0).getAsJsonObject();
        JsonObject forecast = getJson(String.format(FORECAST_URL,
                place.get("latitude").getAsString(), place.get("longitude").getAsString()));
        if (forecast == null) {
            return null;
        }
        JsonObject current = forecast.getAsJsonObject("current");
        int code = current.get("weather_code").getAsInt();
        return new WeatherReport(
                place.get("name").getAsString(),
                place.has("country_code") ? place.get("country_code").getAsString() : "",
                current.get("temperature_2m").getAsDouble(),
                current.get("apparent_temperature").getAsDouble(),
                current.get("relative_humidity_2m").getAsInt(),
                current.get("wind_speed_10m").getAsDouble(),
                wmoDescription(code),
                wmoCondition(code),
                current.get("is_day").getAsInt() == 1,
                OPEN_METEO,
                now());
    }

    /** @return the parsed body, or null when the provider answers 404 (unknown city) */
    protected JsonObject getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + request.uri().getHost());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    protected long now() {
        return System.currentTimeMillis();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Maps OpenWeatherMap's "main" group (https://openweathermap.org/weather-conditions). */
    static String owmCondition(String main) {
        switch (main) {
            case "Clear":
                return WeatherReport.CLEAR;
            case "Clouds":
                return WeatherReport.CLOUDS;
            case "Drizzle":
                return WeatherReport.DRIZZLE;
            case "Rain":
                return WeatherReport.RAIN;
            case "Snow":
                return WeatherReport.SNOW;
            case "Thunderstorm":
            case "Squall":
            case "Tornado":
                return WeatherReport.STORM;
            default: // Mist, Fog, Haze, Smoke, Dust, Sand, Ash
                return WeatherReport.FOG;
        }
    }

    /** Maps WMO weather interpretation codes used by Open-Meteo. */
    static String wmoCondition(int code) {
        if (code <= 1) {
            return WeatherReport.CLEAR;
        } else if (code <= 3) {
            return WeatherReport.CLOUDS;
        } else if (code <= 48) {
            return WeatherReport.FOG;
        } else if (code <= 57) {
            return WeatherReport.DRIZZLE;
        } else if (code <= 67 || (code >= 80 && code <= 82)) {
            return WeatherReport.RAIN;
        } else if (code <= 86) {
            return WeatherReport.SNOW;
        }
        return WeatherReport.STORM;
    }

    static String wmoDescription(int code) {
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: return "Light rain";
            case 63: return "Moderate rain";
            case 65: return "Heavy rain";
            case 66: case 67: return "Freezing rain";
            case 71: return "Light snow";
            case 73: return "Moderate snow";
            case 75: return "Heavy snow";
            case 77: return "Snow grains";
            case 80: case 81: case 82: return "Rain showers";
            case 85: case 86: return "Snow showers";
            case 95: return "Thunderstorm";
            case 96: case 99: return "Thunderstorm with hail";
            default: return "Unknown";
        }
    }

    private static final class CacheEntry {
        private final WeatherReport report;

        private CacheEntry(WeatherReport report) {
            this.report = report;
        }

        private boolean isFresh(long now, long maxAgeMillis) {
            return now - report.getFetchedAt() < maxAgeMillis;
        }
    }
}
