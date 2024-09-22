mvn compile assembly:single
mkdir dist
cp ./target/save-game-backup-tool*.jar ./dist/BackupTool.jar
cp ./BackupTool.* ./dist
cp ./Launch.sh ./dist
chmod +x ./dist/BackupTool.*
chmod -x ./dist/BackupTool.bat
chmod +x ./dist/Launch.sh
cp ./*.json ./dist
cp ./LICENSE.md ./dist
cp -r ./Test ./dist
mv ./dist Save\ Game\ Backup\ Tool
7z a save-game-backup-tool-java.zip ./Save\ Game\ Backup\ Tool
mvn clean
rm -rf Save\ Game\ Backup\ Tool
