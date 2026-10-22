FROM busybox:stable-glibc AS runtime

COPY ./libz.so.1 /lib/
COPY ./build/native/nativeCompile/a4lb-linux-amd64 /a4lb-linux-amd64
COPY ./build/resources/main/NOTICE ./build/resources/main/LICENSE /

EXPOSE 7070

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O- http://127.0.0.1:7070/ >/dev/null || exit 1

STOPSIGNAL SIGTERM

ENTRYPOINT ["/a4lb-linux-amd64"]
