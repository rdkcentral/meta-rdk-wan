SUMMARY = "RDK WAN Manager component"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "ccsp-common-library hal-cm dbus rdk-logger utopia hal-dhcpv4c libunpriv ccsp-misc"
DEPENDS_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', 'nanomsg', '', d)}"

require recipes-ccsp/ccsp/ccsp_common.inc

# Please use below part only for official release and release candidates
#GIT_TAG = "RC2.11.0a"
#SRC_URI := "git://github.com/rdkcentral/RdkWanManager.git;branch=main;protocol=https;name=WanManager;tag=${GIT_TAG}"
#PV = "${GIT_TAG}+git${SRCPV}"

# Please use below part only for release verification/testing
SRC_URI := "git://github.com/rdkcentral/RdkWanManager.git;branch=releases/2.11.0-main;protocol=https;name=WanManager;"
SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit autotools pkgconfig ${@bb.utils.contains("DISTRO_FEATURES", "kirkstone", "python3native", "pythonnative", d)}

export ISRDKB_WAN_UNIFICATION_ENABLED = "${@bb.utils.contains('DISTRO_FEATURES', 'WanManagerUnificationEnable','true','false', d)}"
export XML_NAME = "${@bb.utils.contains('ISRDKB_WAN_UNIFICATION_ENABLED', 'true','RdkWanManager_v2.xml','RdkWanManager.xml', d)}"

CFLAGS_append = " -fcommon"
CFLAGS_append = " \
    -I${STAGING_INCDIR} \
    -I${STAGING_INCDIR}/dbus-1.0 \
    -I${STAGING_LIBDIR}/dbus-1.0/include \
    -I${STAGING_INCDIR}/ccsp \
    -I ${STAGING_INCDIR}/syscfg \
    -I ${STAGING_INCDIR}/sysevent \
    "
CFLAGS_append  = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', '-DFEATURE_RDKB_WAN_MANAGER', '', d)}"
LDFLAGS_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', '-lnanomsg', '', d)}"
CFLAGS_append  = " ${@bb.utils.contains('DISTRO_FEATURES', 'WanFailOverSupportEnable', '-DRBUS_BUILD_FLAG_ENABLE', '', d)}"
CFLAGS_append  = " ${@bb.utils.contains('DISTRO_FEATURES', 'ipoe_health_check', '-DFEATURE_IPOE_HEALTH_CHECK', '', d)}"
CFLAGS_append += " ${@bb.utils.contains('DISTRO_FEATURES', 'WanFailOverSupportEnable', ' -DWAN_FAILOVER_SUPPORTED', '', d)}"
PACKAGES += "${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', '${PN}-gtest', '', d)}"

CFLAGS_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'bci', '-DCISCO_CONFIG_TRUE_STATIC_IP -DCISCO_CONFIG_DHCPV6_PREFIX_DELEGATION -DCONFIG_CISCO_TRUE_STATIC_IP -D_BCI_FEATURE_REQ', '', d)}"

EXTRA_OECONF_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'WanManagerUnificationEnable', '--enable-wanunificationsupport', '', d)}"
# Define a variable to consolidate the check for MAPT features based on DISTRO_FEATURES
MAPT_FEATURE_ENABLED = "${@bb.utils.contains('DISTRO_FEATURES', 'feature_mapt','true', bb.utils.contains('DISTRO_FEATURES', 'unified_mapt', 'true', 'false', d), d)}"

# Flag for DHCPmanager conf
EXTRA_OECONF += "${@bb.utils.contains("DISTRO_FEATURES", "dhcp_manager", " --enable-dhcp_manager=yes", " ",d)}"

# Use the variable in CFLAGS_append
CFLAGS_append += " ${@'${MAPT_FEATURE_ENABLED}' == 'true' and '-DFEATURE_MAPT' or ''}"
CFLAGS_append += " ${@'${MAPT_FEATURE_ENABLED}' == 'true' and '-DFEATURE_MAPT_DEBUG' or ''}"
CFLAGS_append += " ${@'${MAPT_FEATURE_ENABLED}' == 'true' and '-DNAT46_KERNEL_SUPPORT' or ''}"

LDFLAGS += " -lprivilege -lpthread -lstdc++"

do_compile_prepend () {
    if ${@bb.utils.contains('DISTRO_FEATURES', 'WanFailOverSupportEnable', 'true', 'false', d)}; then
    sed -i '2i <?define RBUS_BUILD_FLAG_ENABLE=True?>' ${S}/config/${XML_NAME}
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'RbusBuildFlagEnable', 'true', 'false', d)}; then
    sed -i '2i <?define RBUS_BUILD_FLAG_ENABLE=True?>' ${S}/config/${XML_NAME}
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'dhcp_manager', 'true', 'false', d)}; then
        sed -i '2i <?define FEATURE_RDKB_DHCP_MANAGER=True?>' ${S}/config/${XML_NAME}
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'WanFailOverSupportEnable', 'true', 'false', d)}; then
    sed -i '2i <?define RBUS_BUILD_FLAG_ENABLE=True?>' ${S}/config/${XML_NAME}
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'RbusBuildFlagEnable', 'true', 'false', d)}; then
    sed -i '2i <?define RBUS_BUILD_FLAG_ENABLE=True?>' ${S}/config/${XML_NAME}
    fi

    if [ "${MAPT_FEATURE_ENABLED}" = "true" ]; then
        sed -i '2i <?define FEATURE_MAPT=True?>' ${S}/config/${XML_NAME}
    fi

    if ${@bb.utils.contains('DISTRO_FEATURES', 'rdkb_wan_manager', 'true', 'false', d)}; then
        (${PYTHON} ${STAGING_BINDIR_NATIVE}/dm_pack_code_gen.py ${S}/config/${XML_NAME} ${S}/source/WanManager/dm_pack_datamodel.c)
    fi
}

do_install_append () {
    # Config files and scripts
    install -d ${D}${exec_prefix}/rdk/wanmanager
    ln -sf ${bindir}/wanmanager ${D}${exec_prefix}/rdk/wanmanager/wanmanager
    ln -sf ${bindir}/netmonitor ${D}${exec_prefix}/rdk/wanmanager/netmonitor
    install -m 644 ${S}/config/${XML_NAME} ${D}/usr/rdk/wanmanager/RdkWanManager.xml
}


FILES_${PN} = " \
   ${exec_prefix}/rdk/wanmanager/wanmanager \
   ${exec_prefix}/rdk/wanmanager/netmonitor \
   ${exec_prefix}/rdk/wanmanager/RdkWanManager.xml \
   ${bindir}/* \
"

FILES_${PN}-dbg = " \
    ${exec_prefix}/rdk/wanmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
FILES_${PN}-gtest = "\
    ${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', '${bindir}/RdkWanManager_gtest.bin', '', d)} \
"
EXTRA_OECONF_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', '--enable-gtestapp', '', d)}"

DOWNLOAD_APPS="${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', 'gtestapp-RdkWanManager', '', d)}"
inherit comcast-package-deploy
CUSTOM_PKG_EXTNS="${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', 'gtest', '', d)}"
SKIP_MAIN_PKG="${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', 'yes', 'no', d)}"
DOWNLOAD_ON_DEMAND="${@bb.utils.contains('DISTRO_FEATURES', 'gtestapp', 'yes', 'no', d)}"

do_configure[depends] += "halinterface:do_install"
