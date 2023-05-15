public class TCBServer {
    int nodeIDServer ; // node ID of server , similar as IP address
    int portNumServer ; // port number of server
    int nodeIDClient ; // node ID of client , similar as IP address
    int portNumClient ; // port number of client

    // CLOSED 1, LISTENING 2, CONNECTED 3, CLOSEWAIT 4
    int stateServer ; // state of server

    
}
