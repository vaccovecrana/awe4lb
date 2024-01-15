gradle clean assemble

cp ./AppRun ./build/distributions

cd ./build
wget -nc https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage
chmod +x appimagetool-x86_64.AppImage
wget -nc https://cdn.azul.com/zulu/bin/zulu11.68.17-ca-jre11.0.21-linux_x64.tar.gz
tar -xvf ./zulu11.68.17-ca-jre11.0.21-linux_x64.tar.gz
mv zulu11.68.17-ca-jre11.0.21-linux_x64 jre
mv jre ./distributions

cd ./distributions
tar --strip-components=1 -xvf *.tar
rm -rfv *.zip
rm -rfv *.tar
rm -rfv ./bin


