# meta-impcas-epics documentation

English | [简体中文](zh-CN/README.md)

This index is part of the [meta-impcas-epics](../README.md) layer.

## Documents

| Document | Read it when you want to ... |
|----------|------------------------------|
| [Getting started](../README.md) | add the layer to a PetaLinux project, select packages, build, and see the result on the target. |
| [EPICS Base](epics-base.md) | know what Base installs on the target, how the cross build is wired, and what an on-target development setup gets you. |
| [Support modules](modules.md) | add or maintain a module recipe such as `asyn` or `autosave`. |
| [IOC applications](ioc.md) | build, package, start and operate an IOC, and understand what the IOC classes generate. |
| [IOC port management](port-allocation.md) | know which instance owns which console port, add an instance, or pin CA/PVA service ports. |
| [Troubleshooting](troubleshooting.md) | a QA check fired, a build failed, or an IOC does not come up on the target. |

## Reading order

A first pass through the layer:

1. [Getting started](../README.md) — the layer in a project, end to end.
2. [EPICS Base](epics-base.md) — the foundation everything else builds against.
3. [IOC applications](ioc.md) — the part you will touch most.
4. [IOC port allocation](port-allocation.md) — read alongside the IOC document.

The remaining two are references: [support modules](modules.md) when writing a
recipe, [troubleshooting](troubleshooting.md) when something breaks.

## Conventions

* Every document exists in English (in `docs/`) and in Chinese (in
  `docs/zh-CN/`) under the same base name. The first line under the title
  links to the other language.
* Cross references stay in the reader's language: an English document links to
  English documents, a Chinese document to Chinese ones.
* Paths in text are target paths (`/opt/epics/...`) unless they carry a
  BitBake variable such as `${RECIPE_SYSROOT}`; those describe the build.
* Commands are shown for the PetaLinux project root unless stated otherwise.
