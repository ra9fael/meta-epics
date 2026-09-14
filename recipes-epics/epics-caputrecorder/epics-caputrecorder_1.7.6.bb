SUMMARY = "EPICS caputRecorder module"
DESCRIPTION = "Record and replay sequences of caputs from an IOC."
HOMEPAGE = "https://github.com/epics-modules/caputRecorder"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
SRC_URI = "git://github.com/epics-modules/caputRecorder;protocol=https;nobranch=1"
# Upstream tag R1-7-6.
SRCREV = "078ddaec92f84f678a9be614345e6028646ffdcb"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "caputRecorder"
