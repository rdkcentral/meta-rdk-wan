SUMMARY = "IPoE Health Check component"

LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "rdk-logger rdk-wanmanager"

# Set the component version here
TAG_VERSION="1.1.0"

# The GIT_TAG will be dynamically determined based on the TAG_VERSION.
# The following code fetches the tag in the following priority order:
# Example v2.7.0 -> RC2.7.0z -> RC2.7.0y -> ... -> RC2.7.0b -> RC2.7.0a
GIT_TAG = "${@os.popen('git ls-remote --tags -q  https://github.com/rdkcentral/IPOEHealthCheck.git ' + ' v' + d.getVar('TAG_VERSION', True) + ' RC' + d.getVar('TAG_VERSION', True) + '[a-z]').read().strip().split('\n')[-1].split('/')[-1]}"

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
