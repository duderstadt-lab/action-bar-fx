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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.scijava.Context;
import org.scijava.app.AppService;

import de.tum.nat.sdmm.actionbarfx.model.BarConfig;

/** Finds bar folders on disk. */
public final class BarLocator {

	/** Folder inside the Fiji installation that is scanned at startup. */
	public static final String SCAN_DIR_NAME = "action-bars";

	/**
	 * Spellings of the scan folder that are accepted, in order of preference.
	 * The documented one is {@code action-bars}; {@code ActionBars} is accepted
	 * too because it is the obvious thing to type, and a bar sitting in a folder
	 * that is nearly right should not silently fail to appear in the menu.
	 */
	private static final String[] SCAN_DIR_NAMES = { SCAN_DIR_NAME, "ActionBars" };

	private BarLocator() {}

	/**
	 * The scan folder in use: the first accepted spelling that exists, otherwise
	 * the documented one. Falls back to the {@code ij.dir} property and then the
	 * working directory when no {@link AppService} is available, so this also
	 * does something sensible outside a full Fiji.
	 */
	public static File scanDirectory(final Context context) {
		final File base = baseDirectory(context);
		for (final String name : SCAN_DIR_NAMES) {
			final File candidate = new File(base, name);
			if (candidate.isDirectory()) return candidate;
		}
		return new File(base, SCAN_DIR_NAME);
	}

	/** Every accepted scan folder that exists. */
	public static List<File> scanDirectories(final Context context) {
		final File base = baseDirectory(context);
		final List<File> dirs = new ArrayList<>();
		for (final String name : SCAN_DIR_NAMES) {
			final File candidate = new File(base, name);
			if (candidate.isDirectory()) dirs.add(candidate);
		}
		return dirs;
	}

	private static File baseDirectory(final Context context) {
		if (context != null) {
			final AppService appService = context.getService(AppService.class);
			if (appService != null && appService.getApp() != null) {
				final File base = appService.getApp().getBaseDirectory();
				if (base != null) return base;
			}
		}
		final String ijDir = System.getProperty("ij.dir");
		if (ijDir != null && !ijDir.isEmpty()) return new File(ijDir);
		return new File(System.getProperty("user.dir", "."));
	}

	/**
	 * Immediate subfolders of {@code scanDir} that contain a {@code bar.json},
	 * sorted by folder name. Missing or unreadable directories yield an empty
	 * list rather than an exception, because this runs during service
	 * initialization.
	 */
	public static List<File> findBars(final File scanDir) {
		final List<File> bars = new ArrayList<>();
		if (scanDir == null || !scanDir.isDirectory()) return bars;
		final File[] children = scanDir.listFiles();
		if (children == null) return bars;
		Arrays.sort(children, Comparator.comparing(File::getName,
			String.CASE_INSENSITIVE_ORDER));
		for (final File child : children)
			if (child.isDirectory() && new File(child, BarIO.BAR_FILE).isFile()) //
				bars.add(child);
		return bars;
	}

	/**
	 * Bars across several scan folders. Deliberately not an overload of
	 * {@link #findBars(File)}: the two would be ambiguous for a null argument.
	 */
	public static List<File> findAllBars(final List<File> scanDirs) {
		final List<File> bars = new ArrayList<>();
		if (scanDirs == null) return bars;
		for (final File scanDir : scanDirs)
			bars.addAll(findBars(scanDir));
		return bars;
	}

	/**
	 * Menu name for a bar: the title from {@code bar.json}, falling back to the
	 * folder name if the file cannot be read. Never throws.
	 */
	public static String barName(final File barDir) {
		try {
			final BarConfig config = BarIO.load(barDir);
			final String title = config.getTitle();
			if (title != null && !title.trim().isEmpty()) return title.trim();
		}
		catch (final IOException | RuntimeException e) {
			// fall through to the folder name
		}
		return barDir.getName();
	}
}
