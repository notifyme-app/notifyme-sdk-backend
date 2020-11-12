package ch.ubique.notifyme.sdk.backend.ws;

import ch.ubique.notifyme.sdk.backend.model.SeedMessageOuterClass;
import ch.ubique.notifyme.sdk.backend.model.SeedMessageOuterClass.SeedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.interfaces.PwHash;
import com.goterl.lazycode.lazysodium.interfaces.SecretBox;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
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
    
    

    @Test
	@Ignore("requires pk and sk")
    public void testDecryptQRTrace()
            throws SodiumException, DecoderException, UnsupportedEncodingException,
                    InvalidProtocolBufferException {
        SodiumJava sodium = new SodiumJava();
        LazySodiumJava lazySodium = new LazySodiumJava(sodium, StandardCharsets.UTF_8);
        SecretBox.Native secretBoxNative = (SecretBox.Native) lazySodium;
        byte[] sk = Hex.decodeHex("");
        byte[] pk = Hex.decodeHex("");
        byte[] ctx =
                fromBase64(
                        "zFpds-bkQTfpx9qi5zzPzjkeZtDhrFgI_V_uqerB9Ww77Lf3w-ASMi8HtZNRx_e6ArBVYBfa5_YBEbt43Yg54TaRT9TGwYJG6T2FTn1nQ4zIWNDAiWIw44XWL0KTELecU-ctPAoBWb30j_nQICpE_7XObn41IBf-RQbbm5YpvliLmPhfI4-SdI3eRmA");
        byte[] msg = new byte[ctx.length - Box.SEALBYTES];
        int result = sodium.crypto_box_seal_open(msg, ctx, ctx.length, pk, sk);
        SeedMessage seed = SeedMessageOuterClass.SeedMessage.parseFrom(msg);
        System.out.println(result + " msg: " + new String(seed.toString()));
        byte[] newPk = new byte[64];
        byte[] newSk = new byte[64];
        sodium.crypto_sign_seed_keypair(newPk, newSk, msg);
        System.out.println(newSk);
        // sk needs to be stored in the db together with start and end time
    }

    private byte[] fromBase64(String base64) throws UnsupportedEncodingException {
        return Base64.getUrlDecoder().decode(base64.getBytes("UTF-8"));
    }
}
