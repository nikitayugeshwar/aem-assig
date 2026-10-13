package com.ifb.aem.core.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.ifb.aem.core.core.services.WeatherReport;
import com.ifb.aem.core.core.services.WeatherService;
import com.ifb.aem.core.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class CityWeatherTest {

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        WeatherService service = city -> "Pune".equals(city)
                ? Optional.of(new WeatherReport("Pune", "IN", 27.6, 29.4, 70, 9.2, "Light rain",
                        WeatherReport.CLEAR, false, "Open-Meteo", 1L))
                : Optional.empty();
        context.registerService(WeatherService.class, service);
    }

    private CityWeather adapt(Object... properties) {
        return context.create().resource("/content/page/cityweather", properties).adaptTo(CityWeather.class);
    }

    @Test
    void showsWeatherOfAuthoredCity() {
        CityWeather weather = adapt("city", "Pune", "heading", "Weather in Pune");

        assertFalse(weather.isEmpty());
        assertTrue(weather.isAvailable());
        assertEquals("Weather in Pune", weather.getHeading());
        assertEquals("Pune, IN", weather.getLocation());
        assertEquals(28, weather.getTemperature());
        assertEquals(29, weather.getFeelsLike());
        assertEquals(9, weather.getWindSpeed());
        assertEquals("🌙", weather.getSymbol()); // clear sky at night
    }

    @Test
    void unknownCityIsNotAvailable() {
        CityWeather weather = adapt("city", "Atlantis");

        assertFalse(weather.isEmpty());
        assertFalse(weather.isAvailable());
        assertEquals("Atlantis", weather.getLocation());
        assertEquals("", weather.getSymbol());
    }

    @Test
    void emptyWithoutCity() {
        CityWeather weather = adapt("heading", "Weather");

        assertTrue(weather.isEmpty());
        assertFalse(weather.isAvailable());
    }
}
