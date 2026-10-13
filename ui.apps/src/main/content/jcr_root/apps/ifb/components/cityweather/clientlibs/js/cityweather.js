/* City Weather (ifb/components/cityweather): C/F toggle, "updated" time and Refresh via the .weather.json servlet */
(function () {
    "use strict";

    var SELECTOR = "[data-ifb-weather]";
    var INITIALIZED = "ifbWeatherInit";
    var UNIT_KEY = "ifb.weather.unit";
    var CONDITIONS = ["clear", "clouds", "fog", "drizzle", "rain", "snow", "storm", "unavailable"];
    var SYMBOLS = {
        day: { clear: "☀️", clouds: "⛅", fog: "🌫️", drizzle: "🌦️",
            rain: "🌧️", snow: "❄️", storm: "⛈️" },
        night: { clear: "🌙", clouds: "☁️" }
    };

    function readUnit() {
        try {
            return window.localStorage.getItem(UNIT_KEY) === "F" ? "F" : "C";
        } catch (e) {
            return "C";
        }
    }

    function saveUnit(unit) {
        try {
            window.localStorage.setItem(UNIT_KEY, unit);
        } catch (e) {
            // Storage blocked: the choice is just not remembered.
        }
    }

    function applyUnit(widget, unit) {
        widget.querySelectorAll("[data-ifb-weather-temp]").forEach(function (el) {
            var celsius = parseFloat(el.getAttribute("data-ifb-weather-temp"));
            if (!isNaN(celsius)) {
                el.textContent = Math.round(unit === "F" ? celsius * 9 / 5 + 32 : celsius);
            }
        });
        widget.querySelectorAll("[data-ifb-weather-deg]").forEach(function (el) {
            el.textContent = "°" + unit;
        });
        widget.querySelectorAll("[data-ifb-weather-unit]").forEach(function (button) {
            var active = button.getAttribute("data-ifb-weather-unit") === unit;
            button.classList.toggle("ifb-weather__unit--active", active);
            button.setAttribute("aria-pressed", String(active));
        });
    }

    function showUpdated(widget) {
        var fetched = parseInt(widget.getAttribute("data-ifb-weather-fetched"), 10);
        var label = widget.querySelector("[data-ifb-weather-updated]");
        if (!label || isNaN(fetched)) {
            return;
        }
        var minutes = Math.max(0, Math.round((Date.now() - fetched) / 60000));
        label.textContent = minutes < 1 ? "Updated just now"
            : "Updated " + minutes + " min" + (minutes === 1 ? "" : "s") + " ago";
    }

    function setField(widget, name, value) {
        var el = widget.querySelector("[data-ifb-weather-field='" + name + "']");
        if (el) {
            el.textContent = value;
        }
    }

    function render(widget, data) {
        var symbols = data.day ? SYMBOLS.day : SYMBOLS.night;
        CONDITIONS.forEach(function (condition) {
            widget.classList.remove("ifb-weather--" + condition);
        });
        widget.classList.add("ifb-weather--" + data.condition);
        widget.classList.toggle("ifb-weather--night", !data.day);

        setField(widget, "symbol", symbols[data.condition] || SYMBOLS.day[data.condition] || "");
        setField(widget, "location", data.country ? data.city + ", " + data.country : data.city);
        setField(widget, "description", data.description);
        setField(widget, "humidity", data.humidity);
        setField(widget, "wind", Math.round(data.windSpeed));
        setField(widget, "provider", data.provider);

        var temps = widget.querySelectorAll("[data-ifb-weather-temp]");
        if (temps[0]) {
            temps[0].setAttribute("data-ifb-weather-temp", data.temperature);
        }
        if (temps[1]) {
            temps[1].setAttribute("data-ifb-weather-temp", data.feelsLike);
        }
        widget.setAttribute("data-ifb-weather-fetched", data.fetchedAt);
        applyUnit(widget, readUnit());
        showUpdated(widget);
    }

    function refresh(widget) {
        var button = widget.querySelector("[data-ifb-weather-refresh]");
        var hasBody = !!widget.querySelector(".ifb-weather__body");

        widget.classList.add("ifb-weather--loading");
        button.disabled = true;
        button.textContent = "Refreshing…";

        fetch(widget.getAttribute("data-ifb-weather"), { credentials: "same-origin" })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }
                return response.json();
            })
            .then(function (data) {
                if (hasBody) {
                    render(widget, data);
                } else {
                    // The page was rendered while the weather was unavailable; reload to get the full markup.
                    window.location.reload();
                }
            })
            .catch(function () {
                var label = widget.querySelector("[data-ifb-weather-updated]");
                if (label) {
                    label.textContent = "Could not refresh the weather. Please try again.";
                }
            })
            .then(function () {
                widget.classList.remove("ifb-weather--loading");
                button.disabled = false;
                button.textContent = "Refresh";
            });
    }

    function init(widget) {
        if (widget.dataset[INITIALIZED]) {
            return;
        }
        widget.dataset[INITIALIZED] = "true";

        applyUnit(widget, readUnit());
        showUpdated(widget);
        window.setInterval(function () {
            showUpdated(widget);
        }, 60000);

        widget.querySelectorAll("[data-ifb-weather-unit]").forEach(function (button) {
            button.addEventListener("click", function () {
                var unit = button.getAttribute("data-ifb-weather-unit");
                saveUnit(unit);
                document.querySelectorAll(SELECTOR).forEach(function (other) {
                    applyUnit(other, unit);
                });
            });
        });

        widget.querySelector("[data-ifb-weather-refresh]").addEventListener("click", function () {
            refresh(widget);
        });
    }

    function initAll() {
        document.querySelectorAll(SELECTOR).forEach(init);
    }

    if (document.readyState !== "loading") {
        initAll();
    } else {
        document.addEventListener("DOMContentLoaded", initAll);
    }

    // Re-initialise when the component is re-rendered in the page editor after a dialog edit.
    if (window.MutationObserver) {
        new MutationObserver(initAll).observe(document.documentElement, { childList: true, subtree: true });
    }
}());
