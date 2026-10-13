package com.ifb.aem.core.core.models;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * One entry of the Team Members multifield (stored as members/item0, members/item1, ...).
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TeamMember {

    @ValueMapValue
    private String name;

    @ValueMapValue
    private String role;

    @ValueMapValue
    private String photo;

    @ValueMapValue
    private String bio;

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getPhoto() {
        return photo;
    }

    public String getBio() {
        return bio;
    }

    /** Up to two initials ("Priya Nair" -> "PN"), shown when no photo is selected. */
    public String getInitials() {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        return Arrays.stream(name.trim().split("\\s+"))
                .limit(2)
                .map(part -> part.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }
}
