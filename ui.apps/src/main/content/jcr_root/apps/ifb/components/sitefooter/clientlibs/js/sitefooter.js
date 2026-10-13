/* Site Footer (ifb/components/sitefooter): smooth "back to top" button */
(function () {
    "use strict";

    function onReady() {
        document.querySelectorAll("[data-ifb-back-to-top]").forEach(function (button) {
            button.addEventListener("click", function () {
                window.scrollTo({ top: 0, behavior: "smooth" });
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", onReady);
    } else {
        onReady();
    }
})();
