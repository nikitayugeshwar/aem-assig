package com.ifb.aem.core.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.ifb.aem.core.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class TeamMembersTest {

    private final AemContext context = AppAemContext.newAemContext();

    private Resource createComponent(String... properties) {
        return context.create().resource("/content/team/teammembers", (Object[]) properties);
    }

    private void addMember(Resource component, String item, String name, String role, String photo) {
        Map<String, Object> props = new HashMap<>();
        props.put("name", name);
        props.put("role", role);
        if (photo != null) {
            props.put("photo", photo);
        }
        context.create().resource(component.getPath() + "/members/" + item, props);
    }

    @Test
    void readsMembersFromMultifield() {
        Resource component = createComponent("heading", "Meet the Team");
        addMember(component, "item0", "Priya Nair", "AEM Developer", "/content/dam/ifb/team/priya.svg");
        addMember(component, "item1", "Vikram Joshi", "QA Engineer", null);

        TeamMembers team = component.adaptTo(TeamMembers.class);

        assertEquals("Meet the Team", team.getHeading());
        assertEquals(2, team.getCount());
        assertFalse(team.isEmpty());

        TeamMember first = team.getMembers().get(0);
        assertEquals("Priya Nair", first.getName());
        assertEquals("AEM Developer", first.getRole());
        assertEquals("/content/dam/ifb/team/priya.svg", first.getPhoto());
        assertEquals("PN", first.getInitials());
    }

    @Test
    void rolesAreDistinctAndKeepAuthoringOrder() {
        Resource component = createComponent();
        addMember(component, "item0", "A", "Developer", null);
        addMember(component, "item1", "B", "Designer", null);
        addMember(component, "item2", "C", "Developer", null);

        TeamMembers team = component.adaptTo(TeamMembers.class);

        List<String> expected = Arrays.asList("Developer", "Designer");
        assertEquals(expected, team.getRoles());
        assertTrue(team.isShowFilter());
    }

    @Test
    void filterHiddenForSingleRoleOrWhenAuthorDisablesIt() {
        Resource single = createComponent();
        addMember(single, "item0", "A", "Developer", null);
        assertFalse(single.adaptTo(TeamMembers.class).isShowFilter());

        Resource disabled = context.create().resource("/content/team/disabled", "hideFilter", true);
        addMember(disabled, "item0", "A", "Developer", null);
        addMember(disabled, "item1", "B", "Designer", null);
        assertFalse(disabled.adaptTo(TeamMembers.class).isShowFilter());
    }

    @Test
    void emptyWhenNoMembersOrOnlyBlankNames() {
        Resource component = createComponent();
        assertTrue(component.adaptTo(TeamMembers.class).isEmpty());

        context.create().resource(component.getPath() + "/members/item0", "role", "Developer");
        assertTrue(component.adaptTo(TeamMembers.class).isEmpty());
    }
}
