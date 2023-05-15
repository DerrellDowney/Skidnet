
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;


public class Server {

    private static ServerSocket serverSocket;

    private static Socket socket;

    static int socksr;
    
    // declaring tcb table
    static TCBServer[] table;
    // max number of servers
    static int maxServerConnections = 10;

    static final int CLOSE_WAIT_TIMEOUT = 1000;

    static Thread segHandler;

    public static ObjectInputStream input;
    public static ObjectOutputStream output;

    private static int expect_seqNum;
    private static String[] recvBuff = new String[100];
    private static int usedBufLen = 0;

    public static void main(String[] args) throws Exception{
        Server server = new Server();
        server.startOverlay();

        server.initSRTServer();

        int socksr = server.createSockSRTServer(88);
        if (socksr == -1) {
            throw new Error();
        }

        server.acceptSRTServer(socksr);

        Thread.sleep(1000);

        // Error: Not getting to this if statement
        if(table[socksr].stateServer == 3) {
            // System.out.print("never prints");
            segHandler.join();
            server.rcvSRTClient();
            
        }
        server.closeSRTServer(socksr);

        Thread.sleep(1003);

        server.stopOverlay();
    }

    public void startOverlay() throws Exception{


        serverSocket = new ServerSocket(59089);
        System.out.println("Trying to accept the connection");

        socket = serverSocket.accept();
        System.out.println("Accepted the connection");
    }

    public void initSRTServer() throws Exception{
        
        // initialize TCB table marking all entries as null
        table = new TCBServer[maxServerConnections];
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
    }

    public int createSockSRTServer(int serverPort) {
        int serverID = -1;
        for (int i = 0; i < table.length; i++) {
            if (table[i] == null) {
                table[i] = new TCBServer();
                table[i].nodeIDServer = 0;
                table[i].portNumServer = 0;
                table[i].nodeIDClient = 0;
                table[i].portNumClient = serverPort;
                // state set to CLOSED
                table[i].stateServer = 1;

                serverID = i;
                break;
            }
        }
        return serverID;
    }

    public void acceptSRTServer(int socksr) {
        // Changes start of server to listening
        table[socksr].stateServer = 2;

        //starts segHandler thread to handle incoming segments
        segHandler = new ListenThread();
        segHandler.start();
    }

    public String rcvSRTClient() {
        String result = "";
            if (usedBufLen > 0) {
                result = recvBuff[usedBufLen];
                recvBuff[usedBufLen] = null;
                usedBufLen--;
            }

        return result; 
    }

    public void closeSRTServer(int socksr) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (table[socksr].stateServer == 4) {
                    table[socksr].stateServer = 1;
                }
                else {
                    throw new Error("item in TCB server table was not in the right state.\nExpected state 4, but was in state " + table[socksr].stateServer);
                }
            }
        };

        Timer timer = new Timer();

        timer.schedule(task, CLOSE_WAIT_TIMEOUT);
    }

    public void stopOverlay() throws IOException, InterruptedException {
        System.out.println(table[socksr].stateServer);
        
        if (table[socksr].stateServer == 1) {
            table[socksr] = null;
            serverSocket.close();
            socket.close();
            usedBufLen = 0;
            recvBuff = null;
            System.out.println("Server successfully closed");
        }
        // If not then throw an error
        else {
            throw new Error("Server was not in the right state to close");
        }
    }

    public class ListenThread extends Thread {
        boolean running = true;
        @Override
        public void run() {
            try {
                input = new ObjectInputStream(socket.getInputStream());
                output = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            while (running) {
                try {
                    
                    // Getting the segment from the Object reader
                    Segment recvSegment = (Segment) input.readObject();
                    System.out.println("Segment type: " + recvSegment.type);
                
                    if (recvSegment.type == 0) {
                        System.out.println("Received SYN. Sending SYNACK");

                        // Sending SYNACK to client
                        Segment synackSeg = new Segment(1, recvSegment.seq_num+1);
                        output.writeObject(synackSeg);

                        // Changes state of server to connected
                        table[socksr].stateServer = 3;
                        expect_seqNum = recvSegment.seq_num;
                        
                    }

                    if(recvSegment.type == 2) {
                        System.out.println("Received FIN. Sending FINACK");

                        // Sending SYNACK to client
                        Segment finackSeg = new Segment(3, recvSegment.seq_num+1);
                        
                        output.writeObject(finackSeg);

                        // Changing state of server to closewait
                        table[socksr].stateServer = 4;
                    }

                    if(recvSegment.type == 4 ) {
                        System.out.println("Received Data segment");
                        if (recvSegment.seq_num == expect_seqNum){
                            recvSegment.data = recvBuff[usedBufLen];
                            usedBufLen++;
                            expect_seqNum++;
                        }
                        
                        Segment dataACK = new Segment(5, expect_seqNum);
                        output.writeObject(dataACK);
                    }
                }
                catch (IOException | ClassNotFoundException e) {

                }   
            }
        }
    } 
}

