SUMMARY = "RDK GPON Manager component"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "ccsp-common-library dbus rdk-logger utopia hal-platform json-hal-lib"

require recipes-ccsp/ccsp/ccsp_common.inc

GIT_TAG = "v1.2.0"
SRC_URI = "git://github.com/rdkcentral/RdkGponManager.git;branch=main;protocol=https;name=GponManager;tag=${GIT_TAG}"
PV = "${GIT_TAG}+git${SRCPV}"
EXTRA_OECONF_append  = " --with-ccsp-platform=bcm --with-ccsp-arch=arm "

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

inherit autotools pkgconfig

export ISRDKB_WAN_UNIFICATION_ENABLED = "${@bb.utils.contains('DISTRO_FEATURES', 'WanManagerUnificationEnable','true','false', d)}"
export SCHEMA_FILE = "${@bb.utils.contains('ISRDKB_WAN_UNIFICATION_ENABLED', 'true','gpon_wan_unify_hal_schema.json','gpon_hal_schema.json', d)}"
export CONF_FILE = "${@bb.utils.contains('ISRDKB_WAN_UNIFICATION_ENABLED', 'true','gpon_manager_wan_unify_conf.json','gpon_manager_conf.json', d)}"

CFLAGS_append = " \
    -I${STAGING_INCDIR} \
    -I${STAGING_INCDIR}/dbus-1.0 \
    -I${STAGING_LIBDIR}/dbus-1.0/include \
    -I${STAGING_INCDIR}/ccsp \
    -I ${STAGING_INCDIR}/syscfg \
    -I ${STAGING_INCDIR}/sysevent \
    -Werror \
    -Wall \
    -Wno-error=switch \
    "
CFLAGS_append  = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', '-DFEATURE_RDKB_WAN_MANAGER', '', d)}"

do_compile_prepend () {
    (${PYTHON} ${STAGING_BINDIR_NATIVE}/dm_pack_code_gen.py ${S}/config/RdkGponManager.xml ${S}/source/GponManager/dm_pack_datamodel.c)
}


do_install () {
    # Config files and scripts
    install -d ${D}/usr/rdk
    install -d ${D}/usr/rdk/gponmanager
    install -d ${D}${bindir}
    install -d ${D}${sysconfdir}/rdk/conf
    install -d ${D}${sysconfdir}/rdk/schemas
    install -m 755 ${B}/source/GponManager/GponManager ${D}${bindir}
    install -m 644 ${S}/config/RdkGponManager.xml ${D}/usr/rdk/gponmanager
    install -m 644 ${S}/config/${CONF_FILE} ${D}${sysconfdir}/rdk/conf
    install -m 644 ${S}/hal_schema/${SCHEMA_FILE} ${D}${sysconfdir}/rdk/schemas
}

FILES_${PN} = " \
   ${bindir}/GponManager \
   ${prefix}/rdk/gponmanager/RdkGponManager.xml \
   ${sysconfdir}/rdk/conf \
   ${sysconfdir}/rdk/conf/${CONF_FILE} \
   ${sysconfdir}/rdk/schemas \
   ${sysconfdir}/rdk/schemas/${SCHEMA_FILE} \
"

FILES_${PN}-dbg = " \
    ${prefix}/rdk/gponmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
INSANE_SKIP_${PN} += "dev-deps"
