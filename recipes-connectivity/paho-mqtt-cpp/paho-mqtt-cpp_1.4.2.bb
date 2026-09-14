SUMMARY = "Paho MQTT - C++ libraries for the MQTT and MQTT-SN protocols"
DESCRIPTION = "Client implementation of open and standard messaging protocols for Machine-to-Machine (M2M) and Internet of Things (IoT)."
HOMEPAGE = "http://www.eclipse.org/paho/"
SECTION = "console/network"
LICENSE = "EPL-2.0 | EDL-1.0"

LIC_FILES_CHKSUM = " \
    file://include/mqtt/message.h;beginline=9;endline=18;md5=c5ceecf5ab99d44dcfaaabdce289071b \
    file://edl-v10;md5=3adfcc70f5aeb7a44f3f9b495aa1fbf3 \
    file://epl-v20;md5=d9fc0efef5228704e7f5b37f27192723 \
"

# Newer than meta-oe's 1.3.2: the epics-modules/mqtt module uses
# mqtt::token::get_error_message(), which only exists from 1.4.x on. 1.4.x
# also switched to GNUInstallDirs (CMAKE_INSTALL_LIBDIR/BINDIR), so the
# libdir patch meta-oe carries for 1.3.2 is no longer needed.
SRC_URI = "git://github.com/eclipse/paho.mqtt.cpp;protocol=https;branch=v1.4.x"
SRCREV = "c310578ee68d38cd53e79d7107fb41dc68dbafe0"

DEPENDS = "openssl paho-mqtt-c"

S = "${WORKDIR}/git"

inherit cmake

EXTRA_OECMAKE += "-DPAHO_WITH_SSL=ON"
