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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tum.nat.sdmm.actionbarfx.model.ActionSpec;
import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.model.ButtonSpec;
import de.tum.nat.sdmm.actionbarfx.model.SeparatorSpec;

/** Milestone 1: round-trip a handwritten {@code bar.json}. */
public class BarIORoundTripTest {

	/**
	 * Handwritten on purpose, including a field ({@code experimentalGlow}) this
	 * build knows nothing about.
	 */
	private static final String HANDWRITTEN = "{\n" + //
		"  \"formatVersion\": 1,\n" + //
		"  \"title\": \"FRET power tools\",\n" + //
		"  \"palette\": \"okabe-ito\",\n" + //
		"  \"width\": 310,\n" + //
		"  \"items\": [\n" + //
		"    {\n" + //
		"      \"type\": \"button\",\n" + //
		"      \"label\": \"Beam profile corrector\",\n" + //
		"      \"paletteIndex\": 0,\n" + //
		"      \"icon\": \"mdi2b-blur-radial\",\n" + //
		"      \"action\": {\n" + //
		"        \"type\": \"command\",\n" + //
		"        \"class\": " + //
		"\"de.mpg.biochem.mars.image.commands.BeamProfileCorrectionCommand\",\n" + //
		"        \"menuPath\": \"Plugins>Mars>Image>Util>Beam Profile Corrector\"\n" + //
		"      }\n" + //
		"    },\n" + //
		"    { \"type\": \"separator\" },\n" + //
		"    {\n" + //
		"      \"type\": \"button\",\n" + //
		"      \"label\": \"Add molecule tags\",\n" + //
		"      \"step\": 1,\n" + //
		"      \"paletteIndex\": 5,\n" + //
		"      \"color\": \"#7C89B4\",\n" + //
		"      \"icon\": \"mdi2t-tag-outline\",\n" + //
		"      \"experimentalGlow\": true,\n" + //
		"      \"action\": { \"type\": \"script\", " + //
		"\"path\": \"scripts/01_add_tags.groovy\" }\n" + //
		"    },\n" + //
		"    {\n" + //
		"      \"type\": \"button\",\n" + //
		"      \"label\": \"Close log\",\n" + //
		"      \"action\": { \"type\": \"ij1\", \"macro\": " + //
		"\"if (isOpen(\\\"Log\\\")) { selectWindow(\\\"Log\\\"); run(\\\"Close\\\"); }\" }\n" + //
		"    }\n" + //
		"  ]\n" + //
		"}\n";

	@Test
	public void readsHandwrittenBar() throws IOException {
		final BarConfig config = BarIO.fromJson(HANDWRITTEN);

		assertEquals(1, config.getFormatVersion());
		assertEquals("FRET power tools", config.getTitle());
		assertEquals("okabe-ito", config.getPalette());
		assertEquals(310, config.getWidth(), 1e-9);
		assertEquals(4, config.getItems().size());

		final ButtonSpec first = assertInstanceOf(ButtonSpec.class, config
			.getItems().get(0));
		assertEquals("Beam profile corrector", first.getLabel());
		assertEquals(Integer.valueOf(0), first.getPaletteIndex());
		assertNull(first.getStep());
		assertEquals("mdi2b-blur-radial", first.getIcon());
		assertEquals(ActionSpec.COMMAND, first.getAction().getType());
		assertEquals(
			"de.mpg.biochem.mars.image.commands.BeamProfileCorrectionCommand", first
				.getAction().getCommandClass());
		assertEquals("Plugins>Mars>Image>Util>Beam Profile Corrector", first
			.getAction().getMenuPath());

		assertInstanceOf(SeparatorSpec.class, config.getItems().get(1));

		final ButtonSpec tagged = assertInstanceOf(ButtonSpec.class, config
			.getItems().get(2));
		assertEquals(Integer.valueOf(1), tagged.getStep());
		assertEquals("#7C89B4", tagged.getColor());
		assertEquals(ActionSpec.SCRIPT, tagged.getAction().getType());
		assertEquals("scripts/01_add_tags.groovy", tagged.getAction().getPath());

		final ButtonSpec log = assertInstanceOf(ButtonSpec.class, config.getItems()
			.get(3));
		assertEquals(ActionSpec.IJ1, log.getAction().getType());
		assertNotNull(log.getAction().getMacro());
		assertTrue(log.getAction().getMacro().contains("selectWindow"));
	}

	@Test
	public void preservesUnknownFields() throws IOException {
		final BarConfig config = BarIO.fromJson(HANDWRITTEN);
		final ButtonSpec tagged = (ButtonSpec) config.getItems().get(2);
		assertEquals(Boolean.TRUE, tagged.getExtras().get("experimentalGlow"));

		final String written = BarIO.toJson(config);
		assertTrue(written.contains("experimentalGlow"), //
			"A field this build does not know must survive a round trip.");
	}

	@Test
	public void roundTripsThroughDisk(@TempDir final Path tempDir)
		throws IOException
	{
		final BarConfig original = BarIO.fromJson(HANDWRITTEN);
		final File barDir = tempDir.resolve("fret-power-tools").toFile();
		BarIO.save(original, barDir);

		final File barFile = new File(barDir, BarIO.BAR_FILE);
		assertTrue(barFile.isFile(), "save() must write " + BarIO.BAR_FILE);

		// Loading the folder and loading the file itself must be equivalent.
		assertEquals(BarIO.toJson(BarIO.load(barDir)), BarIO.toJson(BarIO.load(
			barFile)));

		final BarConfig reloaded = BarIO.load(barDir);
		assertEquals(BarIO.toJson(original), BarIO.toJson(reloaded));

		// And a second save produces byte-identical output.
		final String first = new String(Files.readAllBytes(barFile.toPath()),
			StandardCharsets.UTF_8);
		BarIO.save(reloaded, barDir);
		final String second = new String(Files.readAllBytes(barFile.toPath()),
			StandardCharsets.UTF_8);
		assertEquals(first, second);
	}

	@Test
	public void defaultsAreSaneForANewBar() {
		final BarConfig config = new BarConfig();
		assertEquals(BarConfig.FORMAT_VERSION, config.getFormatVersion());
		assertEquals("okabe-ito", config.getPalette());
		assertTrue(config.getItems().isEmpty());
	}
}
