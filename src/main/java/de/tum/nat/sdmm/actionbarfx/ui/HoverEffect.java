/*-
 * #%L
 * JavaFX action bars for Fiji. Vertical bars of colored buttons that launch ImageJ commands and scripts.
 * %%
 * Copyright (C) 2026 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.tum.nat.sdmm.actionbarfx.ui;

import javafx.scene.Node;

/**
 * The hover animation for action buttons.
 * <p>
 * On the JavaFX baseline Fiji ships (23) this is nothing but a style class:
 * CSS transitions exist, so the scale animation lives in {@code base.css}.
 * Background and border interpolation only arrived in JavaFX 24, so the color
 * change on hover snaps rather than fades — which is arguably better for a
 * button clicked hundreds of times a day.
 * <p>
 * It stays a helper so that when Fiji moves to a newer JavaFX and richer
 * transitions become worthwhile, there is one place to change.
 */
public final class HoverEffect {

	public static final String STYLE_CLASS = "hover-lift";

	private HoverEffect() {}

	public static void install(final Node node) {
		if (!node.getStyleClass().contains(STYLE_CLASS)) //
			node.getStyleClass().add(STYLE_CLASS);
	}

	public static void uninstall(final Node node) {
		node.getStyleClass().remove(STYLE_CLASS);
	}
}
