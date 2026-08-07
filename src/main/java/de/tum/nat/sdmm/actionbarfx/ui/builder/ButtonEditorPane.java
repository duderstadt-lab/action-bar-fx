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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;
import org.scijava.Context;
import org.scijava.module.ModuleInfo;

import de.tum.nat.sdmm.actionbarfx.model.ActionSpec;
import de.tum.nat.sdmm.actionbarfx.model.ButtonSpec;
import de.tum.nat.sdmm.actionbarfx.model.Palette;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

/** Edits one {@link ButtonSpec}. */
public class ButtonEditorPane extends VBox {

	private final Context context;
	private final File barDir;

	private ButtonSpec spec;
	private Palette palette;
	private Runnable onChange;

	/** Suppresses change events while the form is being populated. */
	private boolean loading;

	private final TextField labelField = new TextField();
	private final CheckBox stepCheck = new CheckBox("Step");
	private final Spinner<Integer> stepSpinner = new Spinner<>(1, 99, 1);
	private final ComboBox<Integer> paletteCombo = new ComboBox<>();
	private final CheckBox overrideCheck = new CheckBox("Custom color");
	private final ColorPicker colorPicker = new ColorPicker();
	private final TextField iconField = new TextField();
	private final StackPane iconPreview = new StackPane();
	private final TextField tooltipField = new TextField();

	private final ComboBox<String> actionType = new ComboBox<>();
	private final StackPane actionEditors = new StackPane();

	private final TextField classField = new TextField();
	private final TextField menuPathField = new TextField();
	private final TextField scriptField = new TextField();
	private final TextArea macroArea = new TextArea();

	private final VBox commandEditor;
	private final VBox scriptEditor;
	private final VBox macroEditor;

	public ButtonEditorPane(final Context context, final File barDir) {
		this.context = context;
		this.barDir = barDir;

		setSpacing(10);
		setPadding(new Insets(10));

		commandEditor = buildCommandEditor();
		scriptEditor = buildScriptEditor();
		macroEditor = buildMacroEditor();
		actionEditors.getChildren().addAll(commandEditor, scriptEditor,
			macroEditor);

		getChildren().addAll(buildForm(), new Label("Action"), actionType,
			actionEditors);
		VBox.setVgrow(actionEditors, Priority.ALWAYS);

		setDisable(true);
	}

	public void setOnChange(final Runnable onChange) {
		this.onChange = onChange;
	}

	/** Loads a button into the form. Pass null to clear it. */
	public void setSpec(final ButtonSpec spec, final Palette palette) {
		this.spec = spec;
		this.palette = palette;
		setDisable(spec == null);
		if (spec == null) return;

		loading = true;
		try {
			labelField.setText(spec.getLabel() == null ? "" : spec.getLabel());
			tooltipField.setText(spec.getTooltip() == null ? "" : spec.getTooltip());

			stepCheck.setSelected(spec.getStep() != null);
			stepSpinner.setDisable(spec.getStep() == null);
			if (spec.getStep() != null) stepSpinner.getValueFactory().setValue(spec
				.getStep());

			reloadPaletteChoices();
			paletteCombo.setValue(spec.getPaletteIndex());

			final boolean hasOverride = spec.getColor() != null && !spec.getColor()
				.isEmpty();
			overrideCheck.setSelected(hasOverride);
			colorPicker.setDisable(!hasOverride);
			colorPicker.setValue(parseColor(hasOverride ? spec.getColor() : palette
				.colorFor(spec)));

			iconField.setText(spec.getIcon() == null ? "" : spec.getIcon());
			refreshIconPreview();

			final ActionSpec action = action();
			actionType.setValue(action.getType());
			classField.setText(nullToEmpty(action.getCommandClass()));
			menuPathField.setText(nullToEmpty(action.getMenuPath()));
			scriptField.setText(nullToEmpty(action.getPath()));
			macroArea.setText(nullToEmpty(action.getMacro()));
			showActionEditor(action.getType());
		}
		finally {
			loading = false;
		}
	}

	/** Re-reads the palette, for instance after the bar palette was switched. */
	public void setPalette(final Palette palette) {
		this.palette = palette;
		if (spec != null) setSpec(spec, palette);
	}

	// -- Form construction --

	private GridPane buildForm() {
		final GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(8);
		final ColumnConstraints labels = new ColumnConstraints();
		labels.setMinWidth(70);
		final ColumnConstraints fields = new ColumnConstraints();
		fields.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(labels, fields);

		labelField.textProperty().addListener((obs, old, text) -> {
			if (loading || spec == null) return;
			spec.setLabel(text);
			changed();
		});

		stepCheck.selectedProperty().addListener((obs, old, selected) -> {
			stepSpinner.setDisable(!selected);
			if (loading || spec == null) return;
			spec.setStep(selected ? stepSpinner.getValue() : null);
			changed();
		});
		stepSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
			1, 99, 1));
		stepSpinner.setPrefWidth(80);
		stepSpinner.valueProperty().addListener((obs, old, value) -> {
			if (loading || spec == null || !stepCheck.isSelected()) return;
			spec.setStep(value);
			changed();
		});

		paletteCombo.setCellFactory(view -> new SwatchCell());
		paletteCombo.setButtonCell(new SwatchCell());
		paletteCombo.valueProperty().addListener((obs, old, index) -> {
			if (loading || spec == null) return;
			spec.setPaletteIndex(index);
			changed();
		});

		overrideCheck.selectedProperty().addListener((obs, old, selected) -> {
			colorPicker.setDisable(!selected);
			if (loading || spec == null) return;
			spec.setColor(selected ? toHex(colorPicker.getValue()) : null);
			changed();
		});
		colorPicker.valueProperty().addListener((obs, old, color) -> {
			if (loading || spec == null || !overrideCheck.isSelected()) return;
			spec.setColor(toHex(color));
			changed();
		});

		iconField.setEditable(false);
		iconField.setPromptText("none");
		final Button chooseIcon = new Button("Choose…");
		chooseIcon.setOnAction(e -> IconPickerPane.showDialog(getScene() == null
			? null : getScene().getWindow(), spec.getIcon()).ifPresent(literal -> {
				spec.setIcon(literal);
				iconField.setText(literal);
				refreshIconPreview();
				changed();
			}));
		final Button clearIcon = new Button("Clear");
		clearIcon.setOnAction(e -> {
			spec.setIcon(null);
			iconField.setText("");
			refreshIconPreview();
			changed();
		});

		tooltipField.textProperty().addListener((obs, old, text) -> {
			if (loading || spec == null) return;
			spec.setTooltip(text == null || text.isEmpty() ? null : text);
			changed();
		});

		final HBox stepRow = new HBox(8, stepCheck, stepSpinner);
		stepRow.setAlignment(Pos.CENTER_LEFT);

		final HBox colorRow = new HBox(8, paletteCombo, overrideCheck, colorPicker);
		colorRow.setAlignment(Pos.CENTER_LEFT);

		iconPreview.setMinSize(24, 24);
		final HBox iconRow = new HBox(8, iconPreview, iconField, chooseIcon,
			clearIcon);
		iconRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(iconField, Priority.ALWAYS);

		int row = 0;
		grid.addRow(row++, new Label("Label"), labelField);
		grid.addRow(row++, new Label("Badge"), stepRow);
		grid.addRow(row++, new Label("Color"), colorRow);
		grid.addRow(row++, new Label("Icon"), iconRow);
		grid.addRow(row++, new Label("Tooltip"), tooltipField);

		actionType.setItems(FXCollections.observableArrayList(ActionSpec.COMMAND,
			ActionSpec.SCRIPT, ActionSpec.IJ1));
		actionType.valueProperty().addListener((obs, old, type) -> {
			showActionEditor(type);
			if (loading || spec == null) return;
			action().setType(type);
			changed();
		});

		return grid;
	}

	private VBox buildCommandEditor() {
		classField.setEditable(false);
		classField.setPromptText("class name, resolved first");
		menuPathField.setPromptText("Plugins>Mars>Image>Peak Finder");

		final Button choose = new Button("Choose command…");
		choose.setOnAction(e -> CommandPickerPane.showDialog(getScene() == null
			? null : getScene().getWindow(), context).ifPresent(this::applyCommand));

		menuPathField.textProperty().addListener((obs, old, text) -> {
			if (loading || spec == null) return;
			action().setMenuPath(emptyToNull(text));
			changed();
		});

		final VBox box = new VBox(8, choose, labeled("Class", classField), labeled(
			"Menu path", menuPathField));
		box.setPadding(new Insets(4, 0, 0, 0));
		return box;
	}

	private VBox buildScriptEditor() {
		scriptField.setPromptText("scripts/01_add_tags.groovy");
		scriptField.textProperty().addListener((obs, old, text) -> {
			if (loading || spec == null) return;
			action().setPath(emptyToNull(text));
			changed();
		});

		final Button browse = new Button("Choose script…");
		browse.setOnAction(e -> chooseScript());

		final VBox box = new VBox(8, browse, labeled("Path", scriptField), new Label(
			"Paths are relative to the bar folder, so the folder stays portable."));
		box.setPadding(new Insets(4, 0, 0, 0));
		return box;
	}

	private VBox buildMacroEditor() {
		macroArea.setPromptText(
			"if (isOpen(\"Log\")) { selectWindow(\"Log\"); run(\"Close\"); }");
		macroArea.setPrefRowCount(6);
		macroArea.textProperty().addListener((obs, old, text) -> {
			if (loading || spec == null) return;
			action().setMacro(emptyToNull(text));
			changed();
		});
		final VBox box = new VBox(8, new Label("IJ1 macro"), macroArea);
		box.setPadding(new Insets(4, 0, 0, 0));
		VBox.setVgrow(macroArea, Priority.ALWAYS);
		return box;
	}

	private static VBox labeled(final String text, final Region field) {
		final VBox box = new VBox(4, new Label(text), field);
		return box;
	}

	// -- Actions --

	private void applyCommand(final ModuleInfo info) {
		final ActionSpec action = action();
		action.setType(ActionSpec.COMMAND);
		action.setCommandClass(info.getDelegateClassName());
		action.setMenuPath(info.getMenuPath() == null ? null : info.getMenuPath()
			.getMenuString());
		action.setPath(null);
		action.setMacro(null);

		if (spec.getLabel() == null || spec.getLabel().isEmpty()) //
			spec.setLabel(info.getTitle());

		setSpec(spec, palette);
		changed();
	}

	private void chooseScript() {
		final FileChooser chooser = new FileChooser();
		chooser.setTitle("Choose script");
		final File scripts = new File(barDir, "scripts");
		chooser.setInitialDirectory(scripts.isDirectory() ? scripts : barDir);
		final File chosen = chooser.showOpenDialog(getScene() == null ? null
			: getScene().getWindow());
		if (chosen == null) return;

		final File inBar = copyIntoBar(chosen);
		final String relative = relativize(inBar);
		action().setType(ActionSpec.SCRIPT);
		action().setPath(relative);
		if (spec.getLabel() == null || spec.getLabel().isEmpty()) //
			spec.setLabel(inBar.getName());
		setSpec(spec, palette);
		changed();
	}

	/**
	 * Scripts picked from elsewhere are copied into {@code scripts/} so the bar
	 * folder can be zipped and shared unchanged.
	 */
	private File copyIntoBar(final File chosen) {
		try {
			if (chosen.getCanonicalPath().startsWith(barDir.getCanonicalPath())) //
				return chosen;
			final File scripts = new File(barDir, "scripts");
			if (!scripts.isDirectory() && !scripts.mkdirs()) return chosen;
			final File target = new File(scripts, chosen.getName());
			Files.copy(chosen.toPath(), target.toPath(),
				StandardCopyOption.REPLACE_EXISTING);
			return target;
		}
		catch (final IOException e) {
			return chosen;
		}
	}

	private String relativize(final File file) {
		try {
			return barDir.getCanonicalFile().toPath().relativize(file
				.getCanonicalFile().toPath()).toString().replace(File.separatorChar,
					'/');
		}
		catch (final IOException | IllegalArgumentException e) {
			return file.getAbsolutePath();
		}
	}

	// -- Helper methods --

	private ActionSpec action() {
		if (spec.getAction() == null) spec.setAction(new ActionSpec());
		return spec.getAction();
	}

	private void showActionEditor(final String type) {
		commandEditor.setVisible(ActionSpec.COMMAND.equals(type));
		scriptEditor.setVisible(ActionSpec.SCRIPT.equals(type));
		macroEditor.setVisible(ActionSpec.IJ1.equals(type));
		commandEditor.setManaged(commandEditor.isVisible());
		scriptEditor.setManaged(scriptEditor.isVisible());
		macroEditor.setManaged(macroEditor.isVisible());
	}

	private void reloadPaletteChoices() {
		final List<Integer> indices = new ArrayList<>();
		indices.add(null);
		for (int i = 0; i < palette.size(); i++)
			indices.add(i);
		paletteCombo.setItems(FXCollections.observableArrayList(indices));
	}

	private void refreshIconPreview() {
		iconPreview.getChildren().clear();
		final String literal = spec == null ? null : spec.getIcon();
		if (literal == null || literal.isEmpty()) return;
		try {
			final FontIcon icon = new FontIcon(literal);
			icon.setIconSize(18);
			iconPreview.getChildren().add(icon);
		}
		catch (final RuntimeException e) {
			// Unknown literal: show nothing.
		}
	}

	private void changed() {
		if (onChange != null) onChange.run();
	}

	private static String nullToEmpty(final String value) {
		return value == null ? "" : value;
	}

	private static String emptyToNull(final String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	static Color parseColor(final String hex) {
		try {
			return Color.web(hex);
		}
		catch (final RuntimeException e) {
			return Color.web(Palette.NEUTRAL);
		}
	}

	static String toHex(final Color color) {
		return String.format("#%02X%02X%02X", Math.round(color.getRed() * 255), Math
			.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
	}

	/** Palette index rendered as a color swatch. */
	private class SwatchCell extends ListCell<Integer> {

		@Override
		protected void updateItem(final Integer index, final boolean empty) {
			super.updateItem(index, empty);
			if (empty || index == null) {
				setText(empty ? null : "No palette color");
				setGraphic(null);
				return;
			}
			setText(String.valueOf(index));
			final Region swatch = new Region();
			swatch.setMinSize(16, 16);
			swatch.setPrefSize(16, 16);
			swatch.setStyle("-fx-background-color: " + palette.color(index) +
				"; -fx-background-radius: 3;");
			setGraphic(swatch);
		}
	}
}
