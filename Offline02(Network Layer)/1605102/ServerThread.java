import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

public class ServerThread implements Runnable
{
    private static int hopCount = 0;
    NetworkUtility networkUtility;
    EndDevice endDevice;
    String routePath;


    ServerThread(NetworkUtility networkUtility, EndDevice endDevice)
    {
        this.networkUtility = networkUtility;
        this.endDevice = endDevice;
        System.out.println("Server Ready for client " + NetworkLayerServer.clientCount);
        new Thread(this).start();
    }

    @Override
    public void run()
    {
        /**
         * Synchronize actions with client.
         */
        
        /*
        Tasks:
        1. Upon receiving a packet and recipient, call deliverPacket(packet)
        2. If the packet contains "SHOW_ROUTE" request, then fetch the required information
                and send back to client
        3. Either send acknowledgement with number of hops or send failure message back to client
        */

        System.out.println("Sending client device setup to client...");
        try
        {
            networkUtility.write(endDevice);
        } catch (IOException ignored)
        {

        }

        System.out.println("Sending active client list to sender client...");
        try
        {
            networkUtility.write(NetworkLayerServer.endDevices);
        } catch (IOException ignored)
        {

        }

        while (true)
        {
            Packet messagePacket;
            try
            {
                System.out.println("Receiving message packet and IP from client...");
                messagePacket = (Packet) networkUtility.read();
            } catch (IOException | ClassNotFoundException e)
            {
                break;
            }

            if ("End of transmission".equals(messagePacket.getSpecialMessage()))
            {
                System.out.println("Client " + endDevice.getDeviceID() + 1 + " finished transmission.");
                break;
            }

            System.out.println("Sending packet to recipient client...");
            boolean isPacketDelivered = false;
            hopCount = 0;
            try
            {
                isPacketDelivered = deliverPacket(messagePacket);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            Packet packet;
            if (isPacketDelivered)
            {

                System.out.println("Hop count for current transmission was " + hopCount);
                System.out.println("Sending acknowledgement message and hop count to sender client...");
                packet = new Packet("Message transmission successful.",
                        "", messagePacket.getSourceIP(),
                        messagePacket.getDestinationIP());
                packet.setHopCount(hopCount);

                if ("SHOW_ROUTE".equals(messagePacket.getSpecialMessage()))
                {
                    packet.setSpecialMessage(routePath + "\n\n\n" + NetworkLayerServer.strrouters());
                }
            } else
            {
                System.out.println("Sending failure message to sender client...");
                packet = new Packet("Message transmission unsuccessful.",
                        "", messagePacket.getSourceIP(),
                        messagePacket.getDestinationIP());
            }

            try
            {
                networkUtility.write(packet);
            } catch (IOException ignored)
            {

            }
        }


    }


    public Boolean deliverPacket(Packet p) throws InterruptedException
    {

        
        /*
        1. Find the router s which has an interface
                such that the interface and source end device have same network address.
        2. Find the router d which has an interface
                such that the interface and destination end device have same network address.
        3. Implement forwarding, i.e., s forwards to its gateway router x considering d as the destination.
                similarly, x forwards to the next gateway router y considering d as the destination,
                and eventually the packet reaches to destination router d.

            3(a) If, while forwarding, any gateway x, found from routingTable of router r is in down state[x.state==FALSE]
                    (i) Drop packet
                    (ii) Update the entry with distance Constants.INFTY
                    (iii) Block NetworkLayerServer.stateChanger.t
                    (iv) Apply DVR starting from router r.
                    (v) Resume NetworkLayerServer.stateChanger.t

            3(b) If, while forwarding, a router x receives the packet from router y,
                    but routingTableEntry shows Constants.INFTY distance from x to y,
                    (i) Update the entry with distance 1
                    (ii) Block NetworkLayerServer.stateChanger.t
                    (iii) Apply DVR starting from router x.
                    (iv) Resume NetworkLayerServer.stateChanger.t

        4. If 3(a) occurs at any stage, packet will be dropped,
            otherwise successfully sent to the destination router
        */
        Router senderRouter = null, receiverRouter = null, gatewayRouter;

        for (Router router : NetworkLayerServer.routers)
        {
            for (IPAddress ipAddress : router.getInterfaceAddresses())
            {
                if (ipAddress.getBytes()[0].equals(p.getSourceIP().getBytes()[0]) &&
                        ipAddress.getBytes()[1].equals(p.getSourceIP().getBytes()[1]) &&
                        ipAddress.getBytes()[2].equals(p.getSourceIP().getBytes()[2]))
                {
                    senderRouter = router;
                }

                if (ipAddress.getBytes()[0].equals(p.getDestinationIP().getBytes()[0]) &&
                        ipAddress.getBytes()[1].equals(p.getDestinationIP().getBytes()[1]) &&
                        ipAddress.getBytes()[2].equals(p.getDestinationIP().getBytes()[2]))
                {
                    receiverRouter = router;
                }
            }
        }

        routePath = "" + senderRouter.getRouterId();

        int gatewayRouterId = senderRouter.getRoutingTable().get(senderRouter.getIndexOf(receiverRouter.getRouterId()))
                .getGatewayRouterId();

        gatewayRouter = (gatewayRouterId != -1) ? NetworkLayerServer.routerMap.get(gatewayRouterId) : null;

        while (gatewayRouter != receiverRouter)
        {
            if (gatewayRouter == null || !gatewayRouter.getState())
            {
                System.out.println("Packet dropped during transmission...");

                if (gatewayRouter != null)
                {
                    System.out.println("Updating routing tables...");
                    for (int i = 0; i < senderRouter.getRoutingTable().size(); i++)
                    {
                        RoutingTableEntry routingTableEntry = senderRouter.getRoutingTable().get(i);
                        if (routingTableEntry == senderRouter.getRoutingTable().
                                get(senderRouter.getIndexOf(gatewayRouter.getRouterId())))
                        {
                            senderRouter.getRoutingTable().set(i, new RoutingTableEntry(gatewayRouter.getRouterId(),
                                    Constants.INFINITY, -1));
                            break;
                        }
                    }

                    synchronized (NetworkLayerServer.stateChanger)
                    {
                        NetworkLayerServer.stateChanger.wait();
                    }

                    //NetworkLayerServer.simpleDVR(getawayRouter.getRouterId());
                    NetworkLayerServer.DVR(gatewayRouter.getRouterId());

                    synchronized (NetworkLayerServer.stateChanger)
                    {
                        NetworkLayerServer.stateChanger.notifyAll();
                    }

                }
                return false;
            } else if (gatewayRouter.getRoutingTable().get(gatewayRouter.getIndexOf(senderRouter.getRouterId())).
                    getDistance() == Constants.INFINITY)
            {
                for (int i = 0; i < gatewayRouter.getRoutingTable().size(); i++)
                {
                    RoutingTableEntry routingTableEntry = gatewayRouter.getRoutingTable().get(i);
                    if (routingTableEntry == gatewayRouter.getRoutingTable().
                            get(gatewayRouter.getIndexOf(senderRouter.getRouterId())))
                    {
                        gatewayRouter.getRoutingTable().set(i, new RoutingTableEntry(senderRouter.getRouterId(),
                                1, senderRouter.getRouterId()));
                        break;
                    }
                }

                synchronized (NetworkLayerServer.stateChanger)
                {
                    NetworkLayerServer.stateChanger.wait();
                }

                //NetworkLayerServer.simpleDVR(getawayRouter.getRouterId());
                NetworkLayerServer.DVR(gatewayRouter.getRouterId());

                synchronized (NetworkLayerServer.stateChanger)
                {
                    NetworkLayerServer.stateChanger.notifyAll();
                }

            }
            hopCount++;

            senderRouter = gatewayRouter;
            gatewayRouter = NetworkLayerServer.routerMap.get(gatewayRouter.getRoutingTable().
                    get(gatewayRouter.getIndexOf(receiverRouter.getRouterId())).getGatewayRouterId());

            routePath += "---------------------->" + senderRouter.getRouterId();
        }

        routePath += "---------------------->" + receiverRouter.getRouterId();
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }
}
