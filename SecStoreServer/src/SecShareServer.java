import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.MessageDigest;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Scanner;
import java.io.BufferedReader;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * SecStoreServer
 * @author AndreFRocha
 */

/*NOTA: Para o servidor poder receber os clientes, tem de ter um ficheiro
 * usernames.txt dentro da sua pasta C:/Server/, de modo a que estejam
 * lá identificados os usernames e as suas respectivas passwords, separados por ":".
 * 
 * Antes de executar o server, certifique-se que usernames.txt está cifrado
 * Para o cifrar execute a classe CifUserNames
 * 
 * Exemplo:
 *  user:sapo
 *  usertwo:bica
 *  userthree:bicho
 */


public class SecShareServer {
	public static void main(String[] args) throws IOException {

		File serverDir = new File("C://Server");
		boolean dirFlag = false;
		if(!serverDir.exists()){
			try{
				dirFlag = serverDir.mkdir();
			}catch(SecurityException Se){
				System.out.println("Erro ao criar directorio do servidor: "+Se);
			}
			if(dirFlag)
				System.out.println("Directório do servidor criado com sucesso!");
			else
				System.out.println("Directório não foi criado");
		}
		System.setProperty("javax.net.ssl.keyStore","keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "bicabica");
		File regFicheiros = new File("C://Server/regFicheiros.txt");
		boolean dirFlag2 = false;
		if(!regFicheiros.exists()){
			try{
				dirFlag2 = regFicheiros.createNewFile();
			}catch(SecurityException Se){
				System.out.println("Erro ao criar registo de ficheiros: "+Se);
			}
			if(dirFlag2)
				System.out.println("Registo de ficheiros criado com sucesso!");
			else
				System.out.println("O Registo de ficheiros não foi criado");
			//cifra o ficheiro de usernames && passwords
			try {
				CifUserNames.main(null);
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		File regPartilhas = new File("C://Server/regPartilhas.txt");
		boolean dirFlag3 = false;
		if(!regPartilhas.exists()){
			try{
				dirFlag3 = regPartilhas.createNewFile();
			}catch(SecurityException Se){
				System.out.println("Erro ao criar registo de partilhas do servidor: "+Se);
			}
			if(dirFlag3)
				System.out.println("Registo de partilhas do servidor criado com sucesso!");
			else
				System.out.println("O registo de partilhas do servidor não foi criado");
		}
		SecShareServer server = new SecShareServer();
		server.startServer();

	}


	public void startServer () {


		Scanner scan = new Scanner(System.in);
		int porto;
		System.out.println("Bem-vindo ao SecShareServer!\nIntroduza o Porto: ");
		porto = scan.nextInt();
		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );
		ServerSocket sSoc = null;
		try {
			sSoc = ssf.createServerSocket(porto);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		System.out.println("Servidor online!");
		scan.close();
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				//new SSLSimpleServer(sSoc.accept()).start( );
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class ServerThread extends Thread {
		private Socket socket = null;
		ServerThread(Socket inSoc) {
			socket = inSoc;
		}

		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				String user = null;
				String passwd = null;
				try {
					try {
						DecUserNames.main(null);
					} catch (Exception e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					String contauser = user+":"+passwd;
					FileInputStream fis2 = new FileInputStream("C://Server/usernames.txt");
					BufferedReader br = new BufferedReader(new InputStreamReader(fis2));
					String linhaUsernames = null;
					int signal=0;
					while (signal == 0 && ((linhaUsernames = br.readLine()) != null) ) {
						if(linhaUsernames.equals(contauser)){
							fis2.close();
							br.close();
							try {
								CifUserNames.main(null);
							} catch (Exception e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							}
							signal++;
							outStream.writeObject(new Boolean(true));
							System.out.println("O cliente "+user+" entrou!");
							File clienteDir = new File("C://Server/"+user);
							boolean dirFlag = false;
							if(!clienteDir.exists()){
								try{
									dirFlag = clienteDir.mkdir();
								}catch(SecurityException Se){
									System.out.println("Erro ao criar directorio do cliente: "+Se);
								}
								if(dirFlag)
									System.out.println("Directório do cliente "+user+" foi criado com sucesso!");
								else
									System.out.println("Directório do cliente não foi criado");
							}
							FileOutputStream fos = null;
							FileInputStream fis = null;
							switch((Integer)inStream.readObject()){
							case 1://opcao copiar ficheiros
								byte[] buffer = new byte[1024];
								Object o = inStream.readObject();
								String nomeFicheiro = o.toString();
								Object clienteDataFile = inStream.readObject();
								File file = new File(clienteDir+"/"+nomeFicheiro);
								if(file.lastModified()<(long)clienteDataFile){//versao do ficheiro do cliente mais actual
									outStream.writeObject(true);
									fos = new FileOutputStream(clienteDir+"/"+nomeFicheiro);
									Integer bytesRead = 0;
									do{
										o = inStream.readObject();
										bytesRead = (Integer)o;
										o = inStream.readObject();
										buffer = (byte[])o;
										fos.write(buffer, 0, bytesRead);
									} while(bytesRead == 1024);
									System.out.print("O cliente "+user+" transferiu o ficheiro ");
									System.out.println(nomeFicheiro+" para o servidor!");

									FileInputStream replaceString = new FileInputStream("C://Server/regFicheiros.txt");
									BufferedReader brd = new BufferedReader(new InputStreamReader(replaceString));
									String linha = null;
									int existe=0;
									while((linha = brd.readLine()) != null){
										if(linha.equals(nomeFicheiro+":"+user)){
											existe++;
										}

									}
									if(existe==0){
										PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:/Server/regFicheiros.txt", true)));
										out.println(nomeFicheiro+":"+user);
										out.close();
										fos.close();
									} brd.close();
								}else{
									outStream.writeObject(false);
									System.out.print("\nO cliente "+user+" tentou tansferir o ficheiro ");
									System.out.print(nomeFicheiro+" \nmas a versao que esta' no servidor e' mais recente");
									System.out.println(" e o arquivo não foi substituído!");
								}
								inStream.close();
								outStream.close();	
								break;
							case 2:
								String ficheiro = (String)inStream.readObject();
								String user2 = (String)inStream.readObject();
								File regPartilha = new File("C://Server/"+user2+"/regPartilha.txt");
								boolean dirFlag2 = false;
								if(!regPartilha.exists()){
									try{
										dirFlag2 = regPartilha.createNewFile();
									}catch(SecurityException Se){
										System.out.println("Erro ao criar regPartilha do utilizador "+user2+" : "+Se);
									}
									if(dirFlag2)
										System.out.println("Registo de partilha do utilizador "+user2+"  criado com sucesso!");
									else
										System.out.println("Registo de partilha do utilizador "+user2+" não foi criado!");
								}
								FileInputStream replaceString2 = new FileInputStream("C://Server/"+user2+"/regPartilha.txt");
								BufferedReader brd2 = new BufferedReader(new InputStreamReader(replaceString2));
								int jaPartilhado=0;
								String linha = null;
								while((linha = brd2.readLine()) != null){
									if(linha.equals(ficheiro+":"+user)){
										jaPartilhado++;
									}

								}
								if(jaPartilhado==0){
									File dir = new File("C://Server/"+user2);
									File[] fList = dir.listFiles();
									int nomesIguais=0;
									for(int i = 0; i<fList.length;i++){
										if(fList[i].getName().equals(ficheiro))
											nomesIguais++;
									}

									PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C://Server/"+user2+"/regPartilha.txt", true)));
									if(nomesIguais>0){

										File direct = new File("C://Server/"+user);
										File[] fLista = direct.listFiles();
										int temFicheiro=0;
										for(int i = 0; i<fLista.length;i++){
											if(fLista[i].getName().equals(ficheiro))
												temFicheiro++;
										}
										if(temFicheiro>0){
											FileInputStream replaceString = new FileInputStream("C://Server/"+user+"/regPartilha.txt");
											BufferedReader brd = new BufferedReader(new InputStreamReader(replaceString));
											String linha2 = null;
											String donoFicheiro = null;
											int autor=0;
											while((linha2 = brd.readLine()) != null){
												String splitted2[]= linha2.split(":");
												if(splitted2[0].equals(ficheiro)){
													autor++;
													donoFicheiro = splitted2[1];
												}

											}
											if(autor>0){
												out.println("p_"+ficheiro+":"+donoFicheiro);
											}else{
												out.println("p_"+ficheiro+":"+user);
											} brd.close();
										}
									}else{
										FileInputStream replaceString = new FileInputStream("C://Server/"+user2+"/regPartilha.txt");
										BufferedReader brd = new BufferedReader(new InputStreamReader(replaceString));
										String linha2 = null;
										String donoFicheiro = null;
										int autor=0;
										while((linha2 = brd.readLine()) != null){
											String splitted2[]= linha2.split(":");
											if(splitted2[0].equals(ficheiro)){
												autor++;
												donoFicheiro = splitted2[1];
											}

										}
										if(autor>0){
											out.println(ficheiro+":"+donoFicheiro);
										}else{
											out.println(ficheiro+":"+user);
										} brd.close();
									}
									FileInputStream regPartilhas = new FileInputStream("C://Server/regPartilhas.txt");
									BufferedReader linhasregPartilhas = new BufferedReader(new InputStreamReader(regPartilhas));
									PrintWriter outt = new PrintWriter(new BufferedWriter(new FileWriter("C://Server/regPartilhas.txt", true)));
									String linha2 = null;
									int existe=0;
									while((linha2=linhasregPartilhas.readLine())!=null){
										String splitted[] = linha2.split(":");
										if(splitted[0] == user)
											existe++;
									}
									if(existe==0){
										outt.println(user+":"+ficheiro+":"+user2);
									}else{
										while((linha2=linhasregPartilhas.readLine())!=null){
											String splitted[] = linha2.split(":");
											if(splitted[0] == user)
												outt.write(linha2+=":"+user2);
										}
									}
									outt.close();	
									out.close();
									linhasregPartilhas.close();
								}
								System.out.print("O cliente "+user+" partilhou o ficheiro ");
								System.out.println(ficheiro+" com o utilizador "+user2+"!");
								inStream.close();
								outStream.close();
								break;
							case 3:
								String ficheiroPactualizar = (String)inStream.readObject();
								File ficheiroAtransferir = new File(clienteDir+"/"+ficheiroPactualizar);
								int signal5=0;
								String userEstrangeiro="";
								String passwdEstrangeira="";
								if(ficheiroAtransferir.exists()){
									outStream.writeObject(ficheiroAtransferir.lastModified());
									fis = new FileInputStream(clienteDir+"/"+ficheiroPactualizar);								
									byte[] buffer3 = new byte[1024];
									Integer bytesRead2 = 0;
									while((bytesRead2=fis.read(buffer3))>0){
										outStream.writeObject(bytesRead2);
										outStream.writeObject(Arrays.copyOf(buffer3,buffer3.length));
									}
								}else{
									signal5++;
									FileInputStream partilhas = new FileInputStream("C://Server/"+user+"/regPartilha.txt");
									BufferedReader reader = new BufferedReader(new InputStreamReader(partilhas));
									String linhas = null;
									while((linhas = reader.readLine()) != null){
										String splitted[] = linhas.split(":");
										fis = new FileInputStream("C://Server/"+splitted[1]+"/"+splitted[0]);
										byte[] buffer3 = new byte[1024];
										Integer bytesRead2 = 0;
										while((bytesRead2=fis.read(buffer3))>0){
											outStream.writeObject(bytesRead2);
											outStream.writeObject(Arrays.copyOf(buffer3,buffer3.length));
										}
										userEstrangeiro=splitted[1];
									}	
									reader.close();
								}
								System.out.println("A transferir o ficheiro "+ficheiroPactualizar+" ao utilizador "+user+"...");
								if((boolean)inStream.readObject()==true){
									if (signal5==1){
										outStream.writeObject(true);
										FileInputStream partilhas = new FileInputStream("C://Server/usernames.txt");
										BufferedReader reader = new BufferedReader(new InputStreamReader(partilhas));
										String linhas = null;
										while((linhas = reader.readLine()) != null){

											String splitted[] = linhas.split(":");
											if(splitted[0].equals(userEstrangeiro)){	
												passwdEstrangeira=splitted[1];
											}

											outStream.writeObject(passwdEstrangeira);

										} reader.close();

									}else{ outStream.writeObject(false);}
									System.out.println("O servidor transferiu o fichero com sucesso!");

								}
								else {
									System.out.println("Versao do ficheiro do utilizador mais actualizada!");
									System.out.println("Ficheiro não transferido!");
								}
								fis.close();
								inStream.close();
								outStream.close();	
								break;
							case 4:
								String ficheiroAsincronizar = (String)inStream.readObject();
								File sincronizacao = new File("C://Server/"+user+"/Sincronizacao.txt");
								File ficheirof = new File("C://Server/"+user+"/"+ficheiroAsincronizar);
								boolean dirFlag3 = false;
								if(!sincronizacao.exists()){
									try{
										dirFlag3 = sincronizacao.createNewFile();
									}catch(SecurityException Se){
										System.out.println("Erro ao criar sincronizacoes do utilizador "+user+" : "+Se);
									}
									if(dirFlag3)
										System.out.println("Registo de sincronizacoes do utilizador "+user+"  criado com sucesso!");
									else
										System.out.println("Registo de sincronizacoes do utilizador "+user+" não foi criado!");
								}
								FileInputStream sincronizacoes = new FileInputStream(sincronizacao);
								BufferedReader linhasSincronizacao = new BufferedReader(new InputStreamReader(sincronizacoes));
								PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sincronizacao, true)));
								String linhass[] = new String[devolveLinhas(user)];
								String linhaAlinha = null;
								int counter =0;
								int Signal = 0;
								while((linhaAlinha=linhasSincronizacao.readLine())!=null){
									linhass[counter]=linhaAlinha;
									if (linhaAlinha.equals(ficheiroAsincronizar))
										Signal++;
									counter++;
								}
								if(Signal == 0){
									out.println(ficheiroAsincronizar);

								}
								out.close();
								long datafile = ficheirof.lastModified();

								while(true){
									outStream.writeObject(false);
									try {
										File ficheirof2 = new File("C://Server/"+user+"/"+ficheiroAsincronizar);
										long datafile2 = ficheirof2.lastModified();
										if((Boolean)inStream.readObject()==true){
											System.out.println("O Ficheiro "+ficheiroAsincronizar+" foi actualizado!");
											fos = new FileOutputStream(clienteDir+"/"+ficheiroAsincronizar);
											Integer bytesRead = 0;
											do{
												o = inStream.readObject();
												bytesRead = (Integer)o;
												o = inStream.readObject();
												buffer = (byte[])o;
												fos.write(buffer, 0, bytesRead);
											} while(bytesRead == 1024);

										}
										if(datafile2>datafile){
											outStream.writeObject(true);
											FileInputStream ficheir = new FileInputStream(ficheirof2); 
											byte[]buffer3 = new byte[1024];  
											Integer bytesRead2 = 0; 
											while ((bytesRead2 = ficheir.read(buffer3)) > 0) {  
												outStream.writeObject(bytesRead2);  
												outStream.writeObject(Arrays.copyOf(buffer3, buffer3.length));  
											} 
											datafile = datafile2;//actualizar variavel datafile
											ficheir.close();
										}else{
											outStream.writeObject(false);
										}

										outStream.writeObject(linhass.length);
										for(int i = 0; i<linhass.length;i++)
											outStream.writeObject(linhass[i]);
										Thread.sleep(15000);
									} catch (IOException | InterruptedException e) {
										try {
											Thread.sleep(15000);
										} catch (InterruptedException e1) {
											e1.printStackTrace();
										}
										e.printStackTrace();
									}
								}

							case 5:
								File dir = new File("C://Server/"+user);
								File[] fList = dir.listFiles();
								outStream.writeObject(fList.length);
								for(int i =0;i<fList.length;i++){
									outStream.writeObject(fList[i].getName());
									Date data = new Date(fList[i].lastModified());
									DateFormat formatador =  new SimpleDateFormat("dd/MM/yyyy hh:MM:ss");
									String dataFormatada = formatador.format(data);
									outStream.writeObject(dataFormatada);
								}

								File regPartilhas = new File("C:/Server/"+user+"/regPartilha.txt");
								if (regPartilhas.exists()){
									FileInputStream partilhas = new FileInputStream("C://Server/"+user+"/regPartilha.txt");
									BufferedReader reader = new BufferedReader(new InputStreamReader(partilhas));
									String linhas = null;
									@SuppressWarnings("unused")
									int contador=0;
									while((linhas = reader.readLine()) != null){
										outStream.writeObject(true);
										String splitted[] = linhas.split(":");
										outStream.writeObject(splitted[0]);
										outStream.writeObject(splitted[1]);
										if(splitted[0].substring(0).equals("_")){
											File fil = new File("C://Server/"+splitted[1]+"/"+splitted[0].substring(2));
											Date data = new Date(fil.lastModified());
											DateFormat formatador =  new SimpleDateFormat("dd/MM/yyyy hh:MM:ss");
											String dataFormatada = formatador.format(data);
											outStream.writeObject(dataFormatada);
											File partilhasss = new File("C://Server/regPartilhas.txt");
											FileInputStream partilhass = new FileInputStream("C://Server/regPartilhas.txt");
											BufferedReader reader2 = new BufferedReader(new InputStreamReader(partilhass));
											String linha2 = null;
											FileReader fr = new FileReader(partilhasss);
											LineNumberReader lnr = new LineNumberReader(fr);

											int linenumber = 0;

											while (lnr.readLine() != null){
												linenumber++;
											}
											lnr.close();
											String[] apartilharCom = new String[linenumber];
											while((linha2 = reader2.readLine()) != null){
												int conta = 0;

												String splitted2[]= linha2.split(":");
												if(splitted2[1].equals(splitted[0])){
													apartilharCom[conta]=splitted2[2];
													conta++;
												}

											}
											outStream.writeObject(apartilharCom.length);
											for(int x = 0; x< apartilharCom.length;x++)
												outStream.writeObject(apartilharCom[x]);
											reader2.close();
										}else{
											File fil = new File("C://Server/"+splitted[1]+"/"+splitted[0]);
											Date data = new Date(fil.lastModified());
											DateFormat formatador =  new SimpleDateFormat("dd/MM/yyyy hh:MM:ss");
											String dataFormatada = formatador.format(data);
											outStream.writeObject(dataFormatada);
											File partilhasss = new File("C://Server/regPartilhas.txt");
											FileInputStream partilhass = new FileInputStream("C://Server/regPartilhas.txt");
											BufferedReader reader2 = new BufferedReader(new InputStreamReader(partilhass));
											String linha2 = null;
											FileReader fr = new FileReader(partilhasss);
											LineNumberReader lnr = new LineNumberReader(fr);

											int linenumber = 0;

											while (lnr.readLine() != null){
												linenumber++;
											}
											String[] apartilharCom = new String[linenumber];
											while((linha2 = reader2.readLine()) != null){
												int conta = 0;

												String splitted2[]= linha2.split(":");
												if(splitted2[1].equals(splitted[0])){
													apartilharCom[conta]=splitted2[2];
													conta++;
												}

											}
											outStream.writeObject(apartilharCom.length);
											for(int x = 0; x< apartilharCom.length;x++)
												outStream.writeObject(apartilharCom[x]);
											reader2.close();
											lnr.close();
										}
										contador++;
									}	reader.close();
								}
								outStream.writeObject(false);
								System.out.println("O cliente "+user+" pediu uma listagem dos seus arquivos!");
								break;
							default:
								inStream.close();
								outStream.close();
								socket.close();
								break;
							}
						} 
					}
					if (signal==0){
						outStream.writeObject(new Boolean(false));
						fis2.close();
						br.close();
						try {
							CifUserNames.main(null);
						} catch (Exception e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
					socket.close();br.close();
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				outStream.close();
				inStream.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		public int devolveLinhas(String user) throws IOException{
			File sincronizacao = new File("C://Server/"+user+"/Sincronizacao.txt");
			if(!sincronizacao.exists()){
				return 0 ;
			}
			FileInputStream sincronizacoes=new FileInputStream(sincronizacao);

			BufferedReader linhasSincronizacao = new BufferedReader(new InputStreamReader(sincronizacoes));		
			int counter=0;
			while(linhasSincronizacao.readLine()!=null){
				counter++;
			}
			linhasSincronizacao.close();
			return counter;
		}
	}
	public void cifrarFicheiro(String password, String ficheiroEntrada, String ficheiroSaida, String username)
	{
		try {
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			Key encryptionKey = kg.generateKey();

			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);

			FileInputStream fis;
			FileOutputStream fos;
			CipherInputStream cis;

			fis = new FileInputStream(ficheiroEntrada);
			fos = new FileOutputStream(ficheiroSaida);

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

			byte[] keyEncoded = encryptionKey.getEncoded();


			FileOutputStream oos = new FileOutputStream("C:/Server/AndreRocha/chave.txt");
			for (int x = 0;x<keyEncoded.length; x++)
				oos.write(keyEncoded[x]);
			oos.close();



			String passwordi = password;
			byte[] key = passwordi.getBytes("UTF-8");

			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16); // use only first 128 bit

			SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

			Cipher cipher2 = Cipher.getInstance("AES");
			cipher2.init(Cipher.ENCRYPT_MODE, skeySpec);

			FileInputStream fis2;
			FileOutputStream fos2;
			CipherInputStream cis2;
			fis2 = new FileInputStream("C:/Server/AndreRocha/chave.txt");
			String[] chaveFicheiro = ficheiroEntrada.split("\\.");
			fos2 = new FileOutputStream(chaveFicheiro[(chaveFicheiro.length-2)]+"_chave.txt");
			cis2 = new CipherInputStream(fis2, cipher2);
			byte[] b2 = new byte[16];  
			int i2 = cis2.read(b2);
			while (i2 != -1) {
				fos2.write(b2, 0, i2);
				i2 = cis2.read(b2);
			}
			cis2.close();
			fos2.close();
			fis2.close();	
			File file = new File(username+"/chave.txt");
			if(file.delete()){
				System.out.println(file.getName() + " foi apagado!");
			}else{
				System.out.println("O ficheiro não foi apagado.");
			}


		}
		catch (Exception e) {
			System.out.println("Erro ao cifrar os dados do ficheiro");
		}
	}


}
