SUMMARY = "procServ process server for EPICS IOCs"
DESCRIPTION = "procServ wraps an EPICS IOC in a supervised child process and \
provides a telnet console for it, optionally restarting the IOC when it exits."
HOMEPAGE = "https://github.com/ralphlange/procServ"

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d32239bcb673463ab874e80d47fae504"

# Master, pinned to a commit. AUTOREV was tried first: it makes BitBake run
# "git ls-remote" on every single recipe parse, so one flaky network link
# breaks every build at parse time and the recipe is rebuilt each run. Pin the
# commit instead and re-resolve master when an update is wanted:
#   git ls-remote https://github.com/ralphlange/procServ HEAD
# PV contains "+git", so package.bbclass appends the revision to PKGV and
# rootfs upgrades are seen when the pin moves.
PV = "2.8.0+git"
SRC_URI = "git://github.com/ralphlange/procServ;protocol=https;branch=master"
# master, aeb33d2083 as of 2026-09-14.
SRCREV = "aeb33d20837291f33b97faca51454c61b1c3329c"

S = "${WORKDIR}/git"

EXTRA_OECONF = "--disable-doc"

# remote = accept console connections from other hosts. It only changes the
# compile-time default; procServ still needs -A/--allow at runtime.
PACKAGECONFIG ??= "remote"
PACKAGECONFIG[remote] = "--enable-access-from-anywhere,--disable-access-from-anywhere"

inherit autotools
