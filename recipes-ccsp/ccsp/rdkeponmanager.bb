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

LDFLAGS:append = " -lrbus -lrdkloggers"

EXTRA_OECONF += "--enable-tests"


do_install_append () {
    # Install HAL mock library (not installed by default make install since it's in tests/)
    install -d ${D}${libdir}
    install -m 755 ${B}/tests/hal_mock/.libs/libepon_hal_mock.so.1.0.0 ${D}${libdir}/
    ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so.1
    ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so
    
    # Install test executables for on-target testing
    install -d ${D}${bindir}/epon-tests
    for test in ${B}/tests/unit/test_*; do
        [ -x "$test" ] && install -m 755 "$test" ${D}${bindir}/epon-tests/ || true
    done
    
    # Config files and scripts directories
    install -d ${D}/usr/rdk/eponmanager
}

FILES_${PN} = " \
   ${bindir}/epon_manager \
   ${bindir}/epon-tests/* \
   ${libdir}/libepon_hal_mock.so* \
   /usr/rdk/eponmanager \
   "

FILES_${PN}-dbg = " \
    ${prefix}/rdk/eponmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
INSANE_SKIP_${PN} += "dev-deps useless-rpaths"
