package com.civicai.maps;

import com.civicai.model.Complaint;
import com.civicai.repository.ComplaintRepository;
import com.civicai.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of IMapService.
 */
public class MapService implements IMapService {

    private static MapService instance;

    private MapService() {}

    public static synchronized MapService getInstance() {
        if (instance == null) {
            instance = new MapService();
        }
        return instance;
    }

    @Override
    public void getNearbyComplaints(double latitude, double longitude, double radiusKm, RepositoryCallback<List<Complaint>> callback) {
        ComplaintRepository.getInstance().getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> complaints) {
                List<Complaint> nearby = new ArrayList<>();
                for (Complaint c : complaints) {
                    if (c.getLatitude() != 0.0 && c.getLongitude() != 0.0) {
                        double distance = calculateDistanceKm(latitude, longitude, c.getLatitude(), c.getLongitude());
                        if (distance <= radiusKm) {
                            nearby.add(c);
                        }
                    }
                }
                callback.onSuccess(nearby);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    @Override
    public void resolveAddressFromCoordinates(double latitude, double longitude, RepositoryCallback<String> callback) {
        // Placeholder address resolution
        callback.onSuccess(String.format("Lat: %.4f, Lng: %.4f", latitude, longitude));
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        return dist * 60 * 1.1515 * 1.609344;
    }
}
