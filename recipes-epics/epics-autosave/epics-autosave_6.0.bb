SUMMARY = "EPICS autosave module"
DESCRIPTION = "Save and restore EPICS process variables across IOC restarts."
HOMEPAGE = "https://github.com/epics-modules/autosave"

LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"

SRC_URI = "git://github.com/epics-modules/autosave;protocol=https;branch=master"
SRCREV = "03b94f803be4e1844699a9c56562c96406242591"
S = "${WORKDIR}/git"

inherit epics-module

# BPN is epics-autosave, so the on-target module name must be set explicitly.
EPICS_MODULE_NAME = "autosave"

# autosave only depends on EPICS Base. The asVerify program is PROD_HOST and is
# therefore not built for the target, so the class default install subdirs are
# sufficient (missing directories are skipped).
