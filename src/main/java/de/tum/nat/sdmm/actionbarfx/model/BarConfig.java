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

/** The contents of a {@code bar.json}. */
public class BarConfig extends Extensible {

	/** Format understood by this build. */
	public static final int FORMAT_VERSION = 1;

	private int formatVersion = FORMAT_VERSION;
	private String title = "Action bar";
	private String palette = PaletteRegistry.DEFAULT_PALETTE;
	private double width = 310;
	private List<BarItem> items = new ArrayList<>();

	public int getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(final int formatVersion) {
		this.formatVersion = formatVersion;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	/** Id of the palette in {@code palettes.json}. */
	public String getPalette() {
		return palette;
	}

	public void setPalette(final String palette) {
		this.palette = palette;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(final double width) {
		this.width = width;
	}

	public List<BarItem> getItems() {
		return items;
	}

	public void setItems(final List<BarItem> items) {
		this.items = items == null ? new ArrayList<>() : items;
	}

	public BarConfig copy() {
		final BarConfig copy = new BarConfig();
		copy.formatVersion = formatVersion;
		copy.title = title;
		copy.palette = palette;
		copy.width = width;
		for (final BarItem item : items)
			copy.items.add(item.copy());
		copy.getExtras().putAll(getExtras());
		return copy;
	}
}
