import java.io.Serializable;

public class Segment implements Serializable{
    // public int src_port ; // currently not used
    // public int dest_port ; // currently not used
    public int seq_num ; // currently not used
    // public int length ; // currently not used

    // public int rcv_win ; // currently not used
    // public int checksum ; // currently not used
    public String data; 
    
    // SYN 0, SYNACK 1, FIN 2, FINACK 3, DATA 4, DATAACK 5
    public int type ; // segment type

    public Segment(int type, int seq_num) {
        this.type = type;
        this.seq_num = seq_num;
    }
    
    
}
