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
package de.tum.nat.sdmm.actionbarfx.ui.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import de.tum.nat.sdmm.actionbarfx.ui.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Browses the Material Design 2 icon pack as a grid.
 * <p>
 * The pack ships about 7,500 icons with no category metadata, so the categories
 * here are keyword groups over the icon names — enough to browse by eye when you
 * do not already know the name to search for. Names are shown on hover and for
 * the current selection rather than beside every icon, which would leave room
 * for only a handful per screen.
 */
public class IconPickerPane extends VBox {

	/** Icons per grid row. */
	private static final int COLUMNS = 10;

	private static final int ICON_SIZE = 22;

	private static List<String> allIcons;

	/**
	 * Category name to the keywords an icon name must contain. Order matters:
	 * this is the order of the tabs.
	 */
	private static final Map<String, List<String>> CATEGORIES = categories();

	private final TextField filterField = new TextField();
	private final ListView<List<String>> grid = new ListView<>();
	private final ObservableList<List<String>> rows = FXCollections
		.observableArrayList();
	private final Label selectionLabel = new Label();

	private String selected;
	private String category = "All";
	private Runnable onAccept;

	public IconPickerPane(final String initial) {
		selected = initial;

		setSpacing(8);
		setPadding(new Insets(10));

		filterField.setPromptText("Search icons, for example 'tag', 'chart', 'dna'");
		filterField.textProperty().addListener((obs, old, text) -> refresh());

		grid.setItems(rows);
		grid.setCellFactory(view -> new IconRowCell());
		grid.setPrefSize(COLUMNS * (ICON_SIZE + 20) + 24, 380);
		grid.setFocusTraversable(false);
		VBox.setVgrow(grid, Priority.ALWAYS);

		selectionLabel.getStyleClass().add("icon-selection");

		getChildren().addAll(filterField, buildCategoryTabs(), grid,
			selectionLabel);
		refresh();
	}

	public String getSelectedIcon() {
		return selected;
	}

	/** Modal picker. Empty when cancelled. */
	public static Optional<String> showDialog(final Window owner,
		final String initial)
	{
		final Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("Choose icon");
		if (owner != null) dialog.initOwner(owner);

		final IconPickerPane pane = new IconPickerPane(initial);
		dialog.getDialogPane().setContent(pane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
			ButtonType.CANCEL);
		ThemeManager.applyStylesheets(dialog.getDialogPane());

		// Double-clicking an icon is the same as choosing it and pressing OK.
		pane.onAccept = () -> dialog.setResult(pane.getSelectedIcon());

		dialog.setResultConverter(button -> button == ButtonType.OK ? pane
			.getSelectedIcon() : null);
		return Optional.ofNullable(dialog.showAndWait().orElse(null));
	}

	// -- Layout --

	private FlowPane buildCategoryTabs() {
		final FlowPane tabs = new FlowPane(6, 6);
		final ToggleGroup group = new ToggleGroup();
		for (final String name : CATEGORIES.keySet()) {
			final ToggleButton tab = new ToggleButton(name);
			tab.setToggleGroup(group);
			tab.setSelected(name.equals(category));
			tab.setOnAction(e -> {
				category = name;
				tab.setSelected(true); // never leave the row with nothing selected
				refresh();
			});
			tabs.getChildren().add(tab);
		}
		return tabs;
	}

	private void refresh() {
		final String needle = filterField.getText() == null ? "" : filterField
			.getText().trim().toLowerCase(Locale.ROOT);
		final List<String> keywords = CATEGORIES.get(category);

		final List<String> matches = new ArrayList<>();
		for (final String literal : icons()) {
			if (!needle.isEmpty() && !literal.contains(needle)) continue;
			if (keywords != null && !matchesCategory(literal, keywords)) continue;
			matches.add(literal);
		}

		rows.clear();
		for (int i = 0; i < matches.size(); i += COLUMNS)
			rows.add(matches.subList(i, Math.min(i + COLUMNS, matches.size())));

		updateSelectionLabel(matches.size());
	}

	/**
	 * Whole-word match against the hyphen-separated parts of an icon name.
	 * Plain substring matching puts {@code abugida-devanagari} under anything
	 * looking for "bug", which is worse than leaving it out.
	 */
	private static boolean matchesCategory(final String literal,
		final List<String> keywords)
	{
		// Drop the pack prefix: mdi2a-account-alert -> account-alert
		final int dash = literal.indexOf('-');
		final String name = dash < 0 ? literal : literal.substring(dash + 1);
		final List<String> parts = Arrays.asList(name.split("-"));

		for (final String keyword : keywords) {
			if (keyword.indexOf('-') >= 0) {
				if (name.contains(keyword)) return true;
			}
			else if (parts.contains(keyword)) return true;
		}
		return false;
	}

	private void updateSelectionLabel(final int shown) {
		final String what = selected == null || selected.isEmpty() ? "no icon"
			: selected;
		selectionLabel.setText(shown + " icons  ·  selected: " + what);
	}

	private void select(final String literal) {
		selected = literal;
		updateSelectionLabel(countShown());
		grid.refresh();
	}

	private int countShown() {
		int n = 0;
		for (final List<String> row : rows)
			n += row.size();
		return n;
	}

	// -- Icon data --

	/**
	 * Every icon literal in the pack. The pack splits its icons across one enum
	 * per initial letter, so they are collected by name rather than by listing 26
	 * imports.
	 */
	private static synchronized List<String> icons() {
		if (allIcons != null) return allIcons;
		final List<String> literals = new ArrayList<>();
		for (char letter = 'A'; letter <= 'Z'; letter++) {
			final String className =
				"org.kordamp.ikonli.materialdesign2.MaterialDesign" + letter;
			try {
				final Object[] constants = Class.forName(className).getEnumConstants();
				if (constants == null) continue;
				for (final Object constant : constants)
					if (constant instanceof Ikon) //
						literals.add(((Ikon) constant).getDescription());
			}
			catch (final ClassNotFoundException e) {
				// Not every letter has a class; skip it.
			}
		}
		Collections.sort(literals);
		allIcons = literals;
		return allIcons;
	}

	private static Map<String, List<String>> categories() {
		final Map<String, List<String>> map = new LinkedHashMap<>();
		map.put("All", null);
		map.put("Science", Arrays.asList("microscope", "atom", "molecule", "dna",
			"flask", "test-tube", "chemical", "bacteria", "virus", "beaker",
			"thermometer", "magnify", "eyedropper", "ruler", "scale", "waves",
			"pulse", "sigma", "function", "math"));
		map.put("Charts", Arrays.asList("chart", "graph", "poll", "table",
			"database", "matrix", "calculator", "counter", "gauge", "speedometer"));
		map.put("Image", Arrays.asList("image", "crop", "layers", "grid", "vector",
			"shape", "square", "circle", "triangle", "hexagon", "brightness",
			"contrast", "blur", "palette", "brush", "format-color", "eye",
			"camera", "aspect", "flip", "rotate", "selection"));
		map.put("Files", Arrays.asList("file", "folder", "archive", "download",
			"upload", "content-save", "export", "import", "zip", "book", "clipboard",
			"printer", "database"));
		map.put("Edit", Arrays.asList("pencil", "edit", "tune", "cog", "wrench",
			"tools", "filter", "eraser", "delete", "plus", "minus", "check", "close",
			"refresh", "sync", "auto-fix", "format", "text", "undo", "redo",
			"content-copy", "content-cut", "trash"));
		map.put("Arrows", Arrays.asList("arrow", "chevron", "swap", "sort",
			"transfer", "arrange", "menu-down", "menu-up", "expand", "collapse",
			"unfold"));
		map.put("Media", Arrays.asList("play", "pause", "stop", "record", "skip",
			"movie", "video", "volume", "music", "camera", "step-forward",
			"step-backward", "rewind", "fast-forward"));
		map.put("Symbols", Arrays.asList("numeric", "alpha", "star", "heart",
			"flag", "bookmark", "tag", "label", "information", "alert", "help",
			"lightbulb", "check-circle", "close-circle", "circle-medium", "shield",
			"lock", "key"));
		map.put("Nature", Arrays.asList("leaf", "tree", "flower", "weather",
			"water", "fire", "earth", "cloud", "snowflake", "bug", "fish", "bird",
			"paw"));
		map.put("Time", Arrays.asList("clock", "timer", "calendar", "history",
			"alarm", "hourglass", "update"));
		return map;
	}

	// -- Cells --

	/** One row of the grid. Chunking keeps the ListView virtualized. */
	private class IconRowCell extends ListCell<List<String>> {

		private final HBox box = new HBox(4);

		IconRowCell() {
			box.setAlignment(Pos.CENTER_LEFT);
			setGraphic(box);
		}

		@Override
		protected void updateItem(final List<String> literals, final boolean empty) {
			super.updateItem(literals, empty);
			box.getChildren().clear();
			if (empty || literals == null) return;

			for (final String literal : literals) {
				final Button tile = new Button();
				tile.getStyleClass().add("icon-tile");
				if (literal.equals(selected)) //
					tile.getStyleClass().add("icon-tile-selected");
				try {
					final FontIcon icon = new FontIcon(literal);
					icon.setIconSize(ICON_SIZE);
					tile.setGraphic(icon);
				}
				catch (final RuntimeException e) {
					tile.setText("?");
				}

				final Tooltip tooltip = new Tooltip(literal);
				tooltip.setShowDelay(Duration.millis(250));
				tile.setTooltip(tooltip);

				tile.setOnAction(e -> select(literal));
				tile.setOnMouseClicked(e -> {
					if (e.getClickCount() == 2 && onAccept != null) {
						select(literal);
						onAccept.run();
					}
				});
				box.getChildren().add(tile);
			}
		}
	}
}
