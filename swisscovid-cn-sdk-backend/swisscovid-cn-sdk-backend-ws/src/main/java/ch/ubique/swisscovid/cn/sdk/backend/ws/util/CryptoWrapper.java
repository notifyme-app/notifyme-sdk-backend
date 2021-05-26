package ch.ubique.swisscovid.cn.sdk.backend.ws.util;

import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.utils.LibraryLoadingException;
import com.herumi.mcl.Mcl;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoWrapper {

  private static final Logger logger = LoggerFactory.getLogger(CryptoWrapper.class);

  private final CryptoUtilV3 cryptoUtilV3;

  public CryptoWrapper(String skHex, String pkHex, String mskHex, String mpkHex) {
    SodiumJava sodium;
    // Do custom loading for the libsodium lib, as it does not work out of the box
    // with spring boot bundled jars. To get a path to the full file, we copy
    // libsodium to a tmpfile and give that absolute path to lazysodium
    try {
      InputStream in =
          getClass()
              .getClassLoader()
              .getResourceAsStream("libsodium/" + getSodiumPathInResources());
      File libTmpFile = File.createTempFile("libsodium", null);
      Files.copy(in, libTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      in.close();
      sodium = new SodiumJava(libTmpFile.getAbsolutePath());
    } catch (Exception e) {
      logger.error("unable to load libsodium", e);
      throw new RuntimeException(e);
    }

    // Do custom loading for the mcl lib, as it does not work out of the box
    // with spring boot bundled jars. To get a path to the full file, we copy
    // libmcl to a tmpfile and load that path
    try {
      InputStream in =
          getClass().getClassLoader().getResourceAsStream("libmcl/" + getMclPathInResources());
      File libTmpFile = File.createTempFile("libmcl", null);
      Files.copy(in, libTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      in.close();
      System.load(libTmpFile.getAbsolutePath());
      Mcl.SystemInit(Mcl.BLS12_381);
    } catch (Exception e) {
      logger.error("unable to load libmcl", e);
      throw new RuntimeException(e);
    }

    cryptoUtilV3 = new CryptoUtilV3(skHex, pkHex, mskHex, mpkHex, sodium);
  }

  /** Contains all CrowdNotifier V3 related methods */
  public CryptoUtilV3 getCryptoUtilV3() {
    return cryptoUtilV3;
  }

  /**
   * Returns the absolute path to sodium library inside JAR (beginning with '/'), e.g.
   * /linux/libsodium.so.
   */
  private static String getSodiumPathInResources() {
    boolean is64Bit = Native.POINTER_SIZE == 8;
    if (Platform.isWindows()) {
      if (is64Bit) {
        return getPath("windows64", "libsodium.dll");
      } else {
        return getPath("windows", "libsodium.dll");
      }
    }
    if (Platform.isARM()) {
      return getPath("armv6", "libsodium.so");
    }
    if (Platform.isLinux()) {
      if (is64Bit) {
        return getPath("linux64", "libsodium.so");
      } else {
        return getPath("linux", "libsodium.so");
      }
    }
    if (Platform.isMac()) {
      return getPath("mac", "libsodium.dylib");
    }

    String message =
        String.format(
            "Unsupported platform: %s/%s",
            System.getProperty("os.name"), System.getProperty("os.arch"));
    throw new LibraryLoadingException(message);
  }

  /**
   * Returns the absolute path to sodium library inside JAR (beginning with '/'), e.g.
   * /linux/libmcljava.so.
   */
  private static String getMclPathInResources() {
    boolean is64Bit = Native.POINTER_SIZE == 8;
    if (Platform.isWindows()) {
      if (is64Bit) {
        throw new UnsupportedOperationException("windows64 not supported");
      } else {
        throw new UnsupportedOperationException("windows not supported");
      }
    }
    if (Platform.isARM()) {
      throw new UnsupportedOperationException("arm not supported");
    }
    if (Platform.isLinux()) {
      if (is64Bit) {
        return getPath("linux64", "libmcljava.so");
      } else {
        throw new UnsupportedOperationException("linux32 not supported");
      }
    }
    if (Platform.isMac()) {
      return getPath("mac", "libmcljava.dylib");
    }

    String message =
        String.format(
            "Unsupported platform: %s/%s",
            System.getProperty("os.name"), System.getProperty("os.arch"));
    throw new UnsupportedOperationException(message);
  }

  private static String getPath(String folder, String name) {
    return folder + File.separator + name;
  }
}
