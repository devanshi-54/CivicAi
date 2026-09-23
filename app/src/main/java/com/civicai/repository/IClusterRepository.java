package com.civicai.repository;

import com.civicai.model.ClusterStatus;
import com.civicai.model.IssueCluster;

import java.util.List;

public interface IClusterRepository {
    void getAllClusters(RepositoryCallback<List<IssueCluster>> callback);
    void getClusterById(String clusterId, RepositoryCallback<IssueCluster> callback);
    void updateClusterStatus(String clusterId, ClusterStatus status, RepositoryCallback<Void> callback);
}
