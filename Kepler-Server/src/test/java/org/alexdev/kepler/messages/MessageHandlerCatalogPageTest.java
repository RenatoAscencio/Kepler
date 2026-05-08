package org.alexdev.kepler.messages;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the catalog page name parsing used to enrich Sentry breadcrumbs
 * for GCAP (Get CAtalogue Page) game-handler invocations. The breadcrumb's
 * `catalog_page` tag is the load-bearing diagnostic for "user opened X then
 * the projector died" — if this parsing regresses we lose that signal.
 */
public class MessageHandlerCatalogPageTest {

    @Test
    void returnsNullForNullBody() {
        assertThat(MessageHandler.extractCatalogPageName(null)).isNull();
    }

    @Test
    void returnsNullForEmptyBody() {
        assertThat(MessageHandler.extractCatalogPageName("")).isNull();
    }

    @Test
    void returnsNullForBodyWithoutSlash() {
        // Some page-open messages carry just an id with no separator —
        // we have no name to attach in that case.
        assertThat(MessageHandler.extractCatalogPageName("LegacyCustom")).isNull();
    }

    @Test
    void returnsPageNameAfterFirstSlash() {
        assertThat(MessageHandler.extractCatalogPageName("exec/Legacy Custom"))
            .isEqualTo("Legacy Custom");
    }

    @Test
    void preservesSpacesAndAccentsInPageName() {
        // Spanish catalog pages live with accented chars after the May 8
        // translation — verify those round-trip into the breadcrumb tag.
        assertThat(MessageHandler.extractCatalogPageName("exec/Salón Habbo"))
            .isEqualTo("Salón Habbo");
    }

    @Test
    void onlyTakesFirstSegmentAfterSlash() {
        // Original implementation used split("/") and parts[1] — preserve
        // that exact behavior so we don't accidentally widen the tag value.
        assertThat(MessageHandler.extractCatalogPageName("a/b/c"))
            .isEqualTo("b");
    }

    @Test
    void returnsNullForTrailingSlashOnly() {
        // "exec/" has parts.length=2 but parts[1] is empty.
        assertThat(MessageHandler.extractCatalogPageName("exec/")).isNull();
    }

    @Test
    void handlesLeadingSlash() {
        // "/Legacy Custom" → parts=[, Legacy Custom] → returns "Legacy Custom"
        assertThat(MessageHandler.extractCatalogPageName("/Legacy Custom"))
            .isEqualTo("Legacy Custom");
    }
}
