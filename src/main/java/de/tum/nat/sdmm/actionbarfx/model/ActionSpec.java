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
package de.tum.nat.sdmm.actionbarfx.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What a button does when it is clicked.
 * <p>
 * The type is kept as a plain string rather than an enum so that an action type
 * introduced by a later version round-trips instead of failing the whole bar to
 * load. An unknown type is reported when the button is pressed, not when the bar
 * is opened.
 */
public class ActionSpec extends Extensible {

	/** An ImageJ2 command or IJ1 legacy command, resolved by class name. */
	public static final String COMMAND = "command";

	/** A script file inside the bar folder. */
	public static final String SCRIPT = "script";

	/** An IJ1 macro string, run through {@code IJ.runMacro}. */
	public static final String IJ1 = "ij1";

	private String type = COMMAND;
	private String commandClass;
	private String menuPath;
	private String path;
	private String macro;

	public ActionSpec() {}

	public static ActionSpec command(final String commandClass,
		final String menuPath)
	{
		final ActionSpec spec = new ActionSpec();
		spec.setType(COMMAND);
		spec.setCommandClass(commandClass);
		spec.setMenuPath(menuPath);
		return spec;
	}

	public static ActionSpec script(final String path) {
		final ActionSpec spec = new ActionSpec();
		spec.setType(SCRIPT);
		spec.setPath(path);
		return spec;
	}

	public static ActionSpec ij1(final String macro) {
		final ActionSpec spec = new ActionSpec();
		spec.setType(IJ1);
		spec.setMacro(macro);
		return spec;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	/**
	 * Fully qualified class name of the command. Resolved first; {@link
	 * #getMenuPath()} is only a fallback, because menu paths get reorganized
	 * between releases and class names do not.
	 */
	@JsonProperty("class")
	public String getCommandClass() {
		return commandClass;
	}

	@JsonProperty("class")
	public void setCommandClass(final String commandClass) {
		this.commandClass = commandClass;
	}

	/** For example {@code Plugins>Mars>Image>Peak Finder}. */
	public String getMenuPath() {
		return menuPath;
	}

	public void setMenuPath(final String menuPath) {
		this.menuPath = menuPath;
	}

	/** Script path, relative to the bar folder, so the folder stays portable. */
	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public String getMacro() {
		return macro;
	}

	public void setMacro(final String macro) {
		this.macro = macro;
	}

	public ActionSpec copy() {
		final ActionSpec copy = new ActionSpec();
		copy.type = type;
		copy.commandClass = commandClass;
		copy.menuPath = menuPath;
		copy.path = path;
		copy.macro = macro;
		copy.getExtras().putAll(getExtras());
		return copy;
	}

	/** Short human-readable description, used in the builder list. */
	@Override
	public String toString() {
		if (SCRIPT.equals(type)) return "script: " + path;
		if (IJ1.equals(type)) return "macro";
		if (COMMAND.equals(type)) {
			if (menuPath != null) return menuPath;
			if (commandClass != null) return commandClass;
			return "command";
		}
		return type == null ? "?" : type;
	}
}
