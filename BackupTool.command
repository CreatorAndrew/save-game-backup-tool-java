#!/bin/bash
cd "${0%/*}"
clear
java -jar BackupTool.jar "$@" --no-gui
