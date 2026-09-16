SUMMARY = "EPICS PVXS module"
DESCRIPTION = "PVAccess network library for server and client applications, \
the modern C++ implementation of PVAccess (the library behind QSRVng). \
Requires EPICS Base 7 or later."
HOMEPAGE = "https://github.com/epics-base/pvxs"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3775480a712fc46a69647678acb234cb"
SRC_URI = "git://github.com/epics-base/pvxs;protocol=https;nobranch=1"
# tag 1.5.2.
SRCREV = "8e00eaecdee5ce8a474704e70d820e6f92693fa1"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "pvxs"

# pvxs is a pure library: it builds no host-side tools, so the host pass
# (which would also require libevent headers on the build host) is skipped,
# the same way the class skips it for recipes with EPICS_MODULES.
EPICS_MAKE_EXTRA = "CROSS_ARCHS="

# libevent lives in the default sysroot search paths, so pvxs's LIBEVENT
# machinery (which would otherwise look for a bundled copy) finds it without
# extra configuration. The threading-enabled build OE produces satisfies the
# #error check in evhelper.cpp.
DEPENDS += "libevent"
RDEPENDS:${PN} += "libevent"
