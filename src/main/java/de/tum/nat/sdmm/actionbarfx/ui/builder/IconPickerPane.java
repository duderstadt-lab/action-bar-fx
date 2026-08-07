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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import de.tum.nat.sdmm.actionbarfx.ui.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Browses the Material Design 2 icon pack. */
public class IconPickerPane extends VBox {

	private static List<String> allIcons;

	private final TextField filterField = new TextField();
	private final ListView<String> list = new ListView<>();
	private final FilteredList<String> filtered;

	public IconPickerPane(final String initial) {
		setSpacing(8);
		setPadding(new Insets(10));

		filterField.setPromptText("Filter icons, for example 'tag' or 'focus'");

		final ObservableList<String> icons = FXCollections.observableArrayList(
			icons());
		filtered = new FilteredList<>(icons, s -> true);
		list.setItems(filtered);
		list.setCellFactory(view -> new IconCell());
		list.setPrefHeight(380);

		filterField.textProperty().addListener((obs, old, text) -> {
			final String needle = text == null ? "" : text.trim().toLowerCase(
				Locale.ROOT);
			filtered.setPredicate(literal -> needle.isEmpty() || literal.toLowerCase(
				Locale.ROOT).contains(needle));
		});

		if (initial != null && !initial.isEmpty()) {
			list.getSelectionModel().select(initial);
			list.scrollTo(initial);
		}

		getChildren().addAll(new Label("Icon"), filterField, list);
		VBox.setVgrow(list, javafx.scene.layout.Priority.ALWAYS);
	}

	public String getSelectedIcon() {
		return list.getSelectionModel().getSelectedItem();
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

		dialog.setResultConverter(button -> button == ButtonType.OK ? pane
			.getSelectedIcon() : null);
		return Optional.ofNullable(dialog.showAndWait().orElse(null));
	}

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

	/** Renders the icon next to its literal. */
	private static class IconCell extends ListCell<String> {

		@Override
		protected void updateItem(final String literal, final boolean empty) {
			super.updateItem(literal, empty);
			if (empty || literal == null) {
				setText(null);
				setGraphic(null);
				return;
			}
			setText(literal);
			try {
				final FontIcon icon = new FontIcon(literal);
				icon.setIconSize(18);
				setGraphic(icon);
			}
			catch (final RuntimeException e) {
				setGraphic(null);
			}
		}
	}
}
