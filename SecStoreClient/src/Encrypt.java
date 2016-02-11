import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

public class Encrypt {
	public static void rsaEncrypt(File file_loc, File file_des)
			throws Exception {
		byte[] data = new byte[32];
		int i;
		Key pubKey = KeyFromFile.readKeyFromFile("public.key");
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		FileInputStream fileIn = new FileInputStream(file_loc);
		FileOutputStream fileOut = new FileOutputStream(file_des);
		CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher);
		// Read in the data from the file and encrypt it
		while ((i = fileIn.read(data)) != -1) {
			cipherOut.write(data, 0, i);
		}
		// Close the encrypted file
		cipherOut.close();
		fileIn.close();
	}
}
