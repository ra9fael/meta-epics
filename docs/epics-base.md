# EPICS Base

## Scope

Stage one builds only `epics-base` 7.0.10 and installs it under a configurable target prefix:

```text
${EPICS_PREFIX}/base-7.0.10
${EPICS_PREFIX}/base -> base-7.0.10
```

Default:

```text
EPICS_PREFIX=/opt/epics
```

This stage does not include asyn, seq, autosave, any IOC, or other modules.

## PetaLinux Integration

See the root [README](../README.md) for the complete PetaLinux integration
procedure. It covers adding the layer, configuring the root filesystem,
building the recipe and image, and verifying the target installation.

## Build model

PetaLinux uses Yocto/BitBake underneath. The recipe does not call the PetaLinux SDK directly or hard-code SDK compiler paths. BitBake provides `${CC}`, `${CXX}`, `${TARGET_PREFIX}`, the target sysroot, and package/rootfs installation tasks.

EPICS Base builds host tools and target libraries. `EPICS_HOST_ARCH` is `linux-x86_64`; `EPICS_TARGET_ARCH` controls the target architecture:

```text
Zynq-7000  linux-arm
ZynqMP     linux-aarch64
```

## Path separation

Build stage:

```text
${RECIPE_SYSROOT}/opt/epics/base
```

Target runtime:

```text
/opt/epics/base
```

The host architecture directories are removed from the target package. EPICS
Perl scripts are copied into `bin/${EPICS_TARGET_ARCH}` because the
upstream cross-build installs them under `bin/${EPICS_HOST_ARCH}` by default.
The shared `lib/perl` directory is retained and target `perl` is a runtime
dependency.

Later modules must obtain Base with `DEPENDS += "epics-base"`. They must not access the target machine's `/opt/epics` during the build.

## Not run yet

The following actions were intentionally not run:

- `petalinux-build`
- `petalinux-build -c epics-base`
- source fetch
- license checksum validation
- Zynq/ZynqMP compilation

The recipe pins the 7.0.10 commit and contains the checksum of its `LICENSE`
file. A build environment still needs network access or a configured source
mirror for the initial fetch.
