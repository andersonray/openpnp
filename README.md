![OpenPNP Logo](https://raw.githubusercontent.com/openpnp/openpnp-logo/develop/logo_small.png)

# OpenPnP

Open Source SMT Pick and Place Hardware and Software

## Introduction

OpenPnP is a project to create the plans, prototype and software for a completely Open Source SMT
pick and place machine that anyone can afford. I believe that with the ubiquity of cheap, precise
motion control hardware, some ingenuity and plenty of Open Source software it should be possible
to build and own a fully functional SMT pick and place machine for under $1000.

## Fork Changes

This is a fork of [openpnp/openpnp](https://github.com/openpnp/openpnp). Everything listed here is
work added in this fork that is not in upstream OpenPnP. An entry is added when a branch is started
and kept current as the work progresses, so the table doubles as a view of what is in flight.

Once a change is merged upstream it is removed from this list, since at that point it is no longer a
difference between this fork and OpenPnP.

| Change | Type | Status |
| --- | --- | --- |
| [Feeder Part Rotation Preview](#feeder-part-rotation-preview) | Feature | Pushed to fork, upstream PR not yet opened |

### Feeder Part Rotation Preview

Branch `feature/feeder-part-rotation-preview`, started 2026-08-21.

Setting a feeder's part rotation used to mean placing a part, looking at how it landed, and guessing
again. This adds a tool that answers the question before anything is placed.

Selecting a feeder on the Feeders tab and pressing the new rotation button captures an image of the
part at its pick location, moves the camera to a placement of that part in the open job, and ghosts
the captured part semi-transparently over the live view of the board, rotated by the angle the
machine will actually apply between picking and placing. Editing the rotation updates the ghost
immediately, and Apply writes the value back to the feeder.

Supporting changes:

* The `Feeder` interface gained `isPartRotationAdjustable()`, `getPartRotation()` and
  `setPartRotation()`, so the part rotation can be read and adjusted without knowing the concrete
  feeder type. Every feeder type still stores the value where it always did.
* `PartImageReticle`, the first reticle that draws a captured image rather than vector geometry.
* Crop and feather helpers in `ImageUtils`.

## Project Status

OpenPnP is stable and in wide use. It is still under heavy development and new features are added continuously. See the [Downloads](http://openpnp.org/downloads) page to get started.

If you would like to keep up with our progress you can
[Watch this project on GitHub](http://github.com/openpnp/openpnp), check out
[our Twitter](http://twitter.com/openpnp), [join the discussion group](http://groups.google.com/group/openpnp),
or come chat with us on [Discord](https://discord.gg/EmsrFVx).

## Contributing

![Build Status](https://github.com/openpnp/openpnp/workflows/Build%20and%20Deploy%20OpenPnP/badge.svg)
[![Help Wanted](https://img.shields.io/github/issues-raw/openpnp/openpnp/help-wanted.svg?label=help-wanted&colorB=5319e7)](https://github.com/openpnp/openpnp/labels/help-wanted)
[![Bugs](https://img.shields.io/github/issues-raw/openpnp/openpnp/bug.svg?label=bugs&colorB=D9472F)](https://github.com/openpnp/openpnp/labels/bug)
[![Feature Requests](https://img.shields.io/github/issues-raw/openpnp/openpnp/feature-request.svg?label=feature-requests&colorB=bfd4f2)](https://github.com/openpnp/openpnp/labels/feature-request)
[![Enhancements](https://img.shields.io/github/issues-raw/openpnp/openpnp/enhancement.svg?label=enhancements&colorB=0052cc)](https://github.com/openpnp/openpnp/labels/enhancement)


Before starting work on a pull request, please read: https://github.com/openpnp/openpnp/wiki/Developers-Guide#contributing

Summary of guidelines:

* One pull request per issue.
* Describe the change.
* Follow the coding style.
* Include tests and documentation.
* Think of the big picture.

## Thanks

Many thanks to ej-technologies for providing a complimentary license of install4j. install4j
creates high quality, professional installers for Java applications.

More information at http://www.ej-technologies.com/products/install4j/overview.html.
