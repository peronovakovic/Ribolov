package ba.ribolov.organizations.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "organization", schema = "organizations")
public class Organization {
    @Id
    private UUID id;
    @Version
    private Long version;
    @Column(nullable = false, length = 240)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private OrganizationType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationStatus status;
    @Column(name = "contact_email", length = 254)
    private String contactEmail;
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;
    @OneToMany(mappedBy = "child", fetch = FetchType.LAZY)
    private List<OrganizationAffiliation> parentAffiliations = new ArrayList<>();
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<OrganizationAffiliation> childAffiliations = new ArrayList<>();

    protected Organization() {}

    public Organization(String name, OrganizationType type, String contactEmail, String contactPhone) {
        this.id = UUID.randomUUID();
        this.type = Objects.requireNonNull(type, "Organization type is required");
        this.status = OrganizationStatus.ACTIVE;
        updateContact(name, contactEmail, contactPhone);
    }

    public void updateContact(String name, String contactEmail, String contactPhone) {
        if (name == null || name.isBlank() || name.strip().length() > 240)
            throw new IllegalArgumentException("Organization name must contain 1 to 240 characters");
        this.name = name.strip();
        this.contactEmail = optionalText(contactEmail, 254);
        this.contactPhone = optionalText(contactPhone, 50);
    }

    private static String optionalText(String text, int maxLength) {
        if (text == null || text.isBlank()) return null;
        if (text.strip().length() > maxLength) throw new IllegalArgumentException("Contact value is too long");
        return text.strip();
    }

    void addParentAffiliation(OrganizationAffiliation affiliation) { parentAffiliations.add(affiliation); }
    void addChildAffiliation(OrganizationAffiliation affiliation) { childAffiliations.add(affiliation); }

    /** Application use cases must check open obligations before invoking this transition. */
    public void archive() { status = OrganizationStatus.ARCHIVED; }
    public boolean isActive() { return status == OrganizationStatus.ACTIVE; }
    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public OrganizationType getType() { return type; }
    public OrganizationStatus getStatus() { return status; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public List<OrganizationAffiliation> getParentAffiliations() { return Collections.unmodifiableList(parentAffiliations); }
    public List<OrganizationAffiliation> getChildAffiliations() { return Collections.unmodifiableList(childAffiliations); }
}
