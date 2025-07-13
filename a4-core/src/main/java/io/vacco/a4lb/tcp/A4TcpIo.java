package io.vacco.a4lb.tcp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.util.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;

public class A4TcpIo implements Closeable {

  public final String id;
  public final Socket socket;
  public A4Backend backend;

  public byte[] peek = new byte[256 * 1024];
  public int    peekBytes;

  public boolean eof = false;
  public Exception rxe, txe;

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
      socket.setSoTimeout(5000);
      socket.setTcpNoDelay(true);
      this.socket = socket;
      this.id = A4Base36.hash4(socket.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Server > Backend socket initialization error - " + dest, e);
    }
  }

  public int read() {
    try {
      Arrays.fill(peek, (byte) 0x00);
      this.peekBytes = this.socket.getInputStream().read(peek);
      this.rxe = null;
      return this.peekBytes;
    } catch (IOException e) {
      this.rxe = e;
      this.eof = true;
      return -1;
    }
  }

  public int writeTo(A4TcpIo target) {
    if (target == null) {
      return -1;
    }
    if (peekBytes == -1) {
      return peekBytes;
    }
    try {
      var out = target.socket.getOutputStream();
      out.write(peek, 0, peekBytes);
      out.flush();
      this.txe = null;
      return peekBytes;
    } catch (IOException e) {
      this.txe = e;
      this.eof = true;
      return -1;
    }
  }

  public A4TcpIo backend(A4Backend backend) {
    this.backend = Objects.requireNonNull(backend);
    return this;
  }

  @Override public void close() {
    A4Io.close(socket);
  }

  @Override public String toString() {
    return id;
  }

}