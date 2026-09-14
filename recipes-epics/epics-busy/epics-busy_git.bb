SUMMARY = "EPICS busy module"
DESCRIPTION = "Busy record support that aggregates activity of scans and other \
busy sources."
HOMEPAGE = "https://github.com/epics-modules/busy"
LICENSE = "EPICS"
# The repository ships no license file or statement; the EPICS Open
# License governs these modules de facto. Ship the license text with
# the recipe so it is checksummed and installed with the sources.
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
PV = "1.7.4+git"
SRC_URI = "git://github.com/epics-modules/busy;protocol=https;nobranch=1"
# Upstream tag R1-7-4.
# master, e015bc7cbdde as of 2026-09-14.
SRCREV = "e015bc7cbdde5d10e725aa76a944aa95373ab3de"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "busy"
EPICS_MODULES = "asyn autosave"
