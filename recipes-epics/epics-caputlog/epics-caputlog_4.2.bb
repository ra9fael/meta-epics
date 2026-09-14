SUMMARY = "EPICS caPutLog module"
DESCRIPTION = "Channel Access put logging: records every caput with old and new \
values for audit trails."
HOMEPAGE = "https://github.com/epics-modules/caPutLog"
LICENSE = "EPICS"
# The repository ships no license file or statement; the EPICS Open
# License governs these modules de facto. Ship the license text with
# the recipe so it is checksummed and installed with the sources.
LIC_FILES_CHKSUM = "file://${WORKDIR}/COPYING.EPICS;md5=a2c259c010f2152379d7769be894bf4a"
SRC_URI = "git://github.com/epics-modules/caPutLog;protocol=https;nobranch=1"
# Upstream tag R4.2.
SRCREV = "6f9eb3f6c75e49201d114f8e194d53cef493a522"
SRC_URI += "file://COPYING.EPICS"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "caPutLog"
