package com.tekijat.summauspalvelu;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Hoitaa keskustelun avaamisen WorkDistributorin kanssa.
 * @author Antti Peltola
 *
 */

public class X {

	private static final int NUMBER_OF_ATTEMPTS = 5;
	private static final int MY_PORT = 3127;
	
	/**
	 * Välittää udp paketin WorkDistrinbutorille, avaa ServerSokectin ja yhteyden avauduttua välittää
	 * Socketin XHandlerille. Jos yhdeyden ottoa ei tule viiden sekuninnin kuluessa lähetetään uusi paketti.
	 * (Yrityksiä 5 kpl) 
	 * @param args args[0] == WorkDistributorin IPv4-osoite(Oletus: localhost) args[1] == WorkDistributorin portti (Oletus: 3126) 
	 * 
	 */
	public static void main(String... args){
		String targetAddress = "localhost";
		int targetPort = 3126;
		//User given address and port for WorkDistibutor. 
		if(args.length >= 1 && args[0] != null) targetAddress = args[0];
		if(args.length >= 2 && args[1] != null) targetPort = Integer.parseInt(args[1]);
		
		
		try{
		InetAddress target = InetAddress.getByName(targetAddress);
		
		byte[] myPortBytes = (String.valueOf(MY_PORT)).getBytes();
		DatagramPacket connectionInitPacket = new DatagramPacket(myPortBytes, myPortBytes.length, target, targetPort);
		DatagramSocket datagSocket = new DatagramSocket();
		
		ServerSocket ss = new ServerSocket(MY_PORT);
		ss.setSoTimeout(5000);//5 sec timeout for each try
		
		System.out.println("Attempting to connect WorkDistributor at " + targetAddress);
		for(int i = 0; i<NUMBER_OF_ATTEMPTS;i++){
			try{
				datagSocket.send(connectionInitPacket);
			}catch (Exception e) {System.out.println("Failed to send the initPacket.");}
			try{
				Socket cs = ss.accept();
				cs.setSoTimeout(60000);
				System.out.println("Connection from" + cs.getInetAddress() + " port " + cs.getPort());
				new XHandler(cs).start();
				break;
			}	catch (Exception e){
				if(NUMBER_OF_ATTEMPTS-(i+1) == 0){
					System.out.println("No attempts left. Exiting...");
				}else
				System.out.println("Attempts left: " + (NUMBER_OF_ATTEMPTS-(i+1)) );
			}
		}//for
		
		ss.close();
		datagSocket.close();
		
		}
		catch(IOException e){
			System.out.println("Program failed to initialize. Check your inputs, please.");
			e.printStackTrace();
		}
	}
}
/**
 * Ylläpittää yhteyttä WorkDistributorin kanssa tehtävän määrittelyn mukaisilla toimenpiteillä.
 * @author Silkka
 *
 */
class XHandler extends Thread{
	
	private static final int SUM_HANDLER_WAIT_TIME = 200;
	private static final int STARTING_PORT = 3128;
	private final Socket client;
	private InputStream iS;
	private OutputStream oS;
	private ObjectOutputStream oOut;
	private ObjectInputStream oIn;
	private int[] portNumbers = new int[10];
	
	/**
	 * Alustaa olion.
	 * @param s s!= null
	 */
	public XHandler(Socket s){
		client = s;
		//The array of available ports for the sum handlers
		for(int i = 0;i<portNumbers.length;i++){
			portNumbers[i] = STARTING_PORT + (i) ;
		}
		
	}
	
	public void run() {
		try{
			
			System.out.println("Starting communication with WorkDistributor");
			iS = client.getInputStream();
			oS = client.getOutputStream();
			oOut = new ObjectOutputStream(oS);
			oIn = new ObjectInputStream(iS);
			//Giving WorkDistributor 5 sec to send the number of sum handlers. If no --> Send -1 and terminate connection.
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						System.out.println("No integer received from WorkDistributor. Exiting...");
						oOut.writeInt(-1);
						oOut.flush();
						closeConnections();
						System.exit(1);
					} catch (IOException e) {
						System.exit(1);
					}
				}
			}, 5000);
			
			int numberOFSumHandlers = oIn.readInt();
			
			timer.cancel();//If WorkDistributor responded in time then there is no need to terminate --> cancel.
			
			System.out.println("Number of requested sum handlers: " + numberOFSumHandlers);
			
			ArrayList<SumServer> sumHandlers = new ArrayList<>();
			ArrayList<SumData> sums = new ArrayList<SumData>();
			
			for(int i = 0; i < numberOFSumHandlers;i++){
				sums.add(new SumData());
				sumHandlers.add(new SumServer(portNumbers[i],sums.get(i)));
				sumHandlers.get(i).start();

			}
			//Waiting for sum handlers to get ready.
			Thread.sleep(1000);
			//Send sum handler ports to WorkDistribution
			for(int i = 0; i<numberOFSumHandlers;i++){
				oOut.writeInt(portNumbers[i]);
				oOut.flush();
			}
			
			while(true){
				try{
					
					int message = oIn.readInt();
					System.out.println("Received message: " + message);
					if(message == 1){
						Thread.sleep(SUM_HANDLER_WAIT_TIME);
						int totalSum = 0;
						synchronized (sums) {
							for(int i = 0; i< sums.size();i++){
								totalSum +=sums.get(i).getSum();
							}
						}
						
						
						
						oOut.writeInt(totalSum);
						oOut.flush();
						System.out.println("Response: " + totalSum);
						
					}
					else if(message == 2){
						Thread.sleep(SUM_HANDLER_WAIT_TIME);
						int max= sums.get(0).getSum();
						int indexMax = 0;
						for(int i = 1;i<sums.size();i++){
							int current = sums.get(i).getSum();
							if(max<=current){
								max = current;
								indexMax = i;
							}
						}
						oOut.writeInt(indexMax+1);
						oOut.flush();
						
						System.out.println("Response: " + indexMax);
					}
					else if(message == 3){
						Thread.sleep(SUM_HANDLER_WAIT_TIME);
						int totalSum = 0;
						synchronized (sums) {
							for(int i = 0; i< sums.size();i++){
								totalSum +=sums.get(i).getNumberOfSums();
							}
						}
						oOut.writeInt(totalSum);
						oOut.flush();
						System.out.println("Response: " + totalSum);
					}
					
					else if(message == 0){
						System.out.println("Exiting...");
						break;
					}
					
					else {
						oOut.writeInt(-1);
						oOut.flush();
					}
					
					
				}catch(EOFException e){
					System.out.println("End of file reached. Exiting...");
					break;
				}
				
			}
			//Received 0 or reached end of file ---> terminate connection
			closeConnections();


		}
		catch(SocketTimeoutException e){
			//No connection from Workdistributor in 60 sec
			System.out.println("Socket timeout (60 sec)");
		}
		catch (Exception e) {
			throw new Error(e.toString());
		}
		
		
	}
	/**
	 * Sulkee viestivirrat ja socketin.
	 * @throws IOException Virtojen ja soketin sulkemiseen liittyvä poikkeus. (Socket.close(), InputStream.close(), jne.)
	 */
	private void closeConnections() throws IOException{
		oOut.close();
		oIn.close();
		oS.close();
		iS.close();
		client.close();
	}
	
	
}


