SUMMARY = "EPICS asyn module"
DESCRIPTION = "AsynDriver: low-level asynchronous device communication for EPICS \
(serial, IP, VXI-11, GPIB, USB) and the asynPortDriver framework."
HOMEPAGE = "https://github.com/epics-modules/asyn"

LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=9f42f43716fb1d5e8498617125cb3c21"

SRC_URI = "git://github.com/epics-modules/asyn;protocol=https;branch=master"
SRCREV = "76f6164757d54b0b7dae22a911fe78fd20a95525"
S = "${WORKDIR}/git"

inherit epics-module

# BPN is epics-asyn, so the on-target module name must be set explicitly.
EPICS_MODULE_NAME = "asyn"

# VXI-11 uses ONC RPC (glibc >= 2.32 dropped SunRPC): libtirpc for the
# headers/library and rpcgen to generate the XDR sources. rpcsvc-proto-native
# supplies a hermetic rpcgen; without it the build would silently fall back to
# the host's /usr/bin/rpcgen.
DEPENDS += "libtirpc rpcsvc-proto-native"
RDEPENDS:${PN} += "libtirpc"

# The test applications are PROD_IOC and would otherwise be built for the
# target and shipped. Keep only what a consumer needs.
EPICS_INSTALL_SUBDIRS = "lib db dbd include cfg templates html documentation"

do_configure:append() {
    # Drop the test application directories. They are not needed, and their
    # generated device-support registration references PV Access symbols that
    # the plain library link does not pull in. Only configure, makeSupport,
    # opi and the asyn library tree remain.
    sed -i -E '/^[[:space:]]*DIRS \+= (test|iocBoot|asyn\/asynPortDriver\/unittest)/d' \
        ${S}/Makefile

    # R4-46 keeps ONC RPC behind TIRPC and VXI-11 behind DRV_VXI11, both off
    # by default, and builds the tirpc include path from $(SYSROOT) (which
    # EPICS Base does not define). configure/CONFIG_SITE -includes this file;
    # the class provides SYSROOT for the target pass.
    cat >> ${S}/configure/CONFIG_SITE.local <<EOF
TIRPC = YES
DRV_VXI11 = YES
EOF
}
