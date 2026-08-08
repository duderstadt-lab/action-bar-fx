# Mars power tools

The [Mars](https://duderstadt-lab.github.io/mars-docs/) commands used across most
workflows, grouped by what they are for. Copy this folder into
`Fiji.app/ActionBar/` and it appears under **Plugins › Action Bar FX › Mars power
tools**.

Every button is a Mars command, so this bar needs Mars installed; it has no
scripts of its own and nothing else to carry with it.

Four groups, separated by rules and by color rather than numbered, since these
are tools rather than steps in a protocol:

| group | color | buttons |
|---|---|---|
| browse | blue | Dataset Explorer |
| load and merge | green | Open Archive, Open Archive (S3), Open Virtual Store, Merge Archives, Merge Virtual Stores |
| find and track | orange | Peak Finder, Peak Tracker, Object Tracker, Molecule Integrator, Molecule Integrator (multiview) |
| change points | pink | Change Point Finder, Single Change Point Finder |
| import | vermillion | Open N5 as ImagePlus (minio) |

The palette is `okabe-ito`, the colorblind-safe default. Right-click the bar to
try the others; buttons in a group share a palette entry, so a palette switch
keeps the grouping intact.
