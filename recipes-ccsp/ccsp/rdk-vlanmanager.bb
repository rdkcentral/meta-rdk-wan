SUMMARY = "RDK VLAN Manager component"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "ccsp-common-library dbus rdk-logger utopia hal-platform libunpriv"

require recipes-ccsp/ccsp/ccsp_common.inc

GIT_TAG = "v1.1.0"
SRC_URI = "git://github.com/rdkcentral/RdkVlanBridgingManager.git;branch=main;protocol=https;name=VlanBridgingManager;tag=${GIT_TAG}"
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
    "

LDFLAGS += " -lprivilege"

CFLAGS_append  = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', '-DFEATURE_RDKB_WAN_MANAGER', '', d)}"

do_compile_prepend () {
    (${PYTHON} ${STAGING_BINDIR_NATIVE}/dm_pack_code_gen.py ${S}/config/RdkVlanManager.xml ${S}/source/RdkVlanManager/dm_pack_datamodel.c)
}

do_install_append () {
    # Config files and scripts
    install -d ${D}/usr/rdk
    install -d ${D}/usr/rdk/vlanmanager
    install -d ${D}${bindir}
    install -d ${D}${sysconfdir}/rdk/conf
    install -m 644 ${S}/config/RdkVlanManager.xml ${D}/usr/rdk/vlanmanager
    install -m 644 ${S}/config/vlan_manager_conf.json ${D}${sysconfdir}/rdk/conf

    #JSON schema files.
    install -d ${D}/${sysconfdir}/rdk/schemas
    install -m 644 ${S}/hal_schema/ethlinkvlanterm_hal_schema.json ${D}/${sysconfdir}/rdk/schemas
}

FILES_${PN} = " \
   ${bindir}/VlanManager \
   ${prefix}/rdk/vlanmanager/RdkVlanManager.xml \
   ${sysconfdir}/rdk/conf/vlan_manager_conf.json \
   ${sysconfdir}/rdk/schemas/ethlinkvlanterm_hal_schema.json \
"

FILES_${PN}-dbg = " \
    ${prefix}/rdk/vlanmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
