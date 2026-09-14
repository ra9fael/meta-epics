SUMMARY = "EPICS MCoreUtils module"
DESCRIPTION = "Utilities shared by motor-based and other EPICS drivers."
HOMEPAGE = "https://github.com/epics-modules/MCoreUtils"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=2eeea17a15fc6ba8501fdcec09b854dc"
SRC_URI = "git://github.com/epics-modules/MCoreUtils;protocol=https;nobranch=1"
# master as of 2026-09-14.
SRCREV = "a86e5ed193abaae5744f9a5545fc883a9348258d"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "MCoreUtils"

do_configure:append() {
    # exampleTop is a nested example IOC whose sub-Makefiles resolve TOP
    # through the example tree; drop it (the *Top wildcard line goes with it)
    # and keep only configure + MCoreUtilsApp.
    rm -rf ${S}/exampleTop
    sed -i '/^[ \t]*DIRS[ \t]*+=[ \t]*\$\(wildcard \*Top\)[ \t]*$/d' ${S}/Makefile
}
