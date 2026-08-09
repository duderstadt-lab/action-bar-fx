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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.scijava.log.LogService;

/**
 * The example bars shipped inside the jar, and unpacking them into the scan
 * folder.
 * <p>
 * Installing the plugin should leave something to look at. The bars are written
 * out rather than run from the jar because a bar is a folder of files — scripts
 * are resolved as real paths, and the whole point of the format is that you can
 * edit a bar and zip it up to give away.
 * <p>
 * Each bar is installed once and then remembered, so it cannot overwrite an edit
 * and does not come back after being deleted. Reappearing every start would make
 * a shipped bar impossible to decline, and deleting one is a clear enough way of
 * saying so. The originals stay in the jar; {@code installMissing} with an empty
 * record puts them back.
 */
public final class BundledBars {

	/** Where the bars live inside the jar. */
	private static final String RESOURCE_ROOT = "de/tum/nat/sdmm/actionbarfx/bars";

	private BundledBars() {}

	/**
	 * Copies any shipped bar that is neither already in {@code scanDir} nor
	 * recorded as installed there before.
	 *
	 * @param installedBefore paths of bar folders installed on an earlier start,
	 *          as returned by {@link #recordKey(File)}; those are left alone
	 *          however the user has since disposed of them
	 * @return the bar folders written, empty when there was nothing to do
	 */
	public static List<File> installMissing(final File scanDir,
		final Collection<String> installedBefore, final LogService log)
	{
		final List<File> written = new ArrayList<>();
		try {
			for (final String bar : bundledBarNames()) {
				final File target = new File(scanDir, bar);
				if (installedBefore != null && installedBefore.contains(recordKey(
					target))) continue;
				if (target.exists()) continue;
				if (copyBar(bar, target)) written.add(target);
			}
			if (!written.isEmpty()) log.info("ActionBarFX: installed " + written
				.size() + " example bar(s) into " + scanDir);
		}
		catch (final Throwable t) {
			// A read-only Fiji, or no write permission. Not worth a stack trace at
			// startup, and certainly not worth failing over.
			log.warn("ActionBarFX could not install its example bars into " + scanDir +
				": " + t.getMessage());
		}
		return written;
	}

	/**
	 * How an installed bar is recorded: the full path of its folder, not its
	 * name.
	 * <p>
	 * Preferences are stored per user, not per Fiji installation, so recording
	 * bare names meant that installing a second Fiji on the same machine skipped
	 * the example bars entirely — the record from the first one already claimed
	 * they were done. Recording the path keeps each installation independent
	 * while still letting a deleted bar stay deleted where it was deleted.
	 */
	public static String recordKey(final File barDir) {
		try {
			return barDir.getCanonicalPath();
		}
		catch (final IOException e) {
			return barDir.getAbsolutePath();
		}
	}

	/** Names of the bar folders shipped in the jar. */
	public static List<String> bundledBarNames() throws IOException {
		final List<String> names = new ArrayList<>();
		final URL root = BundledBars.class.getClassLoader().getResource(
			RESOURCE_ROOT);
		if (root == null) return names;

		if ("jar".equals(root.getProtocol())) {
			final JarURLConnection connection = (JarURLConnection) root
				.openConnection();
			try (final JarFile jar = connection.getJarFile()) {
				final Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					final String name = entries.nextElement().getName();
					if (!name.startsWith(RESOURCE_ROOT + "/")) continue;
					final String rest = name.substring(RESOURCE_ROOT.length() + 1);
					final int slash = rest.indexOf('/');
					if (slash <= 0) continue;
					final String bar = rest.substring(0, slash);
					if (!names.contains(bar)) names.add(bar);
				}
			}
		}
		else {
			// Running from target/classes, or from the source tree in a test.
			final File[] dirs = new File(root.getPath()).listFiles(File::isDirectory);
			if (dirs != null) for (final File dir : dirs)
				names.add(dir.getName());
		}
		names.sort(String::compareTo);
		return names;
	}

	/** Copies one bundled bar folder, with everything under it. */
	private static boolean copyBar(final String bar, final File target)
		throws IOException
	{
		final URL root = BundledBars.class.getClassLoader().getResource(
			RESOURCE_ROOT + "/" + bar);
		if (root == null) return false;

		if ("jar".equals(root.getProtocol())) {
			final String prefix = RESOURCE_ROOT + "/" + bar + "/";
			final JarURLConnection connection = (JarURLConnection) root
				.openConnection();
			try (final JarFile jar = connection.getJarFile()) {
				final Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					if (entry.isDirectory() || !entry.getName().startsWith(prefix))
						continue;
					final Path out = target.toPath().resolve(entry.getName().substring(
						prefix.length()));
					Files.createDirectories(out.getParent());
					try (final InputStream in = jar.getInputStream(entry)) {
						Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
		else {
			copyTree(new File(root.getPath()).toPath(), target.toPath());
		}
		return true;
	}

	private static void copyTree(final Path from, final Path to)
		throws IOException
	{
		try (final java.util.stream.Stream<Path> paths = Files.walk(from)) {
			for (final Path source : paths.toList()) {
				final Path out = to.resolve(from.relativize(source).toString());
				if (Files.isDirectory(source)) Files.createDirectories(out);
				else {
					Files.createDirectories(out.getParent());
					Files.copy(source, out, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}
}
