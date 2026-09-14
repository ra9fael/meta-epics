#!/bin/sh
# SPDX-FileCopyrightText: 2026 IMPCAS
#
# SPDX-License-Identifier: MIT

# Start an EPICS IOC under procServ. epics-ioc-systemd installs this into every
# IOC directory; the per-IOC values come from the generated ioc-env file and
# the instance env file named by $1.

set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
[ -r "$HERE/ioc-env" ] || { echo "$0: missing $HERE/ioc-env" >&2; exit 1; }
. "$HERE/ioc-env"

INSTANCE="$1"
[ -n "$INSTANCE" ] || INSTANCE=default

# Instance configuration: IOC_INSTANCE_INDEX, IOC_PREFIX, IOC_STATE, and the
# optional CA_PORT/PVA_PORT pins.
[ -r "$ENV_DIR/$INSTANCE.env" ] && . "$ENV_DIR/$INSTANCE.env"

. "$HERE/ioc-ports.sh"
ioc_ports_resolve

[ -n "$IOC_PREFIX" ] || IOC_PREFIX="$INSTANCE:"
[ -n "$IOC_STATE" ] || IOC_STATE="/var/lib/$PN/$INSTANCE"
export IOC_PREFIX IOC_STATE

# The EPICS service ports stay dynamic unless the instance pins them. rsrv
# reads EPICS_CA_SERVER_PORT when its server starts, pvAccess reads
# EPICS_PVAS_SERVER_PORT, so exporting here is early enough.
if [ -n "$CA_PORT" ]; then
    export EPICS_CA_SERVER_PORT="$CA_PORT"
fi
if [ -n "$PVA_PORT" ]; then
    export EPICS_PVAS_SERVER_PORT="$PVA_PORT"
fi

mkdir -p "$IOC_STATE" "$RUN_DIR"
cd "$HERE/$IOC_PATH"

# Optional hook, e.g. an external device simulator bound to $APP_PORT_1. It
# runs as a sibling process in this service's cgroup, so systemd stops it with
# the IOC.
[ -r "$HERE/$IOC_PATH/ioc-start.pre" ] && . "$HERE/$IOC_PATH/ioc-start.pre"

$IOC_START_PRE

# The executable either comes from bin/<target-arch> or the st.cmd is run
# directly through its shebang.
if [ -n "$IOC_APP_NAME" ]; then
    set -- "$HERE/bin/$EPICS_TARGET_ARCH/$IOC_APP_NAME" "$IOC_ST_CMD"
else
    set -- "./$IOC_ST_CMD"
fi

# -I makes procServ record PID and endpoints, so the running instance can be
# found without knowing the port in advance.
exec procServ -f -L - -I "$RUN_DIR/$INSTANCE.info" -P "$PS_PORT" $PROCSERV_ARGS "$@"
