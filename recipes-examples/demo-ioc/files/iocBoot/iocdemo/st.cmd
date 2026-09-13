#!../../bin/linux-aarch64/demoIoc

# SPDX-FileCopyrightText: 2026 IMPCAS
#
# SPDX-License-Identifier: MIT

# Ports, PV prefix and state directory come from the instance environment
# exported by ioc-start.sh; iocsh reads $(...) macros from the environment.

< envPaths

cd "${TOP}"

dbLoadDatabase "dbd/demoIoc.dbd"
demoIoc_registerRecordDeviceDriver pdbbase

# asyn IP client to the per-instance echo device (started by ioc-start.pre on
# APP_PORT_1).
drvAsynIPPortConfigure("L0", "127.0.0.1:$(APP_PORT_1)")
asynOctetSetOutputEos("L0", 0, "\n")
asynOctetSetInputEos("L0", 0, "\n")

dbLoadRecords("db/test.db", "P=$(IOC_PREFIX)")

set_requestfile_path("${TOP}/iocBoot/${IOC}")
set_savefile_path("$(IOC_STATE)")
set_pass0_restoreFile("auto.sav", "P=$(IOC_PREFIX)")
set_pass1_restoreFile("auto.sav", "P=$(IOC_PREFIX)")

cd "${TOP}/iocBoot/${IOC}"
iocInit()

create_monitor_set("auto.req", 5, "P=$(IOC_PREFIX)")
