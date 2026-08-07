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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.scijava.Context;

import de.tum.nat.sdmm.actionbarfx.io.BarIO;
import de.tum.nat.sdmm.actionbarfx.io.BarLocator;
import de.tum.nat.sdmm.actionbarfx.ui.FxBootstrap;

/**
 * Milestone 4: service initialization must survive a corrupt bar and must not
 * drag JavaFX into a headless run.
 */
public class ActionBarServiceTest {

	@Test
	public void contextCreationStaysHeadless() {
		try (final Context context = new Context(ActionBarService.class)) {
			final ActionBarService bars = context.getService(ActionBarService.class);
			assertNotNull(bars, "The service must be discovered as an ImageJService.");
			assertNotNull(bars.getScanDirectory());

			// initialize() registers menu entries only. Starting the toolkit there
			// would cost every user the FX startup and break headless runs.
			assertFalse(FxBootstrap.isStarted(),
				"Service initialization must not start the JavaFX toolkit.");

			// A rescan of a directory that does not exist is a no-op, not a failure.
			assertDoesNotThrow(bars::discoverBars);
			assertTrue(bars.getOpenBars().isEmpty());
		}
	}

	@Test
	public void aCorruptBarIsSkippedRatherThanThrowing(@TempDir final Path scanDir)
		throws IOException
	{
		final File good = scanDir.resolve("good-bar").toFile();
		assertTrue(good.mkdirs());
		Files.write(new File(good, BarIO.BAR_FILE).toPath(), //
			"{\"title\": \"Good bar\", \"items\": []}".getBytes(
				StandardCharsets.UTF_8));

		final File broken = scanDir.resolve("broken-bar").toFile();
		assertTrue(broken.mkdirs());
		Files.write(new File(broken, BarIO.BAR_FILE).toPath(), //
			"{ this is not json".getBytes(StandardCharsets.UTF_8));

		final File notABar = scanDir.resolve("not-a-bar").toFile();
		assertTrue(notABar.mkdirs());

		final List<File> bars = BarLocator.findBars(scanDir.toFile());
		assertEquals(2, bars.size(), "Only folders holding a bar.json count.");

		// The name of a bar whose descriptor cannot be parsed falls back to the
		// folder name, so the menu entry is still registered and nothing throws.
		assertEquals("Good bar", BarLocator.barName(good));
		assertEquals("broken-bar", assertDoesNotThrow(() -> BarLocator.barName(
			broken)));
	}

	@Test
	public void aMissingScanDirectoryYieldsNoBars() {
		assertTrue(BarLocator.findBars(new File("/no/such/directory")).isEmpty());
		assertTrue(BarLocator.findBars(null).isEmpty());
		assertTrue(BarLocator.findAllBars(null).isEmpty());
	}

	@Test
	public void bothSpellingsOfTheScanFolderAreScanned(
		@TempDir final Path installDir) throws IOException
	{
		final String previousIjDir = System.getProperty("ij.dir");
		System.setProperty("ij.dir", installDir.toString());
		try {
			// The documented spelling and the obvious one to type.
			for (final String scanDirName : new String[] { "action-bars",
				"ActionBars" })
			{
				final File barDir = installDir.resolve(scanDirName).resolve("a-bar")
					.toFile();
				assertTrue(barDir.mkdirs());
				Files.write(new File(barDir, BarIO.BAR_FILE).toPath(), //
					("{\"title\": \"" + scanDirName + " bar\", \"items\": []}").getBytes(
						StandardCharsets.UTF_8));
			}

			// A null context falls back to ij.dir, which is what this exercises.
			final List<File> scanDirs = BarLocator.scanDirectories(null);
			assertEquals(2, scanDirs.size());
			assertEquals(2, BarLocator.findAllBars(scanDirs).size());
			assertEquals("action-bars", BarLocator.scanDirectory(null).getName(),
				"The documented spelling wins when both exist.");
		}
		finally {
			if (previousIjDir == null) System.clearProperty("ij.dir");
			else System.setProperty("ij.dir", previousIjDir);
		}
	}

	@Test
	public void theExampleBarLoads() throws IOException {
		final File exampleBar = new File("action-bars/fret-power-tools");
		if (!BarIO.barFile(exampleBar).isFile()) return; // not run from the repo

		final de.tum.nat.sdmm.actionbarfx.model.BarConfig config = BarIO.load(
			exampleBar);
		assertEquals("FRET power tools", config.getTitle());

		int buttons = 0;
		int badges = 0;
		for (final de.tum.nat.sdmm.actionbarfx.model.BarItem item : config
			.getItems())
		{
			if (!(item instanceof de.tum.nat.sdmm.actionbarfx.model.ButtonSpec))
				continue;
			final de.tum.nat.sdmm.actionbarfx.model.ButtonSpec button =
				(de.tum.nat.sdmm.actionbarfx.model.ButtonSpec) item;
			buttons++;
			if (button.getStep() != null) badges++;
			assertNotNull(button.getAction(), button.getLabel() + " has no action.");

			// Every script the bar references must actually be in the folder, or it
			// is not the self-contained thing that can be zipped and shared.
			if ("script".equals(button.getAction().getType())) //
				assertTrue(new File(exampleBar, button.getAction().getPath()).isFile(),
					"Missing script: " + button.getAction().getPath());
		}
		assertEquals(11, buttons);
		assertEquals(6, badges);
	}
}
