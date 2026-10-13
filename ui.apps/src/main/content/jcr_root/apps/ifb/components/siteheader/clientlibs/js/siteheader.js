/* Site Header (ifb/components/siteheader): mobile menu toggle and active link highlighting */
(function () {
    "use strict";

    function normalize(path) {
        return path.replace(/\.html$/, "").replace(/\/$/, "");
    }

    function init(header) {
        var toggle = header.querySelector("[data-ifb-header-toggle]");
        var currentPath = normalize(window.location.pathname);

        if (toggle) {
            toggle.addEventListener("click", function () {
                var open = header.classList.toggle("ifb-header--open");
                toggle.setAttribute("aria-expanded", String(open));
            });
        }

        header.querySelectorAll(".ifb-header__link").forEach(function (link) {
            if (normalize(link.pathname) === currentPath) {
                link.classList.add("ifb-header__link--active");
                link.setAttribute("aria-current", "page");
            }
        });
    }

    function onReady() {
        document.querySelectorAll("[data-ifb-header]").forEach(init);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", onReady);
    } else {
        onReady();
    }
})();
