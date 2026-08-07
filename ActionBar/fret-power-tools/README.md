# FRET power tools

The FRET Power Tools bar from the
[Mars docs](https://duderstadt-lab.github.io/mars-docs/tutorials/fretActionBar/),
ported from the legacy ImageJ ActionBar plugin. Copy this whole folder into
`Fiji.app/action-bars/` and it appears under **Plugins › Action Bars › FRET
power tools**.

Eleven buttons: five Mars commands at the top, then the six numbered steps of
the [example FRET workflow](https://duderstadt-lab.github.io/mars-docs/examples/).
The command buttons need [Mars](https://duderstadt-lab.github.io/mars-docs/)
installed.

The colors are the `legacy-pastel` palette, which reproduces the original bar's
hues. Right-click the bar to try the other palettes.

## Scripts

`scripts/` holds copies of the FRET workflow scripts from
[mars-tutorials](https://github.com/duderstadt-lab/mars-tutorials/tree/master/Example_workflows/FRET/scripts),
so that this folder is self-contained and can be zipped and shared unchanged.
Unlike the legacy bar, they do not have to be installed into Fiji's `scripts/`
folder first. Update them from mars-tutorials when the workflow changes there.
