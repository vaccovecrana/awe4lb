package io.vacco.a4lb.impl;

import java.io.Closeable;
import java.util.concurrent.Callable;

public interface A4Srv extends Callable<Void>, Closeable {}
