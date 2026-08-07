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
package de.tum.nat.sdmm.actionbarfx.legacy;

import java.awt.EventQueue;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.scijava.log.LogService;

import ij.IJ;
import ij.ImageJ;
import ij.Menus;

/**
 * Puts the discovered bars into Fiji's own menu bar.
 * <p>
 * Registering a {@code CommandInfo} with the {@link
 * org.scijava.module.ModuleService} is enough for the ImageJ2 UI, but Fiji
 * shows the IJ1 AWT menu bar, and imagej-legacy builds that once at startup from
 * {@code commandService.getCommandsOfType(...)} — the annotated commands in the
 * plugin index. Modules added at runtime are not in that list, so a bar
 * registered this way never appears, however early it is registered. On top of
 * that the map it builds is keyed by {@code CommandInfo.getIdentifier()}, so
 * several entries sharing one command class would collide anyway.
 * <p>
 * Hence this: the submenu is built directly as AWT menu items. Everything here
 * degrades to a no-op when there is no IJ1 user interface, which is what happens
 * headless.
 */
public final class IJ1MenuBridge {

	/** Submenu under Plugins that lists the discovered bars. */
	public static final String SUBMENU = "Action Bars";

	private static final String PLUGINS_MENU = "Plugins";

	private IJ1MenuBridge() {}

	/** Whether an IJ1 user interface exists to add menu items to. */
	public static boolean isAvailable() {
		return IJ.getInstance() != null;
	}

	/**
	 * Rebuilds {@code Plugins > Action Bars} from the given bar folders. Safe to
	 * call from any thread and at any time; does nothing when Fiji's UI is not up.
	 *
	 * @param bars bar folders to list
	 * @param namer menu label for a bar folder
	 * @param onOpen invoked on the AWT thread when an entry is chosen
	 */
	public static void refresh(final List<File> bars,
		final Function<File, String> namer, final Consumer<File> onOpen,
		final LogService log)
	{
		final ImageJ ij = IJ.getInstance();
		if (ij == null) return;

		EventQueue.invokeLater(() -> {
			try {
				install(ij, bars, namer, onOpen, log);
			}
			catch (final Throwable t) {
				// A menu we could not build is not worth taking anything else down
				// for.
				log.warn("ActionBarFX could not add its entries to the Fiji menu bar.",
					t);
			}
		});
	}

	private static void install(final ImageJ ij, final List<File> bars,
		final Function<File, String> namer, final Consumer<File> onOpen,
		final LogService log)
	{
		MenuBar menuBar = ij.getMenuBar();
		if (menuBar == null) menuBar = Menus.getMenuBar();
		if (menuBar == null) {
			log.warn("ActionBarFX: no IJ1 menu bar to add to.");
			return;
		}

		final Menu plugins = findMenu(menuBar);
		if (plugins == null) {
			log.warn("ActionBarFX: no '" + PLUGINS_MENU +
				"' menu in the Fiji menu bar; skipping the " + SUBMENU + " submenu.");
			return;
		}

		Menu submenu = findSubmenu(plugins);
		if (submenu == null) {
			submenu = new Menu(SUBMENU);
			plugins.add(submenu);
		}
		submenu.removeAll();

		for (final File bar : bars) {
			final MenuItem item = new MenuItem(namer.apply(bar));
			item.addActionListener(e -> onOpen.accept(bar));
			submenu.add(item);
		}

		if (bars.isEmpty()) {
			// An empty submenu looks broken; say why it is empty.
			final MenuItem none = new MenuItem("(no bars found)");
			none.setEnabled(false);
			submenu.add(none);
		}
	}

	private static Menu findMenu(final MenuBar menuBar) {
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			final Menu menu = menuBar.getMenu(i);
			if (menu != null && PLUGINS_MENU.equals(menu.getLabel())) return menu;
		}
		return null;
	}

	private static Menu findSubmenu(final Menu plugins) {
		for (int i = 0; i < plugins.getItemCount(); i++) {
			final MenuItem item = plugins.getItem(i);
			if (item instanceof Menu && SUBMENU.equals(item.getLabel())) //
				return (Menu) item;
		}
		return null;
	}
}
