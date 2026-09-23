package com.civicai.repository;

import com.civicai.model.ClusterStatus;
import com.civicai.model.IssueCluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for AI-assisted Issue Clusters.
 */
public class ClusterRepository implements IClusterRepository {

    private static ClusterRepository instance;
    private final Map<String, IssueCluster> clusterCache = new HashMap<>();

    private ClusterRepository() {
        initSampleClusters();
    }

    public static synchronized ClusterRepository getInstance() {
        if (instance == null) {
            instance = new ClusterRepository();
        }
        return instance;
    }

    private void initSampleClusters() {
        IssueCluster cl1 = new IssueCluster(
                "CLS-101",
                "Road Cavities & Drainage Failure on 5th Cross",
                "Roads & Drainage",
                "Indiranagar 5th Cross (500m radius)",
                12.9783,
                77.6408,
                0.91f,
                "4 independent citizen complaints within 300 meters reporting road collapse following heavy rain."
        );
        cl1.setStatus(ClusterStatus.NEEDS_REVIEW);
        clusterCache.put(cl1.getClusterId(), cl1);
    }

    @Override
    public void getAllClusters(RepositoryCallback<List<IssueCluster>> callback) {
        callback.onSuccess(new ArrayList<>(clusterCache.values()));
    }

    @Override
    public void getClusterById(String clusterId, RepositoryCallback<IssueCluster> callback) {
        IssueCluster cluster = clusterCache.get(clusterId);
        if (cluster != null) {
            callback.onSuccess(cluster);
        } else {
            callback.onError(new Exception("Cluster not found: " + clusterId));
        }
    }

    @Override
    public void updateClusterStatus(String clusterId, ClusterStatus status, RepositoryCallback<Void> callback) {
        IssueCluster cluster = clusterCache.get(clusterId);
        if (cluster != null) {
            cluster.setStatus(status);
            cluster.setUpdatedAt(System.currentTimeMillis());
            callback.onSuccess(null);
        } else {
            callback.onError(new Exception("Cluster not found: " + clusterId));
        }
    }
}
