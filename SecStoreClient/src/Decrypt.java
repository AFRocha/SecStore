import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

public class Decrypt {
	public static void rsaDecrypt(File file_loc, File file_des)
			throws Exception {
		//byte[] data = new byte[32];
		int i;
		Key priKey = KeyFromFile.readKeyFromFile("private.key");
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, priKey);
		FileInputStream fileIn = new FileInputStream(file_loc);
		CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
		FileOutputStream fileOut = new FileOutputStream(file_des);
		// Write data to new file
		while ((i = cipherIn.read()) != -1) {
			fileOut.write(i);
		}
		// Close the file
		fileIn.close();
		cipherIn.close();
		fileOut.close();
	}
}
