package com.ifb.aem.core.core.services;

import java.util.Optional;

/**
 * Looks up the current weather for a city. Used by the ifb/components/cityweather component.
 */
public interface WeatherService {

    /**
     * @param city free-text city name as entered by the author, e.g. "Pune" or "London,GB"
     * @return the current weather, or empty when the city is unknown or the weather provider is unreachable
     */
    Optional<WeatherReport> getCurrentWeather(String city);
}
