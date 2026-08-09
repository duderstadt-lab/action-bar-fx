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
import org.scijava.log.LogService;
import org.scijava.log.StderrLogService;

import de.tum.nat.sdmm.actionbarfx.model.ActionSpec;
import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.model.BarItem;
import de.tum.nat.sdmm.actionbarfx.model.ButtonSpec;

/** The example bars shipped in the jar, and unpacking them on a fresh install. */
public class BundledBarsTest {

	private final LogService log = new StderrLogService();

	@Test
	public void bothExampleBarsAreShipped() throws IOException {
		final List<String> names = BundledBars.bundledBarNames();
		assertEquals(List.of("fret-power-tools", "mars-power-tools"), names,
			"The jar must carry both example bars.");
	}

	@Test
	public void aFreshInstallGetsBothBars(@TempDir final Path scanDir)
		throws IOException
	{
		final List<File> written = BundledBars.installMissing(scanDir.toFile(), List.of(), log);
		assertEquals(2, written.size());

		// Both are discoverable exactly like a hand-placed bar.
		final List<File> found = BarLocator.findBars(scanDir.toFile());
		assertEquals(2, found.size());
		assertEquals("FRET power tools", BarLocator.barName(found.get(0)));
		assertEquals("Mars power tools", BarLocator.barName(found.get(1)));
	}

	@Test
	public void theScriptsComeWithTheBarTheyBelongTo(@TempDir final Path scanDir)
		throws IOException
	{
		BundledBars.installMissing(scanDir.toFile(), List.of(), log);

		// A bar whose scripts did not come along would fail at the first click.
		final File fret = scanDir.resolve("fret-power-tools").toFile();
		final BarConfig config = BarIO.load(fret);
		int scripts = 0;
		for (final BarItem item : config.getItems()) {
			if (!(item instanceof ButtonSpec)) continue;
			final ActionSpec action = ((ButtonSpec) item).getAction();
			if (action == null || !ActionSpec.SCRIPT.equals(action.getType())) continue;
			scripts++;
			assertTrue(new File(fret, action.getPath()).isFile(), //
				"Missing script: " + action.getPath());
		}
		assertEquals(6, scripts);
	}

	@Test
	public void anEditedBarIsNeverOverwritten(@TempDir final Path scanDir)
		throws IOException
	{
		BundledBars.installMissing(scanDir.toFile(), List.of(), log);

		final File marsBar = scanDir.resolve("mars-power-tools").resolve(
			BarIO.BAR_FILE).toFile();
		final BarConfig edited = BarIO.load(marsBar);
		edited.setTitle("My own Mars bar");
		BarIO.save(edited, marsBar.getParentFile());

		// A second start must leave the edit alone and write nothing.
		final List<File> written = BundledBars.installMissing(scanDir.toFile(), List.of(), log);
		assertTrue(written.isEmpty(), "Nothing should be written the second time.");
		assertEquals("My own Mars bar", BarIO.load(marsBar).getTitle());
	}

	@Test
	public void aDeletedBarStaysDeleted(@TempDir final Path scanDir)
		throws IOException
	{
		final List<String> installed = new java.util.ArrayList<>();
		for (final File bar : BundledBars.installMissing(scanDir.toFile(),
			installed, log)) installed.add(bar.getName());
		assertEquals(2, installed.size());

		// Someone decides they do not want this one.
		deleteTree(scanDir.resolve("fret-power-tools"));

		// Every later start must respect that, or a shipped bar could never be
		// declined.
		for (int start = 0; start < 3; start++)
			assertTrue(BundledBars.installMissing(scanDir.toFile(), installed, log)
				.isEmpty(), "Nothing should be reinstalled on start " + start);
		assertFalse(scanDir.resolve("fret-power-tools").toFile().exists());

		// And it is recoverable: an empty record puts the originals back.
		assertEquals(1, BundledBars.installMissing(scanDir.toFile(), List.of(),
			log).size());
		assertTrue(scanDir.resolve("fret-power-tools").toFile().isDirectory());
	}

	@Test
	public void theShippedBarsAreValid() throws IOException {
		for (final String name : BundledBars.bundledBarNames()) {
			final String json = new String(BundledBarsTest.class.getClassLoader()
				.getResourceAsStream("de/tum/nat/sdmm/actionbarfx/bars/" + name +
					"/bar.json").readAllBytes(), StandardCharsets.UTF_8);
			final BarConfig config = BarIO.fromJson(json);
			assertNotNull(config.getTitle(), name + " has no title");
			assertFalse(config.getItems().isEmpty(), name + " has no rows");
			for (final BarItem item : config.getItems())
				if (item instanceof ButtonSpec) assertNotNull(((ButtonSpec) item)
					.getAction(), name + ": a button with no action");
		}
	}

	private static void deleteTree(final Path path) throws IOException {
		try (final java.util.stream.Stream<Path> paths = Files.walk(path)) {
			for (final Path p : paths.sorted(java.util.Comparator.reverseOrder())
				.toList()) Files.delete(p);
		}
	}
}
