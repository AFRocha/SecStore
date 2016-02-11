import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;


public class SaveToFile {
	public static void saveToFile(String fileName, BigInteger mod,
			BigInteger exp) throws IOException {
		ObjectOutputStream fileOut = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(fileName)));
		try {
			fileOut.writeObject(mod);
			fileOut.writeObject(exp);
		} catch (Exception e) {
			throw new IOException("Erro!");
		} finally {
			fileOut.close();
		}
	}
}
