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
package de.tum.nat.sdmm.actionbarfx.commands;

import java.io.File;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import de.tum.nat.sdmm.actionbarfx.ActionBarService;

/**
 * Opens a bar folder.
 * <p>
 * Also the command behind every auto-discovered entry under
 * {@code Plugins > Action Bars}: those register a {@code CommandInfo} for this
 * class with {@code barFile} supplied as a preset, which resolves the input so
 * no dialog appears.
 */
@Plugin(type = Command.class,
	menuPath = "Plugins>Action Bar FX>Open bar...")
public class OpenActionBarCommand implements Command {

	@Parameter(label = "Bar folder", style = FileWidget.DIRECTORY_STYLE)
	private File barFile;

	@Parameter
	private ActionBarService actionBars;

	@Override
	public void run() {
		actionBars.open(barFile);
	}

	public void setBarFile(final File barFile) {
		this.barFile = barFile;
	}

	public File getBarFile() {
		return barFile;
	}
}
