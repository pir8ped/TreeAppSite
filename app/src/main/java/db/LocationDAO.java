package db;

import com.john.TreeApp.beans.Location;

import java.util.List;

public interface LocationDAO {

    int insertLocation(Location location);

    Location getLocationById(int locationId);

    List<Location> getAllLocations();

    int deleteLocation(int locationId);

    int updateLocation(Location location);
}

