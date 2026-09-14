SUMMARY = "EPICS caputRecorder module"
DESCRIPTION = "Record and replay sequences of caputs from an IOC."
HOMEPAGE = "https://github.com/epics-modules/caputRecorder"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
PV = "1.7.6+git"
SRC_URI = "git://github.com/epics-modules/caputRecorder;protocol=https;nobranch=1"
# Upstream tag R1-7-6.
# master, f6369cdec9c3 as of 2026-09-14.
SRCREV = "f6369cdec9c334bf3058d1c3ccfdbebc3d261b2d"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "caputRecorder"
