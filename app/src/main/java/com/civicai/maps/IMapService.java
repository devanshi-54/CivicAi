package com.civicai.maps;

import com.civicai.model.Complaint;
import com.civicai.repository.RepositoryCallback;
import java.util.List;

/**
 * Service interface for geographic maps, marker rendering, and location lookups.
 */
public interface IMapService {
    void getNearbyComplaints(double latitude, double longitude, double radiusKm, RepositoryCallback<List<Complaint>> callback);
    void resolveAddressFromCoordinates(double latitude, double longitude, RepositoryCallback<String> callback);
}
