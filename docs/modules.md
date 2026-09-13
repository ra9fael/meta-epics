# Support modules

English | [简体中文](modules.zh-CN.md)

Part of the [documentation index](README.md).

A support module is anything built with EPICS' own build system that is not an
IOC application: `asyn`, `autosave`, and later `calc`, `streamDevice` and so on.
The `epics-module` class knows how to drive that build system from BitBake.

## What the class does

* Generates the toolchain files the module's `configure/` expects:
  `configure/CONFIG_SITE.<host>.<target>` for the target pass (compiler commands
  with `--sysroot`, `LINKER_USE_RPATH = NO` plus an rpath to Base, BitBake's
  `CFLAGS`/`CXXFLAGS`/`LDFLAGS` appended with `+=`) and the host-pass equivalent.
  Appending matters: a command-line assignment would disable the `+=` module
  Makefiles use for feature macros.
* Appends `EPICS_BASE` and `EPICS_RELEASE_EXTRA` to `configure/RELEASE`, all
  pointing into the recipe sysroot.
* Runs `make install.<target>` (and `install.<host>` only when
  `EPICS_HOST_PASS = 1`), then copies `EPICS_INSTALL_SUBDIRS` into
  `${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-<version>` and creates the
  version-independent symlink.
* Removes the host-architecture `bin/` and `lib/` directories, drops the
  generated `CONFIG_SITE` files and scrubs build paths from the installed text
  files.
* Stages the install tree into the sysroot, so dependent recipes see it under
  `${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/<name>`.
* Builds and ships everything in one package, including the versioned `.so`
  symlinks (`dev-so` is skipped deliberately): the target tree stays usable for
  development.

## Recipe skeleton

```bitbake
SUMMARY = "EPICS <name> module"
LICENSE = "..."
LIC_FILES_CHKSUM = "file://LICENSE;md5=..."

SRC_URI = "git://github.com/epics-modules/<name>;protocol=https;branch=master"
SRCREV = "<commit>"
S = "${WORKDIR}/git"

inherit epics-module

# BPN is epics-<name>, so the on-target module name must be set explicitly.
EPICS_MODULE_NAME = "<name>"

DEPENDS += "<build-time dependencies>"
RDEPENDS:${PN} += "<runtime dependencies>"

# Dependencies on other EPICS modules, resolved from the sysroot.
EPICS_RELEASE_EXTRA = "\
    <MODULE> = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/<module>"

# Trim to what a consumer needs; missing directories are skipped.
EPICS_INSTALL_SUBDIRS = "lib db dbd include cfg"
```

`epics-module` already depends on `epics-base` and sets `EPICS_BASE` in
`configure/RELEASE`; only additional modules go into `DEPENDS` and
`EPICS_RELEASE_EXTRA`.

## Modules in this layer

### asyn 4.46

```bitbake
DEPENDS += "libtirpc rpcsvc-proto-native"
RDEPENDS:${PN} += "libtirpc"
EPICS_INSTALL_SUBDIRS = "lib db dbd include cfg templates html documentation"
```

* VXI-11 needs ONC RPC, which glibc no longer ships: `libtirpc` for headers and
  library, `rpcsvc-proto-native` for a hermetic `rpcgen` (otherwise the build
  silently falls back to the host's `rpcgen`).
* The module keeps ONC RPC behind `TIRPC` and VXI-11 behind `DRV_VXI11`, both
  off by default; the recipe appends `TIRPC = YES` and `DRV_VXI11 = YES` to
  `configure/CONFIG_SITE.local`. `SYSROOT` is provided by the class because
  asyn builds include paths from it.
* The `test`, `iocBoot` and `asynPortDriver` unittest directories are removed
  from `DIRS`: they are PROD_IOC test programs and their generated device
  registration references PVA symbols the plain library link does not pull in.

### autosave 6.0

Depends on Base only. Its `asVerify` program is `PROD_HOST`, so the target
build never produces it and the default install subdirs are enough.

## Adding a module

1. Create `recipes-epics/<group>/epics-<name>_<version>.bb` with a pinned
   `SRCREV`, `LICENSE` and `LIC_FILES_CHKSUM`.
2. `inherit epics-module` and set `EPICS_MODULE_NAME` to the module's own name
   when it differs from `epics-<name>`.
3. Declare build dependencies in `DEPENDS` and runtime library dependencies in
   `RDEPENDS:${PN}`; shlibs scanning does not see anything under
   `${EPICS_PREFIX}`.
4. Point `EPICS_RELEASE_EXTRA` at the staged prerequisites, separating several
   entries with `\n`.
5. Trim `EPICS_INSTALL_SUBDIRS`, and remove `DIRS` entries that would build
   target-side test programs.
6. Build it on its own and check the result:

   ```bash
   petalinux-build -c epics-<name>
   ```

   The install tree should contain `lib/<target-arch>/*.so`, `dbd/` and
   `include/`, with no build paths and no host-architecture binaries. See
   [troubleshooting.md](troubleshooting.md) when it does not.

## Runtime resolution

Module libraries are installed under
`/opt/epics/modules/<name>/lib/${EPICS_TARGET_ARCH}`. The class gives every
module an rpath to Base; an IOC application adds its own rpath entries through
`EPICS_IOC_LIBDIRS` (see [ioc.md](ioc.md)).

A module that links another module's library needs that rpath too, and the
class does not add it yet — today only IOC applications handle it. When adding
such a module, expect to extend `epics-module` the same way `epics-ioc` does.

## Planned modules

```text
SNCSEQ -> SSCAN -> CALC -> ASYN -> STREAM
                 \       \-> BUSY
AUTOSAVE -----------> BUSY
XXX -> all selected modules
```

`asyn` and `autosave` are built; new ones follow the checklist above.
