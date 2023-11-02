mvn compile assembly:single
mkdir dist
cp ./target/save-game-backup-tool*.jar ./dist/BackupTool.jar
cp ./BackupTool.* ./dist
cp ./*.json ./dist
cp ./LICENSE ./dist
mv ./dist Save\ Game\ Backup\ Tool
7z a save-game-backup-tool.zip ./Save\ Game\ Backup\ Tool
mvn clean
rm -rf Save\ Game\ Backup\ Tool
