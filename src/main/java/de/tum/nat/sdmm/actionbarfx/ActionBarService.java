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
package de.tum.nat.sdmm.actionbarfx;

import java.io.File;
import java.util.Collection;
import java.util.List;

import net.imagej.ImageJService;

/**
 * Opens, closes and discovers action bars.
 * <p>
 * Registered as a script alias, so a Groovy script can declare
 * {@code #@ ActionBarService actionBars} and open or reload a bar
 * programmatically.
 */
public interface ActionBarService extends ImageJService {

	/** Opens a bar folder (or its {@code bar.json}), fronting it if already open. */
	void open(File barFileOrDir);

	/** Closes an open bar. Does nothing if it is not open. */
	void close(File barFileOrDir);

	/** Re-reads {@code bar.json} for an open bar and redraws it. */
	void reload(File barFileOrDir);

	boolean isOpen(File barFileOrDir);

	/** Folders of the bars currently on screen. */
	Collection<File> getOpenBars();

	/** Opens the builder for an existing bar. */
	void edit(File barFileOrDir);

	/** Creates an empty bar folder and opens the builder on it. */
	void createBar(File barDir, String title);

	/** {@code Fiji.app/action-bars}, scanned at startup. */
	File getScanDirectory();

	/**
	 * Rescans the scan directory and rebuilds the {@code Plugins > Action Bars}
	 * menu entries.
	 */
	void discoverBars();

	/** Bars reopened once the Fiji UI is up. */
	List<File> getStartupBars();

	boolean isStartupBar(File barFileOrDir);

	void addStartupBar(File barFileOrDir);

	void removeStartupBar(File barFileOrDir);
}
