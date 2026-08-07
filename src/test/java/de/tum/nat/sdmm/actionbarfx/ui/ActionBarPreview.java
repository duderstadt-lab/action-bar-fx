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

import de.tum.nat.sdmm.actionbarfx.io.BarIO;
import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.model.Palette;
import de.tum.nat.sdmm.actionbarfx.model.PaletteRegistry;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Standalone launcher for working on the look of a bar, with no Fiji involved.
 * <p>
 * CSSFX watches the stylesheets on disk, so editing {@code base.css},
 * {@code light.css} or {@code dark.css} restyles the running window. It only
 * picks up the copies under {@code target/classes}, so run this from an IDE that
 * compiles resources on save, or re-run {@code mvn process-resources}.
 * <p>
 * Buttons here are inert: no SciJava context means nothing to run.
 *
 * <pre>
 * mvn -q test-compile
 * mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=de.tum.nat.sdmm.actionbarfx.ui.ActionBarPreview \
 *     -Dexec.args=ActionBar/fret-power-tools
 * </pre>
 */
public class ActionBarPreview extends Application {

	private static final String DEFAULT_BAR = "ActionBar/fret-power-tools";

	@Override
	public void start(final Stage stage) throws Exception {
		CSSFX.start();

		final String path = getParameters().getRaw().isEmpty() ? DEFAULT_BAR
			: getParameters().getRaw().get(0);
		final File barDir = new File(path);
		final BarConfig config = BarIO.load(barDir);

		final ActionBarPane pane = new ActionBarPane(barDir, config, null);

		final ComboBox<Palette> palettes = new ComboBox<>();
		palettes.getItems().addAll(PaletteRegistry.all());
		palettes.setValue(PaletteRegistry.get(config.getPalette()));
		palettes.valueProperty().addListener((obs, old, palette) -> {
			if (palette != null) pane.setPalette(palette.getId());
		});

		final Button theme = new Button("Toggle theme");
		theme.setOnAction(e -> ThemeManager.toggle());

		final Button toast = new Button("Test toast");
		toast.setOnAction(e -> pane.message("Command not found: SomeCommand", true));

		final HBox controls = new HBox(8, new Label("Palette"), palettes, theme,
			toast);
		controls.setAlignment(Pos.CENTER_LEFT);
		controls.setPadding(new Insets(10));

		final BorderPane root = new BorderPane(pane);
		root.setBottom(controls);

		final Scene scene = new Scene(root, config.getWidth() + 40, 720);
		ThemeManager.apply(scene);

		stage.setTitle(config.getTitle() + " — preview");
		stage.setScene(scene);
		stage.show();
	}

	public static void main(final String[] args) {
		launch(args);
	}
}
