#!/bin/sh
# SPDX-FileCopyrightText: 2026 IMPCAS
#
# SPDX-License-Identifier: MIT

# Start an EPICS IOC instance under procServ on behalf of the generic
# epics-ioc@.service template. The instance name selects an entry in the
# instance registry:
#
#   /etc/epics/instances/<name>.env   fleet layer, shipped in the image
#   /boot/iocs/<name>.env             machine layer, optional, overrides
#
# The registry entry points at the IOC application directory and carries the
# instance identity (console slot, PV prefix, state directory, optional
# CA/PVA/console-port pins). Instance names are host-global and lowercase
# [a-z0-9-]; the console port is PORT_BASE + 10 * IOC_INSTANCE_INDEX.

set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
[ -r "$HERE/epics-ioc-env" ] || { echo "$0: missing $HERE/epics-ioc-env" >&2; exit 1; }
. "$HERE/epics-ioc-env"

INSTANCE="$1"
if [ -z "$INSTANCE" ] || case "$INSTANCE" in *[!a-z0-9-]*) true ;; *) false ;; esac; then
    echo "usage: $0 <instance-name>   (lowercase [a-z0-9-], host-global)" >&2
    exit 2
fi

[ -r "$ENV_ROOT/$INSTANCE.env" ] || {
    echo "$0: no registry entry $ENV_ROOT/$INSTANCE.env" >&2
    echo "$0: create one with ioc-instance-add $INSTANCE" >&2
    exit 1
}
. "$ENV_ROOT/$INSTANCE.env"

# Machine-level overrides (site config on the writable BOOT partition), last
# one wins: IOC_PREFIX, IOC_STATE, CA_PORT/PVA_PORT, PS_PORT, APP_PORT_1/2.
if [ -r "$MACHINE_ENV_ROOT/$INSTANCE.env" ]; then
    . "$MACHINE_ENV_ROOT/$INSTANCE.env"
fi

for key in IOC_APP_DIR IOC_PATH; do
    eval "value=\${$key}"
    [ -n "$value" ] || {
        echo "$0: $INSTANCE.env does not set $key" >&2
        exit 1
    }
done

app_dir="$IOC_APP_DIR/$IOC_PATH"
[ -d "$app_dir" ] || {
    echo "$0: application directory $app_dir does not exist" >&2
    exit 1
}

. "$HERE/ioc-ports.sh"
ioc_ports_resolve

[ -n "$IOC_PREFIX" ] || IOC_PREFIX="$INSTANCE:"
[ -n "$IOC_STATE" ] || IOC_STATE="/var/lib/epics-ioc/$INSTANCE"
export INSTANCE IOC_PREFIX IOC_STATE EPICS_TARGET_ARCH

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
cd "$app_dir"

# Optional fleet-level hook, e.g. an external device simulator bound to
# $APP_PORT_1. It runs as a sibling process in this service's cgroup, so
# systemd stops it together with the IOC.
if [ -r ./ioc-start.pre ]; then
    . ./ioc-start.pre
fi

# eval, not plain expansion: a quoted hook body must see the values the
# overrides left behind, and a bare function call must still work.
[ -n "$IOC_START_PRE" ] && eval "$IOC_START_PRE"

# The executable either comes from bin/<target-arch> or the st.cmd is run
# directly through its shebang.
if [ -n "$IOC_APP_NAME" ]; then
    set -- "$IOC_APP_DIR/bin/$EPICS_TARGET_ARCH/$IOC_APP_NAME" "${IOC_ST_CMD:-st.cmd}"
else
    set -- "./${IOC_ST_CMD:-st.cmd}"
fi

# -I makes procServ record PID and endpoints, so the running instance can be
# found without knowing the port in advance. --oneshot (in PROCSERV_ARGS)
# hands restart policy to systemd.
exec procServ -f -L - --name="$INSTANCE" -I "$RUN_DIR/$INSTANCE.info" \
    -P "$PS_PORT" $PROCSERV_ARGS "$@"
