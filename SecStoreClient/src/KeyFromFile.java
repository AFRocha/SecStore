import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class KeyFromFile {
	static Key readKeyFromFile(String keyFileName) throws IOException {
		InputStream in = new FileInputStream(keyFileName);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(
				in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			KeyFactory fact = KeyFactory.getInstance("RSA");
			if (keyFileName.startsWith("public"))
				return fact.generatePublic(new RSAPublicKeySpec(m, e));
			else
				return fact.generatePrivate(new RSAPrivateKeySpec(m, e));
		} catch (Exception e) {
			throw new RuntimeException("Erro", e);
		} finally {
			oin.close();
		}
	}
}
