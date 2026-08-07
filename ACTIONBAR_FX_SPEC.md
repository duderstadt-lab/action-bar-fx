# ActionBarFX — implementation brief

A standalone Fiji/SciJava plugin that renders a vertical bar of colored buttons, each
launching an ImageJ command or a script. JavaFX + AtlantaFX. Replaces the legacy
ImageJ ActionBar plugin.

Reference: the FRET Power Tools bar from the Mars docs
(https://duderstadt-lab.github.io/mars-docs/tutorials/fretActionBar/).

---

## 1. Scope of v1

In scope:

- Vertical bar window, one button per row: color swatch background, Ikonli icon, label,
  optional step-number badge, optional separators between groups.
- Light and dark theme, switchable at runtime, remembered in prefs.
- Bar-level color palettes (coordinated sets of 7–15 colors) with per-button override.
- A builder dialog to create and edit bars: pick command or script, label, color, icon,
  step number, with live preview.
- Bars stored as self-contained folders so they can be zipped and shared.
- Bars in a known directory appear automatically under `Plugins > Action Bars > …`.

Out of scope for v1: horizontal/grid layouts, animated icons, drag-to-reorder in the bar
itself (reordering happens in the builder), popup/always-on-top bars, custom CSS injection.

---

## 2. Project setup

Standalone repository, not part of mars-fx.

- Parent POM: `org.scijava:pom-scijava`
- `groupId`: `de.tum.nat.sdmm`, `artifactId`: `actionbar-fx`
- Base package: `de.tum.nat.sdmm.actionbarfx`
  (TUM School of Natural Sciences, Structure and Dynamics of Molecular Machines)
- Dependencies:
  - `org.scijava:scijava-common`
  - `net.imagej:imagej-legacy` (scope: provided)
  - JavaFX (controls, graphics) — match the version mars-fx ships to avoid a second
    JavaFX on the classpath
  - `io.github.mkpaz:atlantafx-base:2.1.0` (requires JavaFX 17+)
  - `org.kordamp.ikonli:ikonli-javafx` + `ikonli-materialdesign2-pack` (BOM 12.4.0, same
    as mars-fx)
  - `com.fasterxml.jackson.core:jackson-databind`
  - `fr.brouillard.oss:cssfx` (scope: provided, dev only) for live stylesheet reload
- Java 8 source/target is NOT required; match whatever mars-fx targets.

**JavaFX baseline: 23**, which is what Fiji currently ships. That means CSS transitions
are available, so hover animation is pure stylesheet:

```css
.action-button { transition: -fx-scale-x 130ms ease-out, -fx-scale-y 130ms ease-out; }
.action-button:hover { -fx-scale-x: 1.02; -fx-scale-y: 1.02; }
```

Two caveats. Background and border interpolation only became available in JavaFX 24, so
on 23 the `-fx-background-color` change on hover snaps rather than fades — fine, and
arguably better for a tool clicked hundreds of times a day. And transitions only work for
properties defined by `<number>` or `<paint>`; `-fx-padding`, `-fx-border-width` and
`-fx-font-size` cannot be transitioned on any version, so don't design an effect that
needs them.

Keep the effect in a `HoverEffect` helper (even though it's currently just a style class)
so that if Fiji moves to a newer JavaFX and richer transitions become worthwhile, there's
one place to change.

**Reuse from mars-fx:** the JavaFX toolkit bootstrap inside Fiji (starting the FX thread
alongside ImageJ's AWT EDT) is already solved there. Lift that pattern rather than
reinventing it.

---

## 3. Bar format

A bar is a directory:

```
fret-power-tools/
  bar.json
  scripts/
    01_add_tags.groovy
    02_profile_correction.groovy
  icons/          (optional, for custom PNG/SVG icons)
```

`bar.json`:

```json
{
  "formatVersion": 1,
  "title": "FRET power tools",
  "palette": "okabe-ito",
  "width": 310,
  "items": [
    {
      "type": "button",
      "label": "Beam profile corrector",
      "paletteIndex": 0,
      "icon": "mdi2f-focus-field",
      "action": {
        "type": "command",
        "class": "de.mpg.biochem.mars.image.commands.BeamProfileCorrectionCommand",
        "menuPath": "Plugins>Mars>Image>Beam Profile Corrector"
      }
    },
    { "type": "separator" },
    {
      "type": "button",
      "label": "Add molecule tags",
      "step": 1,
      "paletteIndex": 5,
      "color": "#7C89B4",
      "icon": "mdi2t-tag-outline",
      "action": { "type": "script", "path": "scripts/01_add_tags.groovy" }
    },
    {
      "type": "button",
      "label": "Close log",
      "action": { "type": "ij1", "macro": "if (isOpen(\"Log\")) { selectWindow(\"Log\"); run(\"Close\"); }" }
    }
  ]
}
```

Rules:

- `paletteIndex` is the source of truth for color. `color` is an optional hex override.
  Switching the bar's palette recolors everything that has no override.
- `script.path` is relative to the bar folder, so the folder stays portable.
- `action.class` is resolved first; `menuPath` is a fallback and a human-readable hint.
  Menu paths get reorganized between releases; class names don't.
- `step` renders the numbered badge. Buttons without `step` still reserve the badge slot
  so labels stay left-aligned across the bar.
- Unknown fields must be preserved on round-trip (configure Jackson with
  `@JsonAnySetter`/`@JsonAnyGetter`) so a newer bar opened in an older build isn't
  silently stripped.

---

## 4. Palettes

Ship as a resource `palettes.json`. Default is `okabe-ito`.

| id | name | n | notes |
|---|---|---|---|
| `okabe-ito` | Okabe–Ito | 8 | colorblind-safe, the default |
| `tol-bright` | Tol bright | 7 | |
| `tableau` | Tableau 10 | 10 | familiar, not fully CVD-safe |
| `tol-muted` | Tol muted | 10 | |
| `tol-extended` | Tol extended | 15 | past ~12, rely on badges/separators too |
| `legacy-pastel` | Original pastels | 11 | matches the existing FRET bar |

```
okabe-ito:     #E69F00 #56B4E9 #009E73 #F0E442 #0072B2 #D55E00 #CC79A7 #999999
tol-bright:    #4477AA #EE6677 #228833 #CCBB44 #66CCEE #AA3377 #BBBBBB
tableau:       #4E79A7 #F28E2B #E15759 #76B7B2 #59A14F #EDC948 #B07AA1 #FF9DA7 #9C755F #BAB0AC
tol-muted:     #332288 #88CCEE #44AA99 #117733 #999933 #DDCC77 #CC6677 #882255 #AA4499 #777777
legacy-pastel: #BFB1AE #F2E8B4 #F4AC6A #E58177 #9AA396 #7C89B4 #A26A64 #A8D3EC #E8DC50 #87CFA9 #CBB5DC
```

Each button stores ONE base color. All visual states derive from it in CSS so a single
hue works in both themes:

```css
.action-button {
    -fx-background-color: -button-accent;
    -fx-text-fill: ladder(-button-accent, white 49%, black 50%);
    -fx-background-radius: 7;
    -fx-padding: 11 14 11 14;
}
.action-button:hover   { -fx-background-color: derive(-button-accent, 12%); }
.action-button:pressed { -fx-background-color: derive(-button-accent, -12%); }
```

`ladder()` picks the text color automatically, so no second color list is needed for
contrast. In `dark.css`, do NOT remap hues — the color coding is identity. Darken
instead, roughly `derive(base, -45%)`, with text lightened from the same hue.

`-button-accent` is set per button as an inline style
(`setStyle("-button-accent: " + hex)`), everything else comes from the stylesheet.

---

## 5. Running actions

Everything in Fiji's menus — IJ1 legacy commands, ImageJ2 `Command` plugins, and script
files — is registered as a `ModuleInfo`. One code path handles all of them.

```java
@Parameter private ModuleService moduleService;
@Parameter private MenuService   menuService;
@Parameter private ScriptService scriptService;

// process=true runs pre/post-processors, i.e. the normal parameter dialog appears
Future<Module> f = moduleService.run(info, true);
```

- Commands: resolve `action.class` via `commandService.getCommand(className)`, falling
  back to a scan of `moduleService.getModules()` matching `menuPath`.
- Scripts: `new ScriptInfo(context, new File(barDir, action.path))` — also a `ModuleInfo`,
  so it goes through the same `moduleService.run`.
- IJ1 macros: `IJ.run(...)` on a background thread. Support this mainly so the existing
  FRET bar's macro strings port over by copy-paste.

Threading: `moduleService.run` dispatches through `ThreadService`, so it is already off
the FX thread. Never call `Future.get()` on the FX thread. Bind a small busy indicator on
the button to the running future, and disable that one button until it completes.

Errors must surface in the bar (a toast or an inline error state on the button), not only
in the ImageJ console.

---

## 6. Menu browsing for the builder

`MenuService` exposes the whole Fiji menu as a UI-agnostic tree:

```java
ShadowMenu root = menuService.getMenu();
// recurse root.getChildren()
// leaf.getModuleInfo()                     -> the ModuleInfo to store
// leaf.getMenuPath().getMenuString()       -> "Plugins>Mars>Molecule>Peak Finder"
```

Render as a `TreeView<ShadowMenu>` with a filter TextField that flattens to matching
leaves while typing. On selection, prefill the label from `info.getTitle()`.

---

## 7. Launching bars

**Explicit open** — always available:

```java
@Plugin(type = Command.class, menuPath = "Plugins>Action Bar FX>Open bar...")
public class OpenActionBarCommand implements Command {
    @Parameter private File barFile;
    @Parameter private ActionBarService bars;
    @Override public void run() { bars.open(barFile); }
}
```

Plus `Plugins>Action Bar FX>New bar...` opening the builder.

**Auto-discovered bars** — scan `Fiji.app/action-bars/*/bar.json` and register one
runtime module per bar, so each appears under `Plugins > Action Bars > <name>`. This is
the same pattern ImageJ uses for its "Open Recent" menu:

```java
CommandInfo info = new CommandInfo(OpenActionBarCommand.class);
info.setPresets(Collections.singletonMap("barFile", barFile));

MenuPath path = new MenuPath();
path.add(new MenuEntry("Plugins"));
path.add(new MenuEntry("Action Bars"));
path.add(new MenuEntry(barName));
info.setMenuPath(path);

moduleService.addModule(info);
```

**Where the scan runs: `ActionBarService.initialize()`.** Fiji creates its `Context` with
all discoverable services, so a `@Plugin(type = Service.class)` is instantiated at
startup, its `@Parameter` services are injected first, and then `initialize()` is called.
This is the same hook `MoleculeArchiveService` uses in mars-core.

```java
@Plugin(type = Service.class)
public class DefaultActionBarService extends AbstractService implements ActionBarService {

    @Parameter private ModuleService moduleService;
    @Parameter private ScriptService scriptService;
    @Parameter private PrefService prefService;
    @Parameter private LogService log;

    @Override
    public void initialize() {
        registerDiscoveredBars();
        scriptService.addAlias(ActionBarService.class);
    }
}
```

with `public interface ActionBarService extends ImageJService` (`net.imagej.ImageJService`,
as in Mars) so it is discovered as a service rather than a bare plugin.

Three constraints on what `initialize()` may do:

- **No JavaFX.** Do not start the FX toolkit here. It would slow Fiji startup for every
  user whether or not they open a bar, and it would break headless runs. `initialize()`
  registers menu entries only; the FX bootstrap happens lazily on the first `open()`.
- **No command resolution.** `initialize()` runs early, and the IJ1 legacy layer may not
  have finished registering its commands yet. Resolve `action.class` lazily when a bar is
  opened, not when it is registered. Registering `CommandInfo` objects with
  `moduleService` is safe at any time.
- **Never throw.** A malformed `bar.json` in the scan directory must log a warning and be
  skipped. An exception here would take down context creation and, with it, Fiji.

The `scriptService.addAlias` line is worth having for the same reason Mars does it — it
lets a Groovy script declare `#@ ActionBarService actionBars` and open or reload a bar
programmatically, which fits how the group already works.

**Startup bars** — a `PrefService`-backed list of bar paths to reopen once the UI is up,
editable from the builder ("open this bar when Fiji starts"). Trigger these from an
`@EventHandler` on `UIShownEvent` rather than from `initialize()`, since they do need
JavaFX and a visible UI.

---

## 8. Suggested class layout

```
de.tum.nat.sdmm.actionbarfx
  ActionBarService            (interface, extends net.imagej.ImageJService)
  DefaultActionBarService     (@Plugin(type = Service.class))
  model/        BarConfig, BarItem, ButtonSpec, ActionSpec, Palette, PaletteRegistry
  io/           BarIO (Jackson load/save), BarLocator (scan action-bars dir)
  run/          ActionRunner (command | script | ij1 -> ModuleInfo -> run)
  ui/           ActionBarWindow, ActionBarPane, ActionButton, HoverEffect, ThemeManager
  ui.builder/   BarBuilderDialog, CommandPickerPane, IconPickerPane, ButtonEditorPane
  commands/     OpenActionBarCommand, NewActionBarCommand
resources/
  palettes.json
  css/base.css, css/light.css, css/dark.css
```

`ThemeManager` sets the AtlantaFX base
(`Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet())`, or
`PrimerDark`) and then layers `base.css` + the theme file on each bar's `Scene`. Persist
the choice via `PrefService`.

---

## 9. Build order

1. Model + Jackson IO + `palettes.json`. Round-trip a handwritten `bar.json` in a test.
2. `ActionButton` and `ActionBarPane` with hardcoded config; get light/dark + palettes
   looking right in a plain JavaFX launcher, no Fiji involved. Wire CSSFX here.
3. `ActionRunner` and `OpenActionBarCommand`. Prove a real Mars command and a real Groovy
   script both launch from a button inside Fiji.
4. `ActionBarService` with auto-discovery and the `Plugins > Action Bars` menu. Verify
   that a corrupt `bar.json` in the scan directory produces a log warning and nothing
   worse, and that Fiji still starts headless.
5. The builder dialog.
6. Port the existing FRET Power Tools bar as the shipped example and dogfood it.

Milestone 3 is where the risk is. Do it before investing in the builder UI.

---

## 10. Acceptance check

Port the FRET Power Tools bar and confirm: all 11 buttons launch, the 6 protocol steps
show numbered badges, the bar reads correctly in both themes, switching the palette
recolors coherently, the folder can be zipped and opened on a colleague's machine
unchanged, and the bar appears in `Plugins > Action Bars` after being dropped into
`Fiji.app/action-bars/`.
