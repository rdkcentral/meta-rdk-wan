SUMMARY = "IPoE Health Check component"

LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

# Please use below part only for official release and release candidates
GIT_TAG = "v1.6.0"

DEPENDS = "rdk-logger rdk-wanmanager"

# Please use below part only for official release and release candidates
SRC_URI := "git://github.com/rdkcentral/ipoe-health-check.git;branch=releases/1.6.0-main;protocol=https;name=IPoEHealthCheck;tag=${GIT_TAG}"
PV = "${GIT_TAG}+git${SRCPV}"
#SRCREV = "${AUTOREV}"

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
