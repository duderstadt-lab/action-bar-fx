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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/** The palettes shipped in {@code palettes.json}. */
public final class PaletteRegistry {

	public static final String DEFAULT_PALETTE = "okabe-ito";

	private static final String RESOURCE =
		"/de/tum/nat/sdmm/actionbarfx/palettes.json";

	private static Map<String, Palette> palettes;

	private PaletteRegistry() {}

	/** All palettes, in the order they appear in the resource. */
	public static synchronized List<Palette> all() {
		return new ArrayList<>(load().values());
	}

	/**
	 * The palette with the given id, or the default palette when the id is
	 * unknown. Never returns null, so an unrecognized palette name in a bar file
	 * degrades to sensible colors rather than failing to open.
	 */
	public static synchronized Palette get(final String id) {
		final Map<String, Palette> map = load();
		final Palette palette = id == null ? null : map.get(id);
		if (palette != null) return palette;
		final Palette fallback = map.get(DEFAULT_PALETTE);
		return fallback != null ? fallback : new Palette(DEFAULT_PALETTE,
			"Okabe–Ito", Collections.singletonList(Palette.NEUTRAL));
	}

	private static Map<String, Palette> load() {
		if (palettes != null) return palettes;
		final Map<String, Palette> map = new LinkedHashMap<>();
		try (final InputStream in = PaletteRegistry.class.getResourceAsStream(
			RESOURCE))
		{
			if (in == null) throw new IOException("Missing resource " + RESOURCE);
			final PaletteFile file = new ObjectMapper().readValue(in,
				PaletteFile.class);
			for (final Palette palette : file.palettes)
				map.put(palette.getId(), palette);
		}
		catch (final IOException e) {
			throw new UncheckedIOException("Could not read " + RESOURCE, e);
		}
		palettes = map;
		return palettes;
	}

	/** Wrapper matching the shape of {@code palettes.json}. */
	private static class PaletteFile {

		public List<Palette> palettes = new ArrayList<>();
	}
}
