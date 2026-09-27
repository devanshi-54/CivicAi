package com.civicai.government;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;
import com.civicai.repository.ComplaintRepository;
import com.civicai.repository.RepositoryCallback;

import java.util.List;

/**
 * Shared ViewModel for Government screens.
 * Holds the complaint list, selected complaint, and decision update state.
 */
public class GovViewModel extends ViewModel {

    private final ComplaintRepository repository = ComplaintRepository.getInstance();

    // Complaint list live data
    private final MutableLiveData<List<Complaint>> complaintsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> listErrorLiveData = new MutableLiveData<>();

    // Selected complaint for details / decision screens
    private final MutableLiveData<Complaint> selectedComplaintLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> detailErrorLiveData = new MutableLiveData<>();

    // Decision update result
    private final MutableLiveData<Boolean> decisionSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> decisionErrorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> decisionLoadingLiveData = new MutableLiveData<>(false);

    public LiveData<List<Complaint>> getComplaints() { return complaintsLiveData; }
    public LiveData<String> getListError() { return listErrorLiveData; }
    public LiveData<Complaint> getSelectedComplaint() { return selectedComplaintLiveData; }
    public LiveData<String> getDetailError() { return detailErrorLiveData; }
    public LiveData<Boolean> getDecisionSuccess() { return decisionSuccessLiveData; }
    public LiveData<String> getDecisionError() { return decisionErrorLiveData; }
    public LiveData<Boolean> getDecisionLoading() { return decisionLoadingLiveData; }

    /** Load all complaints from the repository (sorted by date, newest first). */
    public void loadAllComplaints() {
        repository.getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> result) {
                complaintsLiveData.postValue(result);
            }

            @Override
            public void onError(Exception exception) {
                listErrorLiveData.postValue(exception.getMessage());
            }
        });
    }

    /** Load a single complaint by ID for the details screen. */
    public void loadComplaintById(String complaintId) {
        if (complaintId == null || complaintId.isEmpty()) {
            detailErrorLiveData.setValue("No complaint ID provided.");
            return;
        }
        repository.getComplaintById(complaintId, new RepositoryCallback<Complaint>() {
            @Override
            public void onSuccess(Complaint result) {
                selectedComplaintLiveData.postValue(result);
            }

            @Override
            public void onError(Exception exception) {
                detailErrorLiveData.postValue("Complaint not found: " + exception.getMessage());
            }
        });
    }

    /** Refresh the selected complaint after a decision update. */
    public void refreshSelectedComplaint() {
        Complaint current = selectedComplaintLiveData.getValue();
        if (current != null && current.getComplaintId() != null) {
            loadComplaintById(current.getComplaintId());
        }
    }

    /**
     * Submit the official government decision.
     *
     * @param complaintId     Target complaint ID
     * @param officialPriority Official priority override
     * @param department      Assigned department
     * @param officer         Assigned officer name
     * @param decision        Official decision text
     * @param internalNotes   Internal notes
     * @param newStatus       New complaint status
     */
    public void submitDecision(String complaintId, Priority officialPriority, String department,
                               String officer, String decision, String internalNotes,
                               ComplaintStatus newStatus) {
        decisionLoadingLiveData.setValue(true);
        decisionSuccessLiveData.setValue(null);
        decisionErrorLiveData.setValue(null);

        repository.updateOfficialDecision(complaintId, officialPriority, department, officer,
                decision, internalNotes, newStatus, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        decisionLoadingLiveData.postValue(false);
                        decisionSuccessLiveData.postValue(true);
                        // Refresh list and detail
                        loadAllComplaints();
                        loadComplaintById(complaintId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        decisionLoadingLiveData.postValue(false);
                        decisionErrorLiveData.postValue("Failed to save decision: " + exception.getMessage());
                    }
                });
    }

    /** Clear the decision result flags (after observing them). */
    public void clearDecisionResult() {
        decisionSuccessLiveData.setValue(null);
        decisionErrorLiveData.setValue(null);
    }
}
