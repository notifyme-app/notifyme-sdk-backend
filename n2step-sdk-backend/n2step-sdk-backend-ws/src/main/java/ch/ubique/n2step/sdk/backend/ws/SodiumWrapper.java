package ch.ubique.n2step.sdk.backend.ws;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.utils.LibraryLoader.Mode;

import ch.ubique.n2step.sdk.backend.model.SeedMessageOuterClass;
import ch.ubique.n2step.sdk.backend.model.SeedMessageOuterClass.SeedMessage;

public class SodiumWrapper {

	private static final Logger logger = LoggerFactory.getLogger(SodiumWrapper.class);

	public final byte[] sk;
	public final byte[] pk;
	private final SodiumJava sodium;

	public SodiumWrapper(String skHex, String pkHex) {
		try {
			this.sk = Hex.decodeHex(skHex);
		} catch (DecoderException e) {
			logger.error("unable to parse sk hexstring", e);
			throw new RuntimeException(e);
		}
		try {
			this.pk = Hex.decodeHex(pkHex);
		} catch (DecoderException e) {
			logger.error("unable to parse pk hexstring", e);
			throw new RuntimeException(e);
		}
		this.sodium = new SodiumJava(Mode.PREFER_BUNDLED);
	}

	public byte[] decryptQrTrace(byte[] ctx) throws InvalidProtocolBufferException {
		byte[] msg = new byte[ctx.length - Box.SEALBYTES];
		int result = sodium.crypto_box_seal_open(msg, ctx, ctx.length, pk, sk);
		SeedMessage seed = SeedMessageOuterClass.SeedMessage.parseFrom(msg);
		logger.debug(result + " msg: " + seed.toString());
		byte[] newPk = new byte[64];
		byte[] newSk = new byte[64];
		sodium.crypto_sign_seed_keypair(newPk, newSk, msg);
		logger.debug(newSk.toString());
		return newSk;
	}
}
