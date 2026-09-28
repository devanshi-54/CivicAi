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

    private ClusterRepository() { }

    public static synchronized ClusterRepository getInstance() {
        if (instance == null) {
            instance = new ClusterRepository();
        }
        return instance;
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
