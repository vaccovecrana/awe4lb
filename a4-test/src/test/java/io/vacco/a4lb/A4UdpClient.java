package io.vacco.a4lb;

import java.io.IOException;
import java.net.*;

public class A4UdpClient implements AutoCloseable {

  private final DatagramSocket socket;
  private final InetAddress address;

  public A4UdpClient() throws SocketException, UnknownHostException {
    socket = new DatagramSocket();
    address = InetAddress.getByName("127.0.0.1");
  }

  public String sendEcho(String msg) throws IOException {
    byte[] buf = msg.getBytes();
    var packet = new DatagramPacket(buf, buf.length, address, 8070);
    socket.send(packet);
    packet = new DatagramPacket(buf, buf.length);
    socket.receive(packet);
    return new String(packet.getData(), 0, packet.getLength());
  }

  public void close() {
    socket.close();
  }

}