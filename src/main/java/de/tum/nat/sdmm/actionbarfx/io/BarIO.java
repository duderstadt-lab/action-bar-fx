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
package de.tum.nat.sdmm.actionbarfx.io;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.tum.nat.sdmm.actionbarfx.model.BarConfig;

/** Reads and writes {@code bar.json}. */
public final class BarIO {

	/** Name of the descriptor inside a bar folder. */
	public static final String BAR_FILE = "bar.json";

	private static final ObjectMapper MAPPER = new ObjectMapper()
		.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	private BarIO() {}

	/**
	 * Loads a bar. Accepts either the bar folder or the {@code bar.json} inside
	 * it, since both are natural things to hand to an "open" dialog.
	 */
	public static BarConfig load(final File barFileOrDir) throws IOException {
		return MAPPER.readValue(barFile(barFileOrDir), BarConfig.class);
	}

	/** Writes {@code bar.json} into the given bar folder, creating it if needed. */
	public static void save(final BarConfig config, final File barFileOrDir)
		throws IOException
	{
		final File dir = barDirectory(barFileOrDir);
		if (!dir.isDirectory() && !dir.mkdirs()) //
			throw new IOException("Could not create bar folder " + dir);
		writer().writeValue(new File(dir, BAR_FILE), config);
	}

	/** The {@code bar.json} for a bar folder, or the file itself if given one. */
	public static File barFile(final File barFileOrDir) {
		return isBarFile(barFileOrDir) ? barFileOrDir : new File(barFileOrDir,
			BAR_FILE);
	}

	/** The folder a bar lives in. Script paths are resolved against it. */
	public static File barDirectory(final File barFileOrDir) {
		if (!isBarFile(barFileOrDir)) return barFileOrDir;
		final File parent = barFileOrDir.getParentFile();
		return parent == null ? new File(".") : parent;
	}

	/**
	 * Whether a path names the descriptor rather than the bar folder. A path that
	 * does not exist yet counts as a folder, so {@code save} can create one.
	 */
	private static boolean isBarFile(final File path) {
		return BAR_FILE.equalsIgnoreCase(path.getName()) || path.isFile();
	}

	/** Serializes to a string. Used by the round-trip test. */
	public static String toJson(final BarConfig config) throws IOException {
		return writer().writeValueAsString(config);
	}

	public static BarConfig fromJson(final String json) throws IOException {
		return MAPPER.readValue(json, BarConfig.class);
	}

	private static ObjectWriter writer() {
		final DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		final DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);
		return MAPPER.writer(printer);
	}
}
