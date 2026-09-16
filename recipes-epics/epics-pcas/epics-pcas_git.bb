SUMMARY = "EPICS pcas module"
DESCRIPTION = "The portable Channel Access server library used to build EPICS \
servers outside of an IOC."
HOMEPAGE = "https://github.com/epics-modules/pcas"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=2eeea17a15fc6ba8501fdcec09b854dc"
SRC_URI = "git://github.com/epics-modules/pcas;protocol=https;nobranch=1"
# master as of 2026-09-16 (only supports EPICS 7; the layer builds Base
# 7.0.10, which satisfies it).
SRCREV = "bdf2b0ab4229e0bb69dbe9107dbb40c332db4f33"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "pcas"
