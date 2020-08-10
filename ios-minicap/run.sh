#!/usr/bin/env bash

project_path=$(cd `dirname $0`; pwd)
cd $project_path

type=`ideviceinfo -k ProductType`
RESOLUTION="400x600"

case $type in
    iPhone3,1 | iPhone3,2 | iPhone3,3 | iPhone4,1)
	RESOLUTION="320x480"
        ;;
    iPhone5,1 | iPhone5,2 | iPhone5,3 | iPhone5,4 | iPhone5,2 | iPhone6,1 | iPhone6,2 | iPhone8,4)
	RESOLUTION="320x568"
        ;;
    iPhone7,1 | iPhone8,2 | iPhone9,2 | iPhone9,4 | iPhone10,2 | iPhone10,5)
	RESOLUTION="414x736"
        ;;
    iPhone7,2 | iPhone8,1 | iPhone9,1 | iPhone9,3 | iPhone10,1 | iPhone10,4)
	RESOLUTION="375x667"
        ;;
    iPhone10,3 | iPhone10,6 | iPhone11,2 | iPhone12,3)
	RESOLUTION="375x812"
        ;;
    iPhone11,8 | iPhone11,6 | iPhone11,4 | iPhone12,1 | iPhone12,5)
	RESOLUTION="414x896"
        ;;
     *)
        echo "请更新分辨率"
esac

set -exo pipefail

#UDID=$(system_profiler SPUSBDataType | sed -n -E -e '/(iPhone|iPad)/,/Serial/s/ *Serial Number: *(.+)/\1/p')
UDID=$(idevice_id -l)
PORT=12345

./build/ios_minicap \
    --udid $UDID \
    --port $PORT \
    --resolution $RESOLUTION
