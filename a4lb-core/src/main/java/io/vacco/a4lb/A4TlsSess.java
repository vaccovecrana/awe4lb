package io.vacco.a4lb;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class A4TlsSess extends A4TcpSess {

  public A4TlsSess(A4TcpSrv owner, A4TcpCl client, A4TcpBk backend) {
    super(owner, client, backend);
  }
}
