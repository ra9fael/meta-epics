SUMMARY = "EPICS alive module"
DESCRIPTION = "Heartbeat records that announce the IOC to an alive monitoring \
host."
HOMEPAGE = "https://github.com/epics-modules/alive"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
PV = "1.4.1+git"
SRC_URI = "git://github.com/epics-modules/alive;protocol=https;nobranch=1"
# Upstream tag R1-4-1.
# master, 4108a24d26e9 as of 2026-09-14.
SRCREV = "4108a24d26e9d353a80e637f0dbea78fec1c3256"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "alive"

do_configure:append() {
    # Master added iocs/ (nested example IOC trees with their own configure/
    # that know nothing about the BitBake toolchain) and tests/. Drop both:
    # only aliveApp is needed.
    sed -i '/DIRS.*wildcard iocs/d; /iocs_DEPEND_DIRS/d; /DIRS.*filter-out.*tests/d; /tests_DEPEND_DIRS/d' \
        ${S}/Makefile
}
