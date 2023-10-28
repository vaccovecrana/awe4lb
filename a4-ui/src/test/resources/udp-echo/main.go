package main

import (
	"fmt"
	"net"
	"os"
)

func CheckError(err error) {
	if err != nil {
		fmt.Println("Error: ", err)
		os.Exit(0)
	}
}

func main() {
	ServerAddr, err := net.ResolveUDPAddr("udp", ":7778")
	CheckError(err)
	fmt.Println("listening on :7778")

	ServerConn, err := net.ListenUDP("udp", ServerAddr)
	CheckError(err)
	defer ServerConn.Close()

	buf := make([]byte, 1024)

	for {
		n, addr, err := ServerConn.ReadFromUDP(buf)
		fmt.Printf("received: %s from: %s\n", string(buf[0:n]), addr)
		if err != nil {
			fmt.Println("error: ", err)
		}
		ServerConn.WriteTo(buf[0:n], addr)
	}
}
