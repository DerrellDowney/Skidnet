import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


import java.io.*;

public class Client{

    private static String serverName = "localhost";
    private static int serverPort = 59089;
    static Socket clientSocket;

    // the number of milliseconds to wait for SYNACK before retransmitting SYN
    static final int SYN_TIMEOUT = 100;
    // the max number of SYN retransmissions in srt client connect() 
    static final int SYN_MAX_RETRY = 5;
    // the number of milliseconds to wait for FINACK before retransmitting FIN
    static final int FIN_TIMEOUT = 100;
    // the max number of FIN retransmissions in srt client disconnect() 
    static final int FIN_MAX_RETRY = 5;
    // the max amount of segments that can be in the send buffer
    static final int GBN_WINDOW = 10;
    static final int SENDBUF_POLLION_INTERVAL = 10000;
    static final int DATA_TIMEOUT = 1;

    static int socksr;

    // declaring tcb table
    static TCBClient[] table;
    // max number of clients
    static int maxClientConnections = 10;

    // declaring seghandler thread
    Thread segHandler;

    public static ObjectInputStream input;
    public static ObjectOutputStream output;
    
    // next sequence number to be associated with a new DATA segment
    private int next_seqNum = 0;
    // represents the send buffer
    private LinkedList<SendBufferNode> sendBuffer;
    // index of the first unsent segmen
    private int sendBufunSent;

    private Thread sendHandler;

    private static int send_but_not_acked = 0;

    public static void main(String[] args) throws Exception{
        Client client = new Client();
        // call the startOverlay method , throw error if it returns -1
        client.startOverlay();

        // call the initSRTClient method , throw error if it returns -1
        client.initSRTClient();
        // create a srt client sock on port 87 using the createSockSRTClient (87) and assign to socksr , throw error if it returns -1
        socksr = client.createSockSRTClient(87);
        if (socksr == -1) {
            throw new Error();
        }
        // connect to srt server at port 88 using connectSRTClient ( socksr ,88) , throw error if it returns -1
        
        client.connectSRTClient(socksr, 88);
        // for now , just use a Thread . sleep (10000) here
        Thread.sleep (1000);
        // sendSRTClient only gets called if the Client is connected
        
        
        
        if(table[socksr].stateClient == 3) {
            System.out.println("Calling sendSRTClient");
            client.sendSRTClient();
            client.sendHandler.join();
            // client.segHandler.join();
        }
       
        // disconnect using disconnSRTClient ( socksr ) , throw error if it returns -1
        client.disconnSRTClient(socksr);

        Thread.sleep(100);
        // close using closeSRTClient ( socksr ) , throw error if it returns -1
        client.closeSRTClient(socksr);
        // finally , call stopOverlay () , throw error if it returns -1
        client.stopOverlay();
    }

    private void startOverlay() throws Exception{
        // finds ip address of client and estblish connection with the server
        
        InetAddress clientIP = InetAddress.getByName(serverName);
        System.out.println("Finding client IP");

        clientSocket = new Socket(clientIP, serverPort);
        System.out.println("Creating client socket");
    }

    private synchronized void initSRTClient()throws Exception{
        try{
            // initialize TCB table marking all entries as null
            table = new TCBClient[maxClientConnections];
            for (int i = 0; i < table.length; i++) {
                table[i] = null;
            }

            // for (TCBClient client : Client.table) {
            //     client = null;
            // }
            

            //starts segHandler thread to handle incoming segments
            segHandler = new ListenThread();
            segHandler.start();

        }
        catch(Exception e){
            e.printStackTrace();
             throw new Error();
        } 
    }

    private int createSockSRTClient(int clientPort){
        int clientID = -1;
        for (int i = 0; i < table.length; i++) {
            if (table[i] == null) {
                System.out.println("Creating new TCB client entry");
                table[i] = new TCBClient();
                table[i].nodeIDServer = 0;
                table[i].portNumServer = 0;
                table[i].nodeIDClient = 0;
                table[i].portNumClient = clientPort;
                // state set to CLOSED
                table[i].stateClient = 1;

                sendBuffer = new LinkedList<SendBufferNode>();

                clientID = i;
                break;
            }
        }
        return clientID;
    }

    private synchronized void connectSRTClient(int socksr, int serverPort) throws IOException {
        table[socksr].portNumServer = serverPort;
        
        // Setting the client to SYNSENT state
        table[socksr].stateClient = 2;
		       
        System.out.println("Got to connect SRT Client");
        //segHandler.interrupt();
        
        

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            int numSYNSent = 0;
            
            @Override
            public void run() {

                try {
                    if(table[socksr].stateClient != 3){
                        System.out.println(table[socksr].stateClient);
                        output = new ObjectOutputStream(clientSocket.getOutputStream());
                        
                        // Creating segment
                        Segment seg = new Segment(0, next_seqNum);
                        next_seqNum+=2;
                        //System.out.println("Created segment");

                        // Sending the segment 
                        output.writeObject(seg);
                        System.out.println("Sent SYN segment");

                        // Closing the output stream

                        // Incrementing numSYNSent
                        numSYNSent++;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // If the number of SYNs sent is greater than the max allowed
                if (numSYNSent >= SYN_MAX_RETRY) {
                    // Set state of client to closed
                    table[socksr].stateClient = 1;
                    // cancel the timer
                    timer.cancel();
                }
                timer.cancel();
            }
        };


        

        timer.schedule(task, 0, SYN_TIMEOUT);
    }

    private synchronized void sendSRTClient() {
        String[] myData = {"apple", "banana", "cat", "dog", "elephant", "find", "grape", "hair", "ice", "jacket", "kite", "llama", "money", "nails", "open", "penny", "quest", "rest" ,"space", "tale", "ufo", "vesicle", "whale", "x-word", "yellow", "zebra"};
        
        for (int i = 0; i < myData.length; i++) {
            Segment newSeg = new Segment(4, next_seqNum);
            next_seqNum+=2;
            newSeg.data = myData[i];

            
            SendBufferNode newNode = new SendBufferNode(newSeg);

            sendBuffer.add(newNode);
        }
        
        sendHandler = new SendThread();

        sendHandler.start();

       
        //segHandler.start();

        // Need a time to constantly check the head of sendBuffer to check if theres a timeout
        // If there is then we restart the sendHandler?
        // Either way start sending from the send buffer from the start
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            
            @Override
            public void run() { 
                // creating variable to represent current time
                LocalTime currentTime = LocalTime.now();
                sendHandler.interrupt();
                sendHandler = null;
                
                
                // comparing the current time subtracted by the timeout interval to time of head node
                // System.out.println(currentTime.minusSeconds(DATA_TIMEOUT).compareTo(sendBuffer.getFirst().timeSent));
                if (currentTime.minusSeconds(DATA_TIMEOUT).compareTo(sendBuffer.getFirst().timeSent) > 0) {
                    // System.out.println("got here 1");
                    send_but_not_acked = 0;
                    sendHandler = new SendThread();
                    sendHandler.start();
                    
                }

                if (sendBuffer.size() == 0) {
                    timer.cancel();
                }
            }
        };

        

        timer.schedule(task, SENDBUF_POLLION_INTERVAL, SENDBUF_POLLION_INTERVAL); 
    }
        

    private synchronized void disconnSRTClient(int socksr) {
        
        // Setting the client to SYNSENT state
        table[socksr].stateClient = 4;
		       
        System.out.println("Got to connect SRT Client");

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            int numFINSent = 0;
            
            @Override
            public void run() {
                try {
                    if(table[socksr].stateClient != 1){
                        System.out.println(table[socksr].stateClient);
                        
                        // Creating segment
                        Segment seg = new Segment(2, next_seqNum);
                        next_seqNum+=2;
                        //System.out.println("Created segment");

                        // Sending the segment 
                        output.writeObject(seg);
                        System.out.println("Sent FIN segment");

                        // Closing the output stream

                        // Incrementing numSYNSent
                        numFINSent++;
                    }

                    
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // If the number of SYNs sent is greater than the max allowed
                if (numFINSent >= FIN_MAX_RETRY) {
                    // Set state of client to closed
                    table[socksr].stateClient = 1;
                    
                    // cancel the timer
                    timer.cancel();

                    throw new Error("Encountered error with FINACK");
                }

                System.out.println("Received FINACK, closing client");

                table[socksr].stateClient = 1;
                timer.cancel();
            }  
        };

        timer.schedule(task, 0, FIN_TIMEOUT);
    }

    private void closeSRTClient(int socksr) {
        // If the client is in the right state (closed) remove entry from the tcb table
        if (table[socksr].stateClient == 1) {
            table[socksr] = null;
            System.out.println("Client successfully closed");
        }
        // If not then throw an error
        else {
            throw new Error("Client was not in the right state to close");
        }
    }

    private void stopOverlay() throws IOException{
        clientSocket.close();
    }

    public class ListenThread extends Thread {
        boolean running = true;
        @Override
        public void run() {
            while (running) {
                try {
                    input = new ObjectInputStream(clientSocket.getInputStream());

                    // Getting the segment from the Object reader
                    Segment recvSegment = (Segment) input.readObject();
                    
                    // If client receives a SYNACK
                    if (recvSegment.type == 1) {
                        System.out.println("Received SYNACK. Client is CONNECTED");

                        // Changing the state of the client to CONNECTED
                        table[socksr].stateClient = 3;
                        running = false;
                        
                    }

                    // If client recieves a FINACK
                    if(recvSegment.type == 3) {
                        System.out.println("Received FINACK. Client is CLOSED");

                        // Changing the state of the client to CONNECTED
                        table[socksr].stateClient = 1;
                        running = false;
                    }

                    // If client receives a DATAACK
                    if(recvSegment.type == 5) {
                        System.out.println("Received DATAACK. Removing items from send buffer");

                        // Removing all segments with a lower sequence number than the received DATAACK
                        for(int i = 0; i < sendBuffer.size(); i++) {
                            if (sendBuffer.get(i).segment.seq_num < recvSegment.seq_num) {
                                sendBuffer.remove(i);
                                send_but_not_acked--;
                                sendBufunSent++;
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void stopListenint(){
            this.running = false;
        }
    }

    public class SendThread extends Thread {
        boolean running = true;
        @Override
        public void run() {
            while (running) {
                try {
                    
                    // send segments while send_but_not_acked is less than WINDOW size and the sendBuffer isnt empty
                    // while(( sendBuffer.size() > 0 )) {
                        // System.out.println(send_but_not_acked);

                        while (( send_but_not_acked < GBN_WINDOW )) {

                            output.writeObject(sendBuffer.get(send_but_not_acked).segment);
                            sendBuffer.get(send_but_not_acked).timeSent = LocalTime.now();
                            System.out.println("Sending segment with data: " + sendBuffer.get(send_but_not_acked).segment.data);
                            send_but_not_acked++;    
                            Thread.sleep(50);
                        }
                        
                    // }
                    // interrupt();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void stopListenint(){
            this.running = false;
        }
    }
}





