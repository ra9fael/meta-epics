SUMMARY = "EPICS xxx template module"
DESCRIPTION = "The upstream example/template support module, kept as the \
reference for new module recipes."
HOMEPAGE = "https://github.com/epics-modules/xxx"
LICENSE = "EPICS"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a2c259c010f2152379d7769be894bf4a"
SRC_URI = "git://github.com/epics-modules/xxx;protocol=https;nobranch=1"
# Upstream tag R6-3.
SRCREV = "2ffd39563028123aab340dbc53dc5f69cfbd3172"
S = "${WORKDIR}/git"
inherit epics-module
EPICS_MODULE_NAME = "xxx"
EPICS_MODULES = "asyn autosave"
