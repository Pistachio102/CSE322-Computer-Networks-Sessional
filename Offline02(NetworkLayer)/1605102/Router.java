//Work needed

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Router
{
    private final ArrayList<RoutingTableEntry> routingTable;//used to implement DVR
    private int routerId;
    private int numberOfInterfaces;
    private ArrayList<IPAddress> interfaceAddresses;//list of IP address of all interfaces of the router
    private ArrayList<Integer> neighborRouterIDs;//Contains both "UP" and "DOWN" state routers
    private Boolean state;//true represents "UP" state and false is for "DOWN" state
    private Map<Integer, IPAddress> gatewayIDtoIP;

    public Router()
    {
        interfaceAddresses = new ArrayList<>();
        routingTable = new ArrayList<>();
        neighborRouterIDs = new ArrayList<>();

        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        state = p < Constants.LAMBDA;

        numberOfInterfaces = 0;
    }

    public Router(int routerId, ArrayList<Integer> neighborRouters, ArrayList<IPAddress> interfaceAddresses, Map<Integer, IPAddress> gatewayIDtoIP)
    {
        this.routerId = routerId;
        this.interfaceAddresses = interfaceAddresses;
        this.neighborRouterIDs = neighborRouters;
        this.gatewayIDtoIP = gatewayIDtoIP;
        routingTable = new ArrayList<>();


        /**
         * 80% Probability that the router is up
         */
        Random random = new Random();
        double p = random.nextDouble();
        state = p < Constants.LAMBDA;

        numberOfInterfaces = interfaceAddresses.size();
    }

    @Override
    public String toString()
    {
        String string = "";
        string += "Router ID: " + routerId + "\n" + "Interfaces: \n";
        for (int i = 0; i < numberOfInterfaces; i++)
        {
            string += interfaceAddresses.get(i).getString() + "\t";
        }
        string += "\n" + "Neighbors: \n";
        for (int i = 0; i < neighborRouterIDs.size(); i++)
        {
            string += neighborRouterIDs.get(i) + "\t";
        }
        return string;
    }


    /**
     * Initialize the distance(hop count) for each router.
     * for itself, distance=0; for any connected router with state=true, distance=1; otherwise distance=Constants.INFTY;
     */
    public void initiateRoutingTable()
    {
        ArrayList<Router> tempRouters = NetworkLayerServer.routers;
        for (int i = 0; i < tempRouters.size(); i++)
        {
            if (this.routerId == tempRouters.get(i).getRouterId())
            {
                RoutingTableEntry entry = new RoutingTableEntry(tempRouters.get(i).getRouterId(), 0, this.routerId);
                routingTable.add(entry);
            } else if (neighborRouterIDs.contains(tempRouters.get(i).getRouterId()))
            {
                double distance;
                int gatewayRouterId;
                if (tempRouters.get(i).getState())
                {
                    distance = 1;
                    gatewayRouterId = tempRouters.get(i).getRouterId();
                } else
                {
                    distance = Constants.INFINITY;
                    gatewayRouterId = -1;
                }
                RoutingTableEntry entry = new RoutingTableEntry(tempRouters.get(i).getRouterId(), distance, gatewayRouterId);
                routingTable.add(entry);
            } else
            {
                RoutingTableEntry entry = new RoutingTableEntry(tempRouters.get(i).getRouterId(), Constants.INFINITY, -1);
                routingTable.add(entry);
            }

        }


    }

    /**
     * Delete all the routingTableEntry
     */
    public void clearRoutingTable()
    {
        for (int i = 0; i < routingTable.size(); i++)
        {
            RoutingTableEntry routingTableEntry = routingTable.get(i);
            routingTable.add(i, new RoutingTableEntry(routingTableEntry.getRouterId(), Constants.INFINITY, -1));

        }

    }

    /**
     * Update the routing table for this router using the entries of Router neighbor
     *
     * @param neighbor
     */
    public boolean updateRoutingTable(Router neighbor)
    {
        boolean ifUpdated = false;
        int rID;
        for (int i = 0; i < this.routingTable.size(); i++)
        {
            RoutingTableEntry routingTableEntry = this.routingTable.get(i);
            rID = routingTableEntry.getRouterId();
            if (this.routerId != rID && !this.neighborRouterIDs.contains(rID) &&
                    neighbor.getRoutingTable().get(neighbor.getIndexOf(rID)).getDistance() != Constants.INFINITY)
            {
                routingTable.set(i, new RoutingTableEntry(routingTableEntry.getRouterId(),
                        neighbor.getRoutingTable().get(neighbor.getIndexOf(rID)).getDistance() + 1, neighbor.getRouterId()));
                ifUpdated = true;
            }
        }
        return ifUpdated;
    }

    public boolean sfupdateRoutingTable(Router neighbor)
    {
        boolean ifUpdated = false;
        for (int i = 0; i < this.routingTable.size(); i++)
        {
            RoutingTableEntry routingTableEntry = this.routingTable.get(i);
            int routingTableEntryRouterId = routingTableEntry.getRouterId();
            RoutingTableEntry neighborsCorrespondingRoutingTableEntry = neighbor.getRoutingTable().get
                    (neighbor.getIndexOf(routingTableEntryRouterId));
            if (this.routerId != routingTableEntryRouterId && !this.neighborRouterIDs.contains(routingTableEntryRouterId) &&
                    neighborsCorrespondingRoutingTableEntry.getDistance() != Constants.INFINITY)
            {
                double dist = neighborsCorrespondingRoutingTableEntry.getDistance() + 1;
                if (routingTableEntry.getGatewayRouterId() == neighbor.getRouterId() ||
                        (dist < routingTableEntry.getDistance() && this.routerId != neighborsCorrespondingRoutingTableEntry.getGatewayRouterId()))
                {
                    routingTable.set(i, new RoutingTableEntry(routingTableEntry.getRouterId(), dist, neighbor.getRouterId()));
                    ifUpdated = true;
                }

            }
        }
        return ifUpdated;
    }

    /**
     * If the state was up, down it; if state was down, up it
     */
    public void revertState()
    {
        state = !state;
        if (state)
        {
            initiateRoutingTable();
        } else
        {
            clearRoutingTable();
        }
    }

    public int getIndexOf(int routerId)
    {
        for (int i = 0; i < this.routingTable.size(); i++)
        {
            if (this.routingTable.get(i).getRouterId() == routerId)
            {
                return i;
            }
        }
        return -1;
    }

    public int getRouterId()
    {
        return routerId;
    }

    public void setRouterId(int routerId)
    {
        this.routerId = routerId;
    }

    public int getNumberOfInterfaces()
    {
        return numberOfInterfaces;
    }

    public void setNumberOfInterfaces(int numberOfInterfaces)
    {
        this.numberOfInterfaces = numberOfInterfaces;
    }

    public ArrayList<IPAddress> getInterfaceAddresses()
    {
        return interfaceAddresses;
    }

    public void setInterfaceAddresses(ArrayList<IPAddress> interfaceAddresses)
    {
        this.interfaceAddresses = interfaceAddresses;
        numberOfInterfaces = interfaceAddresses.size();
    }

    public ArrayList<RoutingTableEntry> getRoutingTable()
    {
        return routingTable;
    }

    public void addRoutingTableEntry(RoutingTableEntry entry)
    {
        this.routingTable.add(entry);
    }

    public ArrayList<Integer> getNeighborRouterIDs()
    {
        return neighborRouterIDs;
    }

    public void setNeighborRouterIDs(ArrayList<Integer> neighborRouterIDs)
    {
        this.neighborRouterIDs = neighborRouterIDs;
    }

    public Boolean getState()
    {
        return state;
    }

    public void setState(Boolean state)
    {
        this.state = state;
    }

    public Map<Integer, IPAddress> getGatewayIDtoIP()
    {
        return gatewayIDtoIP;
    }

    public void printRoutingTable()
    {
        System.out.println("Router " + routerId);
        System.out.println("DestID Distance Nexthop");
        for (RoutingTableEntry routingTableEntry : routingTable)
        {
            System.out.println(routingTableEntry.getRouterId() + " " + routingTableEntry.getDistance() + " " + routingTableEntry.getGatewayRouterId());
        }
        System.out.println("-----------------------");
    }

    public String strRoutingTable()
    {
        String string = "Router" + routerId + "\n";
        string += "DestID Distance Nexthop\n";
        for (RoutingTableEntry routingTableEntry : routingTable)
        {
            string += routingTableEntry.getRouterId() + " " + routingTableEntry.getDistance() + " " + routingTableEntry.getGatewayRouterId() + "\n";
        }

        string += "-----------------------\n";
        return string;
    }

}
