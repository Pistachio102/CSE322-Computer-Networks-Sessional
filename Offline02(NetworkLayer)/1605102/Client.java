import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

//Work needed
public class Client
{
    public static void main(String[] args) throws InterruptedException
    {
        NetworkUtility networkUtility = new NetworkUtility("0.0.0.0", 4444);
        System.out.println("Connected to server");


//        Thread.sleep(10000);
        /**
         * Tasks
         */
        
        /*
        1. Receive EndDevice configuration from server
        2. Receive active client list from server
        3. for(int i=0;i<100;i++)
        4. {
        5.      Generate a random message
        6.      Assign a random receiver from active client list
        7.      if(i==20)
        8.      {
        9.            Send the message and recipient IP address to server and a special request "SHOW_ROUTE"
        10.           Display routing path, hop count and routing table of each router [You need to receive
                            all the required info from the server in response to "SHOW_ROUTE" request]
        11.     }
        12.     else
        13.     {
        14.           Simply send the message and recipient IP address to server.
        15.     }
        16.     If server can successfully send the message, client will get an acknowledgement along with hop count
                    Otherwise, client will get a failure message [dropped packet]
        17. }
        18. Report average number of hops and drop rate
        */
        System.out.println("Receiving client device setup from server...");
        EndDevice currentDevice = null;
        try
        {
            currentDevice = (EndDevice) networkUtility.read();
        } catch (ClassNotFoundException | IOException ignored)
        {

        }
        System.out.println("Receiving active client list from server...");
        ArrayList<EndDevice> activeClientList = null;
        try
        {
            activeClientList = (ArrayList<EndDevice>) networkUtility.read();
        } catch (IOException | ClassNotFoundException ignored)
        {

        }


//        Thread.sleep(10000);

        int hopCount = 0, dropCount = 0, totalHopCount = 0, successfulTransmissionCount = 0;

        int loopSize = 5;

        for (int i = 0; i < loopSize; i++)
        {

            Thread.sleep(1000);
            System.out.println("Generating random client from active client list...");

            Random random = new Random();
            int randomIndex = random.nextInt(activeClientList.size());
            EndDevice randomClient = activeClientList.get(randomIndex);

            String randomMessage = getSaltString(20);

            Packet messagePacket = new Packet(randomMessage, null, currentDevice.getIpAddress(), randomClient.getIpAddress());

            if (i == 4)
            {
                System.out.println("Sending packet with special message to server from " + messagePacket.getSourceIP() + " to " + messagePacket.getDestinationIP() + "...");
                messagePacket.setSpecialMessage("SHOW_ROUTE");
                try
                {
                    networkUtility.write(messagePacket);
                } catch (IOException ignored)
                {

                }
            } else
            {
                System.out.println("Sending message packet to server with sender "
                        + messagePacket.getSourceIP() + " and receiver " + messagePacket.getDestinationIP() + "...");
                try
                {
                    networkUtility.write(messagePacket);
                } catch (IOException ignored)
                {

                }
            }

            System.out.println("Receiving acknowledgement message from server...");
            Packet acknowledgement = null;
            try
            {
                acknowledgement = (Packet) networkUtility.read();
            } catch (ClassNotFoundException |IOException e)
            {
                e.printStackTrace();
            }

            if (acknowledgement.getMessage().equals("Message transmission successful."))
            {
                System.out.println("Packet transmission successful of iteration number " + (i + 1) + " from " + messagePacket.getSourceIP() + " to " + messagePacket.getDestinationIP() + "...");
                System.out.println("Receiving hop count from server...");
                successfulTransmissionCount++;
                hopCount = acknowledgement.getHopCount();
                totalHopCount += hopCount;

                System.out.println("Hop count for packet transmission was " + hopCount + ".");

                if (i == 4) {
                    System.out.println("\n--------------------------------\nShowing route and routing table\n" + acknowledgement.getSpecialMessage());
                }
            } else if (acknowledgement.getMessage().equals("Message transmission unsuccessful."))
            {
                System.out.println("Packet transmission unsuccessful of iteration number " + (i + 1) + ".");
                dropCount++;
            }

        }

        System.out.println("Average hop count is " + (totalHopCount * 1.0 / successfulTransmissionCount));
        System.out.println("Average drop count is " + (dropCount * 1.0 / loopSize));

        Packet messagePacket = new Packet(null, "End of transmission", null, null);
        try
        {
            networkUtility.write(messagePacket);
        } catch (IOException ignored)
        {

        }

    }

    static String getSaltString(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();

    }
}
