#!/bin/bash
cd "${0%/*}"
java -jar BackupTool.jar "$@" --no-gui
