import java.io.Serializable;

//Done!
public class Packet implements Serializable
{

    private int hopCount;
    private String message;
    private String specialMessage;  //ex: "SHOW_ROUTE" request
    private IPAddress destinationIP;
    private IPAddress sourceIP;

    public Packet(String message, String specialMessage, IPAddress sourceIP, IPAddress destinationIP)
    {
        this.message = message;
        this.specialMessage = specialMessage;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.hopCount = -1;
    }

    public IPAddress getSourceIP()
    {
        return sourceIP;
    }

    public void setSourceIP(IPAddress sourceIP)
    {
        this.sourceIP = sourceIP;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getSpecialMessage()
    {
        return specialMessage;
    }

    public void setSpecialMessage(String specialMessage)
    {
        this.specialMessage = specialMessage;
    }

    public IPAddress getDestinationIP()
    {
        return destinationIP;
    }

    public void setDestinationIP(IPAddress destinationIP)
    {
        this.destinationIP = destinationIP;
    }

    public int getHopCount()
    {
        return hopCount;
    }

    public void setHopCount(int hopCount)
    {
        this.hopCount = hopCount;
    }

}
