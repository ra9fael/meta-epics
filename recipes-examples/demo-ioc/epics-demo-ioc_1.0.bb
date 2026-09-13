SUMMARY = "EPICS demo IOC (asyn + autosave, per-instance ports)"
DESCRIPTION = "Minimal makeBaseApp IOC, used as the template for the BLM IOC. \
It drives an asyn IP port to a per-instance echo device, exercises \
asynOctetWriteRead with a settable command, saves and restores state with \
autosave, and runs under procServ with the per-instance port allocation from \
epics-ioc-systemd."
HOMEPAGE = "https://github.com/epics-modules"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=81d0885bb2f6b221905b9696cc378e14"

SRC_URI = " \
    file://LICENSE \
    file://Makefile \
    file://configure \
    file://demoIocApp \
    file://iocBoot \
    file://instances \
"
S = "${WORKDIR}"

inherit epics-ioc-systemd

EPICS_MODULE_NAME = "epics-demo-ioc"

DEPENDS += "epics-asyn epics-autosave"
# socat provides the echo device the IOC's asyn IP port talks to.
RDEPENDS:${PN} += "epics-asyn epics-autosave socat"

# configure/RELEASE entries, staged inside the recipe sysroot.
EPICS_RELEASE_EXTRA = "\
    ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn\n\
    AUTOSAVE = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/autosave"

# Runtime search paths for the modules the IOC links.
EPICS_IOC_LIBDIRS = "${EPICS_PREFIX}/modules/asyn/lib/${EPICS_TARGET_ARCH} \
                     ${EPICS_PREFIX}/modules/autosave/lib/${EPICS_TARGET_ARCH}"

# Port block 0 -> 21000-21099.
IOC_PORT_BLOCK_INDEX = "0"

IOC_APP_NAME = "demoIoc"
IOC_PATH = "iocBoot/iocdemo"
IOC_ST_CMD = "st.cmd"

# Two instances (ioc1 at 21000, ioc2 at 21010) demonstrate the isolation.
EPICS_IOC_MULTI_INSTANCE = "1"
EPICS_IOC_START_PRE = "start_sim_device"
EPICS_IOC_INSTANCE_ENVS = "${WORKDIR}/instances/ioc1.env ${WORKDIR}/instances/ioc2.env"
