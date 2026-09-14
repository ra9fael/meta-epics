SUMMARY = "EPICS sscan module"
DESCRIPTION = "Scan records and related support for EPICS."
HOMEPAGE = "https://github.com/epics-modules/sscan"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
SRC_URI = "git://github.com/epics-modules/sscan;protocol=https;nobranch=1"
# Upstream tag R2-12.
SRCREV = "a67107fd9ca533d3056b157ad29a7e0b395f3147"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "sscan"
EPICS_MODULES = "SNCSEQ=seq"
