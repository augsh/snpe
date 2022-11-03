#
# Copyright (c) 2018, 2019 Qualcomm Technologies, Inc.
# All Rights Reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.
#

#############################################################
# Inception V3 setup
#############################################################

mkdir -p inception_v3
mkdir -p inception_v3/images

cd inception_v3

cp -R ../../../../models/inception_v3/data/cropped/*.jpg images
FLOAT_DLC="../../../../models/inception_v3/dlc/inception_v3.dlc"
QUANTIZED_DLC="../../../../models/inception_v3/dlc/inception_v3_quantized.dlc"
UDO_DLC="../../../../models/inception_v3/dlc/inception_v3_udo.dlc"
UDO_QUANTIZED_DLC="../../../../models/inception_v3/dlc/inception_v3_udo_quantized.dlc"
UDO_PACKAGE_PATH="../../../../models/inception_v3/SoftmaxUdoPackage/libs/arm64-v8a/"
UDO_DSP_PACKAGE_PATH="../../../../../models/inception_v3/SoftmaxUdoPackage/libs/dsp_v60/"
UDO_HTP_DSP_PACKAGE_PATH="../../../../../models/inception_v3/udo_dsp/SoftmaxUdoPackage/libs/dsp_v68/"

if [ -f ${UDO_QUANTIZED_DLC} ]; then
    cp -R ${UDO_QUANTIZED_DLC} model.dlc
elif [ -f ${UDO_DLC} ]; then
    cp -R ${UDO_DLC} model.dlc
else
    if [ -f ${QUANTIZED_DLC} ]; then
        cp -R ${QUANTIZED_DLC} model.dlc
    else
        cp -R ${FLOAT_DLC} model.dlc
    fi
fi
if [ -d ${UDO_PACKAGE_PATH} ]; then
    mkdir udo
    cd udo
    mkdir arm64-v8a
    mkdir dsp
    cp -R ../${UDO_PACKAGE_PATH}/* ./arm64-v8a/
    mv ./arm64-v8a/libUdoSoftmaxUdoPackageReg.so ./arm64-v8a/UdoPackageReg.so
    if [ -d ${UDO_HTP_DSP_PACKAGE_PATH} ]; then
        cp -R ${UDO_HTP_DSP_PACKAGE_PATH}/* ./dsp/
    elif [ -d ${UDO_DSP_PACKAGE_PATH} ]; then
        cp -R ${UDO_DSP_PACKAGE_PATH}/* ./dsp/
    fi
    rm -rf ./arm64-v8a/libc++_shared.so
    rm -rf ./arm64-v8a/libOpenCL.so
    cd ../
fi

cp -R ../../../../models/inception_v3/data/imagenet_slim_labels.txt labels.txt

zip -r inception_v3.zip ./*
mkdir -p ../app/src/main/res/raw/
cp inception_v3.zip ../app/src/main/res/raw/

cd ..
rm -rf ./inception_v3
