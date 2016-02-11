import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * SecStoreServer 
 * Decifrar ficheiro usernames
 * @author AndreFRocha
 *
 */
public class DecUserNames {
	public static void main(String[] args) throws Exception {
		final String keyStoreFile = "C:/Server/javacirecep.keystore";
	    KeyStore ks  = KeyStore.getInstance("JCEKS");
	    ks.load(new FileInputStream(keyStoreFile), "javaci123".toCharArray());
		
	    // retrieve the stored key back
	    PasswordProtection keyPassword = new PasswordProtection("pw-secret".toCharArray());
	    KeyStore.Entry entry = ks.getEntry("mySecretKey", keyPassword);
	    SecretKey keyFound = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
	
		try{
			byte[] key = keyFound.getEncoded();
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16); // use only first 128 bit

			SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);

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
	
}
