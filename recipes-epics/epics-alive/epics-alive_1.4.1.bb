SUMMARY = "EPICS alive module"
DESCRIPTION = "Heartbeat records that announce the IOC to an alive monitoring \
host."
HOMEPAGE = "https://github.com/epics-modules/alive"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
SRC_URI = "git://github.com/epics-modules/alive;protocol=https;nobranch=1"
# Upstream tag R1-4-1.
SRCREV = "fd1fd81286cc1ace7f06b9f7c91a8a0c8fecd044"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "alive"
