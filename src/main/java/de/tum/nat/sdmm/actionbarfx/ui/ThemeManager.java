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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

import org.scijava.prefs.PrefService;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Light and dark theme for all open bars.
 * <p>
 * The AtlantaFX Primer theme provides the base look through the user agent
 * stylesheet; {@code base.css} plus the theme file are layered on each bar
 * scene. The choice is remembered with {@link PrefService} when a SciJava
 * context has handed one over, and with plain Java preferences otherwise (the
 * standalone preview launcher, for instance).
 */
public final class ThemeManager {

	public enum Theme {
			LIGHT, DARK
	}

	private static final String BASE_CSS =
		"/de/tum/nat/sdmm/actionbarfx/css/base.css";
	private static final String LIGHT_CSS =
		"/de/tum/nat/sdmm/actionbarfx/css/light.css";
	private static final String DARK_CSS =
		"/de/tum/nat/sdmm/actionbarfx/css/dark.css";

	private static final String PREF_KEY = "theme";

	private static final Set<Scene> SCENES = Collections.newSetFromMap(
		new WeakHashMap<>());

	private static PrefService prefService;
	private static Theme theme;

	private ThemeManager() {}

	/** Hands the manager the SciJava preferences store. */
	public static synchronized void setPrefService(final PrefService service) {
		prefService = service;
		theme = null; // re-read from the new store on next access
	}

	public static synchronized Theme getTheme() {
		if (theme == null) theme = readPreference();
		return theme;
	}

	public static synchronized boolean isDark() {
		return getTheme() == Theme.DARK;
	}

	/** Switches theme, remembers it, and restyles every open bar. */
	public static synchronized void setTheme(final Theme newTheme) {
		if (newTheme == getTheme()) return;
		theme = newTheme;
		writePreference(newTheme);
		for (final Scene scene : SCENES)
			style(scene);
	}

	public static synchronized void toggle() {
		setTheme(isDark() ? Theme.LIGHT : Theme.DARK);
	}

	/**
	 * Applies the current theme to a scene and keeps a weak reference to it so
	 * later theme switches reach it too.
	 */
	public static synchronized void apply(final Scene scene) {
		final boolean isNew = SCENES.add(scene);
		style(scene);
		if (isNew) defend(scene);
	}

	public static synchronized void forget(final Scene scene) {
		SCENES.remove(scene);
	}

	/**
	 * Applies the current stylesheets to a node without tracking it. For
	 * transient things like the command and icon pickers, which are gone long
	 * before anyone switches theme.
	 */
	public static synchronized void applyStylesheets(final Parent parent) {
		parent.getStylesheets().setAll(url(BASE_CSS), url(isDark() ? DARK_CSS
			: LIGHT_CSS));

		// A dialog has no scene until it is shown, so wait for one.
		if (parent.getScene() != null) applyBaseTheme(parent.getScene());
		else parent.sceneProperty().addListener((obs, old, scene) -> {
			if (scene != null) applyBaseTheme(scene);
		});
	}

	private static void style(final Scene scene) {
		applyBaseTheme(scene);
		scene.getStylesheets().setAll(url(BASE_CSS), url(isDark() ? DARK_CSS
			: LIGHT_CSS));
	}

	/**
	 * Puts our stylesheets back if something else replaces them.
	 * <p>
	 * A scene's stylesheet list belongs to whoever created the window, but Fiji
	 * is one JVM shared with other JavaFX plugins, and a plugin that themes
	 * "every open Stage" will clear this one too — leaving a bar with no styling
	 * at all and someone else's CSS resolving against the wrong base theme. Ours
	 * is the only window we can speak for, so we put it back.
	 * <p>
	 * This cannot ping-pong: it only reacts when the list no longer holds what we
	 * put there, and restoring it is what we would have set anyway.
	 */
	private static void defend(final Scene scene) {
		scene.getStylesheets().addListener((ListChangeListener<String>) change -> {
			final List<String> wanted = Arrays.asList(url(BASE_CSS), url(isDark()
				? DARK_CSS : LIGHT_CSS));
			if (scene.getStylesheets().equals(wanted)) return;
			// Re-entrant edits of an observable list under its own listener are
			// not allowed, so restore on the next pulse.
			Platform.runLater(() -> {
				if (!scene.getStylesheets().equals(wanted)) {
					applyBaseTheme(scene);
					scene.getStylesheets().setAll(wanted);
				}
			});
		});
	}

	/**
	 * Puts the AtlantaFX base theme on one scene.
	 * <p>
	 * Deliberately {@code Scene.setUserAgentStylesheet} and never
	 * {@code Application.setUserAgentStylesheet}: the latter is global to the
	 * JVM, and a bar is a guest in a Fiji full of other people's JavaFX windows.
	 * Setting it globally replaced Modena everywhere the moment a bar opened,
	 * which broke every stylesheet written against Modena — mars-fx's among them,
	 * with "Could not resolve '-fx-text-base-color'" and windows that changed
	 * appearance. Per scene, ActionBarFX styles only its own windows.
	 */
	private static void applyBaseTheme(final Scene scene) {
		scene.setUserAgentStylesheet(isDark() ? new PrimerDark()
			.getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
	}

	private static String url(final String resource) {
		return ThemeManager.class.getResource(resource).toExternalForm();
	}

	private static Theme readPreference() {
		final String value = prefService != null ? prefService.get(
			ThemeManager.class, PREF_KEY, Theme.LIGHT.name()) : Preferences
				.userNodeForPackage(ThemeManager.class).get(PREF_KEY, Theme.LIGHT
					.name());
		try {
			return Theme.valueOf(value);
		}
		catch (final IllegalArgumentException e) {
			return Theme.LIGHT;
		}
	}

	private static void writePreference(final Theme value) {
		if (prefService != null) prefService.put(ThemeManager.class, PREF_KEY, value
			.name());
		else Preferences.userNodeForPackage(ThemeManager.class).put(PREF_KEY, value
			.name());
	}
}
