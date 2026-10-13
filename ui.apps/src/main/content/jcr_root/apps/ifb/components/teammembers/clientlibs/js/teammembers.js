/* Team Members (ifb/components/teammembers): role filter + flip cards */
(function () {
    "use strict";

    var SELECTOR = "[data-ifb-team]";
    var INITIALIZED = "ifbTeamInit";

    function updateCount(team, visible, total) {
        var count = team.querySelector("[data-ifb-team-count]");
        if (count) {
            count.textContent = "Showing " + visible + " of " + total + " members";
        }
    }

    function applyFilter(team, role) {
        var members = team.querySelectorAll("[data-ifb-team-role]");
        var visible = 0;

        members.forEach(function (member) {
            var show = !role || member.getAttribute("data-ifb-team-role") === role;
            member.hidden = !show;
            if (show) {
                visible++;
            }
        });

        team.querySelectorAll("[data-ifb-team-filter]").forEach(function (button) {
            var active = button.getAttribute("data-ifb-team-filter") === role;
            button.classList.toggle("ifb-team__filter--active", active);
            button.setAttribute("aria-pressed", String(active));
        });

        updateCount(team, visible, members.length);
    }

    function flip(card) {
        var flipped = card.classList.toggle("ifb-team__card--flipped");
        var front = card.querySelector(".ifb-team__face--front");
        var back = card.querySelector(".ifb-team__face--back");

        // Only the visible face is reachable by keyboard and screen readers
        front.setAttribute("aria-hidden", String(flipped));
        back.setAttribute("aria-hidden", String(!flipped));
        front.querySelector("[data-ifb-team-flip]").setAttribute("aria-expanded", String(flipped));
        front.querySelectorAll("button").forEach(function (b) { b.tabIndex = flipped ? -1 : 0; });
        back.querySelectorAll("button").forEach(function (b) { b.tabIndex = flipped ? 0 : -1; });

        (flipped ? back : front).querySelector("[data-ifb-team-flip]").focus();
    }

    function init(team) {
        if (team.dataset[INITIALIZED]) {
            return;
        }
        team.dataset[INITIALIZED] = "true";

        // One delegated listener per component instance
        team.addEventListener("click", function (event) {
            var filter = event.target.closest("[data-ifb-team-filter]");
            if (filter) {
                applyFilter(team, filter.getAttribute("data-ifb-team-filter"));
                return;
            }
            var flipButton = event.target.closest("[data-ifb-team-flip]");
            if (flipButton) {
                flip(flipButton.closest("[data-ifb-team-card]"));
            }
        });
    }

    function initAll(root) {
        root.querySelectorAll(SELECTOR).forEach(init);
    }

    function onReady() {
        initAll(document);

        // In the page editor, a component is re-rendered after its dialog is saved:
        // watch for newly added markup and initialise it too.
        new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                mutation.addedNodes.forEach(function (node) {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        if (node.matches(SELECTOR)) {
                            init(node);
                        }
                        initAll(node);
                    }
                });
            });
        }).observe(document.body, { childList: true, subtree: true });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", onReady);
    } else {
        onReady();
    }
})();
