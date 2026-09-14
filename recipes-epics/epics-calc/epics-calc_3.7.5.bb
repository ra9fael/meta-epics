SUMMARY = "EPICS calc module"
DESCRIPTION = "Calculation, array calculation and math-related records for EPICS."
HOMEPAGE = "https://github.com/epics-modules/calc"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
SRC_URI = "git://github.com/epics-modules/calc;protocol=https;nobranch=1"
# Upstream tag R3-7-5.
SRCREV = "2f5b175f260bc3fe35bc25a3f6c204e9d6f628c9"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "calc"
EPICS_MODULES = "SNCSEQ=seq sscan"
