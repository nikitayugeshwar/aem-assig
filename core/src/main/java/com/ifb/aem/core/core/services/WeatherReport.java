package com.ifb.aem.core.core.services;

/**
 * Current weather of one city, normalised across weather providers. Temperatures are in Celsius,
 * wind speed in km/h. Also serialised as-is to JSON by the City Weather servlet.
 */
public class WeatherReport {

    /** Broad condition used for the symbol and the card colour: clear, clouds, fog, drizzle, rain, snow, storm. */
    public static final String CLEAR = "clear";
    public static final String CLOUDS = "clouds";
    public static final String FOG = "fog";
    public static final String DRIZZLE = "drizzle";
    public static final String RAIN = "rain";
    public static final String SNOW = "snow";
    public static final String STORM = "storm";

    private final String city;
    private final String country;
    private final double temperature;
    private final double feelsLike;
    private final int humidity;
    private final double windSpeed;
    private final String description;
    private final String condition;
    private final boolean day;
    private final String provider;
    private final long fetchedAt;

    @SuppressWarnings("squid:S00107") // a plain value object; a builder would only add noise
    public WeatherReport(String city, String country, double temperature, double feelsLike, int humidity,
            double windSpeed, String description, String condition, boolean day, String provider, long fetchedAt) {
        this.city = city;
        this.country = country;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
        this.condition = condition;
        this.day = day;
        this.provider = provider;
        this.fetchedAt = fetchedAt;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getDescription() {
        return description;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isDay() {
        return day;
    }

    public String getProvider() {
        return provider;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }
}
