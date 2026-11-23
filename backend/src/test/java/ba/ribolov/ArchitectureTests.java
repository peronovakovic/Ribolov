package ba.ribolov;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTests {
    @Test
    void allModulesAreDiscoveredAndTheirBoundariesAreRespected() {
        var modules = ApplicationModules.of(RibolovApplication.class);
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder("access", "people", "membership", "waters", "billing",
                        "permits", "fieldwork", "reporting", "documents", "notifications", "audit");
        modules.verify();
    }
}
