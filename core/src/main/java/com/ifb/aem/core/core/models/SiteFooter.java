package com.ifb.aem.core.core.models;

import java.time.Year;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Model behind the ifb/components/sitefooter component (used in the "IFB Footer" experience fragment).
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SiteFooter {

    @ValueMapValue
    private String brandName;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String linksHeading;

    @ValueMapValue
    private String copyright;

    @ChildResource
    private List<NavLink> links;

    public String getBrandName() {
        return brandName;
    }

    public String getDescription() {
        return description;
    }

    public String getLinksHeading() {
        return linksHeading;
    }

    /** Copyright line; the token {year} is replaced with the current year so it never goes stale. */
    public String getCopyright() {
        return StringUtils.replace(copyright, "{year}", String.valueOf(Year.now().getValue()));
    }

    public List<NavLink> getLinks() {
        return links == null ? Collections.emptyList() : links;
    }

    public boolean isEmpty() {
        return StringUtils.isAllBlank(brandName, description, copyright) && getLinks().isEmpty();
    }
}
