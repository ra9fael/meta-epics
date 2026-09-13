# EPICS Base

## Scope

Builds EPICS Base 7.0.10 with the bundled PV Access modules and installs it
under a configurable target prefix:

```text
${EPICS_PREFIX}/base-7.0.10
${EPICS_PREFIX}/base -> base-7.0.10
```

Default:

```text
EPICS_PREFIX=/opt/epics
```

Base is the foundation the other recipes build against. Support modules install
under `${EPICS_PREFIX}/modules/` and IOC applications under
`${EPICS_PREFIX}/iocs/`; see the root [README](../README.md) for the recipe
list, [ioc.md](ioc.md) for applications and
[port-allocation.md](port-allocation.md) for the IOC port scheme.

## PetaLinux Integration

See the root [README](../README.md) for the complete PetaLinux integration
procedure. It covers adding the layer, configuring the root filesystem,
building the recipe and image, and verifying the target installation.

## Build model

PetaLinux uses Yocto/BitBake underneath. The recipe does not call the PetaLinux
SDK directly or hard-code SDK compiler paths. BitBake provides `${CC}`, `${CXX}`,
`${TARGET_PREFIX}`, the target sysroot, and package/rootfs installation tasks.

EPICS Base builds host tools and target libraries. `EPICS_HOST_ARCH` is
`linux-x86_64`; `EPICS_TARGET_ARCH` controls the target architecture:

```text
Zynq-7000  linux-arm
ZynqMP     linux-aarch64
```

The PV Access modules are git submodules. `EPICS_PVA_ENABLE` (default `1`)
selects the `gitsm` fetcher so they are checked out and built; set it to `0` for
a core-only build with no `libpvAccess`, `softIocPVA`, `pvget` or QSRV.

## Path separation

Build stage:

```text
${RECIPE_SYSROOT}/opt/epics/base
${RECIPE_SYSROOT}/opt/epics/modules/<name>
```

Target runtime:

```text
/opt/epics/base
/opt/epics/modules/<name>
```

Support modules are configured with `EPICS_INSTALL_BASE`
(`${EPICS_PREFIX}/modules` by default); EPICS Base overrides it back to
`${EPICS_PREFIX}` so Base keeps its historical `/opt/epics/base` location. IOC
applications use `${EPICS_PREFIX}/iocs`.

Later recipes must obtain Base with `DEPENDS += "epics-base"`. They must not
access the target machine's `/opt/epics` during the build.

## On-target development

The target package keeps what is needed to build EPICS modules and IOCs on the
target itself:

* only `${EPICS_TARGET_ARCH}` binaries and libraries — the host architecture
  `bin/` and `lib/` directories are not packaged;
* the architecture-independent EPICS Perl scripts, copied into
  `bin/${EPICS_TARGET_ARCH}` because upstream installs them under
  `bin/${EPICS_HOST_ARCH}` by default. They need the target `perl` package;
* `lib/perl` for those scripts, exposed through `PERL5LIB`;
* `msi` and `iocLogServer`, which upstream marks host-only but which are built
  for the target here.

`/etc/profile.d/epics.sh` exports `EPICS_PREFIX`, `EPICS_BASE`,
`EPICS_BASE_VERSION`, `EPICS_BASE_BIN`, `EPICS_BASE_LIB`,
`EPICS_TARGET_ARCH`, `EPICS_HOST_ARCH`, `PATH`, `LD_LIBRARY_PATH` and
`PERL5LIB`.

The installed `configure/CONFIG_SITE` deliberately leaves `EPICS_HOST_ARCH`,
`CROSS_COMPILER_TARGET_ARCHS` and `INSTALL_LOCATION` unset — those describe the
build host, and Base includes that file on every build. It sets
`SHARED_LIBRARIES = YES`, `STATIC_BUILD = NO` and

```text
LINKER_USE_RPATH = ORIGIN
LINKER_ORIGIN_ROOT = ${EPICS_PREFIX}
```

so executables link the shared libraries and resolve them relative to
`${EPICS_PREFIX}` without `LD_LIBRARY_PATH`.

## Rpath and the sysroot

The class builds modules with `LINKER_USE_RPATH = NO` plus an explicit
`-Wl,-rpath,${EPICS_PREFIX}/base/lib/${EPICS_TARGET_ARCH}`. The installed Base
settings above would otherwise make `makeRPath.py` embed build-tree paths, which
do not exist on the target.

Base's own host tools (`bin/${EPICS_HOST_ARCH}`, `lib/${EPICS_HOST_ARCH}`) are
staged into the recipe sysroot after packaging, so dependent module recipes can
run `makeBaseApp.pl`, `convertRelease.pl`, `dbdExpand.pl` and friends without
those host binaries ever entering a package.

## Base settings

| Variable             | Value                    |
|----------------------|--------------------------|
| `EPICS_PVA_ENABLE`   | `1` (bundled PV Access)  |
| `SHARED_LIBRARIES`   | `YES`                    |
| `STATIC_BUILD`       | `NO` on the target       |
| `EPICS_HOST_ARCH`    | `linux-x86_64`           |
| `EPICS_INSTALL_BASE` | `${EPICS_PREFIX}`        |
