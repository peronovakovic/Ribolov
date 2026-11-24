package ba.ribolov.organizations.model;

public enum OrganizationType {
    MINISTRY, FEDERATION, ASSOCIATION;

    public boolean acceptsParent(OrganizationType parent) {
        return (this == FEDERATION && parent == MINISTRY)
                || (this == ASSOCIATION && parent == FEDERATION);
    }
}
