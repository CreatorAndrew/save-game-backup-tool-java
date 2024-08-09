@echo off
pushd "%~dp0"
java -jar BackupTool.jar %* --no-gui
popd
