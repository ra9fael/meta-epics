SUMMARY = "EPICS autoparamDriver module"
DESCRIPTION = "An asyn driver that creates its parameters and records from the \
EPICS database instead of knowing them ahead of time."
HOMEPAGE = "https://github.com/Cosylab/autoparamDriver"
LICENSE = "EPICS | MIT | MIT-0"
LIC_FILES_CHKSUM = "file://LICENSES/EPICS.txt;md5=43c7610db53c3693e0ba4467d7874db9"
SRC_URI = "git://github.com/Cosylab/autoparamDriver;protocol=https;nobranch=1"
# Upstream tag v2.1.0.
SRCREV = "2159559b24eb45eeb5f77008d0236a80df933519"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "autoparamDriver"
EPICS_MODULES = "asyn"

do_configure:append() {
    # docs/ is a Sphinx project (needs sphinx-build on the build host); drop it.
    sed -i '/^[ \t]*DIRS[ \t]*+=[ \t]*docs[ \t]*$/d' ${S}/Makefile
}
