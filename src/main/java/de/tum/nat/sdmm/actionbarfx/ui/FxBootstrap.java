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

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

/**
 * Starts the JavaFX toolkit alongside ImageJ's AWT event thread.
 * <p>
 * Same pattern mars-fx uses: constructing a {@link JFXPanel} initializes the FX
 * environment, and implicit exit is switched off so that closing the last bar
 * does not shut the toolkit down for the rest of the Fiji session.
 * <p>
 * This is deliberately never called from
 * {@code DefaultActionBarService.initialize()} — starting FX at Fiji startup
 * would cost every user the toolkit whether or not they open a bar, and would
 * break headless runs. It happens lazily on the first bar that is opened.
 */
public final class FxBootstrap {

	private static final AtomicBoolean STARTED = new AtomicBoolean();

	private FxBootstrap() {}

	/** Starts the toolkit once per JVM. Safe to call repeatedly. */
	public static void start() {
		if (STARTED.compareAndSet(false, true)) {
			new JFXPanel(); // initializes the JavaFX environment
			Platform.setImplicitExit(false);
		}
	}

	/** Starts the toolkit if needed, then runs the task on the FX thread. */
	public static void runOnFx(final Runnable task) {
		start();
		if (Platform.isFxApplicationThread()) task.run();
		else Platform.runLater(task);
	}

	/** Whether the toolkit has been started by us. */
	public static boolean isStarted() {
		return STARTED.get();
	}
}
