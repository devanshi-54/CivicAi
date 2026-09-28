package com.civicai;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.civicai.citizen.CitizenComplaintStore;
import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;
import com.civicai.repository.ComplaintRepository;
import com.civicai.repository.RepositoryCallback;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class IntegrationFlowTest {

    @Test
    public void testCompleteCitizenToGovernmentToCitizenFlow() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        ComplaintRepository repo = ComplaintRepository.getInstance();

        // ---------------------------------------------------------
        // PHASE 1: Citizen creates complaint & submits via Repository
        // ---------------------------------------------------------
        String ticketId = "CMP-INTTEST1";
        Complaint citizenComplaint = new Complaint();
        citizenComplaint.setComplaintId(ticketId);
        citizenComplaint.setUserId("citizen-42");
        citizenComplaint.setCitizenName("Devanshi Test");
        citizenComplaint.setTitle("Broken Water Main on 5th Ave");
        citizenComplaint.setDescription("Water main burst causing street flooding and low pressure.");
        citizenComplaint.setCategory("Water Supply");
        citizenComplaint.setLocationAddress("5th Ave & 12th Cross");
        citizenComplaint.setStatus(ComplaintStatus.SUBMITTED);
        citizenComplaint.setCreatedAt(System.currentTimeMillis());
        citizenComplaint.setUpdatedAt(System.currentTimeMillis());

        // Save to local store (local-first experience)
        CitizenComplaintStore.saveComplaint(context, citizenComplaint);
        Complaint storedLocal = CitizenComplaintStore.findComplaint(context, ticketId);
        Assert.assertNotNull("Complaint should exist in local store", storedLocal);

        // Submit via ComplaintRepository (synced to Firestore + memoryCache)
        CountDownLatch submitLatch = new CountDownLatch(1);
        AtomicBoolean submitSuccess = new AtomicBoolean(false);
        repo.submitComplaint(citizenComplaint, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                submitSuccess.set(true);
                submitLatch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                submitSuccess.set(false);
                submitLatch.countDown();
            }
        });
        Assert.assertTrue("Submit latch timed out", submitLatch.await(10, TimeUnit.SECONDS));
        Assert.assertTrue("Submit should succeed", submitSuccess.get());

        // ---------------------------------------------------------
        // PHASE 2: Government reads complaints (Firestore -> Gov)
        // ---------------------------------------------------------
        CountDownLatch govLoadLatch = new CountDownLatch(1);
        AtomicReference<List<Complaint>> govComplaintsRef = new AtomicReference<>();
        repo.getAllComplaints(new RepositoryCallback<List<Complaint>>() {
            @Override
            public void onSuccess(List<Complaint> result) {
                govComplaintsRef.set(result);
                govLoadLatch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                govLoadLatch.countDown();
            }
        });
        Assert.assertTrue("Gov load latch timed out", govLoadLatch.await(10, TimeUnit.SECONDS));
        List<Complaint> govList = govComplaintsRef.get();
        Assert.assertNotNull("Government complaints list should not be null", govList);
        Assert.assertFalse("Government complaints list should not be empty", govList.isEmpty());

        boolean found = false;
        for (Complaint c : govList) {
            if (ticketId.equals(c.getComplaintId())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue("Submitted complaint must be visible to government", found);

        // ---------------------------------------------------------
        // PHASE 3: Government records official decision (Gov -> Firestore)
        // ---------------------------------------------------------
        CountDownLatch decisionLatch = new CountDownLatch(1);
        AtomicBoolean decisionSuccess = new AtomicBoolean(false);
        repo.updateOfficialDecision(
                ticketId,
                Priority.HIGH,
                "Water Board",
                "Engineer Patel",
                "Emergency repair crew dispatched",
                "Pipe section isolated",
                ComplaintStatus.IN_PROGRESS,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        decisionSuccess.set(true);
                        decisionLatch.countDown();
                    }

                    @Override
                    public void onError(Exception exception) {
                        decisionSuccess.set(false);
                        decisionLatch.countDown();
                    }
                }
        );
        Assert.assertTrue("Decision latch timed out", decisionLatch.await(10, TimeUnit.SECONDS));
        Assert.assertTrue("Government decision update should succeed", decisionSuccess.get());

        // ---------------------------------------------------------
        // PHASE 4: Citizen tracking reads updated decision (Firestore -> Citizen)
        // ---------------------------------------------------------
        CountDownLatch trackingLatch = new CountDownLatch(1);
        AtomicReference<Complaint> updatedRef = new AtomicReference<>();
        repo.getComplaintById(ticketId, new RepositoryCallback<Complaint>() {
            @Override
            public void onSuccess(Complaint result) {
                updatedRef.set(result);
                trackingLatch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                trackingLatch.countDown();
            }
        });
        Assert.assertTrue("Citizen tracking latch timed out", trackingLatch.await(10, TimeUnit.SECONDS));
        Complaint trackedComplaint = updatedRef.get();
        Assert.assertNotNull("Tracked complaint should not be null", trackedComplaint);

        // Verify that official decision & updated status are visible to citizen
        Assert.assertEquals("Official priority should be HIGH", Priority.HIGH, trackedComplaint.getOfficialPriority());
        Assert.assertEquals("Status should be IN_PROGRESS", ComplaintStatus.IN_PROGRESS, trackedComplaint.getStatus());
        Assert.assertEquals("Department should be Water Board", "Water Board", trackedComplaint.getAssignedDepartment());
        Assert.assertEquals("Officer should be Engineer Patel", "Engineer Patel", trackedComplaint.getAssignedOfficer());
        Assert.assertEquals("Decision should match", "Emergency repair crew dispatched", trackedComplaint.getOfficialDecision());

        // Save back to local store (matching CitizenMainActivity openComplaintDetails behavior)
        CitizenComplaintStore.saveComplaint(context, trackedComplaint);
        Complaint finalLocal = CitizenComplaintStore.findComplaint(context, ticketId);
        Assert.assertNotNull("Final local complaint must exist", finalLocal);
        Assert.assertEquals("Local complaint status must reflect IN_PROGRESS", ComplaintStatus.IN_PROGRESS, finalLocal.getStatus());
        Assert.assertEquals("Local complaint decision must reflect official decision", "Emergency repair crew dispatched", finalLocal.getOfficialDecision());
    }
}
