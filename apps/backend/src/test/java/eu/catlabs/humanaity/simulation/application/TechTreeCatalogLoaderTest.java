package eu.catlabs.humanaity.simulation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.simulation.domain.TechTreeCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TechTreeCatalogLoaderTest {

    private final TechTreeCatalogLoader loader = new TechTreeCatalogLoader(new ObjectMapper());

    @Test
    void classpathTechTreeLoadsWithAllNodeTypes() {
        TechTreeCatalog catalog = loader.load(new ClassPathResource("tech-tree.json"));

        assertThat(catalog.version()).isEqualTo("v1");
        assertThat(catalog.discoveries()).isNotEmpty();
        assertThat(catalog.inventions()).isNotEmpty();
        assertThat(catalog.applications()).isNotEmpty();
        assertThat(catalog.nodesById()).containsKeys("DISC_FIRE", "INV_CAMPFIRE", "APP_COOK_FOOD");
    }

    @Test
    void invalidTechTreeFailsWithClearMessage() {
        String invalidJson = """
                {
                  "version": "v1",
                  "discoveries": [{"id": "DISC_FIRE", "prerequisites": []}],
                  "inventions": [{"id": "INV_CAMPFIRE", "prerequisites": ["MISSING_DISC"]}],
                  "applications": [{"id": "APP_COOK_FOOD", "prerequisites": ["INV_CAMPFIRE"]}]
                }
                """;

        assertThatThrownBy(() -> loader.load(new ByteArrayResource(invalidJson.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("references missing prerequisite MISSING_DISC");
    }
}
