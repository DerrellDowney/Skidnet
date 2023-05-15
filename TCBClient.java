
public class TCBClient {
    int nodeIDServer ; // node ID of server , similar as IP address
    int portNumServer ; // port number of server
    int nodeIDClient ; // node ID of client , similar as IP address
    int portNumClient ; // port number of client
    
    // CLOSED 1, SYNSENT 2, CONNECTED 3, FINWAIT 4
    int stateClient ; // state of client
}
