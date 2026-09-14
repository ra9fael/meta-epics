SUMMARY = "EPICS StreamDevice module"
DESCRIPTION = "StreamDevice: protocol-file driven communication with serial and \
network devices over asyn."
HOMEPAGE = "https://github.com/paulscherrerinstitute/StreamDevice"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"
SRC_URI = "git://github.com/paulscherrerinstitute/StreamDevice;protocol=https;nobranch=1"
# Upstream tag 2.8.26.
SRCREV = "668d1d525509604ab4ccd7382022dfb469c99841"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "stream"
# The upstream RELEASE variable names do not all match the directory names in
# this layer, so the RELEASE lines are written by hand here instead of being
# derived from EPICS_MODULES (which still supplies DEPENDS/RDEPENDS/libdirs).
EPICS_MODULES = "asyn SNCSEQ=seq sscan calc"
DEPENDS += "libpcre"
RDEPENDS:${PN} += "libpcre"
EPICS_RELEASE_EXTRA = "\
    ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn\n\
    SNCSEQ = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/seq\n\
    SSCAN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/sscan\n\
    CALC = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/calc\n\
    PCRE = ${RECIPE_SYSROOT}${prefix}"

do_configure:append() {
    # Upstream builds the pcre submodule into TOP/lib/<arch> and the Makefile
    # expects libpcre there as a make prerequisite ("LIB_LIBS += pcre"). Yocto
    # builds libpcre for the target without static archives and installs it to
    # /usr/lib, so it is neither at TOP/lib nor an .a. Copy the staged shared
    # library into TOP/lib/<arch> where both the prerequisite and the
    # SHRLIB_SEARCH_DIRS find it; do_install:prepend removes the copies again
    # so they do not enter the package.
    mkdir -p ${S}/lib/${EPICS_TARGET_ARCH}
    cp ${RECIPE_SYSROOT}/usr/lib/libpcre.so* ${S}/lib/${EPICS_TARGET_ARCH}/
}

do_install:prepend() {
    rm -f ${S}/lib/${EPICS_TARGET_ARCH}/libpcre*
}
