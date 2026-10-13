package com.ifb.aem.core.core.models;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Model behind the ifb/components/teammembers component.
 * The "members" multifield items are adapted to {@link TeamMember} via @ChildResource.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TeamMembers {

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private boolean hideFilter;

    @ChildResource
    private List<TeamMember> members;

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    public List<TeamMember> getMembers() {
        if (members == null) {
            return Collections.emptyList();
        }
        return members.stream()
                .filter(member -> StringUtils.isNotBlank(member.getName()))
                .collect(Collectors.toList());
    }

    /** Distinct roles in authoring order; used to build the filter buttons. */
    public List<String> getRoles() {
        return getMembers().stream()
                .map(TeamMember::getRole)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Filter is only useful when there is more than one role to choose from. */
    public boolean isShowFilter() {
        return !hideFilter && getRoles().size() > 1;
    }

    public int getCount() {
        return getMembers().size();
    }

    public boolean isEmpty() {
        return getMembers().isEmpty();
    }
}
