#!/bin/bash

BASEDIR=$(pwd)

echo "----------------------------------------"
echo "Install nodejs modules "
echo "----------------------------------------"

npm install


echo "----------------------------------------"
echo "Run server  "
echo "----------------------------------------"

export NODE_CONFIG_DIR=$BASEDIR/config && export NODE_ENV=development-mac && node bin/www