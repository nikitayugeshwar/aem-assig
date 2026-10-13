package com.ifb.aem.core.core.models;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.ifb.aem.core.core.services.WeatherReport;
import com.ifb.aem.core.core.services.WeatherService;

/**
 * Model behind the ifb/components/cityweather component: looks up the current weather of the city
 * entered in the dialog. Temperatures are rounded for display; the clientlib converts them to Fahrenheit.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CityWeather {

    private static final Map<String, String> DAY_SYMBOLS = new HashMap<>();
    private static final Map<String, String> NIGHT_SYMBOLS = new HashMap<>();

    static {
        DAY_SYMBOLS.put(WeatherReport.CLEAR, "☀️");
        DAY_SYMBOLS.put(WeatherReport.CLOUDS, "⛅");
        DAY_SYMBOLS.put(WeatherReport.FOG, "🌫️");
        DAY_SYMBOLS.put(WeatherReport.DRIZZLE, "🌦️");
        DAY_SYMBOLS.put(WeatherReport.RAIN, "🌧️");
        DAY_SYMBOLS.put(WeatherReport.SNOW, "❄️");
        DAY_SYMBOLS.put(WeatherReport.STORM, "⛈️");
        NIGHT_SYMBOLS.putAll(DAY_SYMBOLS);
        NIGHT_SYMBOLS.put(WeatherReport.CLEAR, "🌙");
        NIGHT_SYMBOLS.put(WeatherReport.CLOUDS, "☁️");
    }

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String city;

    @OSGiService
    private WeatherService weatherService;

    private WeatherReport report;

    @PostConstruct
    protected void init() {
        if (StringUtils.isNotBlank(city) && weatherService != null) {
            report = weatherService.getCurrentWeather(city).orElse(null);
        }
    }

    public String getHeading() {
        return heading;
    }

    public String getCity() {
        return StringUtils.trimToEmpty(city);
    }

    /** No city entered yet: the component shows the author placeholder. */
    public boolean isEmpty() {
        return StringUtils.isBlank(city);
    }

    public boolean isAvailable() {
        return report != null;
    }

    public WeatherReport getReport() {
        return report;
    }

    /** "Pune, IN" as resolved by the provider, which may differ from the authored spelling. */
    public String getLocation() {
        if (report == null) {
            return getCity();
        }
        return StringUtils.isEmpty(report.getCountry()) ? report.getCity()
                : report.getCity() + ", " + report.getCountry();
    }

    public long getTemperature() {
        return report != null ? Math.round(report.getTemperature()) : 0;
    }

    public long getFeelsLike() {
        return report != null ? Math.round(report.getFeelsLike()) : 0;
    }

    public long getWindSpeed() {
        return report != null ? Math.round(report.getWindSpeed()) : 0;
    }

    public String getSymbol() {
        if (report == null) {
            return "";
        }
        return (report.isDay() ? DAY_SYMBOLS : NIGHT_SYMBOLS).getOrDefault(report.getCondition(), "");
    }
}
