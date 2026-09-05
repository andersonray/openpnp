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
| [Feeder Part Rotation Preview](#feeder-part-rotation-preview) | Feature | [Fork PR #1](https://github.com/andersonray/openpnp/pull/1) open, not yet proposed upstream |
| [Push-Pull Feeder 2](#push-pull-feeder-2) | Feature | In development |
| [Beep on Pick Failure](#beep-on-pick-failure) | Feature | In development |
| [Configurable Hotkeys](#configurable-hotkeys) | Feature | [Fork PR #3](https://github.com/andersonray/openpnp/pull/3) open, not yet proposed upstream |
| [Push-Pull Feeder Vision Retry](#push-pull-feeder-vision-retry) | Fix | [Fork PR #4](https://github.com/andersonray/openpnp/pull/4) open, not yet proposed upstream |

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

### Push-Pull Feeder 2

Branch `feature/push-pull-feeder-2`, started 2026-08-22.

`ReferencePushPullFeeder2` is a new feeder class that starts life as an exact copy of
`ReferencePushPullFeeder`, along with copies of its two configuration wizards. It is the working
surface for reworking how the push-pull feeder behaves, so that changes can be tried on a real
machine without putting existing push-pull feeders at risk. Both feeders appear in the Add Feeder
dialog and can be used side by side.

The copy is standalone rather than a subclass of the original. `ReferencePushPullFeeder` finds its
template, OCR and clone-target feeders with an `instanceof` scan over the whole feeder pool, and
hardcodes its own constructor in `createNewAtLocation()`, so a subclass would show up in the
original's pool and interfere with those operations.

Supporting notes:

* Field names are unchanged from the original, so an existing feeder can be migrated by editing the
  `class` attribute of its `<feeder>` element in `machine.xml`.
* Vision is untouched. The sprocket-hole and OCR pipelines come from the shared `FeederVisionHelper`
  and have no per-feeder-class resources.
* Registered in `ReferenceMachine.getCompatibleFeederClasses()`, the only place the original feeder
  was referenced outside its own files.

### Beep on Pick Failure

Branch `feature/pick-failure-signal`, started 2026-08-24.

The machine now beeps when a pick attempt fails, so an unattended job draws attention to itself
instead of quietly working around a problem.

Upstream OpenPnP already has a `Signaler` SPI with a `SoundSignaler`, but it only fires on the job
processor's lifecycle states, of which `ERROR` is the only bad one. A pick that failed and then
recovered on a retry therefore left no trace the operator would notice, and a feeder or nozzle tip
going bad stayed silent until the job actually halted. That is precisely the point at which it is
too late to do anything cheap about it.

The signal fires on *every* failed pick attempt, including the ones that recover, which is the whole
value of it. A pick that exhausts its retries still additionally plays the existing error sound when
the job stops.

Supporting changes:

* `Signaler.signalJobProcessorWarning(Warning)`, a second signal channel for job problems that do
  not stop the job, alongside the existing `signalJobProcessorState(State)`. `AbstractSignaler`
  no-ops it, so `ActuatorSignaler` and `Neoden4Signaler` are unaffected.
* `AbstractJobProcessor.Warning`, currently just `PICK_FAILURE`, plus a `fireJobWarning()` helper
  mirroring `fireJobState()`.
* Fired from `ReferencePnpJobProcessor.Pick.feederPickRetry()`, covering the "no part
  vacuum-detected after pick" case as well as pick motion and post-pick errors.
* `SoundSignaler` answers with `Toolkit.beep()`, deliberately distinct from the error sound. No wav
  is bundled, but a `sounds/pick-failure.wav` in the configuration directory overrides the beep,
  reusing the override mechanism the error and success sounds already have.
* A new "Play sound on pick failure?" option, defaulting to on so existing `machine.xml` files pick
  it up without editing.

Deliberately left alone: the part-off check before a pick, and the part-on check after alignment.
Both are distinct faults that already halt the job and so already produce the error sound.

### Configurable Hotkeys

Branch `feature/configurable-hotkeys`, started 2026-08-24.

Keyboard shortcuts can now be reassigned from a Window &rarr; Keyboard Shortcuts dialog, and can
optionally fire while OpenPnP is not the focused window. Together that makes a cheap USB macropad or
foot pedal at the machine into a set of physical buttons for pausing and stopping a job, so the
operator does not have to walk back to the computer and find a toolbar button with the mouse.

Upstream OpenPnP does have a global hotkey table, and it already covers start/pause, stop and step.
But the bindings are hardcoded in the `MainFrame` constructor with no UI and no persistence, so the
only way to use a dedicated key such as F13 was to edit Java. And because the table is hooked onto
the AWT event queue, the shortcuts are dead whenever another application has focus, which makes a
physical stop button at the machine unreliable in exactly the situation it is wanted.

The shipped defaults are the previously hardcoded bindings, so nothing changes until the user changes
it. An action can hold more than one shortcut, which the jog actions depend on: they are bound to
both Ctrl and Ctrl+Shift so that `JogControlsPanel` can read the shift state separately for coarse
jogging.

Supporting changes:

* `HotkeyDispatcher`, the dispatch logic extracted out of the anonymous `EventQueue` in `MainFrame`.
  It knows nothing about `MainFrame`, AWT or the native hook, so it is unit testable; both key paths
  funnel through it, which is what keeps them behaving identically.
* `HotkeyActions`, a registry of bindable actions keyed by stable ids, resolving the `Action` lazily
  through `MainFrame.get()`. It has a `register()` method, so binding a `ScriptAction` later needs no
  change here.
* `hotkeys.xml` in the configuration directory, via `HotkeysConfiguration` and `HotkeyBinding`. It is
  loaded non-fatally, unlike the other configuration files: a broken `machine.xml` should stop
  OpenPnP, a broken shortcut list should not.
* `GlobalHotkeyHook` and `NativeKeyStrokes`, wrapping a new `com.github.kwhat:jnativehook` dependency.
  Off by default, and every failure path falls back to focus-only shortcuts.
* Only `KEY_PRESSED` is acted on, and the `KEY_TYPED`/`KEY_RELEASED` of a consumed keypress are
  swallowed. Previously the lookup ran three times per keypress and the focused component still saw
  the tail of a press OpenPnP had claimed.

### Push-Pull Feeder Vision Retry

Branch `feature/push-pull-ocr-retry`, started 2026-09-04.

A push-pull feeder's vision and OCR check now gets up to three attempts before it is treated as a
failure.

At job start the job processor's PreFlight step visits every push-pull feeder the job uses and runs
a sprocket-hole calibration and an OCR read of the part label on it. A single failure there aborts
the entire job before anything is placed, and the ways it can fail are all intermittent: one misread
character leaves the OCR text matching no part, an ambiguous partial match matches two, and a
marginal camera frame loses the sprocket holes. Looking again almost always succeeds, but nothing on
that path ever retried.

`retryVisionOperation()` repeats such an operation up to `visionRetryCount` times, defaulting to 3.
Every attempt builds a fresh vision pipeline and re-moves and re-captures the camera, so a retry
genuinely works on a new image rather than reprocessing the old one, and any vision offset a
partially successful attempt stored is discarded first so the next attempt starts clean. Both
`prepareForJob()` and the bulk OCR the Feeders panel runs over all feeders go through it.

Supporting changes:

* `visionRetryCount` sits with `calibrateMaxPasses` and the other `machine.xml`-tweakable feeder
  attributes rather than on the wizard. Existing `machine.xml` files deserialize unchanged and pick
  up the default of 3.
* `OcrActionPerformedException` marks the one failure that must not be retried: when OCR correctly
  reads a part other than the one the feeder is configured for and the configured wrong-part action
  has already swapped feeders or changed the part, a retry would find the part correct and silently
  swallow the stop the user asked for, and after a swap it would be looking at a different location
  entirely. The job start check performs no action, so a wrong part read there is still retried.

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
