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
import java.util.function.BiConsumer;

import org.kordamp.ikonli.javafx.FontIcon;

import de.tum.nat.sdmm.actionbarfx.model.ButtonSpec;
import de.tum.nat.sdmm.actionbarfx.run.ActionRunner;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * One row of a bar.
 * <p>
 * The button stores a single base color; every other visual state is derived
 * from it in CSS via the {@code -button-accent} looked-up color, which is the
 * only thing set inline. That is what lets one hue work in both themes.
 */
public class ActionButton extends Button {

	private static final double BADGE_SLOT_WIDTH = 22;
	private static final double ICON_SLOT_WIDTH = 20;

	private final ButtonSpec spec;
	private final File barDir;
	private final ActionRunner runner;
	private final BiConsumer<String, Boolean> messages;

	private final Label badge = new Label();
	private final StackPane badgeSlot = new StackPane(badge);
	private final StackPane iconSlot = new StackPane();
	private final Label text = new Label();
	private final ProgressIndicator busy = new ProgressIndicator();

	/**
	 * @param spec what to draw
	 * @param barDir folder the bar lives in, for resolving script paths
	 * @param runner runs the action; null makes the button inert, which is what
	 *          the builder preview wants
	 * @param messages receives (message, isError) for anything worth showing in
	 *          the bar; may be null
	 */
	public ActionButton(final ButtonSpec spec, final File barDir,
		final ActionRunner runner, final BiConsumer<String, Boolean> messages)
	{
		this.spec = spec;
		this.barDir = barDir;
		this.runner = runner;
		this.messages = messages;

		getStyleClass().add("action-button");
		HoverEffect.install(this);

		badge.getStyleClass().add("step-badge");
		badgeSlot.setMinWidth(BADGE_SLOT_WIDTH);
		badgeSlot.setPrefWidth(BADGE_SLOT_WIDTH);
		badgeSlot.setMaxWidth(BADGE_SLOT_WIDTH);

		iconSlot.setMinWidth(ICON_SLOT_WIDTH);
		iconSlot.setPrefWidth(ICON_SLOT_WIDTH);
		iconSlot.setMaxWidth(ICON_SLOT_WIDTH);

		text.getStyleClass().add("action-label");
		text.setWrapText(true);
		// The button carries no text of its own, but wrapText makes its skin
		// report a horizontal content bias. Without that, the parent asks for a
		// preferred height with no width, the wrapping label inside the graphic
		// has nothing to wrap against, and every button ends up as tall as its
		// label has characters.
		setWrapText(true);

		busy.getStyleClass().add("action-busy");
		busy.setPrefSize(14, 14);
		busy.setMaxSize(14, 14);
		busy.setVisible(false);
		busy.setManaged(false);
		iconSlot.getChildren().add(busy);

		final HBox row = new HBox(8, badgeSlot, iconSlot, text);
		row.setAlignment(Pos.CENTER_LEFT);

		setGraphic(row);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		setMaxWidth(Double.MAX_VALUE);
		setAlignment(Pos.CENTER_LEFT);

		setOnAction(e -> fire0());
		refresh();
	}

	public ButtonSpec getSpec() {
		return spec;
	}

	/** Re-reads everything from the spec. Called after the builder edits it. */
	public final void refresh() {
		text.setText(spec.getLabel() == null ? "" : spec.getLabel());

		if (spec.getStep() != null) {
			badge.setText(String.valueOf(spec.getStep()));
			badge.setVisible(true);
		}
		else {
			badge.setText("");
			// Not managed away: buttons without a step still reserve the slot so
			// labels stay left-aligned across the bar.
			badge.setVisible(false);
		}

		iconSlot.getChildren().removeIf(node -> node instanceof FontIcon);
		final FontIcon icon = icon(spec.getIcon());
		if (icon != null) iconSlot.getChildren().add(0, icon);

		final String tooltip = spec.getTooltip() != null ? spec.getTooltip()
			: spec.getAction() != null ? spec.getAction().toString() : null;
		setTooltip(tooltip == null || tooltip.isEmpty() ? null : new Tooltip(
			tooltip));
	}

	/** Sets the base color the stylesheet derives every state from. */
	public void setAccent(final String hex) {
		setStyle("-button-accent: " + hex + ";");
	}

	private static FontIcon icon(final String literal) {
		if (literal == null || literal.isEmpty()) return null;
		try {
			final FontIcon icon = new FontIcon(literal);
			icon.setIconSize(16);
			return icon;
		}
		catch (final RuntimeException e) {
			// Unknown icon literal: draw the button without one rather than
			// refusing to open the bar.
			return null;
		}
	}

	private void fire0() {
		if (runner == null) return;
		setRunning(true);
		getStyleClass().remove("action-error");
		runner.run(barDir, spec.getAction(), failure -> Platform.runLater(() -> {
			setRunning(false);
			if (failure != null) showFailure(failure);
		}));
	}

	private void setRunning(final boolean running) {
		setDisable(running);
		busy.setVisible(running);
		busy.setManaged(running);
		for (final javafx.scene.Node node : iconSlot.getChildren())
			if (node instanceof FontIcon) node.setVisible(!running);
	}

	private void showFailure(final Throwable failure) {
		if (!getStyleClass().contains("action-error")) //
			getStyleClass().add("action-error");
		final Throwable cause = failure.getCause() != null && failure
			.getMessage() == null ? failure.getCause() : failure;
		final String message = cause.getMessage() == null ? cause.getClass()
			.getSimpleName() : cause.getMessage();
		setTooltip(new Tooltip(message));
		if (messages != null) messages.accept(spec.getLabel() + ": " + message,
			true);
	}
}
