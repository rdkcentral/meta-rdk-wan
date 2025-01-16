SUMMARY = "RDK PPP Manager component"

LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://LICENSE;md5=175792518e4ac015ab6696d16c4f607e"

DEPENDS = "ccsp-common-library dbus rdk-logger utopia halinterface libunpriv"

require recipes-ccsp/ccsp/ccsp_common.inc

# Set the component version here
TAG_VERSION="1.2.0"

# The GIT_TAG will be dynamically determined based on the TAG_VERSION.
# The following code fetches the tag in the following priority order:
# Example v2.7.0 -> RC2.7.0z -> RC2.7.0y -> ... -> RC2.7.0b -> RC2.7.0a
GIT_TAG = "${@os.popen('git ls-remote --tags -q --sort=-version:refname git://github.com/rdkcentral/RdkPppManager.git ' + ' v' + d.getVar('TAG_VERSION', True) + ' RC' + d.getVar('TAG_VERSION', True) + '[a-z]').read().strip().split()[1].split('/')[-1]}"

SRC_URI := "git://github.com/rdkcentral/RdkPppManager.git;branch=main;protocol=https;name=PppManager;tag=${GIT_TAG}"
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
    -I${STAGING_INCDIR}/utapi \
    -I${STAGING_INCDIR}/utctx \
    "

LDFLAGS += " -lprivilege"

CFLAGS_append = " ${@bb.utils.contains('DISTRO_FEATURES', 'safec', ' -fPIC -I${STAGING_INCDIR}/libsafec', '-fPIC', d)}"
LDFLAGS_append = " -ldbus-1"
LDFLAGS_remove_morty = " -ldbus-1"

EXTRA_OECONF_append  = " --with-ccsp-platform=bcm --with-ccsp-arch=arm "

do_install_append () {
    # Config files and scripts
    install -d ${D}${exec_prefix}/rdk/pppmanager
    ln -sf ${bindir}/pppmanager ${D}${exec_prefix}/rdk/pppmanager/pppmanager
    install -m 644 ${S}/config/RdkPppManager.xml ${D}/usr/rdk/pppmanager/
    echo "sha value for recipe " + ${SRCREV_PppManager}
}


FILES_${PN} = " \
   ${exec_prefix}/rdk/pppmanager/pppmanager \
   ${exec_prefix}/rdk/pppmanager/RdkPppManager.xml \
   ${bindir}/* \
"

FILES_${PN}-dbg = " \
    ${exec_prefix}/ccsp/pppmanager/.debug \
    /usr/src/debug \
    ${bindir}/.debug \
    ${libdir}/.debug \
"
