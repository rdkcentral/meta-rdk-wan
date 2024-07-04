SUMMARY = "IPoE Health Check component"

LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "rdk-logger rdk-wanmanager"

GIT_TAG = "v1.0.0"
SRC_URI := "git://github.com/rdkcentral/IPOEHealthCheck.git;branch=main;protocol=https;name=IPoEHealthCheck;tag=${GIT_TAG}"
PV = "${GIT_TAG}+git${SRCPV}"

S = "${WORKDIR}/git"

inherit autotools pkgconfig

CFLAGS_append = " \
    -I${STAGING_INCDIR} \
    -I${STAGING_INCDIR}/ccsp \
    -I ${STAGING_INCDIR}/syscfg \
    -I ${STAGING_INCDIR}/sysevent \
    "

CFLAGS_append += " ${@bb.utils.contains('DISTRO_FEATURES', 'feature_mapt', '-DFEATURE_MAPT', '', d)}"

FILES_${PN} = " \
   ${bindir}/ipoe_health_check \
"
