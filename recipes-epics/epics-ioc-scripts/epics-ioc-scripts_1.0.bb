SUMMARY = "Start and port-management scripts for EPICS IOC applications"
DESCRIPTION = "The ioc-start.sh procServ wrapper, the ioc-ports.sh slot \
helpers and the ioc-instance-add helper that epics-ioc-systemd installs into \
every IOC directory it builds. The per-IOC values are supplied by a small \
ioc-env file generated next to the copies in each IOC directory."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://ioc-start.sh \
           file://ioc-ports.sh \
           file://ioc-instance-add \
"
S = "${WORKDIR}"

do_install() {
    install -d ${D}${datadir}/epics/ioc
    install -m 0755 ${WORKDIR}/ioc-start.sh \
                    ${WORKDIR}/ioc-ports.sh \
                    ${WORKDIR}/ioc-instance-add \
        ${D}${datadir}/epics/ioc/
}

FILES:${PN} = "${datadir}/epics/ioc/"
