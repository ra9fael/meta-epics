SUMMARY = "procServ process server for EPICS IOCs"
DESCRIPTION = "procServ wraps an EPICS IOC in a supervised child process and \
provides a telnet console for it, optionally restarting the IOC when it exits."
HOMEPAGE = "https://github.com/ralphlange/procServ"

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=d32239bcb673463ab874e80d47fae504"

# Track the latest master so the console keeps up with upstream fixes. AUTOREV
# makes BitBake query the remote on every parse (BB_SRCREV_POLICY defaults to
# "clear") and marks the recipe BB_DONT_CACHE, so it is rebuilt each run and
# needs network access. PV contains "+git", so package.bbclass appends the
# revision to PKGV and rootfs upgrades are seen when master moves.
PV = "2.8.0+git"
SRC_URI = "git://github.com/ralphlange/procServ;protocol=https;branch=master"
SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

EXTRA_OECONF = "--disable-doc"

# remote = accept console connections from other hosts. It only changes the
# compile-time default; procServ still needs -A/--allow at runtime.
PACKAGECONFIG ??= "remote"
PACKAGECONFIG[remote] = "--enable-access-from-anywhere,--disable-access-from-anywhere"

inherit autotools
