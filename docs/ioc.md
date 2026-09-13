# IOC applications

English | [简体中文](ioc.zh-CN.md)

Part of the [meta-epics documentation](README.md).

An IOC application is a `makeBaseApp`-style tree: an application directory with
a `configure/`, an `*App/src` that produces the IOC executable and its `.dbd`,
an `*App/Db` with the records, and an `iocBoot/<ioc>` with `st.cmd`.

Two classes build and run it:

* `epics-ioc` (inherits `epics-module`) builds and packages the application.
* `epics-ioc-systemd` (inherits `epics-ioc`) adds procServ and a systemd unit,
  and gives each instance its own console port.

`epics-asyn-scope-ioc` in `recipes-examples/asyn-scope-ioc/` is a complete
example: it builds asyn's own simulated oscilloscope test IOC
(`testAsynPortDriver`) straight from the asyn sources. Read its recipe alongside
this document.

## Install layout

```text
/opt/epics/iocs/asyn-scope-ioc -> asyn-scope-ioc-1.0
/opt/epics/iocs/asyn-scope-ioc-1.0/
├── bin/linux-aarch64/testAsynPortDriver
├── lib/linux-aarch64/libtestAsynPortDriverSupport.so
├── db/testAsynPortDriver.db
├── dbd/testAsynPortDriver.dbd
├── iocBoot/ioctestAsynPortDriver/{st.cmd,envPaths}
├── ioc-start.sh
├── ioc-ports.sh
└── ioc-instance-add
/etc/epics/asyn-scope-ioc/{example.env,ioc0.env,ioc1.env}
/usr/lib/systemd/system/epics-asyn-scope-ioc@.service
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
| `IOC_APP_NAME`              | `""`                                 | Executable under `bin/<target-arch>/`; empty to run `st.cmd` through its shebang. |
| `IOC_PATH`                  | `""`                                 | Directory with `st.cmd`, e.g. `iocBoot/iocmy`. |
| `IOC_ST_CMD`                | `"st.cmd"`                           | Name of the startup script. |
| `PROCSERV_ARGS`             | `"-A"`                               | Extra procServ arguments; `-A` allows remote consoles. |
| `EPICS_IOC_MULTI_INSTANCE`  | `"0"`                                | `1` ships a systemd template unit instead of a plain one. |
| `EPICS_IOC_INSTANCE_ENVS`   | `""`                                 | Instance env files to install, as source paths. |
| `EPICS_IOC_START_PRE`       | `""`                                 | Shell statement run by `ioc-start.sh` before procServ starts. |
| `EPICS_IOC_PORT_BASE`       | `"21000"`                            | First console port; see [port-allocation.md](port-allocation.md). |

`RDEPENDS` must list every module whose library the IOC links; shlibs scanning
does not see anything under `${EPICS_PREFIX}`, so it cannot work them out.

Use `/opt/epics` paths in `configure/RELEASE`, not the build sysroot — the
sysroot only applies at build time.

## Instances

Every instance gets its own console port; the CA and PVA service ports are
shared with the host's EPICS defaults or dynamically assigned. The slot scheme,
the client configuration and the optional port pinning are in
[port-allocation.md](port-allocation.md).

An instance is selected by the unit instance name, which is also the name of
its env file:

```bash
cp /etc/epics/asyn-scope-ioc/example.env /etc/epics/asyn-scope-ioc/ioc1.env
systemctl enable --now 'epics-asyn-scope-ioc@ioc1'
```

The env file supplies the instance to the start script:

```sh
IOC_INSTANCE_INDEX=1        # console 21010; global across every IOC on the target
IOC_PREFIX=ioc1:            # record name prefix, passed to the IOC
IOC_STATE=/var/lib/asyn-scope-ioc/ioc1

#CA_PORT=21013              # optional: pin the CA server port (else dynamic)
#PVA_PORT=21014             # optional: pin the PVA server port (else dynamic)
#PS_PORT=...                # optional: override the console port
#APP_PORT_1=...             # optional: for IOCs that open their own sockets
#APP_PORT_2=...
```

`IOC_INSTANCE_INDEX` is the only number that has to be unique, and it is unique
across every IOC on the target. `ioc-instance-add <name>` creates an env file
with the smallest free slot, and `ioc-ports.sh --show [instance]` prints the
ports (and, for a running instance, the actual endpoints):

```sh
/opt/epics/iocs/asyn-scope-ioc/ioc-ports.sh --show ioc1
/opt/epics/iocs/asyn-scope-ioc/ioc-ports.sh --next
/opt/epics/iocs/asyn-scope-ioc/ioc-ports.sh --audit
```

## The generated start script

systemd cannot do the port arithmetic, so `ExecStart` is a generated script,
not procServ directly. `ioc-start.sh <instance>`:

1. sources `/etc/epics/<PN>/<instance>.env`,
2. derives the console and application ports (`ioc-ports.sh`),
3. exports `EPICS_CA_SERVER_PORT` / `EPICS_PVAS_SERVER_PORT` when the env file
   pinned `CA_PORT` / `PVA_PORT` (the servers read them at startup),
4. defaults `IOC_PREFIX` and `IOC_STATE` and creates the state directory,
5. `cd`s into `IOC_PATH` and sources the optional `ioc-start.pre` hook,
6. runs `EPICS_IOC_START_PRE`, then `exec procServ -f -L - -I <info file>
   -P "$PS_PORT" ...`.

`IOC_PREFIX` and `IOC_STATE` are exported, and iocsh reads `.cmd` macros from
the process environment, so `st.cmd` can use `$(IOC_PREFIX)`, `$(IOC_STATE)` or
any other instance setting directly. The `-I` info file under
`/run/epics/<PN>/` records the running server's PID and endpoints;
`ioc-ports.sh --show <instance>` reads it.

The hook file `<iocdir>/<IOC_PATH>/ioc-start.pre` is sourced, so it can define
the function named by `EPICS_IOC_START_PRE` -- for example starting an external
device simulator on `$APP_PORT_1`. It runs as a sibling process in the service's
cgroup, so systemd stops it together with the IOC.

## Target operations

```bash
systemctl is-enabled 'epics-asyn-scope-ioc@ioc0'   # disabled: installed, not enabled
systemctl enable --now 'epics-asyn-scope-ioc@ioc0'
systemctl enable --now 'epics-asyn-scope-ioc@ioc1'
ss -ltnp | grep -E '2100[01]|2101[01]'      # consoles 21000 / 21010
cat /run/epics/asyn-scope-ioc/ioc1.info     # PID and endpoints of the running IOC

telnet <board-ip> 21000                     # console of ioc0
telnet <board-ip> 21010                     # console of ioc1
```

The console is an iocsh prompt for the running IOC (`help`, `dbpr`, ...); the
IOC is a child of procServ, so killing it from the console makes procServ
restart it.

Records are reached over CA with no client-side configuration, because every
instance on the host receives the broadcast search and replies with its own
port:

```bash
caget ioc0:scope1:Waveform1.VAL
caget ioc1:scope1:Waveform1.VAL             # the other instance, still no config
```

The simulated oscilloscope does not open sockets of its own, so its instances
use no application ports. An IOC that does -- one that acts as a Modbus or
stream-device server, say -- pins them in its env file and documents them in
the port table of [port-allocation.md](port-allocation.md).

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
