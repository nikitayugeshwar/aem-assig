package com.ifb.aem.core.core.servlets;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ifb.aem.core.core.services.WeatherReport;
import com.ifb.aem.core.core.services.WeatherService;

/**
 * Serves the current weather of a City Weather component as JSON, e.g.
 * GET /content/ifb/us/en/our-team/jcr:content/root/main/content/cityweather.weather.json.
 * The city comes from the component's own properties, never from the request, so the endpoint
 * cannot be used as an open proxy to the weather provider. Used by the component's Refresh button.
 */
@Component(service = { Servlet.class })
@SlingServletResourceTypes(
        resourceTypes = "ifb/components/cityweather",
        methods = HttpConstants.METHOD_GET,
        selectors = "weather",
        extensions = "json")
@ServiceDescription("IFB City Weather JSON Servlet")
public class CityWeatherServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    @Reference
    private transient WeatherService weatherService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        String city = request.getResource().getValueMap().get("city", "");
        Optional<WeatherReport> report = weatherService.getCurrentWeather(city);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "max-age=300");
        if (report.isPresent()) {
            response.getWriter().write(GSON.toJson(report.get()));
        } else {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Weather is not available for '" + city + "'");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(error.toString());
        }
    }
}
