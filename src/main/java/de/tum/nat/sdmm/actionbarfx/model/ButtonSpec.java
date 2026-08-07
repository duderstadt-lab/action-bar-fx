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

/** One colored button. */
public class ButtonSpec extends BarItem {

	private String label = "";
	private Integer step;
	private Integer paletteIndex;
	private String color;
	private String icon;
	private String tooltip;
	private ActionSpec action;

	@Override
	public String getType() {
		return BUTTON;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	/**
	 * Protocol step number rendered as a badge. Buttons without a step still
	 * reserve the badge slot so labels stay left-aligned across the bar.
	 */
	public Integer getStep() {
		return step;
	}

	public void setStep(final Integer step) {
		this.step = step;
	}

	/** Index into the bar palette. The source of truth for the button color. */
	public Integer getPaletteIndex() {
		return paletteIndex;
	}

	public void setPaletteIndex(final Integer paletteIndex) {
		this.paletteIndex = paletteIndex;
	}

	/**
	 * Optional hex override. When set, switching the bar palette leaves this
	 * button alone.
	 */
	public String getColor() {
		return color;
	}

	public void setColor(final String color) {
		this.color = color;
	}

	/** Ikonli icon literal, for example {@code mdi2t-tag-outline}. */
	public String getIcon() {
		return icon;
	}

	public void setIcon(final String icon) {
		this.icon = icon;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(final String tooltip) {
		this.tooltip = tooltip;
	}

	public ActionSpec getAction() {
		return action;
	}

	public void setAction(final ActionSpec action) {
		this.action = action;
	}

	@Override
	public ButtonSpec copy() {
		final ButtonSpec copy = new ButtonSpec();
		copy.label = label;
		copy.step = step;
		copy.paletteIndex = paletteIndex;
		copy.color = color;
		copy.icon = icon;
		copy.tooltip = tooltip;
		copy.action = action == null ? null : action.copy();
		copy.getExtras().putAll(getExtras());
		return copy;
	}

	@Override
	public String toString() {
		final String text = label == null || label.isEmpty() ? "(no label)" : label;
		return step == null ? text : step + "  " + text;
	}
}
