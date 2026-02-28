package com.example.pijava.ui.component;

/**
 * Base interface for all reusable TUI components.
 *
 * <p>Each component knows how to render itself onto a {@link RenderContext}.
 * Components are stateless renderers — any mutable state they need (e.g.
 * messages, input buffer) is passed in via constructor or setter before
 * {@code render} is called.</p>
 *
 * <p>By programming to this interface, screens can compose an arbitrary set
 * of components and render them in sequence without knowing the details.</p>
 */
public interface Component {

    /**
     * Draw this component onto the given render context.
     *
     * @param ctx the shared rendering context (graphics surface + terminal size)
     */
    void render(RenderContext ctx);
}
