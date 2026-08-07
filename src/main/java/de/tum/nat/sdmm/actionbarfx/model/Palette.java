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

import java.util.ArrayList;
import java.util.List;

/** A coordinated set of button colors. */
public class Palette {

	/** Used when a button names neither a palette index nor a color. */
	public static final String NEUTRAL = "#9E9E9E";

	private String id;
	private String name;
	private List<String> colors = new ArrayList<>();
	private String notes;

	public Palette() {}

	public Palette(final String id, final String name,
		final List<String> colors)
	{
		this.id = id;
		this.name = name;
		this.colors = colors;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(final List<String> colors) {
		this.colors = colors == null ? new ArrayList<>() : colors;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(final String notes) {
		this.notes = notes;
	}

	public int size() {
		return colors.size();
	}

	/** Color at the given index, wrapping around for indices past the end. */
	public String color(final int index) {
		if (colors.isEmpty()) return NEUTRAL;
		return colors.get(Math.floorMod(index, colors.size()));
	}

	/**
	 * The color a button should be drawn in: the explicit override if it has
	 * one, otherwise the palette entry, otherwise a neutral gray.
	 */
	public String colorFor(final ButtonSpec button) {
		if (button.getColor() != null && !button.getColor().isEmpty()) //
			return button.getColor();
		if (button.getPaletteIndex() != null) //
			return color(button.getPaletteIndex());
		return NEUTRAL;
	}

	@Override
	public String toString() {
		return name == null ? id : name;
	}
}
