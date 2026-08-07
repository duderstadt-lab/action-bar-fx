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
import java.io.IOException;

import org.scijava.Context;
import org.scijava.prefs.PrefService;

import de.tum.nat.sdmm.actionbarfx.ActionBarService;
import de.tum.nat.sdmm.actionbarfx.io.BarIO;
import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.model.Palette;
import de.tum.nat.sdmm.actionbarfx.model.PaletteRegistry;
import de.tum.nat.sdmm.actionbarfx.run.ActionRunner;
import de.tum.nat.sdmm.actionbarfx.ui.builder.BarBuilderDialog;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * A bar window. Must be constructed on the JavaFX thread; see
 * {@link FxBootstrap}.
 */
public class ActionBarWindow {

	private final Context context;
	private final File barDir;
	private final BarConfig config;

	private final Stage stage;
	private final ActionBarPane pane;

	private Runnable onClosed;

	/** The menu currently on screen, so a second right-click replaces it. */
	private ContextMenu contextMenu;

	public ActionBarWindow(final Context context, final File barDir,
		final BarConfig config)
	{
		this.context = context;
		this.barDir = barDir;
		this.config = config;

		final PrefService prefService = context.getService(PrefService.class);
		if (prefService != null) ThemeManager.setPrefService(prefService);

		pane = new ActionBarPane(barDir, config, new ActionRunner(context));
		pane.setOnContextMenuRequested(e -> {
			// The menu is rebuilt each time so its checkmarks reflect the current
			// theme, palette and startup setting. Whatever is already open has to
			// be dismissed first, or every right-click leaves another popup behind:
			// a popup does not auto-hide because a different popup opened.
			if (contextMenu != null) contextMenu.hide();
			contextMenu = buildContextMenu();
			contextMenu.show(pane, e.getScreenX(), e.getScreenY());
			e.consume();
		});

		final Scene scene = new Scene(pane);
		ThemeManager.apply(scene);

		stage = new Stage();
		stage.setTitle(config.getTitle());
		stage.setScene(scene);
		stage.setWidth(config.getWidth());
		stage.setOnHidden(e -> {
			if (contextMenu != null) contextMenu.hide();
			ThemeManager.forget(scene);
			if (onClosed != null) onClosed.run();
		});
	}

	public Stage getStage() {
		return stage;
	}

	public ActionBarPane getPane() {
		return pane;
	}

	public File getBarDirectory() {
		return barDir;
	}

	public void setOnClosed(final Runnable onClosed) {
		this.onClosed = onClosed;
	}

	public void show() {
		stage.show();
		stage.toFront();
	}

	public void close() {
		stage.close();
	}

	/** Re-reads {@code bar.json} and redraws. */
	public void reload() {
		try {
			final BarConfig fresh = BarIO.load(barDir);
			config.setTitle(fresh.getTitle());
			config.setPalette(fresh.getPalette());
			config.setWidth(fresh.getWidth());
			config.setItems(fresh.getItems());
			stage.setTitle(config.getTitle());
			pane.rebuild();
		}
		catch (final IOException e) {
			pane.message("Could not reload bar: " + e.getMessage(), true);
		}
	}

	private ContextMenu buildContextMenu() {
		final ContextMenu menu = new ContextMenu();

		final CheckMenuItem dark = new CheckMenuItem("Dark theme");
		dark.setSelected(ThemeManager.isDark());
		dark.setOnAction(e -> ThemeManager.setTheme(dark.isSelected()
			? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT));

		final Menu palettes = new Menu("Palette");
		final ToggleGroup group = new ToggleGroup();
		for (final Palette palette : PaletteRegistry.all()) {
			final RadioMenuItem item = new RadioMenuItem(palette.getName() + "  (" +
				palette.size() + ")");
			item.setToggleGroup(group);
			item.setSelected(palette.getId().equals(config.getPalette()));
			item.setOnAction(e -> {
				pane.setPalette(palette.getId());
				save();
			});
			palettes.getItems().add(item);
		}

		final MenuItem edit = new MenuItem("Edit bar…");
		edit.setOnAction(e -> new BarBuilderDialog(context, barDir, config, this)
			.show());

		final MenuItem reload = new MenuItem("Reload");
		reload.setOnAction(e -> reload());

		final CheckMenuItem startup = new CheckMenuItem("Open when Fiji starts");
		final ActionBarService bars = context.getService(ActionBarService.class);
		startup.setDisable(bars == null);
		if (bars != null) startup.setSelected(bars.isStartupBar(barDir));
		startup.setOnAction(e -> {
			if (bars == null) return;
			if (startup.isSelected()) bars.addStartupBar(barDir);
			else bars.removeStartupBar(barDir);
		});

		final MenuItem close = new MenuItem("Close bar");
		close.setOnAction(e -> close());

		menu.getItems().addAll(dark, palettes, new SeparatorMenuItem(), edit,
			reload, startup, new SeparatorMenuItem(), close);
		return menu;
	}

	private void save() {
		try {
			BarIO.save(config, barDir);
		}
		catch (final IOException e) {
			pane.message("Could not save bar: " + e.getMessage(), true);
		}
	}
}
