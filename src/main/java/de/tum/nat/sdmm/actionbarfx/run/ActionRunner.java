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
package de.tum.nat.sdmm.actionbarfx.run;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptInfo;
import org.scijava.thread.ThreadService;

import de.tum.nat.sdmm.actionbarfx.model.ActionSpec;

/**
 * Runs the action behind a button.
 * <p>
 * Commands, scripts and IJ1 legacy commands are all registered as
 * {@link ModuleInfo}, so all three go through {@code moduleService.run(info,
 * true)} — {@code process = true} means the normal parameter dialog appears.
 * Raw IJ1 macro strings are the one exception; they go to {@code IJ.runMacro} on
 * a worker thread, and exist mainly so macros from the legacy ActionBar port
 * over by copy-paste.
 */
public class ActionRunner {

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private LogService log;

	private final Context context;

	public ActionRunner(final Context context) {
		this.context = context;
		context.inject(this);
	}

	/**
	 * Runs an action and reports completion on a worker thread.
	 *
	 * @param barDir folder the bar lives in; script paths resolve against it
	 * @param action what to run
	 * @param onFinished called with null on success and with the failure
	 *          otherwise. Never called on the JavaFX thread, so callers must hop
	 *          back themselves.
	 */
	public void run(final File barDir, final ActionSpec action,
		final Consumer<Throwable> onFinished)
	{
		threadService.run(() -> {
			try {
				runAndWait(barDir, action);
				if (onFinished != null) onFinished.accept(null);
			}
			catch (final Throwable t) {
				log.error("Action failed: " + describe(action), t);
				if (onFinished != null) onFinished.accept(t);
			}
		});
	}

	/**
	 * Runs an action on the calling thread and blocks until it finishes. Must
	 * never be called from the JavaFX thread.
	 */
	public void runAndWait(final File barDir, final ActionSpec action)
		throws InterruptedException, ExecutionException
	{
		if (action == null) throw new ActionException("Button has no action.");
		final String type = action.getType();

		if (ActionSpec.IJ1.equals(type)) {
			runMacro(action);
			return;
		}

		final ModuleInfo info = resolve(barDir, action);
		final Future<Module> future = moduleService.run(info, true);
		if (future != null) future.get();
	}

	/**
	 * Finds the {@link ModuleInfo} for a command or script action. Resolution is
	 * deliberately lazy — it happens when a bar is opened, not when it is
	 * registered at startup, because the IJ1 legacy layer may not have finished
	 * registering its commands that early.
	 */
	public ModuleInfo resolve(final File barDir, final ActionSpec action) {
		final String type = action.getType();

		if (ActionSpec.COMMAND.equals(type)) return resolveCommand(action);
		if (ActionSpec.SCRIPT.equals(type)) return resolveScript(barDir, action);

		throw new ActionException("Unsupported action type '" + type +
			"'. This bar may have been written by a newer version of ActionBarFX.");
	}

	private ModuleInfo resolveCommand(final ActionSpec action) {
		final String className = action.getCommandClass();
		if (className != null && !className.isEmpty()) {
			final CommandInfo info = commandService.getCommand(className);
			if (info != null) return info;
		}

		// Fall back to the menu path. Only a hint: menu paths get reorganized
		// between releases, class names do not.
		final String menuPath = action.getMenuPath();
		if (menuPath != null && !menuPath.isEmpty()) {
			final ModuleInfo info = findByMenuPath(menuPath);
			if (info != null) return info;
		}

		throw new ActionException("Command not found: " + (className != null
			? className : menuPath) + ". Is the plugin providing it installed?");
	}

	private ModuleInfo findByMenuPath(final String menuPath) {
		final String wanted = normalize(menuPath);
		for (final ModuleInfo info : moduleService.getModules()) {
			if (info.getMenuPath() == null) continue;
			if (wanted.equals(normalize(info.getMenuPath().getMenuString()))) //
				return info;
		}
		return null;
	}

	private static String normalize(final String menuPath) {
		if (menuPath == null) return "";
		final StringBuilder sb = new StringBuilder();
		for (final String part : menuPath.split(">")) {
			if (sb.length() > 0) sb.append('>');
			sb.append(part.trim().toLowerCase());
		}
		return sb.toString();
	}

	private ModuleInfo resolveScript(final File barDir, final ActionSpec action) {
		final String path = action.getPath();
		if (path == null || path.isEmpty()) //
			throw new ActionException("Script action has no path.");
		final File script = new File(barDir, path);
		if (!script.isFile()) //
			throw new ActionException("Script not found: " + script);
		return new ScriptInfo(context, script);
	}

	private void runMacro(final ActionSpec action) {
		final String macro = action.getMacro();
		if (macro == null || macro.isEmpty()) //
			throw new ActionException("Macro action has no macro text.");
		try {
			ij.IJ.runMacro(macro);
		}
		catch (final Throwable t) {
			throw new ActionException("Macro failed: " + t.getMessage(), t);
		}
	}

	/** Short description used in error messages. */
	public static String describe(final ActionSpec action) {
		return action == null ? "(none)" : action.toString();
	}
}
