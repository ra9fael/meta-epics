SUMMARY = "EPICS iocStats module"
DESCRIPTION = "IOC resident status: uptime, memory, CPU and related IOC health \
records."
HOMEPAGE = "https://github.com/epics-modules/iocStats"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=76d18f9132055ed510b481f6f211e0d7"
SRC_URI = "git://github.com/epics-modules/iocStats;protocol=https;nobranch=1"
# Upstream tag 4.0.1.
SRCREV = "b0a51778f50b97cd9c50d0aa0e03a7b7ee2f3a84"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "iocStats"
