SUMMARY = "RDK EPON Manager component"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS = "rdk-logger rbus"

require recipes-ccsp/ccsp/ccsp_common.inc

# Build from feature/implementation branch
SRC_URI = "git://github.com/rdkcentral/epon-manager.git;branch=feature/implementation-rdk;protocol=https;name=EponManager;"
SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

inherit autotools pkgconfig

CFLAGS += " \
    -I${STAGING_INCDIR} \
    -Werror \
    -Wall \
    -Wno-error=switch \
    "

LDFLAGS:append = " -lrbus -lrdkloggers"

# Enable HAL mock library build for integration testing
EXTRA_OECONF += "--enable-tests"

do_install_append () {
    # Install HAL mock library for integration testing
    install -d ${D}${libdir}
    install -m 755 ${B}/tests/hal_mock/.libs/libepon_hal_mock.so.1.0.0 ${D}${libdir}/
    ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so.1
    ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so

    # Note: epon_hal_trigger is already installed by Makefile via libtool

    # Config files and scripts directories
    install -d ${D}/usr/rdk/eponmanager
}

FILES_${PN} = " \
   ${bindir}/epon_manager \
   ${bindir}/epon_hal_trigger \
   ${libdir}/libepon_hal_mock.so* \
   /usr/rdk/eponmanager \
   ${sysconfdir}/epon \
   "

FILES_${PN}-dbg = " \
    ${prefix}/rdk/eponmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
INSANE_SKIP:${PN} += "dev-deps useless-rpaths"
