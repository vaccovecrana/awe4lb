package io.vacco.a4lb.udp;

import io.vacco.a4lb.cfg.A4Backend;
import io.vacco.a4lb.util.A4Io;
import java.io.Closeable;
import java.net.*;
import java.nio.channels.*;
import java.util.Objects;

public class A4UdpIo implements Closeable {

  public A4Backend          backend;
  public DatagramChannel    channel;
  public SelectionKey       channelKey;
  public InetSocketAddress  client;

  public A4UdpIo(Selector selector, A4Backend backend, InetSocketAddress client) {
    try {
      this.backend = Objects.requireNonNull(backend);
      this.channel = DatagramChannel.open().connect(new InetSocketAddress(backend.addr.host, backend.addr.port));
      this.client = Objects.requireNonNull(client);
      this.channel.configureBlocking(false);
      this.channelKey = channel.register(selector, SelectionKey.OP_READ);
      this.channelKey.attach(this);
    } catch (Exception e) {
      throw new IllegalStateException("Client-Server channel initialization error - " + client, e);
    }
  }

  @Override public void close() {
    channelKey.attach(null);
    channelKey.cancel();
    A4Io.close(channel);
    this.backend = null;
  }

}
