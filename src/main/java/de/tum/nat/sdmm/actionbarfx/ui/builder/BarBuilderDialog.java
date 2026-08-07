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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.scijava.Context;

import de.tum.nat.sdmm.actionbarfx.ActionBarService;
import de.tum.nat.sdmm.actionbarfx.io.BarIO;
import de.tum.nat.sdmm.actionbarfx.model.ActionSpec;
import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.model.BarItem;
import de.tum.nat.sdmm.actionbarfx.model.ButtonSpec;
import de.tum.nat.sdmm.actionbarfx.model.Palette;
import de.tum.nat.sdmm.actionbarfx.model.PaletteRegistry;
import de.tum.nat.sdmm.actionbarfx.model.SeparatorSpec;
import de.tum.nat.sdmm.actionbarfx.ui.ActionBarPane;
import de.tum.nat.sdmm.actionbarfx.ui.ActionBarWindow;
import de.tum.nat.sdmm.actionbarfx.ui.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Creates and edits bars: pick a command or script, label, color, icon and step
 * number, with a live preview of the bar next to the form.
 */
public class BarBuilderDialog {

	private final Context context;
	private final File barDir;
	private final BarConfig config;
	private final ActionBarWindow window;

	private final Stage stage = new Stage();
	private final ObservableList<BarItem> items = FXCollections
		.observableArrayList();
	private final ListView<BarItem> itemList = new ListView<>(items);
	private final ButtonEditorPane editor;
	private final StackPane previewHolder = new StackPane();

	private final TextField titleField = new TextField();
	private final ComboBox<Palette> paletteCombo = new ComboBox<>();
	private final Spinner<Integer> widthSpinner = new Spinner<>(160, 900, 310, 10);
	private final CheckBox startupCheck = new CheckBox("Open when Fiji starts");

	/**
	 * @param window the open bar this edits, or null when editing a bar that is
	 *          not on screen
	 */
	public BarBuilderDialog(final Context context, final File barDir,
		final BarConfig config, final ActionBarWindow window)
	{
		this.context = context;
		this.barDir = barDir;
		this.config = config;
		this.window = window;

		editor = new ButtonEditorPane(context, barDir);
		editor.setOnChange(() -> {
			itemList.refresh();
			refreshPreview();
		});

		items.setAll(config.getItems());

		final BorderPane root = new BorderPane();
		root.setTop(buildBarSettings());
		root.setCenter(buildCenter());
		root.setBottom(buildButtons());

		final Scene scene = new Scene(root, 1080, 640);
		ThemeManager.apply(scene);

		stage.setTitle("Action bar builder — " + barDir.getName());
		stage.setScene(scene);

		refreshPreview();
	}

	public void show() {
		stage.show();
		stage.toFront();
	}

	public Stage getStage() {
		return stage;
	}

	// -- Layout --

	private Region buildBarSettings() {
		titleField.setText(config.getTitle());
		titleField.textProperty().addListener((obs, old, text) -> config.setTitle(
			text));
		HBox.setHgrow(titleField, Priority.ALWAYS);

		paletteCombo.setItems(FXCollections.observableArrayList(PaletteRegistry
			.all()));
		paletteCombo.setValue(PaletteRegistry.get(config.getPalette()));
		paletteCombo.valueProperty().addListener((obs, old, palette) -> {
			if (palette == null) return;
			config.setPalette(palette.getId());
			editor.setPalette(palette);
			refreshPreview();
		});

		widthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
			160, 900, (int) config.getWidth(), 10));
		widthSpinner.setPrefWidth(90);
		widthSpinner.valueProperty().addListener((obs, old, width) -> {
			config.setWidth(width);
			refreshPreview();
		});

		final HBox box = new HBox(10, new Label("Title"), titleField, new Label(
			"Palette"), paletteCombo, new Label("Width"), widthSpinner);
		box.setAlignment(Pos.CENTER_LEFT);
		box.setPadding(new Insets(10));
		return box;
	}

	private Region buildCenter() {
		itemList.setPrefWidth(240);
		final Label empty = new Label("No rows yet.\nPress Button to add one.");
		empty.getStyleClass().add("form-hint");
		empty.setWrapText(true);
		itemList.setPlaceholder(empty);
		itemList.getSelectionModel().selectedItemProperty().addListener((obs, old,
			item) -> editor.setSpec(item instanceof ButtonSpec ? (ButtonSpec) item
				: null, palette()));

		final VBox left = new VBox(8, itemList, buildItemToolbar());
		left.setPadding(new Insets(10));
		VBox.setVgrow(itemList, Priority.ALWAYS);

		final ScrollPane previewScroll = new ScrollPane(previewHolder);
		previewScroll.setFitToWidth(true);
		final VBox right = new VBox(8, new Label("Preview"), previewScroll);
		right.setPadding(new Insets(10));
		right.setPrefWidth(360);
		VBox.setVgrow(previewScroll, Priority.ALWAYS);

		final SplitPane split = new SplitPane(left, editor, right);
		split.setDividerPositions(0.24, 0.68);
		return split;
	}

	private Region buildItemToolbar() {
		final Button addButton = new Button("Button");
		addButton.setOnAction(e -> addItem(newButton()));

		final Button addSeparator = new Button("Separator");
		addSeparator.setOnAction(e -> addItem(new SeparatorSpec()));

		final Button duplicate = new Button("Copy");
		duplicate.setOnAction(e -> {
			final BarItem selected = itemList.getSelectionModel().getSelectedItem();
			if (selected != null) addItem(selected.copy());
		});

		final Button remove = new Button("Remove");
		remove.setOnAction(e -> {
			final int index = itemList.getSelectionModel().getSelectedIndex();
			if (index < 0) return;
			items.remove(index);
			syncItems();
		});

		final Button up = new Button("↑");
		up.setOnAction(e -> move(-1));

		final Button down = new Button("↓");
		down.setOnAction(e -> move(1));

		// A FlowPane, not an HBox: six buttons do not fit the width of the item
		// list, and an HBox truncates them to "Butt...", "Separa..." rather than
		// wrapping.
		final FlowPane box = new FlowPane(6, 6, addButton, addSeparator, duplicate,
			remove, up, down);
		box.setAlignment(Pos.CENTER_LEFT);
		return box;
	}

	private Region buildButtons() {
		final ActionBarService bars = context.getService(ActionBarService.class);
		startupCheck.setDisable(bars == null);
		if (bars != null) startupCheck.setSelected(bars.isStartupBar(barDir));
		startupCheck.setOnAction(e -> {
			if (bars == null) return;
			if (startupCheck.isSelected()) bars.addStartupBar(barDir);
			else bars.removeStartupBar(barDir);
		});

		final Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		final Button save = new Button("Save");
		save.setDefaultButton(true);
		save.setOnAction(e -> save());

		final Button close = new Button("Close");
		close.setOnAction(e -> stage.close());

		final HBox box = new HBox(10, startupCheck, spacer, save, close);
		box.setAlignment(Pos.CENTER_LEFT);
		box.setPadding(new Insets(10));
		return box;
	}

	// -- Editing --

	private ButtonSpec newButton() {
		final ButtonSpec spec = new ButtonSpec();
		spec.setLabel("New button");
		spec.setPaletteIndex(buttonCount() % Math.max(1, palette().size()));
		spec.setAction(new ActionSpec());
		spec.getAction().setType(ActionSpec.COMMAND);
		return spec;
	}

	private int buttonCount() {
		int count = 0;
		for (final BarItem item : items)
			if (item instanceof ButtonSpec) count++;
		return count;
	}

	private void addItem(final BarItem item) {
		final int index = itemList.getSelectionModel().getSelectedIndex();
		if (index < 0) items.add(item);
		else items.add(index + 1, item);
		syncItems();
		itemList.getSelectionModel().select(item);
	}

	private void move(final int delta) {
		final int index = itemList.getSelectionModel().getSelectedIndex();
		final int target = index + delta;
		if (index < 0 || target < 0 || target >= items.size()) return;
		final BarItem item = items.remove(index);
		items.add(target, item);
		syncItems();
		itemList.getSelectionModel().select(target);
	}

	private void syncItems() {
		config.setItems(new ArrayList<>(items));
		refreshPreview();
	}

	private Palette palette() {
		return PaletteRegistry.get(config.getPalette());
	}

	private void refreshPreview() {
		config.setItems(new ArrayList<>(items));
		final ActionBarPane preview = new ActionBarPane(barDir, config, null);
		preview.setPrefWidth(config.getWidth());
		preview.setMaxWidth(config.getWidth());
		previewHolder.getChildren().setAll(preview);
	}

	private void save() {
		config.setItems(new ArrayList<>(items));
		try {
			BarIO.save(config, barDir);
		}
		catch (final IOException e) {
			final Alert alert = new Alert(Alert.AlertType.ERROR,
				"Could not save the bar: " + e.getMessage());
			alert.initOwner(stage);
			alert.showAndWait();
			return;
		}

		if (window != null) {
			window.getStage().setTitle(config.getTitle());
			window.getStage().setWidth(config.getWidth());
			window.getPane().rebuild();
		}

		// The title may have changed, which changes the Plugins > Action Bars
		// entry.
		final ActionBarService bars = context.getService(ActionBarService.class);
		if (bars != null) bars.discoverBars();
	}
}
