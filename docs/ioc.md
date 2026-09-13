# IOC applications

English | [简体中文](ioc.zh-CN.md)

Part of the [meta-epics documentation](README.md).

An IOC application is a `makeBaseApp` tree: an application directory with a
`configure/`, an `*App/src` that produces the IOC executable and its `.dbd`,
an `*App/Db` with the records, and an `iocBoot/<ioc>` with `st.cmd`.

Two classes build and run it:

* `epics-ioc` (inherits `epics-module`) builds and packages the application.
* `epics-ioc-systemd` (inherits `epics-ioc`) adds procServ and a systemd unit,
  and gives each instance its own ports.

`epics-demo-ioc` in `recipes-examples/demo-ioc/` is a complete example and the
template for new IOCs. Read its recipe alongside this document.

## Install layout

```text
/opt/epics/iocs/epics-demo-ioc -> epics-demo-ioc-1.0
/opt/epics/iocs/epics-demo-ioc-1.0/
├── bin/linux-aarch64/demoIoc
├── db/test.db
├── dbd/demoIoc.dbd
├── iocBoot/iocdemo/{st.cmd,envPaths,auto.req,echo.sh,ioc-start.pre}
├── ioc-start.sh
├── ioc-ports.sh
└── ioc-instance-add
/etc/epics/epics-demo-ioc/{example.env,ioc1.env,ioc2.env}
/usr/lib/systemd/system/epics-demo-ioc@.service
```

Applications install under `${EPICS_PREFIX}/iocs` (`EPICS_INSTALL_BASE`), the
same way support modules install under `${EPICS_PREFIX}/modules`.

## Writing an IOC recipe

```bitbake
inherit epics-ioc-systemd

EPICS_MODULE_NAME = "my-ioc"
DEPENDS += "epics-asyn epics-autosave"
RDEPENDS:${PN} += "epics-asyn epics-autosave"

EPICS_RELEASE_EXTRA = "\
    ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn\n\
    AUTOSAVE = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/autosave"
EPICS_IOC_LIBDIRS = "${EPICS_PREFIX}/modules/asyn/lib/${EPICS_TARGET_ARCH} \
                     ${EPICS_PREFIX}/modules/autosave/lib/${EPICS_TARGET_ARCH}"

IOC_PORT_BLOCK_INDEX = "0"
IOC_APP_NAME = "myIoc"
IOC_PATH     = "iocBoot/iocmy"
```

Variables the classes read:

| Variable                    | Default                              | Meaning |
|-----------------------------|--------------------------------------|---------|
| `EPICS_MODULE_NAME`         | `${BPN}`                             | Directory name under `iocs/`. |
| `EPICS_INSTALL_BASE`        | `${EPICS_PREFIX}/iocs`               | Install root. |
| `EPICS_RELEASE_EXTRA`       | `""`                                 | `configure/RELEASE` entries; separate several with `\n`. |
| `EPICS_IOC_LIBDIRS`         | `""`                                 | Runtime library directories of the linked modules; turned into rpath entries. |
| `IOC_PORT_BLOCK_INDEX`      | `"0"`                                | Which 100-port block this IOC type owns. |
| `IOC_APP_NAME`              | `""`                                 | Executable under `bin/<target-arch>/`; empty to run `st.cmd` through its shebang. |
| `IOC_PATH`                  | `""`                                 | Directory with `st.cmd`, e.g. `iocBoot/iocmy`. |
| `IOC_ST_CMD`                | `"st.cmd"`                           | Name of the startup script. |
| `PROCSERV_ARGS`             | `"-A"`                               | Extra procServ arguments; `-A` allows remote consoles. |
| `EPICS_IOC_MULTI_INSTANCE`  | `"0"`                                | `1` ships a systemd template unit instead of a plain one. |
| `EPICS_IOC_INSTANCE_ENVS`   | `""`                                 | Instance env files to install, as source paths. |
| `EPICS_IOC_START_PRE`       | `""`                                 | Shell statement run by `ioc-start.sh` before procServ starts. |

`RDEPENDS` must list every module whose library the IOC links; shlibs scanning
does not see anything under `${EPICS_PREFIX}`, so it cannot work them out.

Use `/opt/epics` paths in `configure/RELEASE`, not the build sysroot — the
sysroot only applies at build time.

## Instances and ports

Every instance needs its own procServ console port and its own application
ports; CA and PVA stay on their system defaults and are shared. The allocation
scheme, the block registry and the CA/PVA reasoning are in
[port-allocation.md](port-allocation.md).

An instance is selected by the unit instance name, which is also the name of
its env file:

```bash
cp /etc/epics/epics-demo-ioc/example.env /etc/epics/epics-demo-ioc/ioc1.env
systemctl enable --now 'epics-demo-ioc@ioc1'
```

The env file supplies the instance to the start script:

```sh
IOC_INSTANCE_INDEX=0        # picks the 10-port stride inside the IOC's block
IOC_PREFIX=ioc1:            # record name prefix, passed to the IOC
IOC_STATE=/var/lib/epics-demo-ioc/ioc1

#PS_PORT=...                # optional explicit overrides
#APP_PORT_1=...
#APP_PORT_2=...
```

`IOC_INSTANCE_INDEX` is the only number that has to be unique per IOC type.
`ioc-instance-add <name>` creates an env file with the smallest free index, and
`ioc-ports.sh --show` prints the ports an index resolves to:

```sh
/opt/epics/iocs/epics-demo-ioc/ioc-ports.sh --show   # with IOC_INSTANCE_INDEX set
/opt/epics/iocs/epics-demo-ioc/ioc-ports.sh --next
/opt/epics/iocs/epics-demo-ioc/ioc-ports.sh --audit
```

## The generated start script

`systemd` cannot do the port arithmetic, so `ExecStart` is a generated script,
not procServ directly. `ioc-start.sh <instance>`:

1. sources `/etc/epics/<PN>/<instance>.env`,
2. resolves `PS_PORT`, `APP_PORT_1` and `APP_PORT_2` (`ioc-ports.sh`),
3. defaults `IOC_PREFIX` and `IOC_STATE` and creates the state directory,
4. `cd`s into `IOC_PATH` and sources the optional `ioc-start.pre` hook,
5. runs `EPICS_IOC_START_PRE`, then `exec procServ -f -L - -P "$PS_PORT" ...`.

`IOC_PREFIX` and `IOC_STATE` are exported, and iocsh reads `.cmd` macros from
the process environment, so `st.cmd` can use `$(IOC_PREFIX)`, `$(IOC_STATE)`
and `$(APP_PORT_1)` directly.

The hook file `<iocdir>/<IOC_PATH>/ioc-start.pre` is sourced, so it can define
the function named by `EPICS_IOC_START_PRE`. The demo uses it to start an echo
device on `$APP_PORT_1`:

```sh
start_sim_device() {
    socat -d -d "TCP-LISTEN:$APP_PORT_1,reuseaddr,fork" EXEC:"./echo.sh" &
}
```

That process is a sibling in the service's cgroup, so systemd stops it together
with the IOC.

## Target operations

```bash
systemctl is-enabled 'epics-demo-ioc@ioc1'   # disabled: installed, not enabled
systemctl enable --now 'epics-demo-ioc@ioc1'
systemctl enable --now 'epics-demo-ioc@ioc2'
ss -ltnp | grep -E '2100[01]|2101[01]'      # ioc1 21000/21001, ioc2 21010/21011

telnet <board-ip> 21000                      # procServ console for ioc1
telnet <board-ip> 21010                      # procServ console for ioc2
```

The console is an iocsh prompt for the running IOC (`help`, `dbpr`, ...); the
IOC is a child of procServ, so killing it from the console makes procServ
restart it.

Records are reached over CA with no client-side port configuration, because
both instances answer on the host's default CA port:

```bash
caput ioc1:cmdset world
caput ioc1:cmd.PROC 1
caget ioc1:cmd                              # world
caget ioc2:cmd                              # independent of ioc1
```

Autosave writes into `$IOC_STATE` (`/var/lib/epics-demo-ioc/<instance>`), so
each instance has its own `.sav` set.

## Why the classes do what they do

Useful when an IOC build misbehaves:

* **`envPaths` is host-only.** `iocBoot/<ioc>/Makefile` pins
  `ARCH = $(EPICS_HOST_ARCH)` and runs `convertRelease.pl` only through its host
  `buildInstall` target, so the class runs that action explicitly after the
  target compile.
* **No host-architecture build.** EPICS makes a cross target depend on the host
  one (`configure/RULES_ARCHS`), which would link a host IOC against host module
  libraries that are deliberately not packaged. `EPICS_MAKE_EXTRA` clears
  `CROSS_ARCHS` so only the target is built.
* **`IOCS_APPL_TOP`.** EPICS records the application top in `envPaths` and in the
  generated `*_registerRecordDeviceDriver.cpp`, which compares it against the
  runtime `TOP` at `iocInit`. Left alone it is the build directory, which embeds
  a build path in the binary and warns on every start, so the class points it at
  the install location.
* **rpath instead of `LD_LIBRARY_PATH`.** The target gets explicit rpath entries
  for Base and for `EPICS_IOC_LIBDIRS`, so an IOC starts without any environment
  setup. `envPaths`, meanwhile, records the sysroot path from
  `configure/RELEASE`; the class strips it and rewrites the versioned install
  directory to the version-independent symlink.
