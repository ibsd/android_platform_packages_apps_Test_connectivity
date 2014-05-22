#! /bin/bash
#This script does a clean build of sl4a and install it to your device through adb
#Requires envsetup.sh in the branch

#Change to jdk6
function setup_jdk() {
  # Remove the current JDK from PATH
  if [ -n "$JAVA_HOME" ] ; then
    PATH=${PATH/$JAVA_HOME\/bin:/}
  fi
  export JAVA_HOME=$1
  export PATH=$JAVA_HOME/bin:$PATH
}
setup_jdk /usr/lib/jvm/java-6-sun

#Color code for echo
r='\e[0;31m'
brn='\e[0;33m'
y='\e[1;33m'
g='\e[0;32m'
cy='\e[0;36m'
lb='\e[1;34m'
p='\e[0;35m'
lg='\e[0;37m'
NC='\e[0m' # No Color

echo -e "Welcome to ${r}U${brn}N${y}I${g}C${lb}O${cy}R${p}N${NC} build system for ${g}sl4a${NC}"

#List of sl4a dependency libs to be built in order
declare -a lib_list=("Utils" "Common" "BluetoothFacade" "SignalStrengthFacade" "TextToSpeechFacade" "WebCamFacade" "InterpreterForAndroid" "ScriptingLayer" "QuickAction")

declare -a test_list=("Utils" "Common")

APP_NAME=sl4a
APP_PACKAGE_NAME=com.googlecode.android_scripting

BRANCH_ROOT=~/My_workshope/klp-wireless-dev
SL4A_ROOT=$BRANCH_ROOT/vendor/google_testing/comms/Tools/sl4a
SHARED_LIB_JAR_ROOT=$BRANCH_ROOT/out/target/common/obj/JAVA_LIBRARIES
APP_JAR_ROOT=$BRANCH_ROOT/out/target/common/obj/APPS
APK_ROOT=$BRANCH_ROOT/out/target/product/hammerhead/data/app
SL4A_PROJ_DIR=$SL4A_ROOT/ScriptingLayerForAndroid

echo -e "${y}Removing intermediates of all the dependency libs${NC}"
for i in "${lib_list[@]}"
do
  rm -r $SHARED_LIB_JAR_ROOT/sl4a."$i"_intermediates
done

echo -e "${y}Removing intermeidates of the app${NC}"
rm -r $APP_JAR_ROOT/"$APP_NAME"_intermediates
#Remove the apk file
rm $APK_ROOT/"$APP_NAME".apk

#Build all the dependency libs
. $BRANCH_ROOT/build/envsetup.sh

exec () {
  ${@:1:($#-1)}
  if [ $? -ne 0 ]; then
    echo -e "${r}Encountered error when ${@:$#}${NC}"
    echo -e "${lg}UNICORN ${r}DIED${NC}!"
    exit 1
  fi
}


for i in "${lib_list[@]}"
do
  echo -e "${lb}+++++++ Building $i +++++++${NC}"
  cd $SL4A_ROOT/"$i"
  exec mm -B "building $i"
  echo
done

echo -e "${lb}+++++++ Building $APP_NAME.apk +++++++${NC}"
cd $SL4A_PROJ_DIR
exec mm -B "building $APP_NAME.apk"
echo

echo -e "${y}Uninstalling old apk from device${NC}"
adb uninstall $APP_PACKAGE_NAME

echo -e "${lb}Installing apk to device${NC}"
cd $APK_ROOT
exec adb install $APP_NAME.apk "installing apk to device"

echo "All clear!"
echo -e " ${r}U${brn}N${y}I${g}C${cy}O${lb}R${p}N ${r}P${brn}O${y}W${g}E${cy}R${lb}!${p}!${NC}"

