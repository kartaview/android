#!/bin/bash

set -e

# Set your own NDK here
#NDK=~/Android/android-ndk-r10e

#export NDK=`grep ndk.dir $PROPS | cut -d'=' -f2`

#if [ "$NDK" = "" ] || [ ! -d $NDK ]; then
#    echo "NDK variable not set or path to NDK is invalid, exiting..."
#    exit 1
#fi

export TARGET=$1

ARM_PLATFORM=$NDK/platforms/android-9/arch-arm/
ARM_PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64

ARM64_PLATFORM=$NDK/platforms/android-21/arch-arm64/
ARM64_PREBUILT=$NDK/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64

X86_PLATFORM=$NDK/platforms/android-9/arch-x86/
X86_PREBUILT=$NDK/toolchains/x86-4.9/prebuilt/darwin-x86_64

X86_64_PLATFORM=$NDK/platforms/android-21/arch-x86_64/
X86_64_PREBUILT=$NDK/toolchains/x86_64-4.9/prebuilt/darwin-x86_64

MIPS_PLATFORM=$NDK/platforms/android-9/arch-mips/
MIPS_PREBUILT=$NDK/toolchains/mipsel-linux-android-4.9/prebuilt/darwin-x86_64

BUILD_DIR=`pwd`/../jniLibs

FFMPEG_VERSION="3.0.1"

if [ ! -d "ffmpeg-${FFMPEG_VERSION}" ]; then
    echo "Downloading ffmpeg-${FFMPEG_VERSION}.tar.bz2"
    curl -LO http://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.bz2

    tar -xf ffmpeg-${FFMPEG_VERSION}.tar.bz2
    rm -f ffmpeg-${FFMPEG_VERSION}.tar.bz2

    for i in `find diffs -type f`; do
        (cd ffmpeg-${FFMPEG_VERSION} && patch -p1 < ../$i)
    done
else
    echo "Using existing ffmpeg-${FFMPEG_VERSION}"
fi

if [ ! -d "x264" ]; then
    echo "Downloading last_stable_x264.tar.bz2"
    curl -LO https://ftp.videolan.org/pub/videolan/x264/snapshots/last_stable_x264.tar.bz2

    tar -xf last_stable_x264.tar.bz2
    mv -f x264* x264
    rm -f last_stable_x264.tar.bz2
else
    echo "last_stable_x264.tar.bz2"
fi



function build_one
{
if [ $ARCH == "arm" ]
then
    PLATFORM=$ARM_PLATFORM
    PREBUILT=$ARM_PREBUILT
    HOST=arm-linux-androideabi
#added by alexvas
elif [ $ARCH == "arm64" ]
then
    PLATFORM=$ARM64_PLATFORM
    PREBUILT=$ARM64_PREBUILT
    HOST=aarch64-linux-android
elif [ $ARCH == "mips" ]
then
    PLATFORM=$MIPS_PLATFORM
    PREBUILT=$MIPS_PREBUILT
    HOST=mipsel-linux-android
#alexvas
elif [ $ARCH == "x86_64" ]
then
    PLATFORM=$X86_64_PLATFORM
    PREBUILT=$X86_64_PREBUILT
    HOST=x86_64-linux-android
else
    PLATFORM=$X86_PLATFORM
    PREBUILT=$X86_PREBUILT
    HOST=i686-linux-android
fi

#    --prefix=$PREFIX \

#--incdir=$BUILD_DIR/include \
#--libdir=$BUILD_DIR/lib/$CPU \

#    --extra-cflags="-fvisibility=hidden -fdata-sections -ffunction-sections -Os -fPIC -DANDROID -DHAVE_SYS_UIO_H=1 -Dipv6mr_interface=ipv6mr_ifindex -fasm -Wno-psabi -fno-short-enums -fno-strict-aliasing -finline-limit=300 $OPTIMIZE_CFLAGS " \
#    --extra-ldflags="-Wl,-rpath-link=$PLATFORM/usr/lib -L$PLATFORM/usr/lib -nostdlib -lc -lm -ldl -llog" \

    build_x264
# TODO Adding aac decoder brings "libnative.so has text relocations. This is wasting memory and prevents security hardening. Please fix." message in Android.


    # --enable-small \
    # --disable-everything \
    # --enable-runtime-cpudetect \
pushd ffmpeg-${FFMPEG_VERSION}
#--disable-stripping //todo when debugging only
./configure --target-os=android \
    --incdir=$BUILD_DIR/$TARGET/include \
    --libdir=$BUILD_DIR/$TARGET \
    --enable-cross-compile \
    --extra-libs="-lgcc" \
    --arch=$ARCH \
    --cc=$PREBUILT/bin/$HOST-gcc \
    --cross-prefix=$PREBUILT/bin/$HOST- \
    --nm=$PREBUILT/bin/$HOST-nm \
    --sysroot=$PLATFORM \
    --extra-cflags="$OPTIMIZE_CFLAGS  -I$BUILD_DIR/$TARGET/include -I../x264" \
    --enable-shared \
    --disable-static \
    --extra-ldflags="-Wl,-rpath-link=$PLATFORM/usr/lib -L$PLATFORM/usr/lib -nostdlib -lc -lm -ldl -llog -L$BUILD_DIR/$TARGET -L$PREFIX/lib -L../x264 " \
    --disable-symver \
    --disable-ffserver \
    --disable-doc \
    --disable-avdevice \
    --disable-postproc \
    --disable-bsfs \
    --disable-protocols \
    --disable-indevs \
    --disable-outdevs \
    --disable-devices \
    --disable-filters \
    --disable-encoders \
    --disable-decoders \
    --disable-parsers \
    --disable-muxers \
    --disable-demuxers \
    --enable-ffmpeg \
    --enable-gpl \
    --enable-libx264 \
    --enable-ffplay \
    --enable-ffmpeg \
    --enable-ffprobe \
    --enable-avfilter \
    --enable-encoder=png \
    --enable-protocol=file,http,https,mmsh,mmst,pipe,rtmp,rtmps,rtmpt,rtmpts,rtp \
    --enable-aesni \
    --enable-armv5te \
    --enable-armv6 \
    --enable-armv6t2 \
    --enable-vfp \
    --enable-neon \
    --enable-libx264 \
    --enable-encoder=libx264 \
    --enable-parser=h264 \
    --enable-parser=mjpeg \
	    --enable-demuxer=h264 \
	    --enable-demuxer=mjpeg \
	    --enable-hwaccel=h264_vaapi \
	    --enable-hwaccel=h264_vaapi \
	    --enable-hwaccel=h264_dxva2 \
	    --enable-hwaccel=mpeg4_vaapi \
	    --enable-muxer=mp4 \
	    --enable-muxer=h264 \
	    --enable-decoder=mjpeg \
	    --enable-encoder=mjpeg \
        --enable-filter=transpose \
        --enable-filter=vflip \
        --enable-filter=hflip \
        --enable-filter=scale \
        --enable-filter=rotate \
        --enable-ffserver \
        --enable-ffplay \
        --enable-avfilter \
    $ADDITIONAL_CONFIGURE_FLAG

make -i -s clean
make -j8 -i -s install
$PREBUILT/bin/$HOST-ar d libavcodec/libavcodec.a inverse.o
#$PREBUILT/bin/$HOST-ld -rpath-link=$PLATFORM/usr/lib -L$PLATFORM/usr/lib  -soname libffmpeg.so -shared -nostdlib  -z,noexecstack -Bsymbolic --whole-archive --no-undefined -o $PREFIX/libffmpeg.so libavcodec/libavcodec.a libavformat/libavformat.a libavutil/libavutil.a libswscale/libswscale.a -lc -lm -lz -ldl -llog  --warn-once  --dynamic-linker=/system/bin/linker $PREBUILT/lib/gcc/$HOST/4.6/libgcc.a
popd

## copy the binaries
#mkdir -p $PREFIX
#cp -r $BUILD_DIR/$TARGET/* $PREFIX
}

function build_x264 {
    echo "Starting build for x264"
    pushd x264
    ./configure \
	    --prefix=$PREFIX \
	    --host=$ARCH-linux \
	    --enable-pic \
	    --libdir=$BUILD_DIR/$TARGET \
        --cross-prefix=$PREBUILT/bin/$HOST- \
        --sysroot=$PLATFORM \
        --extra-cflags="$OPTIMIZE_CFLAGS " \
        --enable-static \
        --extra-ldflags="-Wl,-rpath-link=$PLATFORM/usr/lib -L$PLATFORM/usr/lib -lc -lm -ldl -llog" \
        --enable-pic \
        --disable-asm \
        $ADDITIONAL_CONFIGURE_FLAG
    make -i -s clean
    make -j4 -i -s install
    make -i -s clean
    popd
}

if [ $TARGET == 'arm-v5te' ]; then
    #arm v5te
    CPU=armv5te
    ARCH=arm
    OPTIMIZE_CFLAGS="-marm -march=$CPU"
    PREFIX=$BUILD_DIR/$CPU
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'arm-v6' ]; then
    #arm v6
    CPU=armv6
    ARCH=arm
    OPTIMIZE_CFLAGS="-marm -march=$CPU"
    PREFIX=$BUILD_DIR/$CPU
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'arm-v7vfpv3' ]; then
    #arm v7vfpv3
    CPU=armv7-a
    ARCH=arm
    OPTIMIZE_CFLAGS="-mfloat-abi=softfp -mfpu=vfpv3-d16 -marm -march=$CPU "
    PREFIX=$BUILD_DIR/$CPU
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'arm-v7vfp' ]; then
    #arm v7vfp
    CPU=armv7-a
    ARCH=arm
    OPTIMIZE_CFLAGS="-mfloat-abi=softfp -mfpu=vfp -marm -march=$CPU "
    PREFIX=$BUILD_DIR/$CPU-vfp
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'armeabi-v7a-neon' ]; then
    #arm v7n
    CPU=armv7-a
    ARCH=arm
    OPTIMIZE_CFLAGS="-mfloat-abi=softfp -mfpu=neon -marm -march=$CPU -mtune=cortex-a8 -mfloat-abi=softfp -mfpu=neon -marm -march=$CPU -mtune=cortex-a8 -mthumb -D__thumb__ "
    PREFIX=$BUILD_DIR/armeabi-v7a-neon
    ADDITIONAL_CONFIGURE_FLAG=--enable-neon
    build_one
fi

if [ $TARGET == 'arm-v6+vfp' ]; then
    #arm v6+vfp
    CPU=armv6
    ARCH=arm
    OPTIMIZE_CFLAGS="-DCMP_HAVE_VFP -mfloat-abi=softfp -mfpu=vfp -marm -march=$CPU"
    PREFIX=$BUILD_DIR/${CPU}_vfp
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'arm64-v8a' ]; then
    #arm64-v8a
    CPU=arm64-v8a
    ARCH=arm64
    OPTIMIZE_CFLAGS=
    PREFIX=$BUILD_DIR/$CPU
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'x86_64' ]; then
    #x86_64
    CPU=x86_64
    ARCH=x86_64
    OPTIMIZE_CFLAGS="-fomit-frame-pointer"
    #PREFIX=$BUILD_DIR/$CPU
    PREFIX=$BUILD_DIR/x86_64
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'x86' ]; then
    #x86
    CPU=i686
    ARCH=i686
    OPTIMIZE_CFLAGS="-fomit-frame-pointer"
    #PREFIX=$BUILD_DIR/$CPU
    PREFIX=$BUILD_DIR/x86
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'mips' ]; then
    #mips
    CPU=mips
    ARCH=mips
    OPTIMIZE_CFLAGS="-std=c99 -O3 -Wall -pipe -fpic -fasm \
-ftree-vectorize -ffunction-sections -funwind-tables -fomit-frame-pointer -funswitch-loops \
-finline-limit=300 -finline-functions -fpredictive-commoning -fgcse-after-reload -fipa-cp-clone \
-Wno-psabi -Wa,--noexecstack"
    #PREFIX=$BUILD_DIR/$CPU
    PREFIX=$BUILD_DIR/mips
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'armeabi-v7a' ]; then
    #arm armv7-a
    CPU=armv7-a
    ARCH=arm
    OPTIMIZE_CFLAGS="-mfloat-abi=softfp -marm -march=$CPU "
    #PREFIX=`pwd`/ffmpeg-android/$CPU
    PREFIX=$BUILD_DIR/armeabi-v7a
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi

if [ $TARGET == 'armeabi' ]; then
    #arm arm
    CPU=arm
    ARCH=arm
    OPTIMIZE_CFLAGS=""
    #PREFIX=`pwd`/ffmpeg-android/$CPU
    PREFIX=$BUILD_DIR/armeabi
    ADDITIONAL_CONFIGURE_FLAG=
    build_one
fi
