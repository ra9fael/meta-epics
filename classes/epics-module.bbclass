# Common EPICS build machinery for cross-built EPICS modules.
#
# EPICS Base uses this class as well: it sets EPICS_MODULE_NAME="base",
# EPICS_WRITE_RELEASE="0", EPICS_HOST_PASS="1" and overrides do_configure /
# do_install for its own layout (filtered bin/lib, Perl modules, profile.d).
#
# Modules inherit the class and normally only provide the usual recipe
# metadata plus EPICS_MODULES (or EPICS_RELEASE_EXTRA) for their own EPICS
# dependencies. The shared variables and helper functions live in
# epics-common.bbclass; this class adds the module-specific defaults and the
# build tasks.

inherit epics-common

# Install root for support modules. EPICS Base overrides this to
# ${EPICS_PREFIX} so Base stays at /opt/epics/base[-<ver>]; every other
# module installs under /opt/epics/modules/<name>[-<ver>].
EPICS_INSTALL_BASE ?= "${EPICS_PREFIX}/modules"

# Build-time dependency on EPICS Base; empty when the recipe is Base itself.
EPICS_DEPENDS_BASE ??= "epics-base"

# Runtime dependencies; shlibs scanning does not cover ${EPICS_PREFIX}, so they
# must be declared explicitly.
EPICS_RDEPENDS ??= "epics-base"

# Generate configure/RELEASE with EPICS_BASE. EPICS Base has no
# configure/RELEASE: set to 0 there.
EPICS_WRITE_RELEASE ??= "1"

# Build and install the host-architecture pass before the target pass. Modules
# normally do not build host tools (they use the staged Base tools); modules
# that ship a host tool (for example SNCSEQ's snc) set this to 1.
EPICS_HOST_PASS ??= "0"

DEPENDS += "${EPICS_DEPENDS_BASE}"
RDEPENDS:${PN} += "${EPICS_RDEPENDS}"

# Stage this module for future EPICS module recipes. Staging the whole
# ${EPICS_PREFIX} would also export unrelated files if a user sets
# EPICS_PREFIX to a broad directory such as /usr.
SYSROOT_DIRS += " \
    ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION} \
    ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME} \
"

FILES:${PN} += " \
    ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION} \
    ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME} \
"

# Everything ships in ${PN} (single-package layout, kept intentionally so the
# target can be used for development). The versioned .so symlinks therefore
# live in the runtime package, which dev-so QA would otherwise reject.
INSANE_SKIP:${PN} += "dev-so"

FILES:${PN}-staticdev += " \
    ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION}/lib/${EPICS_TARGET_ARCH}/*.a \
"

do_configure() {
    install -d ${S}/configure
    epics_generate_config_site
    epics_generate_host_config_site
    if [ "${EPICS_WRITE_RELEASE}" = "1" ]; then
        epics_generate_release
    fi

    # A module that links other EPICS modules needs an rpath to each of their
    # library directories -- both so its own executables link (the linker
    # resolves the DT_NEEDED of linked module libraries through it) and so
    # they load on the target without LD_LIBRARY_PATH.
    for modlibdir in ${EPICS_MODULE_LIBDIRS}; do
        echo "USR_LDFLAGS += -Wl,-rpath,${modlibdir}" \
            >> ${S}/${EPICS_CONFIG_SITE_FILE}
    done
}

do_compile() {
    # Host tools run during the build need the staged Base host libraries and
    # the host tools built in this tree, so expose those directories only for
    # this task.
    export LD_LIBRARY_PATH="${RECIPE_SYSROOT}${EPICS_PREFIX}/base/lib/${EPICS_HOST_ARCH}:${S}/lib/${EPICS_HOST_ARCH}:${RECIPE_SYSROOT_NATIVE}/usr/lib:${LD_LIBRARY_PATH}"

    # The BitBake flag sets (CFLAGS/BUILD_CFLAGS, ...) reach the build through
    # the generated CONFIG_SITE files as += additions; do not pass them as
    # make command-line variables, which would disable the += that module
    # Makefiles use for feature macros.
    if [ "${EPICS_HOST_PASS}" = "1" ]; then
        oe_runmake -C ${S} install.${EPICS_HOST_ARCH} \
            EPICS_HOST_ARCH="${EPICS_HOST_ARCH}" \
            CROSS_COMPILER_TARGET_ARCHS="${EPICS_TARGET_ARCH}" \
            ${EPICS_MAKE_EXTRA}
    fi

    # Build and install target libraries and executables with the BitBake cross
    # compiler. The compiler commands (including --sysroot) and the flag sets
    # come from the generated CONFIG_SITE files.
    if [ "${EPICS_TARGET_ARCH}" != "${EPICS_HOST_ARCH}" ]; then
        oe_runmake -C ${S} install.${EPICS_TARGET_ARCH} \
            EPICS_HOST_ARCH="${EPICS_HOST_ARCH}" \
            CROSS_COMPILER_TARGET_ARCHS="${EPICS_TARGET_ARCH}" \
            ${EPICS_MAKE_EXTRA}
    fi
}

do_install() {
    install_dir=${D}${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION}
    install -d ${install_dir}

    epics_install_subdirs ${install_dir}

    # A cross build installs both a host and a target bin/lib tree. Only the
    # target architecture belongs in the package: the target strip/objcopy
    # cannot process host binaries and they must not run on the target.
    if [ "${EPICS_HOST_ARCH}" != "${EPICS_TARGET_ARCH}" ]; then
        rm -rf ${install_dir}/bin/${EPICS_HOST_ARCH} \
               ${install_dir}/lib/${EPICS_HOST_ARCH}
    fi

    # The generated CONFIG_SITE files are build-time only: they embed the
    # build-host toolchain and flags. Dependents generate their own, so the
    # installed copies would only leak build paths.
    rm -f ${install_dir}/${EPICS_CONFIG_SITE_FILE} \
          ${install_dir}/${EPICS_CONFIG_SITE_HOST_FILE}

    epics_scrub_build_paths ${install_dir}
    epics_install_symlink
}
