SUMMARY = "RdkXdslManager component"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "ccsp-common-library dbus rdk-logger utopia json-hal-lib avro-c hal-platform libparodus libunpriv"
require recipes-ccsp/ccsp/ccsp_common.inc

GIT_TAG = "v1.0.0"
SRC_URI = "git://github.com/rdkcentral/RdkXdslManager.git;branch=main;protocol=https;name=xDSLManager;tag=${GIT_TAG}"
PV = "${GIT_TAG}+git${SRCPV}"

S = "${WORKDIR}/git"

inherit autotools pkgconfig

CFLAGS_append = " \
    -I${STAGING_INCDIR} \
    -I${STAGING_INCDIR}/dbus-1.0 \
    -I${STAGING_LIBDIR}/dbus-1.0/include \
    -I${STAGING_INCDIR}/ccsp \
    -I ${STAGING_INCDIR}/syscfg \
    -I ${STAGING_INCDIR}/sysevent \
    -I${STAGING_INCDIR}/libparodus \
    "

DEPENDS_append = "${@bb.utils.contains("DISTRO_FEATURES", "seshat", " libseshat ", " ", d)}"
CFLAGS_append = "${@bb.utils.contains("DISTRO_FEATURES", "seshat", " -DENABLE_SESHAT ", " ", d)}"
LDFLAGS_append = "${@bb.utils.contains("DISTRO_FEATURES", "seshat", " -llibseshat ", " ", d)}"

EXTRA_OECONF_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', '--enable-gtestapp', '', d)}"
EXTRA_OECONF_append  = " ${@bb.utils.contains('DISTRO_FEATURES','kirkstone','','--with-ccsp-platform=bcm --with-ccsp-arch=arm',d)} "

LDFLAGS += " -lprivilege"

CFLAGS_append = "\
    ${@bb.utils.contains("DISTRO_FEATURES", "seshat", "-I${STAGING_INCDIR}/libseshat ", " ", d)} \
"
CFLAGS_append  = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', '-DFEATURE_RDKB_WAN_MANAGER', '', d)}"
LDFLAGS_append = " -lrt -lm"
LDFLAGS_remove_morty = " -lrt -lm"

do_install_append () {
    # Config files and scripts
    install -d ${D}/usr/rdk/xdslmanager
    install -d ${D}${bindir}
    install -d ${D}${sysconfdir}/rdk/conf
    install -d ${D}${exec_prefix}/ccsp/harvester/
    install -m 644 ${S}/source/TR-181/integration_src.shared/XdslReport.avsc ${D}/usr/ccsp/harvester/
    install -m 644 ${S}/config/RdkXdslManager.xml ${D}/usr/rdk/xdslmanager
    install -m 644 ${S}/config/xdsl_manager_conf.json ${D}${sysconfdir}/rdk/conf

    #JSON schema file
    install -d ${D}/${sysconfdir}/rdk/schemas
    install -m 644 ${S}/hal_schema/xdsl_hal_schema.json ${D}/${sysconfdir}/rdk/schemas
}

PACKAGES =+ "${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', '${PN}-gtest', '', d)}"

FILES_${PN}-gtest = "\
    ${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', '${bindir}/RdkXdslManager_gtest.bin', '', d)} \
"

FILES_${PN} = " \
   ${libdir}/systemd \
   ${bindir}/xdslmanager \
   ${exec_prefix}/ccsp/harvester/XdslReport.avsc \
   ${prefix}/rdk/xdslmanager/RdkXdslManager.xml \
   ${sysconfdir}/rdk/conf/xdsl_manager_conf.json \
   ${sysconfdir}/rdk/schemas/xdsl_hal_schema.json \
"

FILES_${PN}-dbg = " \
    ${prefix}/rdk/xdslmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"

DOWNLOAD_APPS="${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', 'gtestapp-rdkxdslmanager', '', d)}"
inherit comcast-package-deploy
CUSTOM_PKG_EXTNS="gtest"
SKIP_MAIN_PKG="yes"
DOWNLOAD_ON_DEMAND="yes"
