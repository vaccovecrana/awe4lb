FROM golang:1.9-alpine

WORKDIR /app
COPY *.go ./
RUN go build -o udp-echo main.go

ENTRYPOINT ["/app/udp-echo"]
