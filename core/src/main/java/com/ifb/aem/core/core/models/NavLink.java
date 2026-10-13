package com.ifb.aem.core.core.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * One item of a "links" multifield (label + link + open-in-new-tab flag).
 * Each multifield entry is stored as a child node (item0, item1, ...) and adapted to this model.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NavLink {

    @ValueMapValue
    private String label;

    @ValueMapValue
    private String link;

    @ValueMapValue
    private boolean newTab;

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return toUrl(link);
    }

    public boolean isNewTab() {
        return newTab;
    }

    /**
     * Turns an AEM page path (/content/...) into a browsable URL by adding ".html".
     * External URLs, anchors and paths that already have an extension are returned unchanged.
     */
    static String toUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return "#";
        }
        if (path.startsWith("/content/") && !path.startsWith("/content/dam/")
                && !StringUtils.containsAny(path, ".#?")) {
            return path + ".html";
        }
        return path;
    }
}
