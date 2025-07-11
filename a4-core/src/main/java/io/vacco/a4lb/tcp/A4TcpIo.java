package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.util.*;
import java.io.*;
import java.net.*;
import java.util.Objects;

public class A4TcpIo implements Closeable {

  public final String id;
  public final Socket socket;
  public A4Backend backend;

  public A4TcpIo(Socket socket) {
    this.socket = Objects.requireNonNull(socket);
    this.id = A4Base36.hash4(socket.toString());
  }

  public A4TcpIo(InetSocketAddress dest, boolean openTls) {
    try {
      Socket socket;
      if (openTls) {
        var factory = A4TlsCerts.trustAllContext().getSocketFactory();
        socket = factory.createSocket(dest.getHostName(), dest.getPort());
      } else {
        socket = new Socket(dest.getHostName(), dest.getPort());
      }
      this.socket = socket;
      this.id = A4Base36.hash4(socket.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Server > Backend socket initialization error - " + dest, e);
    }
  }

  public long transferTo(A4TcpIo target) throws IOException {
    var in = socket.getInputStream();
    var out = target.socket.getOutputStream();
    long bytesTransferred = in.transferTo(out);
    out.flush();
    return bytesTransferred;
  }

  public A4TcpIo backend(A4Backend backend) {
    this.backend = Objects.requireNonNull(backend);
    return this;
  }

  @Override public void close() {
    A4Io.close(socket);
    this.backend = null;
  }

  @Override public String toString() {
    return id;
  }

}