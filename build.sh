mvn compile assembly:single
mkdir dist
cp ./target/save-game-backup-tool*.jar ./dist/BackupTool.jar
cp ./BackupTool.* ./dist
cp ./*.json ./dist
7z a "Save Game Backup Tool.zip" ./dist/*
mvn clean
rm -rf dist
