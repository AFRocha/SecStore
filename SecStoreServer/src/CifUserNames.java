import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * SecStoreServer 
 * Cifrar ficheiro usernames
 * @author AndreFRocha
 *
 */
public class CifUserNames {
	public static void main(String[] args) throws Exception {
		final String keyStoreFile = "C:/Server/javacirecep.keystore";
		KeyStore keyStore = createKeyStore(keyStoreFile, "javaci123");

		// generate a secret key for AES encryption
		SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

		// store the secret key
		KeyStore.SecretKeyEntry keyStoreEntry = new KeyStore.SecretKeyEntry(secretKey);
		PasswordProtection keyPassword = new PasswordProtection("pw-secret".toCharArray());
		keyStore.setEntry("mySecretKey", keyStoreEntry, keyPassword);
		keyStore.store(new FileOutputStream(keyStoreFile), "javaci123".toCharArray());

		// retrieve the stored key back
		
		try{
			KeyStore.Entry entry = keyStore.getEntry("mySecretKey", keyPassword);
			SecretKey keyFound = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
			byte[] key = keyFound.getEncoded();
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16); // use only first 128 bit

			SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

			FileInputStream fis;
			FileOutputStream fos;
			CipherInputStream cis;

			fis = new FileInputStream("C:/Server/usernames.txt");
			fos = new FileOutputStream("C:/Server/loading.txt");

			cis = new CipherInputStream(fis, cipher);
			byte[] b = new byte[16];  
			int i = cis.read(b);
			while (i != -1) {
				fos.write(b, 0, i);
				i = cis.read(b);
			}
			cis.close();
			fos.close();
			fis.close();

			File loadingFile = new File("C:/Server/loading.txt");
			File passFile = new File("C:/Server/usernames.txt");
			passFile.delete();
			File novaFile = new File("C:/Server/usernames.txt");
			loadingFile.renameTo(novaFile);
		}

		catch (Exception e) {
			System.out.println("Erro ao cifrar os dados do ficheiro");
		}
	}
	private static KeyStore createKeyStore(String fileName, String pw) throws Exception {
		File file = new File(fileName);

		final KeyStore keyStore = KeyStore.getInstance("JCEKS");
		if (file.exists()) {
			// .keystore file already exists => load it
			keyStore.load(new FileInputStream(file), pw.toCharArray());
		} else {
			// .keystore file not created yet => create it
			keyStore.load(null, null);
			keyStore.store(new FileOutputStream(fileName), pw.toCharArray());
		}

		return keyStore;
	}
}
