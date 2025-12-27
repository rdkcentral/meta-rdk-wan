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

CFLAGS:append = " \
    -I${STAGING_INCDIR} \
    -I${STAGING_INCDIR}/rbus \
    -Werror \
    -Wall \
    -Wno-error=switch \
    "
CFLAGS:append = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', '-DFEATURE_RDKB_WAN_MANAGER', '', d)}"

LDFLAGS:append = " -lrbus -lrdkloggers"

EXTRA_OECONF += "RBUS_CFLAGS='-I${STAGING_INCDIR}/rbus' --enable-tests"


do_install () {
    # Install the epon_manager binary (use .libs for actual binary, not libtool wrapper)
    install -d ${D}${bindir}
    install -m 755 ${B}/src/core/.libs/epon_manager ${D}${bindir}/epon_manager
    
    # Install HAL mock library
    install -d ${D}${libdir}
    install -m 755 ${B}/tests/hal_mock/.libs/libepon_hal_mock.so.1.0.0 ${D}${libdir}/
    ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so.1
    ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so
    
    # Config files and scripts directories
    install -d ${D}/usr/rdk/eponmanager
    install -d ${D}${sysconfdir}/rdk/conf
    install -d ${D}${sysconfdir}/rdk/schemas
}

FILES_${PN} = " \
   ${bindir}/epon_manager \
   ${libdir}/libepon_hal_mock.so* \
   /usr/rdk/eponmanager \
   ${sysconfdir}/rdk/conf \
   ${sysconfdir}/rdk/schemas \
   "

FILES_${PN}-dbg = " \
    ${prefix}/rdk/eponmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
INSANE_SKIP_${PN} += "dev-deps useless-rpaths"
