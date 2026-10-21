package io.vacco.a4lb;

import io.vacco.a4lb.cfg.A4Format;
import io.vacco.a4lb.util.A4Exceptions;
import io.vacco.a4lb.util.A4Options;
import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.nio.file.Files;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class A4OptionsTest {
  static {
    it("Applies defaults for a valid configuration path", () -> {
      var dir = Files.createTempDirectory("a4-options");
      var opt = A4Options.from(new String[]{A4Options.flagOf(A4Options.kConfig, dir.toString())});
      assertEquals(dir.toFile(), opt.root);
      assertEquals(A4Options.DefaultHost, opt.api.host);
      assertEquals(A4Options.DefaultPort, opt.api.port);
      assertEquals(A4Format.text, opt.logFormat);
      assertEquals(A4Options.LogLevel.info, opt.logLevel);
    });

    it("Parses explicit flags, including case-insensitive enums", () -> {
      var dir = Files.createTempDirectory("a4-options");
      var opt = A4Options.from(new String[]{
        A4Options.flagOf(A4Options.kConfig, dir.toString()),
        A4Options.flagOf(A4Options.kApiHost, "0.0.0.0"),
        A4Options.flagOf(A4Options.kApiPort, "8081"),
        A4Options.flagOf(A4Options.kLogFormat, "JSON"),
        A4Options.flagOf(A4Options.kLogLevel, "DEBUG")
      });
      assertEquals("0.0.0.0", opt.api.host);
      assertEquals(8081, opt.api.port);
      assertEquals(A4Format.json, opt.logFormat);
      assertEquals(A4Options.LogLevel.debug, opt.logLevel);
    });

    it("Reports a validation error instead of throwing an NPE when --config is missing", () -> {
      assertThrows(A4Exceptions.A4ValidationException.class, () -> A4Options.from(new String[]{}));
    });

    it("Reports a validation error for a missing path", () -> {
      assertThrows(A4Exceptions.A4ValidationException.class,
        () -> A4Options.from(new String[]{A4Options.flagOf(A4Options.kConfig, "/definitely/not/here")}));
    });

    it("Rejects unknown log levels", () -> {
      var dir = Files.createTempDirectory("a4-options");
      assertThrows(IllegalArgumentException.class, () -> A4Options.from(new String[]{
        A4Options.flagOf(A4Options.kConfig, dir.toString()),
        A4Options.flagOf(A4Options.kLogLevel, "verbose")
      }));
    });
  }
}
