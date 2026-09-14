SUMMARY = "EPICS mqtt module"
DESCRIPTION = "MQTT support for EPICS: an asyn port driver bridging MQTT \
brokers and EPICS records."
HOMEPAGE = "https://github.com/epics-modules/mqtt"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=e49f4652534af377a713df3d9dec60cb"
SRC_URI = "git://github.com/epics-modules/mqtt;protocol=https;nobranch=1"
# Upstream tag 1.0.0.
SRCREV = "56112dce218cd503deb4219391d16a599b49ff17"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "mqtt"

# mqttSupport links asyn, autoparamDriver, qsrv (from Base's bundled PV Access)
# and the Eclipse paho client libraries.
EPICS_MODULES = "asyn autoparamDriver"
DEPENDS += "paho-mqtt-cpp"
RDEPENDS:${PN} += "paho-mqtt-cpp"

# PAHO_CPP_LIB is used both for -L and a hardcoded -Wl,-rpath; the Yocto paho
# libraries live in /usr/lib (the default runtime search path), so keep the
# -L/-I pointing into the sysroot and drop the rpath that would only embed a
# build-host path.
EPICS_RELEASE_EXTRA = "\
    ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn\n\
    AUTOPARAMDRIVER = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/autoparamDriver\n\
    PAHO_CPP_INC = ${RECIPE_SYSROOT}/usr/include\n\
    PAHO_CPP_LIB = ${RECIPE_SYSROOT}/usr/lib"

do_configure:append() {
    sed -i 's|[ \t]*-Wl,-rpath,$(PAHO_CPP_LIB)||' ${S}/mqttSup/src/Makefile
}
