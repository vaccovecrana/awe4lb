package io.vacco.a4lb;

import stormpot.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;

public class A4TcpBk extends BasePoolable implements AutoCloseable {

  public final String id;
  public final SelectionKey channelKey;
  public final SocketChannel channel;

  public ByteBuffer buffer;

  public A4TcpBk(Slot slot, String id, SelectionKey channelKey,
                 SocketChannel channel, ByteBuffer buffer) {
    super(slot);
    this.id = Objects.requireNonNull(id);
    this.channelKey = Objects.requireNonNull(channelKey);
    this.channel = Objects.requireNonNull(channel);
    this.buffer = Objects.requireNonNull(buffer);
  }

  @Override public void close() {
    release();
  }

}
