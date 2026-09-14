SUMMARY = "EPICS xxx template module"
DESCRIPTION = "The upstream example/template support module, kept as the \
reference for new module recipes."
HOMEPAGE = "https://github.com/epics-modules/xxx"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
PV = "6.3+git"
SRC_URI = "git://github.com/epics-modules/xxx;protocol=https;nobranch=1"
# Upstream tag R6-3.
# master, 0244c0dd2617 as of 2026-09-14.
SRCREV = "0244c0dd26171573c4077dfab4f0986e4c4bde6d"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "xxx"
EPICS_MODULES = "asyn autosave"
