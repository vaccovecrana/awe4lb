FROM busybox:stable-glibc as runtime

COPY ./libz.so.1 /lib/
COPY ./build/native/nativeCompile/a4lb-linux-amd64 /a4lb-linux-amd64
COPY ./build/resources/main/NOTICE ./build/resources/main/LICENSE /
RUN rm -rfv /bin/*

EXPOSE 7070

ENTRYPOINT ["/a4lb-linux-amd64"]
