package supportlib;

import android.graphics.Path;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Ilya on 17/11/16.
 *
 * This class is our implementation of Nearest Neighbour algorithm,
 * It is a greedy algo that will allow us to traverse through the graph visiting all points.
 */

public class NearestNeighbour {
    /**
     * This is the approximated path using the nearest neighbour algorithm.
     * In the first pass, we will find the paths using the means of transportation with the best cost per time
    **/
    public static PathsAndCost getApproximatedPath(HashMap<Integer,Location> locations,double budget,int hotelID){
        ArrayList<PathInfo> visited = new ArrayList<PathInfo>();
        int current_location_id = 0;
        // First step is to find and iterate through all points in the first node (hotel)
        HashMap<Integer,Location>  originalLocation = new HashMap<Integer, Location>();
        originalLocation.put(hotelID,locations.get(hotelID));
        int originalSize = locations.size();
        current_location_id = hotelID;
        PathInfo nextPath = getNextLocation(SearchUtils.getConnected(current_location_id,locations));
        double cost = nextPath.getCost();
        visited.add(nextPath);

        while(visited.size() < originalSize){
            locations.remove(current_location_id);
            current_location_id = nextPath.getToId();
            originalLocation.put(current_location_id,locations.get(current_location_id));
            if(visited.size() == originalSize-1)
                locations.put(hotelID,originalLocation.get(hotelID));
            nextPath = getNextLocation(SearchUtils.getConnected(current_location_id, locations));
            cost += nextPath.getCost();
            visited.add(nextPath);

        }
        PathsAndCost pnc = relaxPathInfo(visited,cost, budget, originalLocation);

        return pnc;
    }

    /**
    *  Following the principle of dijkstra, this method will loop through the
    **/
    public static PathsAndCost relaxPathInfo(ArrayList<PathInfo> paths,double cost, double budget, HashMap<Integer,Location> locations){
        HashMap<Integer,Location> blah = locations;
        ArrayList<PathInfo> relaxedPath = paths;
        if(cost> budget) {
            for (int i = 0; i < 2; i++) {
                for (PathInfo path : relaxedPath) {
                    cost -= path.getCost();
                    PathInfo newPath = SearchUtils.getVehicleConnection(path, locations);
                    cost += newPath.getCost();
                    relaxedPath.set(relaxedPath.indexOf(path),newPath);
                    if (cost < budget) break;
                }
                if (cost < budget) break;
            }
        }
        return new PathsAndCost(relaxedPath,cost);
    }

    /**
     * This method loops through the number of edges and chooses the edge with the shortest duration.
     */
    public static PathInfo getNextLocation(ArrayList<PathInfo> edges){
        int shortest_duration = 0;
        PathInfo shortest_path = edges.get(0);
        for (PathInfo pi : edges){
           if (shortest_duration> pi.getDuration()){
               shortest_duration = pi.getDuration();
               shortest_path = pi;
           }
        }
        return shortest_path;
    }
}
