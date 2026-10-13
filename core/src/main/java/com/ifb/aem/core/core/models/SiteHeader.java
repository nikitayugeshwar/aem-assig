package com.ifb.aem.core.core.models;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Model behind the ifb/components/siteheader component (used in the "IFB Header" experience fragment).
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SiteHeader {

    @ValueMapValue
    private String brandName;

    @ValueMapValue
    private String logo;

    @ValueMapValue
    private String homeLink;

    @ChildResource
    private List<NavLink> links;

    public String getBrandName() {
        return brandName;
    }

    /** First letter of the brand name, shown as a monogram when no logo image is chosen. */
    public String getInitial() {
        return StringUtils.isBlank(brandName) ? "" : brandName.trim().substring(0, 1).toUpperCase();
    }

    public String getLogo() {
        return logo;
    }

    public String getHomeUrl() {
        return NavLink.toUrl(homeLink);
    }

    public List<NavLink> getLinks() {
        return links == null ? Collections.emptyList() : links;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(brandName) && getLinks().isEmpty();
    }
}
