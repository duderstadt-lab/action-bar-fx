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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.command.CommandInfo;
import org.scijava.event.EventHandler;
import org.scijava.log.LogService;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.event.UIShownEvent;

import de.tum.nat.sdmm.actionbarfx.commands.OpenActionBarCommand;
import de.tum.nat.sdmm.actionbarfx.io.BarIO;
import de.tum.nat.sdmm.actionbarfx.io.BarLocator;
import de.tum.nat.sdmm.actionbarfx.model.BarConfig;
import de.tum.nat.sdmm.actionbarfx.ui.ActionBarWindow;
import de.tum.nat.sdmm.actionbarfx.ui.FxBootstrap;
import de.tum.nat.sdmm.actionbarfx.ui.ThemeManager;
import de.tum.nat.sdmm.actionbarfx.ui.builder.BarBuilderDialog;

/**
 * Default {@link ActionBarService}.
 * <p>
 * {@link #initialize()} runs while Fiji is building its context, which puts
 * three constraints on it:
 * <ul>
 * <li>No JavaFX. Starting the toolkit here would cost every user the FX startup
 * whether or not they open a bar, and would break headless runs. It happens
 * lazily on the first {@link #open(File)}.</li>
 * <li>No command resolution. The IJ1 legacy layer may not have registered its
 * commands yet, so {@code action.class} is resolved when a bar is opened, not
 * when it is registered. Registering {@link CommandInfo} objects with the
 * {@link ModuleService} is safe at any time.</li>
 * <li>Never throw. A malformed {@code bar.json} in the scan directory is logged
 * and skipped; an exception here would take down context creation and, with it,
 * Fiji.</li>
 * </ul>
 */
@Plugin(type = Service.class)
public class DefaultActionBarService extends AbstractService implements
	ActionBarService
{

	private static final String STARTUP_BARS_KEY = "startupBars";

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private ScriptService scriptService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private LogService log;

	/** Bar folder canonical path to window. */
	private final Map<String, ActionBarWindow> openBars =
		new ConcurrentHashMap<>();

	/** Menu entries we registered, so a rescan can replace them. */
	private final List<ModuleInfo> registered = Collections.synchronizedList(
		new ArrayList<>());

	private final AtomicBoolean startupBarsOpened = new AtomicBoolean();

	// -- Service methods --

	@Override
	public void initialize() {
		try {
			discoverBars();
		}
		catch (final Throwable t) {
			log.warn("ActionBarFX could not scan " + getScanDirectory(), t);
		}
		try {
			// Lets a script declare '#@ ActionBarService actionBars'.
			scriptService.addAlias(ActionBarService.class);
		}
		catch (final Throwable t) {
			log.warn("ActionBarFX could not register its script alias.", t);
		}
	}

	@Override
	public void dispose() {
		if (!FxBootstrap.isStarted()) return;
		final List<ActionBarWindow> windows = new ArrayList<>(openBars.values());
		openBars.clear();
		FxBootstrap.runOnFx(() -> windows.forEach(ActionBarWindow::close));
	}

	// -- ActionBarService methods --

	@Override
	public void open(final File barFileOrDir) {
		final File barDir = BarIO.barDirectory(barFileOrDir);
		final String key = key(barDir);

		final ActionBarWindow existing = openBars.get(key);
		if (existing != null) {
			FxBootstrap.runOnFx(existing::show);
			return;
		}

		final BarConfig config;
		try {
			config = BarIO.load(barDir);
		}
		catch (final IOException | RuntimeException e) {
			log.error("Could not read " + BarIO.barFile(barDir), e);
			return;
		}

		ThemeManager.setPrefService(prefService);
		FxBootstrap.runOnFx(() -> {
			final ActionBarWindow window = new ActionBarWindow(context(), barDir,
				config);
			window.setOnClosed(() -> openBars.remove(key));
			openBars.put(key, window);
			window.show();
		});
	}

	@Override
	public void close(final File barFileOrDir) {
		final ActionBarWindow window = openBars.remove(key(BarIO.barDirectory(
			barFileOrDir)));
		if (window != null) FxBootstrap.runOnFx(window::close);
	}

	@Override
	public void reload(final File barFileOrDir) {
		final ActionBarWindow window = openBars.get(key(BarIO.barDirectory(
			barFileOrDir)));
		if (window != null) FxBootstrap.runOnFx(window::reload);
	}

	@Override
	public boolean isOpen(final File barFileOrDir) {
		return openBars.containsKey(key(BarIO.barDirectory(barFileOrDir)));
	}

	@Override
	public Collection<File> getOpenBars() {
		final List<File> bars = new ArrayList<>();
		for (final ActionBarWindow window : openBars.values())
			bars.add(window.getBarDirectory());
		return bars;
	}

	@Override
	public void edit(final File barFileOrDir) {
		final File barDir = BarIO.barDirectory(barFileOrDir);
		final BarConfig config;
		try {
			config = BarIO.load(barDir);
		}
		catch (final IOException | RuntimeException e) {
			log.error("Could not read " + BarIO.barFile(barDir), e);
			return;
		}
		ThemeManager.setPrefService(prefService);
		FxBootstrap.runOnFx(() -> {
			final ActionBarWindow window = openBars.get(key(barDir));
			new BarBuilderDialog(context(), barDir, window != null ? window.getPane()
				.getConfig() : config, window).show();
		});
	}

	@Override
	public void createBar(final File barDir, final String title) {
		final BarConfig config = new BarConfig();
		config.setTitle(title == null || title.trim().isEmpty() ? barDir.getName()
			: title.trim());
		try {
			BarIO.save(config, barDir);
			final File scripts = new File(barDir, "scripts");
			if (!scripts.isDirectory() && !scripts.mkdirs()) //
				log.warn("Could not create " + scripts);
		}
		catch (final IOException e) {
			log.error("Could not create bar in " + barDir, e);
			return;
		}
		discoverBars();
		ThemeManager.setPrefService(prefService);
		FxBootstrap.runOnFx(() -> new BarBuilderDialog(context(), barDir, config,
			null).show());
	}

	@Override
	public File getScanDirectory() {
		return BarLocator.scanDirectory(context());
	}

	@Override
	public void discoverBars() {
		// Drop the entries from the previous scan first, so renamed or removed
		// bars do not leave stale menu items behind.
		synchronized (registered) {
			for (final ModuleInfo info : registered)
				moduleService.removeModule(info);
			registered.clear();
		}

		for (final File barDir : BarLocator.findAllBars(BarLocator.scanDirectories(
			context())))
		{
			try {
				registerBarMenuEntry(barDir);
			}
			catch (final Throwable t) {
				log.warn("Skipping action bar " + barDir.getName() + ": " + t
					.getMessage());
			}
		}
	}

	/**
	 * One runtime module per discovered bar, the same pattern ImageJ uses for its
	 * "Open Recent" menu.
	 */
	private void registerBarMenuEntry(final File barDir) {
		final CommandInfo info = new CommandInfo(OpenActionBarCommand.class);

		final Map<String, Object> presets = new LinkedHashMap<>();
		presets.put("barFile", barDir);
		info.setPresets(presets);

		final MenuPath path = new MenuPath();
		path.add(new MenuEntry("Plugins"));
		path.add(new MenuEntry("Action Bars"));
		path.add(new MenuEntry(BarLocator.barName(barDir)));
		info.setMenuPath(path);

		moduleService.addModule(info);
		registered.add(info);
	}

	@Override
	public List<File> getStartupBars() {
		final String value = prefService.get(ActionBarService.class,
			STARTUP_BARS_KEY, "");
		final List<File> bars = new ArrayList<>();
		if (value == null || value.isEmpty()) return bars;
		for (final String path : value.split(File.pathSeparator))
			if (!path.trim().isEmpty()) bars.add(new File(path.trim()));
		return bars;
	}

	@Override
	public boolean isStartupBar(final File barFileOrDir) {
		final String key = key(BarIO.barDirectory(barFileOrDir));
		for (final File bar : getStartupBars())
			if (key.equals(key(bar))) return true;
		return false;
	}

	@Override
	public void addStartupBar(final File barFileOrDir) {
		final File barDir = BarIO.barDirectory(barFileOrDir);
		if (isStartupBar(barDir)) return;
		final List<File> bars = getStartupBars();
		bars.add(barDir);
		setStartupBars(bars);
	}

	@Override
	public void removeStartupBar(final File barFileOrDir) {
		final String key = key(BarIO.barDirectory(barFileOrDir));
		final List<File> bars = getStartupBars();
		bars.removeIf(bar -> key.equals(key(bar)));
		setStartupBars(bars);
	}

	private void setStartupBars(final List<File> bars) {
		final StringBuilder sb = new StringBuilder();
		for (final File bar : bars) {
			if (sb.length() > 0) sb.append(File.pathSeparator);
			sb.append(bar.getAbsolutePath());
		}
		prefService.put(ActionBarService.class, STARTUP_BARS_KEY, sb.toString());
	}

	// -- Event handlers --

	/**
	 * Startup bars need JavaFX and a visible UI, so they wait for the UI rather
	 * than opening from {@link #initialize()}.
	 */
	@EventHandler
	protected void onEvent(final UIShownEvent event) {
		if (!startupBarsOpened.compareAndSet(false, true)) return;
		for (final File bar : getStartupBars()) {
			if (!BarIO.barFile(bar).isFile()) {
				log.warn("Startup action bar no longer exists: " + bar);
				continue;
			}
			open(bar);
		}
	}

	// -- Helper methods --

	private String key(final File barDir) {
		try {
			return barDir.getCanonicalPath();
		}
		catch (final IOException e) {
			return barDir.getAbsolutePath();
		}
	}
}
