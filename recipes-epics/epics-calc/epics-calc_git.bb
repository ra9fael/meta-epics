SUMMARY = "EPICS calc module"
DESCRIPTION = "Calculation, array calculation and math-related records for EPICS."
HOMEPAGE = "https://github.com/epics-modules/calc"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
PV = "3.7.5+git"
SRC_URI = "git://github.com/epics-modules/calc;protocol=https;nobranch=1"
# Upstream tag R3-7-5.
# master, 4217e83a9b90 as of 2026-09-14.
SRCREV = "4217e83a9b9067017f4dc74da6b70e3669972f16"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "calc"
EPICS_MODULES = "SNCSEQ=seq sscan"
