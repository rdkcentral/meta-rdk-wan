SUMMARY = "RDK EPON Manager component"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS = "rdk-logger rbus rdkb-halif-epon hal-epon"
RDEPENDS_${PN} = "hal-epon"

require recipes-ccsp/ccsp/ccsp_common.inc

# Please use below part only for official release and release candidates
SRC_URI = "git://github.com/rdkcentral/epon-manager.git;branch=releases/1.0.0-main;protocol=https;name=EponManager;"
SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

inherit autotools pkgconfig systemd

CFLAGS += " \
    -I${STAGING_INCDIR} \
    -Werror \
    -Wall \
    -Wno-error=switch \
    "

LDFLAGS += " -lrbus -lrdkloggers -lhal_epon"

# Enable HAL mock library build for integration testing
# When --enable-tests is set, HAL mock library is built and used
# When --enable-tests is disabled, the actual HAL library (libhal_epon) is linked
ENABLE_TESTS ?= "--disable-tests"
EXTRA_OECONF += "${ENABLE_TESTS}"

# Systemd service
SYSTEMD_SERVICE_${PN} = "rdkeponmanager.service"

do_install_append () {
    # Install HAL mock library and trigger for integration testing (only if tests enabled)
    if [ "${ENABLE_TESTS}" = "--enable-tests" ]; then
        install -d ${D}${libdir}
        install -m 755 ${B}/tests/hal_mock/.libs/libepon_hal_mock.so.1.0.0 ${D}${libdir}/
        ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so.1
        ln -sf libepon_hal_mock.so.1.0.0 ${D}${libdir}/libepon_hal_mock.so
        # Note: epon_hal_trigger is already installed by Makefile via libtool
    fi

    # Config files and scripts directories
    install -d ${D}/usr/rdk/eponmanager

    # Install systemd service file
    install -d ${D}${systemd_unitdir}/system
    install -d ${D}${systemd_unitdir}/system/multi-user.target.wants
    install -m 0644 ${S}/systemd/utils/rdkeponmanager.service ${D}${systemd_unitdir}/system/
    ln -sf ../rdkeponmanager.service ${D}${systemd_unitdir}/system/multi-user.target.wants/
}

FILES_${PN} = " \
   ${bindir}/epon_manager \
   /usr/rdk/eponmanager \
   ${sysconfdir}/epon \
   ${systemd_unitdir}/system/rdkeponmanager.service \
   ${systemd_unitdir}/system/multi-user.target.wants/rdkeponmanager.service \
   "

# Add test files only when tests are enabled
FILES_${PN} += "${@bb.utils.contains('ENABLE_TESTS', '--enable-tests', '${bindir}/epon_hal_trigger ${libdir}/libepon_hal_mock.so*', '', d)}"

FILES_${PN}-dbg = " \
    ${prefix}/rdk/eponmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
INSANE_SKIP_${PN} += "dev-deps useless-rpaths"
