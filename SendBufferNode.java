import java.io.Serializable;
import java.time.LocalTime;


public class SendBufferNode implements Serializable{
        public LocalTime timeSent;
        public Segment segment;

        public SendBufferNode(Segment segment){
                this.segment = segment;
        }
}
