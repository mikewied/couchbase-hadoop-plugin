#bin/bash
#
# Author Couchbase <info@couchbase.com>
# Copyright 2011 Couchbase, Inc.
# All rights reserved.
#

function check_file_exists {
  if [ -f $1 ]; then
    echo "$1 FOUND"
  else
    echo "$1 NOT FOUND"
    echo
    echo "Install Failed"
    exit 1
  fi
}

f_config=$PWD/couchsqoop-config.xml
f_manager=$PWD/couchsqoop-manager
f_plugin=$PWD/couchsqoop-plugin-1.0.jar

if [ $# -ne 1 ]; then
  echo "usage: ./install.sh path_to_sqoop_home"
  exit 1;
fi

sqoop_home=$1

echo
echo "---Checking for install files---"

check_file_exists $f_config
check_file_exists $f_manager
check_file_exists $f_plugin

mkdir -p $sqoop_home/lib/
mkdir -p $sqoop_home/conf/managers.d/

echo
echo "Installing files to Sqoop"
cp -f $f_config $sqoop_home/conf/
cp -f $f_manager $sqoop_home/conf/managers.d/
cp -f *.jar $sqoop_home/lib/


echo
echo "Install Successful!"
