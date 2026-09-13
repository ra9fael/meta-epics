# IOC port allocation

Every IOC instance that runs under procServ on the target needs a unique
listener port: the procServ console, plus any application socket the IOC opens.
This document defines how those ports are chosen so several IOC types, and
several instances of one type, can run on the same board without colliding.

See [ioc.md](ioc.md) for how a recipe and its instances are set up.

The next section explains why the EPICS client ports (CA, PVA) are **not** part
of this scheme.

## Range and structure

The site range is `21000-21999`, set by `EPICS_IOC_PORT_BASE` (the class default
is `21000`). It is below the Linux ephemeral range (`32768-60999`), so a
short-lived outbound connection cannot steal an IOC port.

```
21000 -+-- block 0  (IOC type 0)   21000-21099
       +-- block 1  (IOC type 1)   21100-21199
       +-- ...
       +-- block 9                 21900-21999
```

* **One 100-port block per IOC type.** A recipe sets `IOC_PORT_BLOCK_INDEX`
  (0-9); the class computes
  `IOC_PORT_BLOCK = EPICS_IOC_PORT_BASE + 100 * IOC_PORT_BLOCK_INDEX`.
* **A 10-port stride per instance.** An instance's env file sets
  `IOC_INSTANCE_INDEX` (0-9); the start script computes
  `INSTANCE_BASE = IOC_PORT_BLOCK + 10 * IOC_INSTANCE_INDEX`.

Offsets within an instance's 10 ports:

| Offset | Variable     | Purpose                                        |
|--------|--------------|------------------------------------------------|
| `+0`   | `PS_PORT`    | procServ console (telnet)                      |
| `+1`   | `APP_PORT_1` | IOC primary listener (e.g. the demo echo port) |
| `+2`   | `APP_PORT_2` | IOC secondary listener                         |
| `+3..+9` | -          | reserved (fixed PVA/CA if ever needed)         |

Capacity: 10 IOC types x 10 instances x 10 ports in the range.

## Block registry

| Block index | IOC type              | Range       | Notes                    |
|-------------|-----------------------|-------------|--------------------------|
| 0           | `epics-demo-ioc`      | 21000-21099 | demo/template            |
| 1           | `impcas-ioc-blm-zux`  | 21100-21199 | BLM production IOC       |
| 2-9         | reserved              | 21200-21999 |                          |

## Instance table

| IOC type         | Instance | `IOC_INSTANCE_INDEX` | `PS_PORT` | `APP_PORT_1` | `APP_PORT_2` |
|------------------|----------|----------------------|-----------|--------------|--------------|
| `epics-demo-ioc` | `ioc1`   | 0                    | 21000     | 21001        | 21002        |
| `epics-demo-ioc` | `ioc2`   | 1                    | 21010     | 21011        | 21012        |
| `impcas-ioc-blm-zux` | `iocblm` | 0                | 21100     | 21101        | 21102        |

The mapper is `<iocdir>/ioc-ports.sh`, installed with the IOC:

```sh
ioc-ports.sh --show     # print the ports resolved for this instance
ioc-ports.sh --next     # print the first free IOC_INSTANCE_INDEX
ioc-ports.sh --audit    # scan the instance env files and report collisions
```

A new instance is created from the shipped example:

```sh
cp /etc/epics/<PN>/example.env /etc/epics/<PN>/<name>.env
# edit IOC_INSTANCE_INDEX / IOC_PREFIX, then
systemctl enable --now '<PN>@<name>'
```

`ioc-ports.sh --next` exists so the index does not have to be tracked by hand.
An explicit `PS_PORT`, `APP_PORT_1` or `APP_PORT_2` in the instance env file
overrides the derived value.

## How clients find PVs (CA and PVA are not in this scheme)

A CA or PVA client does not need a PV-to-port map: the port is resolved by the
protocol at runtime. CA sends its search to UDP 5064, and the server's search
socket is opened with address fanout (`SO_REUSEPORT`), so **every** IOC on the
host receives it. The instance that owns the PV replies and the reply carries
its own TCP port. PVA works the same way through its UDP beacon, which carries
the server port.

This is why CA/PVA keep their system defaults (`5064`/`5065` and `5075`/`5076`)
for all instances: several IOCs coexist on one host and the client just uses the
PV name (`ioc1:...` vs `ioc2:...`).

`EPICS_CA_ADDR_LIST` therefore only names the hosts to search:

| Situation                             | Value                                            |
|---------------------------------------|--------------------------------------------------|
| Client and board in one broadcast domain | empty (use the default broadcast), or the board IP |
| Client on another subnet              | `EPICS_CA_ADDR_LIST=<board-ip>`, space separated for several boards |
| Port                                  | do **not** append one; `5064` is implicit        |

Only a CA server moved off `5064` would need `ip:port` entries, and then every
client would have to change too -- which is why it stays at the default.

The console port (`21000`, `21010`, ...) and the application ports
(`APP_PORT_1`) belong to the **telnet operator** and to programs that connect to
the IOC's own sockets. They are not CA ports and must never appear in
`EPICS_CA_ADDR_LIST`; their mapping is the table above, and the instance env
file records it.
