FROM debian:bookworm-slim as builder
RUN apt-get update && apt-get install -y build-essential wget && rm -rf /var/lib/apt/lists/*
RUN wget https://www.zlib.net/zlib-1.3.1.tar.gz && tar -xzf zlib-1.3.1.tar.gz
WORKDIR zlib-1.3.1
RUN ./configure --prefix=/usr/local && make && make install

FROM busybox:stable-glibc as runtime
COPY --from=builder /usr/local/lib/libz.so* /lib/
COPY ./build/native/nativeCompile/a4lb-linux-amd64 /a4lb-linux-amd64

ENTRYPOINT ["/a4lb-linux-amd64"]
