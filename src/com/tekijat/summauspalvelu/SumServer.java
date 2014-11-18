package com.tekijat.summauspalvelu;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
/**
 * Hoitaa yhteyden avaamisen sumHandlerille.
 * @author Antti Peltola
 *
 */
public class SumServer extends Thread{
	private int portti;
	public SumData sumData;
	private ServerSocket ss;
	/**
	 * Alustaa luokan.
	 * @param portti portti != null
	 * @param sumData sumData != null
	 */
	public SumServer(int portti, SumData sumData){
		this.portti = portti;
		this.sumData = sumData;
	}
	/**
	 * Avaa serverSocketin konstruktorissa annettuun porttiin ja saadessaan yhteyden, luo uuden 
	 * SumHandlerin ja v‰litt‰‰ sille socketin.
	 */
	public void run(){
		try {
			ss = new ServerSocket(portti);
			ss.setSoTimeout(10000);
			Socket cs = ss.accept();
			System.out.println("Connection from" + cs.getInetAddress() + "port" + cs.getPort());
			new SumHandler(cs,sumData).start();
		}catch(SocketTimeoutException e){
			System.out.println("Sum handler timeout...");
			throw new Error(e.toString());
		} catch (IOException e) {
			throw new Error(e.toString());
		}
	}
}
/**
 * V‰litt‰‰ soketilta saamansa alkiot annettulle sumData-oliolle ja p‰‰tt‰‰ yhteyden saatuaan nollan tai end of linen.
 * @author Silkka
 */
class SumHandler extends Thread{
	private final Socket client;
	private SumData sumData;
	/**
	 * Alustaa luokan
	 * @param s s!= null
	 * @param sumData sumData != null
	 */
	public SumHandler(Socket s,SumData sumData){
		client = s;
		this.sumData = sumData;
		
	}
	public void run(){
		try{
			System.out.println("Spawning thread ...");
			InputStream iS = client.getInputStream();
			ObjectInputStream oIn = new ObjectInputStream(iS);
			
			while(true){
				try{
					int received = oIn.readInt();
					if(received == 0){
						break;
					}
					sumData.addSum(received);
				}catch (Exception e){
					break;
				}
				
				
			}
			oIn.close();
			iS.close();
			client.close();
			
		}catch (Exception e) {
			throw new Error(e.toString());
		}
	}

}