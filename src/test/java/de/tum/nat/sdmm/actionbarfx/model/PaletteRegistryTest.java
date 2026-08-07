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
package de.tum.nat.sdmm.actionbarfx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The shipped palettes, and how a button picks its color. */
public class PaletteRegistryTest {

	@Test
	public void shipsTheDocumentedPalettes() {
		final List<Palette> palettes = PaletteRegistry.all();
		assertEquals(6, palettes.size());
		assertEquals("okabe-ito", palettes.get(0).getId());

		assertEquals(8, PaletteRegistry.get("okabe-ito").size());
		assertEquals(7, PaletteRegistry.get("tol-bright").size());
		assertEquals(10, PaletteRegistry.get("tableau").size());
		assertEquals(10, PaletteRegistry.get("tol-muted").size());
		assertEquals(15, PaletteRegistry.get("tol-extended").size());
		assertEquals(11, PaletteRegistry.get("legacy-pastel").size());

		for (final Palette palette : palettes)
			for (final String color : palette.getColors())
				assertTrue(color.matches("#[0-9A-Fa-f]{6}"), palette.getId() +
					" has a malformed color: " + color);
	}

	@Test
	public void unknownPaletteFallsBackToTheDefault() {
		final Palette palette = PaletteRegistry.get("no-such-palette");
		assertNotNull(palette);
		assertEquals("okabe-ito", palette.getId());
	}

	@Test
	public void colorOverrideWinsOverPaletteIndex() {
		final Palette palette = PaletteRegistry.get("okabe-ito");

		final ButtonSpec indexed = new ButtonSpec();
		indexed.setPaletteIndex(1);
		assertEquals(palette.color(1), palette.colorFor(indexed));

		final ButtonSpec overridden = new ButtonSpec();
		overridden.setPaletteIndex(1);
		overridden.setColor("#7C89B4");
		assertEquals("#7C89B4", palette.colorFor(overridden));

		// Neither: a neutral gray rather than pretending to be the first button.
		final ButtonSpec bare = new ButtonSpec();
		assertEquals(Palette.NEUTRAL, palette.colorFor(bare));
	}

	@Test
	public void paletteIndexWrapsPastTheEnd() {
		final Palette palette = PaletteRegistry.get("tol-bright");
		assertEquals(palette.color(0), palette.color(palette.size()));
		assertFalse(palette.color(3).isEmpty());
	}
}
