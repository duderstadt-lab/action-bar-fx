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

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Brief message overlaid on a bar.
 * <p>
 * Failures have to surface in the bar itself, not only in the ImageJ console —
 * a user clicking buttons in a bar is not watching the console.
 */
public final class Toast {

	private static final Duration FADE = Duration.millis(180);

	private Toast() {}

	public static void show(final StackPane host, final String message,
		final boolean error)
	{
		final Label label = new Label(message);
		label.getStyleClass().add("toast");
		if (error) label.getStyleClass().add("toast-error");
		label.setWrapText(true);
		label.setMaxWidth(Double.MAX_VALUE);
		label.setMouseTransparent(true);

		StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
		StackPane.setMargin(label, new Insets(0, 12, 12, 12));
		host.getChildren().add(label);

		final FadeTransition in = new FadeTransition(FADE, label);
		in.setFromValue(0);
		in.setToValue(1);
		final PauseTransition hold = new PauseTransition(Duration.seconds(error ? 6
			: 3));
		final FadeTransition out = new FadeTransition(FADE, label);
		out.setFromValue(1);
		out.setToValue(0);

		final SequentialTransition sequence = new SequentialTransition(in, hold,
			out);
		sequence.setOnFinished(e -> host.getChildren().remove(label));
		sequence.play();
	}
}
