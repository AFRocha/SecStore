import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Scanner;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;


/**
 * SecStoreClient
 * @author AndreFRocha
 * 
 * 
 * Nota: Uma vez criadas as suas chaves, não as perca, 
 * pois não terá mais acesso aos seus documentos 
 * que guardou anteriormente no servidor. Estas chaves
 * são criadas a primeira vez que correr o programa.
 */


public class SecShareClient {

	public static void main(String[] args) throws NoSuchAlgorithmException, ClassNotFoundException, IOException {

		try{
			File pubkey = new File("public.key");
			File prvkey = new File("private.key");
			if( !prvkey.exists() || !pubkey.exists()){
				KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
				kpg.initialize(2048);
				KeyPair kp = kpg.genKeyPair();
				PublicKey publicKey = kp.getPublic();
				PrivateKey privateKey = kp.getPrivate();
				System.out.println("Keys criadas");
				KeyFactory fact = KeyFactory.getInstance("RSA");
				RSAPublicKeySpec pub = fact.getKeySpec(publicKey, RSAPublicKeySpec.class);
				RSAPrivateKeySpec priv = fact.getKeySpec(privateKey,RSAPrivateKeySpec.class);
				SaveToFile.saveToFile("public.key", pub.getModulus(), pub.getPublicExponent());
				SaveToFile.saveToFile("private.key", priv.getModulus(), priv.getPrivateExponent());
			}
			System.out.println("Bem-vindo ao SecShareClient!\n");
			System.out.println("Por favor introduza um dos comandos válidos, por exemplo: \n");
			System.out.println("-u <userId> -a <serverAddress> -c <filenames>");
			System.out.println("Copia os seus ficheiros para o servidor\n");
			System.out.println("-u <userId> -a <serverAddress> -g <filenames>");
			System.out.println("Actualiza os seus ficheiros se existirem mais recentes no servidor\n");
			System.out.println("-u <userId> -a <serverAddress> -s <filenames>");
			System.out.println("Abre sessão de sincronização entre o seu computador e o servidor.\n"
					+ "Quando o seu ficheiro for alterado, é alterado automaticamente no servidor.");
			System.out.println("-u <userId> -a <serverAddress> -l");
			System.out.println("Lista todos os seus ficheiros que estão no servidor\n");
			System.out.println("Instruções: ");
			System.out.println("<UserId>: O seu ID");
			System.out.println("<ServerAddress>: O endereço do servidor(exemplo: 255.255.25.1:5478)");
			System.out.println("<filenames>: Os ficheiros(exemplo: C:/imagens/peixes.png)");
			int sinalSaida = 0;
			int numeroOperações=0;
			String password=null;//Password so vai ser pedida ao utilizador a primeira vez
			System.setProperty("javax.net.ssl.trustStore", "cacerts.truststore");
			while(sinalSaida==0){
				Scanner scan = new Scanner(System.in);
				System.out.println("Introduza um comando: ");
				String leitura = scan.nextLine();
				String[] comando = leitura.split(" "); 
				if(comando.length < 5 && comando.length > 7 )
					break;
				if(!comando[0].equals("-u") || !comando[2].equals("-a"))
					break;
				String serverAddress[]=comando[3].split(":");
				String ip = serverAddress[0];
				String port = serverAddress[1];
				String user = comando[1];
				if(numeroOperações==0){
					System.out.println("\nIntroduza a password: ");
					password = scan.next();			
				}
				int porto = Integer.parseInt(port);
				SocketFactory sf = SSLSocketFactory.getDefault();
				Socket sck = sf.createSocket(ip, porto);
				System.out.println("A estabelecer ligação com o servidor...");
				ObjectOutputStream out = new ObjectOutputStream(sck.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(sck.getInputStream());
				out.writeObject(user);
				out.writeObject(password);
				Object recebidoDoServidor =(Boolean)in.readObject();

				if((boolean)recebidoDoServidor == false){
					sck.close();
					if(numeroOperações==0)
						System.out.println("Password Errada/Utilizador não existe!");
				} else {//cliente entrou no servidor
					if(numeroOperações==0)
						System.out.println("Password Correcta! Bem-vindo.");
					switch(comando[4]){
					case "-c":
						out.writeObject(1);
						File ficheiro = new File(comando[5]);
						String nomedoficheiro = ficheiro.getName();
						int pos = nomedoficheiro.lastIndexOf(".");
						String nomeficheiro = nomedoficheiro.substring(0, pos);
						String extension = "";
						int counter = nomedoficheiro.lastIndexOf('.');
						if (counter >= 0) {
							extension = nomedoficheiro.substring(counter+1);
						}
						nomeficheiro += ".";
						String ficheiroSaida = "cifrado_"+nomeficheiro+extension;
						File ficheirosaida = new File(ficheiroSaida);

						try {
							Encrypt.rsaEncrypt(ficheiro, ficheirosaida);  
						}

						catch (Exception e) {
							System.out.println("Erro ao cifrar os dados do ficheiro");
						}
						out.writeObject(nomeficheiro+extension); 
						out.writeObject(ficheiro.lastModified());
						FileInputStream fis1 = new FileInputStream(ficheirosaida);  
						byte[]buffer = new byte[1024];  
						Integer bytesRead = 0;  
						while ((bytesRead = fis1.read(buffer)) > 0) {  
							out.writeObject(bytesRead);  
							out.writeObject(Arrays.copyOf(buffer, buffer.length));  
						}  
						File cifrado = new File (ficheiroSaida);
						cifrado.delete();
						fis1.close();
						if((boolean)in.readObject()==true){
							System.out.println("Ficheiro transferido com sucesso!");
							System.out.println("Pretende efectuar mais operações? (s/n)");
							ficheirosaida.delete();
						}else{
							System.out.println("Versao no servidor mais actualizada");
							System.out.println("O Ficheiro não foi transferido!");
							System.out.println("Pretende efectuar mais operações? (s/n)");
						}
						if(scan.next().equals("n")){
							numeroOperações++;
							sinalSaida++;
							scan.close();
							in.close();
							out.close();
							break;
						} else {
							numeroOperações++;
							break;
						}
					case "-p":
						out.writeObject(2);
						out.writeObject(comando[6]); 
						out.writeObject(comando[5]);

						System.out.println("Ficheiro partilhado com sucesso!");
						System.out.println("Pretende efectuar mais operações? (s/n)");
						if(scan.next().equals("n")){
							numeroOperações++;
							sinalSaida++;
							scan.close();
							in.close();
							out.close();
							break;
						} else {
							numeroOperações++;
							break;
						}
					case "-g":
						out.writeObject(3);
						out.writeObject(comando[5]); 
						System.out.println("Introduza o directorio para guardar o ficheiro: ");
						String directorioAreceber = scan.next();
						String directorio = directorioAreceber;
						directorioAreceber+=(comando[5]);
						byte[] buffer2 = new byte[1024];
						File ficheiro2 = new File(directorioAreceber);
						String extension2 = "";
						int counter3 = comando[5].lastIndexOf('.');
						if (counter3 >= 0) {
							extension2 = comando[5].substring(counter3+1);
						}
						if(ficheiro2.exists()){
							if(ficheiro2.lastModified() < (long)in.readObject()){
								out.writeObject(true);
								FileOutputStream fos1 = new FileOutputStream(ficheiro2);
								Integer bytesRead2 = 0;
								do{
									Object o = in.readObject();
									bytesRead2 = (Integer)o;
									o = in.readObject();
									buffer2=(byte[])o;
									fos1.write(buffer2,0,bytesRead2);

								} while(bytesRead2 == 1024);
								fos1.close();
								System.out.println("Ficheiro recebido com sucesso!");
								try{
									File loadingFile = new File(directorio+"loading."+extension2);
									Decrypt.rsaDecrypt(ficheiro2,loadingFile);
									ficheiro2.delete();
									File novaFile = new File(directorioAreceber);
									loadingFile.renameTo(novaFile);
								}catch (Exception e) {
									System.out.println("Erro ao decifrar os dados do ficheiro");
								}
							}else{
								out.writeObject(false);
								System.out.println("O seu ficheiro e' mais actual que o do servidor!");
								System.out.println("Ficheiro não recebido!");
							}
						}else{
							ficheiro2.createNewFile();
							out.writeObject(true);
							FileOutputStream fos1 = new FileOutputStream(ficheiro2);
							Integer bytesRead2 = 0;
							@SuppressWarnings("unused")
							long lastModified = (long)in.readObject();
							do{
								Object o = in.readObject();
								bytesRead2 = (Integer)o;
								o = in.readObject();
								buffer2=(byte[])o;
								fos1.write(buffer2,0,bytesRead2);
							} while(bytesRead2 == 1024);
							fos1.close();
							@SuppressWarnings("unused")
							String passwordi = "";
							if ((boolean)in.readObject()==true){
								passwordi= (String) in.readObject();
							}else{
								passwordi = password;
							}
							try{
								File loadingFile = new File(directorio+"loading."+extension2);
								Decrypt.rsaDecrypt(ficheiro2,loadingFile);
								ficheiro2.delete();
								File novaFile = new File(directorioAreceber);
								loadingFile.renameTo(novaFile);
							}catch (Exception e) {
								System.out.println("Erro ao decifrar os dados do ficheiro");
							}
							System.out.println("Ficheiro recebido com sucesso!");
						}
						System.out.println("Pretende efectuar mais operações? (s/n)");
						if(scan.next().equals("n")){
							numeroOperações++;
							sinalSaida++;
							scan.close();
							in.close();
							out.close();
							break;
						} else {
							numeroOperações++;
							break;
						}

					case "-s":
						out.writeObject(4);
						File fich = new File(comando[5]);
						out.writeObject(fich.getName()); 
						long datafile = fich.lastModified();
						while((boolean)in.readObject()!=true){
							
							System.out.println("---A sincronizar:---");
							
							long datafile2 = fich.lastModified();
							if(datafile2>datafile){
								String nomedoficheiro2 = fich.getName();
								int pos2 = nomedoficheiro2.lastIndexOf(".");
								String nomeficheiro2 = nomedoficheiro2.substring(0, pos2);
								String extension3 = "";
								int counter2 = nomedoficheiro2.lastIndexOf('.');
								if (counter2 >= 0) {
									extension3 = nomedoficheiro2.substring(counter2+1);
								}
								nomeficheiro2 += ".";
								String ficheiroSaida2 = "cifrado_"+nomeficheiro2+extension3;
								File ficheirosaida2 = new File(ficheiroSaida2);
								try {
									Encrypt.rsaEncrypt(fich, ficheirosaida2);  
								}

								catch (Exception e) {
									System.out.println("Erro ao cifrar os dados do ficheiro");
								}
								out.writeObject(true);
								FileInputStream fis = new FileInputStream(ficheirosaida2);  
								byte[]buffer3 = new byte[1024];  
								Integer bytesRead2 = 0;  
								while ((bytesRead2 = fis.read(buffer3)) > 0) {  
									out.writeObject(bytesRead2);  
									out.writeObject(Arrays.copyOf(buffer3, buffer3.length));  
								}  
								File cifrado2 = new File (ficheiroSaida2);
								cifrado2.delete();
								fis.close();
								datafile = datafile2;//actualizar variavel datafile
							}else{
								out.writeObject(false);
							}
							if((Boolean)in.readObject()==true){
								System.out.println("O Ficheiro "+fich.getName()+" foi actualizado!");
								FileOutputStream fos1 = new FileOutputStream(comando[5]);
								Integer bytesRead2 = 0;
								do{
									Object o = in.readObject();
									bytesRead2 = (Integer)o;
									o = in.readObject();
									buffer2=(byte[])o;
									fos1.write(buffer2,0,bytesRead2);

								} while(bytesRead2 == 1024);
							  fos1.close();
							}

							int contador = (Integer)in.readObject();
							for(int i1 = 0; i1<contador;i1++)
								System.out.println(in.readObject());
							System.out.println("-------------------");
						} 
						break;
					case "-l":
						out.writeObject(5);
						int conta = (Integer)in.readObject();
						System.out.println("\nOs seus ficheiros: ");
						while(conta>0){
							Object nomeFicheiro = in.readObject();
							Object dataFicheiro = in.readObject();
							if(!nomeFicheiro.equals("regPartilha.txt")){
								System.out.print("-"+nomeFicheiro);
								System.out.println("  Data:"+dataFicheiro);
							}
							conta--;
						}

						System.out.println("\nEstá a ser partilhado consigo: ");
						int signal=0;
						while((Boolean)in.readObject()!=false){
							System.out.print("-"+in.readObject());
							System.out.print("   Dono:"+in.readObject());
							System.out.print("  Data:"+in.readObject());
							int contador = (Integer)in.readObject();
							System.out.print(" E tambem esta' a ser partilhado com: ");
							while(contador>0){
								System.out.println(in.readObject());
								System.out.print(" ");
								contador--;
							}
							signal++;
						}
						if(signal==0)
							System.out.println("--Não tem partilhas--");
						System.out.println("\nListagem concluída!");
						System.out.println("Pretende efectuar mais operações? (s/n)");
						if(scan.next().equals("n")){
							numeroOperações++;
							sinalSaida++;
							scan.close();
							in.close();
							out.close();
							break;
						} else {
							numeroOperações++;
							break;
						}

					default:
						System.out.println("Comando inválido!");
						break;
					}

				}

			}

			System.out.println("Obrigado por usar SecShareClient! Número de operações realizadas: "+numeroOperações+".");
		}catch(Exception e){
			System.out.println("Falha na ligação ao servidor!");
		}


	}

	
	
	


}
