package io.vacco.a4lb.niossl;

import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * <p>A wrapper around a real {@link ServerSocketChannel} that produces {@link SSLSocketChannel} on {@link #accept()}. The real ServerSocketChannel must be
 * bound externally (to this class) before calling the accept method.</p>
 *
 * @see SSLSocketChannel
 */
public class SSLServerSocketChannel extends ServerSocketChannel {

  /** Should the SS server ask for client certificate authentication? Default is {@code false}. */
  public boolean wantClientAuthentication;

  /** Should the SSL server require client certificate authentication? Default is {@code false}. */
  public boolean needClientAuthentication;

  private final String[] protocols, ciphers;

  private final ServerSocketChannel serverSocketChannel;
  private final SSLContext sslContext;
  private final SNIMatcher sniMatcher;
  private final ExecutorService threadPool;

  /**
   * @param serverSocketChannel The real server socket channel that accepts network requests.
   * @param sslContext          The SSL context used to create the {@link SSLEngine} for incoming requests.
   * @param threadPool          The thread pool passed to SSLSocketChannel used to execute long-running, blocking SSL operations such as certificate validation with a CA (<a href="http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLEngineResult.HandshakeStatus.html#NEED_TASK">NEED_TASK</a>)
   */
  public SSLServerSocketChannel(ServerSocketChannel serverSocketChannel,
                                SSLContext sslContext, ExecutorService threadPool,
                                SNIMatcher sniMatcher, String[] protocols, String[] ciphers) {
    super(serverSocketChannel.provider());
    this.serverSocketChannel = serverSocketChannel;
    this.sslContext = sslContext;
    this.sniMatcher = sniMatcher;
    this.threadPool = threadPool;
    this.protocols = protocols;
    this.ciphers = ciphers;
  }

  /**
   * Convenience call to keep from having to cast {@code SocketChannel} into {@link SSLSocketChannel} when calling {@link #accept()}.
   *
   * @return An SSLSocketChannel or {@code null} if this channel is in non-blocking mode and no connection is available to be accepted.
   * @see #accept()
   */
  public SSLSocketChannel acceptOverSSL() throws IOException {
    return (SSLSocketChannel) accept();
  }

  /**
   * <p>Accepts a connection made to this channel's socket.</p>
   * <p>If this channel is in non-blocking mode then this method will immediately return null if there are no pending connections. Otherwise it will block indefinitely until a new connection is available or an I/O error occurs.</p>
   * <p>The socket channel returned by this method, if any, will be in blocking mode regardless of the blocking mode of this channel.</p>
   * <p>This method performs exactly the same security checks as the accept method of the ServerSocket class. That is, if a security manager has been installed then for each new connection this method verifies that the address and port number of the connection's remote endpoint are permitted by the security manager's checkAccept method.</p>
   *
   * @return An SSLSocketChannel or {@code null} if this channel is in non-blocking mode and no connection is available to be accepted.
   * @throws java.nio.channels.NotYetConnectedException   If this channel is not yet connected
   * @throws java.nio.channels.ClosedChannelException     If this channel is closed
   * @throws java.nio.channels.AsynchronousCloseException If another thread closes this channel while the read operation is in progress
   * @throws java.nio.channels.ClosedByInterruptException If another thread interrupts the current thread while the read operation is in progress, thereby closing the channel and setting the current thread's interrupt status
   * @throws IOException                                  If some other I/O error occurs
   */
  @Override
  public SocketChannel accept() throws IOException {
    SocketChannel channel = serverSocketChannel.accept();
    if (channel == null) {
      return null;
    } else {
      channel.configureBlocking(false);

      SSLEngine sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(false);
      sslEngine.setWantClientAuth(wantClientAuthentication);
      sslEngine.setNeedClientAuth(needClientAuthentication);

      if (protocols != null && protocols.length > 0) {
        sslEngine.setEnabledProtocols(this.protocols);
      }
      if (ciphers != null && ciphers.length > 0) {
        sslEngine.setEnabledCipherSuites(this.ciphers);
      }

      var params = sslEngine.getSSLParameters();
      params.setSNIMatchers(List.of(sniMatcher));
      sslEngine.setSSLParameters(params);

      return new SSLSocketChannel(channel, sslEngine, threadPool);
    }
  }

  @Override
  public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
    return serverSocketChannel.bind(local, backlog);
  }

  @Override
  public SocketAddress getLocalAddress() throws IOException {
    return serverSocketChannel.getLocalAddress();
  }

  @Override
  public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
    return serverSocketChannel.setOption(name, value);
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException {
    return serverSocketChannel.getOption(name);
  }

  @Override
  public Set<SocketOption<?>> supportedOptions() {
    return serverSocketChannel.supportedOptions();
  }

  @Override
  public ServerSocket socket() {
    return serverSocketChannel.socket();
  }

  @Override
  protected void implCloseSelectableChannel() throws IOException {
    serverSocketChannel.close();
  }

  @Override
  protected void implConfigureBlocking(boolean b) throws IOException {
    serverSocketChannel.configureBlocking(b);
  }

}
