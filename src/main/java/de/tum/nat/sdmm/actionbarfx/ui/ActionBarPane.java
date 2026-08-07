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

import java.io.File;

import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.model.BarItem;
import de.tum.nat.sdmm.actionbarfx.model.ButtonSpec;
import de.tum.nat.sdmm.actionbarfx.model.Palette;
import de.tum.nat.sdmm.actionbarfx.model.PaletteRegistry;
import de.tum.nat.sdmm.actionbarfx.model.SeparatorSpec;
import de.tum.nat.sdmm.actionbarfx.run.ActionRunner;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The vertical stack of buttons, scrollable, with a toast overlay for errors.
 * Used both for real bars and for the live preview in the builder.
 */
public class ActionBarPane extends StackPane {

	private final File barDir;
	private final BarConfig config;
	private final ActionRunner runner;

	private final VBox rows = new VBox();
	private final ScrollPane scroller = new ScrollPane(rows);

	/**
	 * @param barDir folder the bar lives in
	 * @param config what to render
	 * @param runner runs button actions; null gives an inert preview
	 */
	public ActionBarPane(final File barDir, final BarConfig config,
		final ActionRunner runner)
	{
		this.barDir = barDir;
		this.config = config;
		this.runner = runner;

		getStyleClass().add("action-bar");
		rows.getStyleClass().add("action-bar-rows");

		scroller.setFitToWidth(true);
		scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scroller.getStyleClass().add("action-bar-scroll");

		getChildren().add(scroller);
		rebuild();
	}

	public BarConfig getConfig() {
		return config;
	}

	public File getBarDirectory() {
		return barDir;
	}

	/** Rebuilds every row from the current config. */
	public final void rebuild() {
		final Palette palette = PaletteRegistry.get(config.getPalette());
		rows.getChildren().clear();
		for (final BarItem item : config.getItems()) {
			if (item instanceof ButtonSpec) {
				final ButtonSpec spec = (ButtonSpec) item;
				final ActionButton button = new ActionButton(spec, barDir, runner,
					this::message);
				button.setAccent(palette.colorFor(spec));
				rows.getChildren().add(button);
			}
			else if (item instanceof SeparatorSpec) {
				final Separator separator = new Separator();
				separator.getStyleClass().add("bar-separator");
				rows.getChildren().add(separator);
			}
		}
		setPrefWidth(config.getWidth());
	}

	/**
	 * Switches the bar palette. Everything without an explicit color override is
	 * recolored.
	 */
	public void setPalette(final String paletteId) {
		config.setPalette(paletteId);
		final Palette palette = PaletteRegistry.get(paletteId);
		for (final javafx.scene.Node node : rows.getChildren())
			if (node instanceof ActionButton) {
				final ActionButton button = (ActionButton) node;
				button.setAccent(palette.colorFor(button.getSpec()));
			}
	}

	/** Shows a message in the bar itself. */
	public void message(final String message, final boolean error) {
		Toast.show(this, message, error);
	}
}
