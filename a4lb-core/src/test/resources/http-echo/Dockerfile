FROM docker.io/golang:1.19

WORKDIR /app
COPY go.mod ./
COPY *.go ./

RUN CGO_ENABLED=0 GOOS=linux go build
RUN addgroup http-echo
RUN useradd -g http-echo -u 1005 http-echo

USER 1005
ENTRYPOINT ["/app/echo"]
