package ch.ubique.n2step.sdk.backend.ws;

import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.PwHash;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class SodiumTest {

    @Test
    public void testDependency() throws SodiumException {
        SodiumJava sodium = new SodiumJava();
        LazySodiumJava lazySodium = new LazySodiumJava(sodium, StandardCharsets.UTF_8);
        // Now you can cast and use the enhanced native
        // Libsodium functions
        byte[] pw = lazySodium.bytes("A cool password");
        byte[] outputHash = new byte[PwHash.STR_BYTES];
        PwHash.Native pwHash = (PwHash.Native) lazySodium;
        boolean success =
                pwHash.cryptoPwHashStr(
                        outputHash, pw, pw.length, PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);

        // ... or you can use the super-powered lazy functions.
        // For example, this is equivalent to the above.
        PwHash.Lazy pwHashLazy = (PwHash.Lazy) lazySodium;
        String hash =
                pwHashLazy.cryptoPwHashStr(
                        "A cool password", PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);
    }
}
