SUMMARY = "Generic runtime for EPICS IOC instances under systemd"
DESCRIPTION = "The epics-ioc@.service template, the ioc-start.sh dispatcher \
and the port-slot helpers. An instance is a named registry entry \
(/etc/epics/instances/<name>.env in the image, optional overrides in \
/boot/iocs/<name>.env) that points at an IOC application directory and \
carries the instance identity; procServ runs one-shot so restart policy \
stays with systemd."
HOMEPAGE = "https://github.com/ra9fael/meta-impcas-epics"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://ioc-start.sh \
           file://ioc-ports.sh \
           file://ioc-instance-add \
           file://epics-ioc@.service \
"
S = "${WORKDIR}"

inherit systemd

# Site-level defaults, written into the generated epics-ioc-env data file.
EPICS_IOC_ENV_ROOT ?= "/etc/epics/instances"
EPICS_IOC_MACHINE_ENV_ROOT ?= "/boot/iocs"
EPICS_IOC_PORT_BASE ?= "21000"
EPICS_IOC_RUN_DIR ?= "/run/epics"
PROCSERV_ARGS ?= "-A --oneshot"

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "epics-ioc@.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install() {
    install -d ${D}${libexecdir}/epics-ioc ${D}${sbindir} ${D}${systemd_system_unitdir}
    install -d ${D}${EPICS_IOC_ENV_ROOT}

    # Static runtime data. EPICS_TARGET_ARCH is baked in: this package is
    # built for the target.
    cat > ${D}${libexecdir}/epics-ioc/epics-ioc-env <<EOF
# Site-level values for the epics-ioc runtime scripts (part of the
# epics-ioc-scripts package). Per-instance values live in the registry.
ENV_ROOT="${EPICS_IOC_ENV_ROOT}"
MACHINE_ENV_ROOT="${EPICS_IOC_MACHINE_ENV_ROOT}"
PORT_BASE="${EPICS_IOC_PORT_BASE}"
RUN_DIR="${EPICS_IOC_RUN_DIR}"
PROCSERV_ARGS="${PROCSERV_ARGS}"
EPICS_TARGET_ARCH="${EPICS_TARGET_ARCH}"
EOF

    install -m 0755 ${WORKDIR}/ioc-start.sh ${WORKDIR}/ioc-ports.sh \
        ${D}${libexecdir}/epics-ioc/
    install -m 0755 ${WORKDIR}/ioc-instance-add ${D}${sbindir}/ioc-instance-add
    install -m 0644 ${WORKDIR}/epics-ioc@.service ${D}${systemd_system_unitdir}/epics-ioc@.service
    sed -i s,@LIBEXECDIR@,${libexecdir},g ${D}${systemd_system_unitdir}/epics-ioc@.service
}

FILES:${PN} = "${libexecdir}/epics-ioc/ \
               ${sbindir}/ioc-instance-add \
               ${systemd_system_unitdir}/epics-ioc@.service \
               ${EPICS_IOC_ENV_ROOT} \
"

RDEPENDS:${PN} = "procserv"
