#!/bin/sh
# SPDX-FileCopyrightText: 2026 IMPCAS
#
# SPDX-License-Identifier: MIT

# Echo server for the demo IOC's asyn IP port. socat runs this on APP_PORT_1
# with EXEC:; cat sends every received line straight back.
exec cat
