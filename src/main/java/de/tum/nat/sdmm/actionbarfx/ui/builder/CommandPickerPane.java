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

import java.util.Locale;
import java.util.Optional;

import org.scijava.Context;
import org.scijava.menu.MenuService;
import org.scijava.menu.ShadowMenu;
import org.scijava.module.ModuleInfo;

import de.tum.nat.sdmm.actionbarfx.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Browses the whole Fiji menu.
 * <p>
 * {@link MenuService} exposes it as a UI-agnostic tree, which covers IJ1 legacy
 * commands, ImageJ2 commands and script files alike — everything in the menus is
 * a {@link ModuleInfo}. Typing in the filter flattens the tree to matching
 * leaves.
 */
public class CommandPickerPane extends VBox {

	private final ShadowMenu root;
	private final TextField filterField = new TextField();
	private final TreeView<ShadowMenu> tree = new TreeView<>();

	public CommandPickerPane(final Context context) {
		setSpacing(8);
		setPadding(new Insets(10));

		final MenuService menuService = context.getService(MenuService.class);
		root = menuService == null ? null : menuService.getMenu();

		filterField.setPromptText("Filter commands, for example 'peak finder'");
		filterField.textProperty().addListener((obs, old, text) -> rebuild(text));

		tree.setShowRoot(false);
		tree.setCellFactory(view -> new MenuCell());
		tree.setPrefSize(460, 380);

		getChildren().addAll(new Label("Command"), filterField, tree);
		VBox.setVgrow(tree, Priority.ALWAYS);

		rebuild("");
	}

	/** The selected command, or null when a submenu is selected. */
	public ModuleInfo getSelected() {
		final TreeItem<ShadowMenu> item = tree.getSelectionModel()
			.getSelectedItem();
		if (item == null || item.getValue() == null) return null;
		return item.getValue().isLeaf() ? item.getValue().getModuleInfo() : null;
	}

	/** Modal picker. Empty when cancelled or nothing was selected. */
	public static Optional<ModuleInfo> showDialog(final Window owner,
		final Context context)
	{
		final Dialog<ModuleInfo> dialog = new Dialog<>();
		dialog.setTitle("Choose command");
		if (owner != null) dialog.initOwner(owner);

		final CommandPickerPane pane = new CommandPickerPane(context);
		dialog.getDialogPane().setContent(pane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
			ButtonType.CANCEL);
		ThemeManager.applyStylesheets(dialog.getDialogPane());

		dialog.setResultConverter(button -> button == ButtonType.OK ? pane
			.getSelected() : null);
		return Optional.ofNullable(dialog.showAndWait().orElse(null));
	}

	private void rebuild(final String filter) {
		if (root == null) {
			tree.setRoot(new TreeItem<>(null));
			return;
		}
		final String needle = filter == null ? "" : filter.trim().toLowerCase(
			Locale.ROOT);
		final TreeItem<ShadowMenu> treeRoot = new TreeItem<>(root);
		if (needle.isEmpty()) {
			addChildren(treeRoot, root);
			treeRoot.setExpanded(true);
		}
		else {
			addMatchingLeaves(treeRoot, root, needle);
		}
		tree.setRoot(treeRoot);
	}

	private void addChildren(final TreeItem<ShadowMenu> parent,
		final ShadowMenu menu)
	{
		for (final ShadowMenu child : menu.getChildren()) {
			final TreeItem<ShadowMenu> item = new TreeItem<>(child);
			parent.getChildren().add(item);
			if (!child.isLeaf()) addChildren(item, child);
		}
	}

	private void addMatchingLeaves(final TreeItem<ShadowMenu> flatRoot,
		final ShadowMenu menu, final String needle)
	{
		for (final ShadowMenu child : menu.getChildren()) {
			if (child.isLeaf()) {
				if (menuString(child).toLowerCase(Locale.ROOT).contains(needle)) //
					flatRoot.getChildren().add(new TreeItem<>(child));
			}
			else addMatchingLeaves(flatRoot, child, needle);
		}
	}

	private static String menuString(final ShadowMenu menu) {
		final ModuleInfo info = menu.getModuleInfo();
		if (info != null && info.getMenuPath() != null) //
			return info.getMenuPath().getMenuString();
		return menu.getName() == null ? "" : menu.getName();
	}

	/** Shows the entry name in tree mode and the full path in filter mode. */
	private class MenuCell extends TreeCell<ShadowMenu> {

		@Override
		protected void updateItem(final ShadowMenu menu, final boolean empty) {
			super.updateItem(menu, empty);
			if (empty || menu == null) {
				setText(null);
				return;
			}
			final boolean filtering = !filterField.getText().trim().isEmpty();
			if (filtering && menu.isLeaf()) setText(menuString(menu));
			else setText(menu.getMenuEntry() == null ? menu.getName() : menu
				.getMenuEntry().getName());
		}
	}
}
