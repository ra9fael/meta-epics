# Build and package an EPICS IOC application (a tree created by makeBaseApp).
#
# The generic epics-module class installs the top-level subdirectories a support
# module exports. An IOC application additionally needs the generated
# iocBoot/<ioc>/envPaths -- which only the host-architecture pass produces -- and
# an ELF runtime search path to every support module it links against.

inherit epics-module

# IOC applications live beside the support modules, but under iocs/. A plain
# assignment, not ?=: epics-module already claimed the variable with ?=, and the
# first ?= wins, so an IOC recipe has to override it here.
EPICS_INSTALL_BASE = "${EPICS_PREFIX}/iocs"

# An IOC has no include/, cfg/ or templates/ to export.
EPICS_INSTALL_SUBDIRS ?= "bin lib db dbd iocBoot"

# Absolute runtime library directories of the support modules this IOC links,
# space separated. Derived from EPICS_MODULES (one entry per module); set it
# explicitly only to add directories EPICS_MODULES does not cover. They are
# turned into rpath entries; the Base library directory is added by
# epics-module already.
EPICS_IOC_LIBDIRS ?= ""

# A target IOC only needs the target pass: Base supplies the host tools. The
# host-architecture iocBoot action is run explicitly below to make envPaths.
EPICS_HOST_PASS ?= "0"

# EPICS makes the cross-architecture target depend on the host-architecture one
# (configure/RULES_ARCHS), which would link a host copy of the IOC against the
# host libraries of every support module. Those are deliberately not packaged,
# and a host IOC is not used, so clear CROSS_ARCHS for these make invocations.
EPICS_MAKE_EXTRA = "CROSS_ARCHS="

# Append the runtime search paths to the generated target CONFIG_SITE. The
# module class sets LINKER_USE_RPATH = NO there, so these explicit entries are
# the only rpaths and the IOC runs without LD_LIBRARY_PATH on the target.
#
# IOCS_APPL_TOP is the install location the application is built for. EPICS
# records it in iocBoot/<ioc>/envPaths and in the generated
# *_registerRecordDeviceDriver.cpp -- which compares it against the runtime TOP
# at iocInit and warns when they differ. Left alone it is the build directory,
# so the build path would be embedded in the binary and every start would warn.
# Both CONFIG_SITE files are set: envPaths comes from the host pass, the driver
# from the target pass. Loop variables are prefixed so that a name like libdir
# cannot collide with a same-named BitBake variable (libdir is /usr/lib).
do_configure:append() {
    for ioc_libdir in \
        ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}/lib/${EPICS_TARGET_ARCH} \
        ${EPICS_IOC_LIBDIRS}; do
        echo "USR_LDFLAGS += -Wl,-rpath,${ioc_libdir}" \
            >> ${S}/${EPICS_CONFIG_SITE_FILE}
    done

    for ioc_site in ${EPICS_CONFIG_SITE_FILE} ${EPICS_CONFIG_SITE_HOST_FILE}; do
        echo "IOCS_APPL_TOP = ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}" \
            >> ${S}/${ioc_site}
    done
}

# iocBoot/<ioc>/Makefile pins ARCH = $(EPICS_HOST_ARCH), so its buildInstall
# target (which runs convertRelease.pl to write envPaths) exists only for the
# host architecture. The target pass above therefore does not create it; run
# that one action separately.
do_compile:append() {
    oe_runmake -C ${S}/iocBoot install.${EPICS_HOST_ARCH} \
        EPICS_HOST_ARCH="${EPICS_HOST_ARCH}" \
        CROSS_COMPILER_TARGET_ARCHS="${EPICS_TARGET_ARCH}"
}

do_install:append() {
    install_dir=${D}${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION}
    # Loop variables are prefixed to stay clear of same-named BitBake variables.
    ioc_versioned="${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION}"

    # envPaths and the EPICS *.local files record the paths from
    # configure/RELEASE, which point into the recipe sysroot. The module class
    # already ran epics_scrub_build_paths, which rewrote the ${S} prefix of
    # those paths to the versioned install directory; the sysroot therefore now
    # appears as <versioned>/recipe-sysroot. Drop that (and the literal
    # RECIPE_SYSROOT, should any file predate the rewrite) so the paths name the
    # installed support modules.
    for ioc_sysroot in "${RECIPE_SYSROOT}" "${ioc_versioned}/recipe-sysroot"; do
        grep -Ilr -- "$ioc_sysroot" ${install_dir} | \
            xargs -r sed -i "s,$ioc_sysroot,,g"
    done

    # The application top was rewritten to the versioned install directory too.
    # Use the version-independent symlink instead, so a version bump does not
    # break the recorded TOP.
    grep -Ilr -- "$ioc_versioned" ${install_dir} | \
        xargs -r sed -i \
            "s,$ioc_versioned,${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME},g"
}
