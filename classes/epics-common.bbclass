# Shared variables and shell helpers for the EPICS classes.
#
# This class defines no tasks of its own: it is the library that
# epics-module (support modules) and, through it, epics-ioc (IOC
# applications) build on. Splitting the helpers out keeps the class
# hierarchy explicit -- common carries the reusable machinery, the
# subclasses carry defaults and tasks.

# Installation prefix on the target.
EPICS_PREFIX ?= "/opt/epics"

# Map the BitBake target architecture onto the EPICS target architecture
# name. aarch64 is the default (PetaLinux ARM64 targets); x86_64 supports
# native-arch builds; 32-bit ARM maps to linux-arm.
def epics_target_arch(d):
    mapping = {
        "aarch64": "linux-aarch64",
        "x86_64":  "linux-x86_64",
        "arm":     "linux-arm",
    }
    return mapping.get(d.getVar("TARGET_ARCH"), "linux-aarch64")

EPICS_TARGET_ARCH ??= "${@epics_target_arch(d)}"

EPICS_HOST_ARCH ?= "linux-x86_64"

# Installed directory below ${EPICS_INSTALL_BASE}. A version-independent
# symlink ${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME} is created so downstream
# recipes can reference a staged dependency without tracking its version.
EPICS_MODULE_NAME ??= "${BPN}"
EPICS_MODULE_VERSION ??= "${PV}"

# File receiving the generated target toolchain settings. EPICS reads
# configure/CONFIG_SITE.$(EPICS_HOST_ARCH).$(T_A) after the settings inherited
# from EPICS Base, so a module-local file takes precedence. EPICS Base itself
# reads configure/os/CONFIG_SITE.$(EPICS_HOST_ARCH).$(T_A) from its own tree.
EPICS_CONFIG_SITE_FILE ??= "configure/CONFIG_SITE.${EPICS_HOST_ARCH}.${EPICS_TARGET_ARCH}"

# Same kind of file, but for the host-architecture pass. The host pass runs
# with T_A = $(EPICS_HOST_ARCH), so the host-specific (not .Common) file is
# the only one the target pass does not read. It carries the BitBake build
# flags so they reach the host pass without being passed as make command-line
# variables (a command-line assignment would make the += in module Makefiles
# a no-op).
EPICS_CONFIG_SITE_HOST_FILE ??= "configure/CONFIG_SITE.${EPICS_HOST_ARCH}.${EPICS_HOST_ARCH}"

# Subdirectories copied verbatim into the target installation. Recipes that
# must filter architecture-specific subdirectories (EPICS Base) trim this
# list and install those parts themselves.
EPICS_INSTALL_SUBDIRS ??= "bin lib db dbd include cfg templates html doc"

# Extra configure/RELEASE lines for this recipe's EPICS dependencies, e.g.
#   EPICS_RELEASE_EXTRA = "ASYN = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/asyn"
# Set automatically from EPICS_MODULES when that variable is used instead.
EPICS_RELEASE_EXTRA ??= ""

# CHECK_RELEASE compares the values each dependency recorded in its own
# configure/RELEASE against the ones used by the current recipe. Staged
# dependencies carry the sysroot path of their own build, which never matches
# the consumer's sysroot, so the check would always report false mismatches.
EPICS_CHECK_RELEASE ??= "NO"

# Extra variable assignments passed to the EPICS make invocations. EPICS makes
# every cross-architecture target depend on the host-architecture one
# (configure/RULES_ARCHS creates "install.<cross> : install.<host>"); a recipe
# that does not need the host pass can clear CROSS_ARCHS to avoid it.
EPICS_MAKE_EXTRA ??= ""

# Stage the built host-architecture bin/lib directories into the recipe
# sysroot (without packaging them). Recipes whose build produces host tools
# that dependent recipes run -- the sequencer's snc is the classic case -- set
# this to 1.
EPICS_STAGE_HOST_TOOLS ??= "0"

inherit perlnative

# EPICS_MODULES lists the EPICS support modules this recipe builds against,
# and derives the repetitive declarations from it. An entry is either a
# directory name ("asyn" -> RELEASE variable ASYN) or an explicit
# "VARIABLE=directory" pair ("SNCSEQ=seq" for the sequencer, whose RELEASE
# variable does not match its directory name). For every entry the class
# appends the matching epics-<directory> recipe to DEPENDS and RDEPENDS:${PN}
# (shlibs scanning does not see anything under ${EPICS_PREFIX}), and derives
# the configure/RELEASE lines. EPICS_RELEASE_EXTRA is only filled when the
# recipe did not set it, so a hand-written value still wins.
python () {
    modules = (d.getVar("EPICS_MODULES") or "").split()
    if not modules:
        return

    depends = []
    release = []
    libdirs = []
    for entry in modules:
        if "=" in entry:
            var, directory = entry.split("=", 1)
        else:
            var, directory = entry.upper(), entry
        depends.append("epics-" + directory)
        release.append("%s = ${RECIPE_SYSROOT}${EPICS_PREFIX}/modules/%s"
                       % (var, directory))
        libdirs.append("${EPICS_PREFIX}/modules/%s/lib/${EPICS_TARGET_ARCH}"
                       % directory)

    deps = " ".join(depends)
    d.appendVar("DEPENDS", " " + deps)
    d.appendVar("RDEPENDS:" + d.getVar("PN"), " " + deps)

    if not (d.getVar("EPICS_RELEASE_EXTRA") or "").strip():
        d.setVar("EPICS_RELEASE_EXTRA", "\\n".join(release))

    libdirs = " ".join(libdirs)
    if not (d.getVar("EPICS_IOC_LIBDIRS") or "").strip():
        d.setVar("EPICS_IOC_LIBDIRS", libdirs)
}

# Write the target toolchain settings. The full BitBake compiler commands are
# passed verbatim: they may consist of several words (ccache, --sysroot=...)
# and GNU make executes them as-is, so no string splitting is needed and ccache
# stays supported. LD keeps the upstream "-r" default (relocatable object
# merging); final linking goes through CCC/CXX.
epics_generate_config_site() {
    install -d "$(dirname ${S}/${EPICS_CONFIG_SITE_FILE})"
    cat > ${S}/${EPICS_CONFIG_SITE_FILE} <<EOF
# Generated by BitBake for PetaLinux.
CHECK_RELEASE = ${EPICS_CHECK_RELEASE}
CC = ${CC}
CXX = ${CXX}
CCC = ${CXX}
LD = ${TARGET_PREFIX}ld -r
AR = ${AR} -rc
RANLIB = ${RANLIB}
# Cross sysroot. Some modules (e.g. asyn >= 4.46) build include paths from
# the SYSROOT variable but EPICS Base does not define it. Mentioned without
# $(...) so the generating shell's heredoc does not try to run it as a command.
SYSROOT = ${RECIPE_SYSROOT}
# The staged Base carries the installed ORIGIN-rpath settings, which would
# make makerrpath embed build-tree paths into cross-built ELF files (they do
# not exist on the target). Link-time library resolution happens through the
# sysroot anyway (CC contains --sysroot); runtime resolution gets an explicit
# rpath to the final Base location instead.
LINKER_USE_RPATH = NO
USR_LDFLAGS += -Wl,-rpath,${EPICS_PREFIX}/base/lib/${EPICS_TARGET_ARCH}
# BitBake flags. They must be appended (+=), not assigned on the make command
# line: a command-line assignment makes the += used by module Makefiles for
# feature macros (e.g. asyn's -DHAVE_DEVINT64) a no-op.
USR_CFLAGS += ${CFLAGS}
USR_CXXFLAGS += ${CXXFLAGS}
USR_LDFLAGS += ${LDFLAGS}
EOF
}

# Same for the host pass, using the BitBake build flags. CHECK_RELEASE is set
# here too: the application template defaults it to YES, and the staged
# dependencies record the sysroot they were built in, which never matches this
# recipe's sysroot, so leaving it on would report false mismatches.
epics_generate_host_config_site() {
    install -d "$(dirname ${S}/${EPICS_CONFIG_SITE_HOST_FILE})"
    cat > ${S}/${EPICS_CONFIG_SITE_HOST_FILE} <<EOF
# Generated by BitBake for PetaLinux (host pass).
CHECK_RELEASE = ${EPICS_CHECK_RELEASE}
USR_CFLAGS += ${BUILD_CFLAGS}
USR_CXXFLAGS += ${BUILD_CXXFLAGS}
USR_LDFLAGS += ${BUILD_LDFLAGS}
EOF
}

# Point configure/RELEASE at the staged EPICS dependencies. Appended so it wins
# over earlier definitions in the file.
epics_generate_release() {
    install -d ${S}/configure
    cat >> ${S}/configure/RELEASE <<EOF

# Generated by BitBake: staged locations inside the recipe sysroot.
EPICS_BASE = ${RECIPE_SYSROOT}${EPICS_PREFIX}/base
EOF
    # printf %b interprets the \n a recipe uses to separate several
    # assignments (BitBake keeps \n as two literal characters in the value).
    if [ -n "${EPICS_RELEASE_EXTRA}" ]; then
        printf '%b\n' "${EPICS_RELEASE_EXTRA}" >> ${S}/configure/RELEASE
    fi
}

epics_install_subdirs() {
    install_dir="$1"
    for subdir in ${EPICS_INSTALL_SUBDIRS}; do
        [ -d ${S}/${subdir} ] || continue
        install -d ${install_dir}/${subdir}
        cp -R --no-preserve=ownership ${S}/${subdir}/. ${install_dir}/${subdir}/
    done
}

# Installed text files must not publish the temporary source path or the build
# TMPDIR (the latter also trips the buildpaths QA check). Only comment lines are
# dropped: those are C preprocessor line markers, never functional settings.
epics_scrub_build_paths() {
    install_dir="$1"
    grep -Ilr "${S}" ${install_dir} | \
        xargs -r sed -i "s|${S}|${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION}|g"
    grep -Ilr "${TMPDIR}" ${install_dir} | \
        xargs -r sed -i "\|^#.*${TMPDIR}|d"
}

epics_install_symlink() {
    install -d ${D}${EPICS_INSTALL_BASE}
    ln -sfn ${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION} \
        ${D}${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}
}

# Stage built host-architecture bin/lib directories into the sysroot for
# dependent recipes, without packaging them (EPICS_STAGE_HOST_TOOLS = 1).
epics_stage_host_tools() {
    [ "${EPICS_STAGE_HOST_TOOLS}" = "1" ] || return 0
    for dir in \
        ${SYSROOT_DESTDIR}${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}-${EPICS_MODULE_VERSION} \
        ${SYSROOT_DESTDIR}${EPICS_INSTALL_BASE}/${EPICS_MODULE_NAME}; do
        install -d ${dir}/bin/${EPICS_HOST_ARCH}
        cp -R --no-preserve=ownership ${S}/bin/${EPICS_HOST_ARCH}/. \
            ${dir}/bin/${EPICS_HOST_ARCH}/
        if [ -d ${S}/lib/${EPICS_HOST_ARCH} ]; then
            install -d ${dir}/lib/${EPICS_HOST_ARCH}
            cp -R --no-preserve=ownership ${S}/lib/${EPICS_HOST_ARCH}/. \
                ${dir}/lib/${EPICS_HOST_ARCH}/
        fi
    done
}
SYSROOT_PREPROCESS_FUNCS:append = " epics_stage_host_tools"
