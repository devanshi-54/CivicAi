package com.civicai.citizen;

import android.util.Patterns;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;

import com.civicai.R;
import com.civicai.ai.AiAnalysisCallback;
import com.civicai.ai.AiAnalysisService;
import com.civicai.firebase.FirestoreHelper;
import com.civicai.model.AiAnalysisResult;
import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.User;
import com.civicai.repository.ComplaintRepository;
import com.civicai.repository.RepositoryCallback;
import com.civicai.repository.UserRepository;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Citizen-facing screens and the local-first complaint reporting workflow. */
public class CitizenMainActivity extends AppCompatActivity {
    private final ArrayDeque<Integer> screenHistory = new ArrayDeque<>();
    private int currentScreen = R.layout.activity_citizen_home;
    private String currentComplaintId;
    private AiAnalysisResult currentAiAnalysisResult;
    private String selectedImageUri = "";
    private Double selectedLatitude;
    private Double selectedLongitude;
    private ActivityResultLauncher<String[]> galleryPicker;
    private ActivityResultLauncher<Void> cameraPicker;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerResultLaunchers();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBack(); }
        });
        if (!isRegistrationComplete(getCurrentProfile())) {
            currentScreen = R.layout.activity_citizen_onboarding_1;
        //    currentScreen = R.layout.activity_citizen_profile_settings;
        }
        showScreen(currentScreen, false);
    }

    private void registerResultLaunchers() {
        cameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap == null) return;
            try {
                File folder = new File(getFilesDir(), "citizen_evidence");
                if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Could not create photo folder");
                File image = new File(folder, "evidence_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream output = new FileOutputStream(image)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) throw new IllegalStateException("Could not encode photo");
                }
                selectedImageUri = Uri.fromFile(image).toString();
                updatePhotoPreview();
            } catch (Exception exception) {
                Toast.makeText(this, "Photo could not be saved. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        galleryPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some document providers grant a temporary read permission only.
            }
            selectedImageUri = uri.toString();
            updatePhotoPreview();
        });
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allowed = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                    || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
            if (allowed) readCurrentLocation();
            else Toast.makeText(this, "Location permission was not granted. You can enter an address instead.", Toast.LENGTH_LONG).show();
        });
    }

    private void showScreen(int layout, boolean rememberCurrent) {
        if (rememberCurrent) screenHistory.push(currentScreen);
        currentScreen = layout;
        setContentView(layout);
        loadOnboardingPhoto(layout);
        wireToolbar();
        wireBottomNavigation();
        wireActions();
        if (layout == R.layout.activity_citizen_report_complaint) restoreDraftToForm();
        if (layout == R.layout.activity_citizen_ai_analysis) bindAnalysisPreview();
        if (layout == R.layout.activity_citizen_complaint_submitted) bindSubmissionConfirmation();
        if (layout == R.layout.activity_citizen_complaint_details) bindComplaintDetails();
        if (layout == R.layout.activity_citizen_profile_settings) bindProfile();
        if (layout == R.layout.activity_citizen_home) bindHomeGreeting();
        populateLists();
        if (layout == R.layout.activity_citizen_home || layout == R.layout.activity_citizen_my_complaints) {
            syncMyComplaints();
        }
    }

    private void loadOnboardingPhoto(int layout) {
        int imageResource;
        if (layout == R.layout.activity_citizen_onboarding_1) {
            imageResource = R.drawable.onboarding_photo_1;
        } else if (layout == R.layout.activity_citizen_onboarding_2) {
            imageResource = R.drawable.onboarding_photo_2;
        } else if (layout == R.layout.activity_citizen_onboarding_3) {
            imageResource = R.drawable.onboarding_photo_3;
        } else {
            return;
        }
        ImageView image = findViewById(R.id.imgOnboarding);
        if (image != null) {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageResource(imageResource);
        }
    }
    private void bindHomeGreeting() {
        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                if (currentScreen != R.layout.activity_citizen_home || user == null) return;
                setLabel(R.id.tvGreeting, "Welcome, " + (TextUtils.isEmpty(user.getName()) ? "Citizen" : user.getName()));
                setLabel(R.id.tvCitizenLocation, TextUtils.isEmpty(user.getWard()) ? "Citizen account" : "Ward " + user.getWard() + " • Citizen");
            }
            @Override public void onError(Exception error) { Log.w("CitizenMainActivity", "Could not load greeting", error); }
        });
    }

    private void wireToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> goBack());
        }
    }

    private void wireBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav == null) return;
        int selected = currentScreen == R.layout.activity_citizen_my_complaints ? R.id.citizen_my_complaints
                : currentScreen == R.layout.activity_citizen_notifications ? R.id.citizen_notifications
                : currentScreen == R.layout.activity_citizen_profile_settings ? R.id.citizen_profile_settings
                : R.id.citizen_home;
        nav.setSelectedItemId(selected);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.citizen_home) showScreen(R.layout.activity_citizen_home, true);
            else if (id == R.id.citizen_my_complaints) showScreen(R.layout.activity_citizen_my_complaints, true);
            else if (id == R.id.citizen_notifications) showScreen(R.layout.activity_citizen_notifications, true);
            else if (id == R.id.citizen_profile_settings) showScreen(R.layout.activity_citizen_profile_settings, true);
            return true;
        });
    }

    private void wireActions() {
        click(R.id.btnReportHero, () -> open(R.layout.activity_citizen_report_complaint));
        click(R.id.btnNavReport, () -> open(R.layout.activity_citizen_report_complaint));
        click(R.id.btnNavMyComplaints, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnNavTrack, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnNotification, () -> open(R.layout.activity_citizen_notifications));
        click(R.id.btnViewAll, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnAnalyze, this::continueToAnalysis);
        click(R.id.btnSubmitFinal, this::submitComplaint);
        click(R.id.btnSaveProfile, this::saveProfile);
        click(R.id.btnTrackComplaint, () -> openComplaintDetails(currentComplaintId));
        click(R.id.btnSendFeedback, this::sendComplaintFeedback);
        click(R.id.btnBackHome, () -> showScreen(R.layout.activity_citizen_home, false));
        click(R.id.btnViewComplaint, () -> open(R.layout.activity_citizen_my_complaints));
        click(R.id.btnSkip, () -> showScreen(R.layout.activity_citizen_home, false));
        click(R.id.btnNext, () -> {
            if (currentScreen == R.layout.activity_citizen_onboarding_1) open(R.layout.activity_citizen_onboarding_2);
            else if (currentScreen == R.layout.activity_citizen_onboarding_2) open(R.layout.activity_citizen_onboarding_3);
            else showScreen(R.layout.activity_citizen_home, false);
        });
        click(R.id.btnGetStarted, () -> showScreen(R.layout.activity_citizen_home, false));
        click(R.id.btnLogout, () -> Toast.makeText(this, "You are signed out", Toast.LENGTH_SHORT).show());
        click(R.id.btnSaveDraft, this::saveDraftFromForm);
        click(R.id.btnCamera, this::launchCameraSafely);
        click(R.id.btnGallery, () -> galleryPicker.launch(new String[]{"image/*"}));
        click(R.id.btnGetLocation, this::requestCurrentLocation);
        click(R.id.btnCopyTicket, this::copyTicketId);
    }

    private void populateLists() {
        List<Row> complaintRows = getComplaintRows();
        RecyclerView recent = findViewById(R.id.rvRecentComplaints);
        if (recent != null) setRows(recent, complaintRows, false);
        RecyclerView complaints = findViewById(R.id.rvMyComplaints);
        if (complaints != null) {
            List<Row> filtered = filterRows(complaintRows, selectedStatusFilter());
            setRows(complaints, filtered, false);
            View empty = findViewById(R.id.tvEmptyComplaints);
            if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            setupComplaintTabs(complaintRows);
        }
        RecyclerView notifications = findViewById(R.id.rvNotifications);
        if (notifications != null) setRows(notifications, new ArrayList<>(), true);
    }

    private void launchCameraSafely() {
        Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (getPackageManager().resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            Toast.makeText(this, "Camera app is not available on this device.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            cameraPicker.launch(null);
        } catch (ActivityNotFoundException | SecurityException | IllegalStateException error) {
            Log.e("CitizenMainActivity", "Unable to open camera", error);
            Toast.makeText(this, "Camera could not be opened. Try choosing a photo instead.", Toast.LENGTH_LONG).show();
        }
    }

    private void syncMyComplaints() {
        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                ComplaintRepository.getInstance().getComplaintsByUser(user.getUserId(), new RepositoryCallback<List<Complaint>>() {
                    @Override public void onSuccess(List<Complaint> complaints) {
                        CitizenComplaintStore.replaceComplaints(CitizenMainActivity.this, complaints);
                        if (currentScreen == R.layout.activity_citizen_home || currentScreen == R.layout.activity_citizen_my_complaints) {
                            populateLists();
                        }
                    }
                    @Override public void onError(Exception error) {
                        Log.w("CitizenMainActivity", "Could not sync citizen complaints: " + error.getMessage());
                        if (currentScreen == R.layout.activity_citizen_my_complaints) {
                            Toast.makeText(CitizenMainActivity.this, "Firebase could not refresh complaints. Showing saved data.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
            @Override public void onError(Exception error) {
                Log.w("CitizenMainActivity", "Could not load citizen identity: " + error.getMessage());
            }
        });
    }

    private List<Row> getComplaintRows() {
        List<Row> rows = new ArrayList<>();
        for (Complaint complaint : CitizenComplaintStore.getComplaints(this)) {
            ComplaintStatus status = complaint.getStatus() == null ? ComplaintStatus.SUBMITTED : complaint.getStatus();
            String date = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(complaint.getCreatedAt()));
            rows.add(new Row(complaint.getComplaintId(), complaint.getTitle(), date,
                    status.getDisplayName(), complaint.getLocationAddress(), complaint));
        }
        return rows;
    }

    private int selectedStatusFilter() {
        TabLayout tabs = findViewById(R.id.tabComplaints);
        return tabs == null ? 0 : Math.max(0, tabs.getSelectedTabPosition());
    }

    private List<Row> filterRows(List<Row> rows, int filter) {
        List<Row> filtered = new ArrayList<>();
        for (Row row : rows) {
            ComplaintStatus status = row.complaint == null || row.complaint.getStatus() == null
                    ? ComplaintStatus.SUBMITTED : row.complaint.getStatus();
            boolean include = filter == 0
                    || (filter == 1 && (status == ComplaintStatus.SUBMITTED || status == ComplaintStatus.UNDER_REVIEW))
                    || (filter == 2 && (status == ComplaintStatus.ASSIGNED || status == ComplaintStatus.IN_PROGRESS))
                    || (filter == 3 && status == ComplaintStatus.RESOLVED);
            if (include) filtered.add(row);
        }
        return filtered;
    }

    private void setupComplaintTabs(List<Row> rows) {
        TabLayout tabs = findViewById(R.id.tabComplaints);
        if (tabs == null) return;
        int[] counts = new int[4];
        counts[0] = rows.size();
        for (Row row : rows) {
            ComplaintStatus status = row.complaint == null || row.complaint.getStatus() == null
                    ? ComplaintStatus.SUBMITTED : row.complaint.getStatus();
            if (status == ComplaintStatus.SUBMITTED || status == ComplaintStatus.UNDER_REVIEW) counts[1]++;
            if (status == ComplaintStatus.ASSIGNED || status == ComplaintStatus.IN_PROGRESS) counts[2]++;
            if (status == ComplaintStatus.RESOLVED) counts[3]++;
        }
        String[] labels = {"All", "Pending", "In Progress", "Resolved"};
        for (int i = 0; i < Math.min(tabs.getTabCount(), labels.length); i++) {
            TabLayout.Tab tab = tabs.getTabAt(i);
            if (tab != null) tab.setText(labels[i] + " (" + counts[i] + ")");
        }
        tabs.clearOnTabSelectedListeners();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                RecyclerView list = findViewById(R.id.rvMyComplaints);
                List<Row> filtered = filterRows(rows, tab.getPosition());
                if (list != null) setRows(list, filtered, false);
                View empty = findViewById(R.id.tvEmptyComplaints);
                if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setRows(RecyclerView list, List<Row> rows, boolean notifications) {
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new CitizenListAdapter(notifications, rows));
    }

    private void click(int id, Runnable action) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(v -> action.run());
    }

    private void continueToAnalysis() {
        CitizenComplaintStore.Draft draft = readDraftFromForm();
        if (!validateDraft(draft)) return;
        CitizenComplaintStore.saveDraft(this, draft);
        User user = getCurrentProfile();
        if (!isRegistrationComplete(user)) {
            Toast.makeText(this, "Pehla profile registration na badha required fields bharo.", Toast.LENGTH_LONG).show();
            open(R.layout.activity_citizen_profile_settings);
            return;
        }
        open(R.layout.activity_citizen_ai_analysis);
    }

    private User getCurrentProfile() {
        final User[] current = new User[1];
        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) { current[0] = user; }
            @Override public void onError(Exception ignored) { }
        });
        return current[0];
    }

    private boolean isRegistrationComplete(User user) {
        return user != null
                && !TextUtils.isEmpty(user.getName())
                && user.getName().trim().length() >= 2
                && !TextUtils.isEmpty(user.getEmail())
                && Patterns.EMAIL_ADDRESS.matcher(user.getEmail().trim()).matches()
                && !TextUtils.isEmpty(user.getPhone())
                && user.getPhone().replaceAll("\\D", "").length() >= 10;
    }

    private CitizenComplaintStore.Draft readDraftFromForm() {
        CitizenComplaintStore.Draft draft = new CitizenComplaintStore.Draft();
        EditText title = findViewById(R.id.etComplaintTitle);
        EditText description = findViewById(R.id.etDescription);
        EditText address = findViewById(R.id.etLocationAddress);
        ChipGroup categories = findViewById(R.id.chipGroupCategory);
        Chip category = categories == null ? null : findViewById(categories.getCheckedChipId());
        draft.title = title == null || title.getText() == null ? "" : title.getText().toString().trim();
        draft.description = description == null || description.getText() == null ? "" : description.getText().toString().trim();
        draft.category = category == null ? "" : category.getText().toString();
        draft.locationAddress = address == null || address.getText() == null ? "" : address.getText().toString().trim();
        draft.imageUri = selectedImageUri;
        draft.latitude = selectedLatitude;
        draft.longitude = selectedLongitude;
        return draft;
    }

    private boolean validateDraft(CitizenComplaintStore.Draft draft) {
        EditText title = findViewById(R.id.etComplaintTitle);
        EditText description = findViewById(R.id.etDescription);
        if (TextUtils.isEmpty(draft.title) || draft.title.length() < 3) {
            if (title != null) title.setError("Enter a title with at least 3 characters");
            return false;
        }
        if (TextUtils.isEmpty(draft.description) || draft.description.length() < 20) {
            if (description != null) description.setError("Describe the issue in at least 20 characters");
            return false;
        }
        if (TextUtils.isEmpty(draft.category)) {
            Toast.makeText(this, "Choose a complaint category", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(draft.locationAddress) && draft.latitude == null) {
            Toast.makeText(this, "Add an address or select your current location", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void saveDraftFromForm() {
        CitizenComplaintStore.saveDraft(this, readDraftFromForm());
        Toast.makeText(this, "Draft saved on this device", Toast.LENGTH_SHORT).show();
    }

    private void restoreDraftToForm() {
        EditText addressField = findViewById(R.id.etLocationAddress);
        if (addressField != null) {
            addressField.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateLocationPreview(); }
                @Override public void afterTextChanged(Editable editable) { }
            });
        }
        CitizenComplaintStore.Draft draft = CitizenComplaintStore.getDraft(this);
        if (draft == null) return;
        setText(R.id.etComplaintTitle, draft.title);
        setText(R.id.etDescription, draft.description);
        setText(R.id.etLocationAddress, draft.locationAddress);
        selectedImageUri = draft.imageUri == null ? "" : draft.imageUri;
        selectedLatitude = draft.latitude;
        selectedLongitude = draft.longitude;
        ChipGroup categories = findViewById(R.id.chipGroupCategory);
        if (categories != null && !TextUtils.isEmpty(draft.category)) {
            for (int i = 0; i < categories.getChildCount(); i++) {
                View child = categories.getChildAt(i);
                if (child instanceof Chip && draft.category.contentEquals(((Chip) child).getText())) {
                    categories.check(child.getId());
                    break;
                }
            }
        }
        updateLocationPreview();
        updatePhotoPreview();
    }

    private void setText(int id, String value) {
        EditText field = findViewById(id);
        if (field != null) field.setText(value == null ? "" : value);
    }

    private void requestCurrentLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) readCurrentLocation();
        else locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @SuppressLint("MissingPermission")
    private void readCurrentLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return;
        CancellationTokenSource cancellation = new CancellationTokenSource();
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(this, "Could not get location. Enter an address manually.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    selectedLatitude = location.getLatitude();
                    selectedLongitude = location.getLongitude();
                    updateLocationPreview();
                    Toast.makeText(this, "Current location added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Location unavailable. Enter an address manually.", Toast.LENGTH_LONG).show());
    }

    private void updateLocationPreview() {
        TextView location = findViewById(R.id.tvSelectedLocation);
        TextView coordinates = findViewById(R.id.tvSelectedCoordinates);
        EditText address = findViewById(R.id.etLocationAddress);
        String manual = address == null || address.getText() == null ? "" : address.getText().toString().trim();
        if (location != null) location.setText(!TextUtils.isEmpty(manual) ? manual
                : selectedLatitude == null ? "No location selected" : "Current device location selected");
        if (coordinates != null) coordinates.setText(selectedLatitude == null ? "Add an address or use current location"
                : String.format(Locale.getDefault(), "Lat %.5f, Long %.5f", selectedLatitude, selectedLongitude));
    }

    private void updatePhotoPreview() {
        ImageView preview = findViewById(R.id.imgSelectedEvidence);
        TextView photoCount = findViewById(R.id.tvPhotoCount);
        boolean hasPhoto = !TextUtils.isEmpty(selectedImageUri);
        if (preview != null) {
            preview.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
            if (hasPhoto) preview.setImageURI(Uri.parse(selectedImageUri));
        }
        if (photoCount != null) photoCount.setText(hasPhoto ? "Evidence photo attached" : "No evidence photo attached");
    }

    private void bindAnalysisPreview() {
        CitizenComplaintStore.Draft draft = CitizenComplaintStore.getDraft(this);
        TextView title = findViewById(R.id.tvAnalysisTitle);
        if (title != null && draft != null) title.setText(draft.title);
        currentAiAnalysisResult = null;
        setAnalyzeSubmitEnabled(false);
        TextView status = findViewById(R.id.tvAiStatus);
        if (status != null) status.setText("Gemini AI is analyzing your report…");
        if (draft == null) return;
        Complaint input = new Complaint();
        input.setTitle(draft.title);
        input.setDescription(draft.description);
        input.setCategory(draft.category);
        AiAnalysisService.getInstance().analyzeComplaint(input, new AiAnalysisCallback() {
            @Override public void onAnalysisComplete(AiAnalysisResult result) {
                if (currentScreen != R.layout.activity_citizen_ai_analysis) return;
                currentAiAnalysisResult = result;
                bindAiAnalysisResult(result);
                setAnalyzeSubmitEnabled(true);
            }

            @Override public void onAnalysisError(String errorMessage, boolean isRetryable) {
                if (currentScreen != R.layout.activity_citizen_ai_analysis) return;
                TextView currentStatus = findViewById(R.id.tvAiStatus);
                if (currentStatus != null) currentStatus.setText("AI unavailable • You can still submit for human review");
                setAnalyzeSubmitEnabled(true);
                Toast.makeText(CitizenMainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindAiAnalysisResult(AiAnalysisResult result) {
        setLabel(R.id.tvAiStatus, "Gemini AI triage complete • recommendation only");
        setLabel(R.id.tvAiPriority, "Suggested priority: " + result.getPriority());
        setLabel(R.id.tvAiSeverity, result.getSeverity());
        setLabel(R.id.tvAiUrgency, result.getUrgency());
        setLabel(R.id.tvAiSafetyRisk, result.getSafetyRisk());
        setLabel(R.id.tvAiAffectedPeople, result.getAffectedPeople() > 0
                ? String.valueOf(result.getAffectedPeople()) : "Unknown");
        setLabel(R.id.tvAiReason, result.getReason());
        setLabel(R.id.tvAiDepartment, result.getSuggestedDepartment());
        setLabel(R.id.tvAiAction, result.getSuggestedAction());
    }

    private void setLabel(int id, String value) {
        TextView view = findViewById(id);
        if (view != null) view.setText(value == null ? "" : value);
    }

    private void setAnalyzeSubmitEnabled(boolean enabled) {
        com.google.android.material.button.MaterialButton submit = findViewById(R.id.btnSubmitFinal);
        if (submit != null) {
            submit.setEnabled(enabled);
            submit.setText(enabled ? "Submit Complaint" : "Analyzing…");
        }
    }

    private void submitComplaint() {
        CitizenComplaintStore.Draft draft = CitizenComplaintStore.getDraft(this);
        if (draft == null || TextUtils.isEmpty(draft.title) || TextUtils.isEmpty(draft.description)) {
            Toast.makeText(this, "Go back and complete the complaint details first", Toast.LENGTH_SHORT).show();
            return;
        }
        User submittingUser = getCurrentProfile();
        if (!isRegistrationComplete(submittingUser)) {
            Toast.makeText(this, "Complaint submit karta pehla registration fields complete karo.", Toast.LENGTH_LONG).show();
            open(R.layout.activity_citizen_profile_settings);
            return;
        }
        Complaint complaint = new Complaint();
        complaint.setComplaintId(createTicketId());
        complaint.setUserId(submittingUser.getUserId());
        complaint.setCitizenName(submittingUser.getName());
        complaint.setTitle(draft.title);
        complaint.setDescription(draft.description);
        complaint.setCategory(draft.category);
        complaint.setLocationAddress(displayLocation(draft));
        if (draft.latitude != null) complaint.setLatitude(draft.latitude);
        if (draft.longitude != null) complaint.setLongitude(draft.longitude);
        String localImageUri = draft.imageUri;
        if (currentAiAnalysisResult != null) {
            complaint.setAiCategory(currentAiAnalysisResult.getCategory());
            complaint.setAiSummary(currentAiAnalysisResult.getSummary());
            complaint.setAiSeverity(currentAiAnalysisResult.getSeverity());
            complaint.setAiUrgency(currentAiAnalysisResult.getUrgency());
            complaint.setAiSafetyRisk(currentAiAnalysisResult.getSafetyRisk());
            complaint.setAiAffectedPeople(currentAiAnalysisResult.getAffectedPeople());
            complaint.setAiPriority(currentAiAnalysisResult.getPriority());
            complaint.setAiReason(currentAiAnalysisResult.getReason());
            complaint.setSuggestedDepartment(currentAiAnalysisResult.getSuggestedDepartment());
            complaint.setSuggestedAction(currentAiAnalysisResult.getSuggestedAction());
            complaint.setAiConfidence(currentAiAnalysisResult.getConfidence());
        }
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setCreatedAt(System.currentTimeMillis());
        complaint.setUpdatedAt(complaint.getCreatedAt());
        CitizenComplaintStore.saveComplaint(this, complaint);
        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                UserRepository.getInstance().saveUser(user, new RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) { }
                    @Override public void onError(Exception error) { Log.w("CitizenMainActivity", "Profile sync failed: " + error.getMessage()); }
                });
            }
            @Override public void onError(Exception ignored) { }
        });
        currentComplaintId = complaint.getComplaintId();
        showScreen(R.layout.activity_citizen_complaint_submitted, true);
        if (TextUtils.isEmpty(localImageUri)) {
            persistComplaint(complaint);
        } else {
            FirestoreHelper.uploadComplaintImage(Uri.parse(localImageUri), complaint.getComplaintId(), new RepositoryCallback<String>() {
                @Override public void onSuccess(String downloadUrl) {
                    complaint.setImageUrl(downloadUrl);
                    CitizenComplaintStore.saveComplaint(CitizenMainActivity.this, complaint);
                    persistComplaint(complaint);
                }
                @Override public void onError(Exception error) {
                    Log.w("CitizenMainActivity", "Evidence upload failed: " + error.getMessage());
                    complaint.setImageUrl(null);
                    CitizenComplaintStore.saveComplaint(CitizenMainActivity.this, complaint);
                    Toast.makeText(CitizenMainActivity.this, "Photo upload failed; submitting complaint without photo.", Toast.LENGTH_LONG).show();
                    persistComplaint(complaint);
                }
            });
        }
    }

    private void persistComplaint(Complaint complaint) {
        ComplaintRepository.getInstance().submitComplaint(complaint, new RepositoryCallback<String>() {
            @Override public void onSuccess(String result) {
                CitizenComplaintStore.clearDraft(CitizenMainActivity.this);
                Log.d("CitizenMainActivity", "Complaint synced to Firestore: " + result);
                Toast.makeText(CitizenMainActivity.this, "Complaint saved to Firebase.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(Exception error) {
                Log.w("CitizenMainActivity", "Firestore sync failed; local copy preserved: " + error.getMessage());
                Toast.makeText(CitizenMainActivity.this, "Firebase save failed. Your draft is kept; check connection and retry.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindProfile() {
        UserRepository.getInstance().refreshCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                TextInputEditText name = findViewById(R.id.editProfileName);
                TextInputEditText email = findViewById(R.id.editProfileEmail);
                TextInputEditText phone = findViewById(R.id.editProfilePhone);
                if (name != null) name.setText(user.getName());
                if (email != null) email.setText(user.getEmail());
                if (phone != null) phone.setText(user.getPhone());
            }
            @Override public void onError(Exception error) { Log.w("CitizenMainActivity", "Profile load failed: " + error.getMessage()); }
        });
    }

    private void saveProfile() {
        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                TextInputEditText name = findViewById(R.id.editProfileName);
                TextInputEditText email = findViewById(R.id.editProfileEmail);
                TextInputEditText phone = findViewById(R.id.editProfilePhone);
                user.setName(name == null ? "" : String.valueOf(name.getText()).trim());
                user.setEmail(email == null ? "" : String.valueOf(email.getText()).trim());
                user.setPhone(phone == null ? "" : String.valueOf(phone.getText()).trim());
                if (TextUtils.isEmpty(user.getName()) || user.getName().length() < 2) {
                    if (name != null) name.setError("Name is required (at least 2 characters)");
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(user.getEmail()).matches()) {
                    if (email != null) email.setError("Enter a valid email address");
                    return;
                }
                if (user.getPhone().replaceAll("\\D", "").length() < 10) {
                    if (phone != null) phone.setError("Enter a valid phone number (at least 10 digits)");
                    return;
                }
                UserRepository.getInstance().saveUser(user, new RepositoryCallback<Void>() {
                    @Override public void onSuccess(Void result) {
                        Toast.makeText(CitizenMainActivity.this, "Profile saved to Firebase", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(Exception error) {
                        Log.w("CitizenMainActivity", "Profile save failed: " + error.getMessage());
                        Toast.makeText(CitizenMainActivity.this, "Profile could not sync. Check Firebase connection.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onError(Exception error) {
                Toast.makeText(CitizenMainActivity.this, "Profile could not be loaded", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String createTicketId() {
        String year = new SimpleDateFormat("yyyy", Locale.US).format(new Date());
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.US);
        return "CIV-" + year + "-" + suffix;
    }

    private String displayLocation(CitizenComplaintStore.Draft draft) {
        if (!TextUtils.isEmpty(draft.locationAddress)) return draft.locationAddress;
        if (draft.latitude != null && draft.longitude != null) {
            return String.format(Locale.getDefault(), "Lat %.5f, Long %.5f", draft.latitude, draft.longitude);
        }
        return "Location not provided";
    }

    private void bindSubmissionConfirmation() {
        Complaint complaint = CitizenComplaintStore.findComplaint(this, currentComplaintId);
        if (complaint == null) return;
        text(findViewById(android.R.id.content), R.id.tvTicketId, complaint.getComplaintId());
        text(findViewById(android.R.id.content), R.id.tvSubmittedTitle, complaint.getTitle());
        text(findViewById(android.R.id.content), R.id.tvSubmittedLocation, "📍 " + complaint.getLocationAddress());
    }

    private void copyTicketId() {
        Complaint complaint = CitizenComplaintStore.findComplaint(this, currentComplaintId);
        if (complaint == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Complaint ticket", complaint.getComplaintId()));
        Toast.makeText(this, "Ticket ID copied", Toast.LENGTH_SHORT).show();
    }

    private void openComplaintDetails(String complaintId) {
        if (TextUtils.isEmpty(complaintId)) {
            Toast.makeText(this, "Select a complaint to track.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentComplaintId = complaintId;
        open(R.layout.activity_citizen_complaint_details);

        // Phase 4: Retrieve latest version from Firestore via ComplaintRepository
        ComplaintRepository.getInstance().getComplaintById(complaintId, new RepositoryCallback<Complaint>() {
            @Override
            public void onSuccess(Complaint remoteComplaint) {
                if (remoteComplaint != null) {
                    CitizenComplaintStore.saveComplaint(CitizenMainActivity.this, remoteComplaint);
                    if (!isFinishing() && !isDestroyed()
                            && currentScreen == R.layout.activity_citizen_complaint_details
                            && complaintId.equals(currentComplaintId)) {
                        bindComplaintDetails();
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.w("CitizenMainActivity", "Firebase status refresh failed: " + exception.getMessage());
                if (currentScreen == R.layout.activity_citizen_complaint_details
                        && complaintId.equals(currentComplaintId)) {
                    Complaint cached = CitizenComplaintStore.findComplaint(CitizenMainActivity.this, complaintId);
                    setLabel(R.id.tvLiveStatusMessage, cached == null
                            ? "Could not load complaint status from Firebase. Check your connection and retry."
                            : "Firebase refresh failed. Showing saved status: " + statusName(cached));
                    Toast.makeText(CitizenMainActivity.this, "Could not refresh complaint status from Firebase.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bindComplaintDetails() {
        Complaint complaint = CitizenComplaintStore.findComplaint(this, currentComplaintId);
        View root = findViewById(android.R.id.content);
        if (complaint == null) {
            text(root, R.id.tvDetailTicket, "Loading complaint from Firebase…");
            text(root, R.id.tvDetailTitle, "");
            text(root, R.id.tvLiveStatusMessage, "Fetching the latest complaint status…");
            text(root, R.id.tvDetailLastUpdated, "");
            ImageView evidence = findViewById(R.id.imgEvidence);
            if (evidence != null) evidence.setVisibility(View.GONE);
            return;
        }
        ComplaintStatus status = complaint.getStatus() == null ? ComplaintStatus.SUBMITTED : complaint.getStatus();
        text(root, R.id.tvDetailTicket, complaint.getComplaintId() + " • " + status.getDisplayName());
        text(root, R.id.tvDetailTitle, complaint.getTitle());

        String locationDetails = "📍 " + complaint.getLocationAddress();
        if (!TextUtils.isEmpty(complaint.getAssignedDepartment())) {
            locationDetails += "\nDept: " + complaint.getAssignedDepartment();
        }
        if (!TextUtils.isEmpty(complaint.getOfficialDecision())) {
            locationDetails += "\nOfficial Decision: " + complaint.getOfficialDecision();
        }
        text(root, R.id.tvDetailLocation, locationDetails);
        text(root, R.id.tvLiveStatusMessage, "Current status: " + status.getDisplayName());
        RatingBar feedbackRating = findViewById(R.id.ratingComplaintFeedback);
        EditText feedbackInput = findViewById(R.id.editComplaintFeedback);
        if (complaint.getFeedbackSubmittedAt() > 0) {
            if (feedbackRating != null) feedbackRating.setRating(complaint.getCitizenFeedbackRating());
            if (feedbackInput != null) feedbackInput.setText(complaint.getCitizenFeedback());
            com.google.android.material.button.MaterialButton feedbackButton = findViewById(R.id.btnSendFeedback);
            if (feedbackButton != null) feedbackButton.setText("Update Feedback");
        }
        long lastUpdated = complaint.getUpdatedAt() > 0 ? complaint.getUpdatedAt() : complaint.getCreatedAt();
        String updatedText = lastUpdated > 0
                ? "Last updated " + new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(new Date(lastUpdated))
                : "Update time not available";
        text(root, R.id.tvDetailLastUpdated, updatedText);

        ImageView image = findViewById(R.id.imgEvidence);
        if (image != null) {
            if (!TextUtils.isEmpty(complaint.getImageUrl())) {
                image.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(this).load(complaint.getImageUrl()).into(image);
            } else {
                image.setVisibility(View.GONE);
            }
        }
    }

    private String statusName(Complaint complaint) {
        ComplaintStatus status = complaint.getStatus() == null ? ComplaintStatus.SUBMITTED : complaint.getStatus();
        return status.getDisplayName();
    }

    private void sendComplaintFeedback() {
        if (TextUtils.isEmpty(currentComplaintId)) {
            Toast.makeText(this, "Open a complaint before sending feedback.", Toast.LENGTH_SHORT).show();
            return;
        }
        String complaintId = currentComplaintId;
        RatingBar ratingBar = findViewById(R.id.ratingComplaintFeedback);
        EditText feedbackInput = findViewById(R.id.editComplaintFeedback);
        View sendButton = findViewById(R.id.btnSendFeedback);
        int rating = ratingBar == null ? 0 : Math.round(ratingBar.getRating());
        String message = feedbackInput == null || feedbackInput.getText() == null
                ? "" : feedbackInput.getText().toString().trim();
        if (rating < 1 || rating > 5) {
            Toast.makeText(this, "Please choose a rating from 1 to 5 stars.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sendButton != null) sendButton.setEnabled(false);
        UserRepository.getInstance().getCurrentUser(new RepositoryCallback<User>() {
            @Override public void onSuccess(User user) {
                FirestoreHelper.submitComplaintFeedback(complaintId, user.getUserId(), rating, message,
                        new RepositoryCallback<Void>() {
                            @Override public void onSuccess(Void ignored) {
                                Complaint saved = CitizenComplaintStore.findComplaint(CitizenMainActivity.this, complaintId);
                                if (saved != null) {
                                    saved.setCitizenFeedback(message);
                                    saved.setCitizenFeedbackRating(rating);
                                    saved.setFeedbackSubmittedAt(System.currentTimeMillis());
                                    CitizenComplaintStore.saveComplaint(CitizenMainActivity.this, saved);
                                }
                                if (sendButton != null) {
                                    sendButton.setEnabled(true);
                                    if (sendButton instanceof com.google.android.material.button.MaterialButton) {
                                        ((com.google.android.material.button.MaterialButton) sendButton).setText("Feedback Sent");
                                    }
                                }
                                if (feedbackInput != null) feedbackInput.setText("");
                                Toast.makeText(CitizenMainActivity.this, "Feedback sent successfully.", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(Exception error) {
                                Log.e("CitizenMainActivity", "Feedback save failed", error);
                                if (sendButton != null) sendButton.setEnabled(true);
                                Toast.makeText(CitizenMainActivity.this, "Feedback failed: " + (error.getMessage() == null ? "Check Firebase connection and rules." : error.getMessage()), Toast.LENGTH_LONG).show();
                            }
                        });
            }
            @Override public void onError(Exception error) {
                if (sendButton != null) sendButton.setEnabled(true);
                Toast.makeText(CitizenMainActivity.this, "Citizen profile could not be loaded.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void open(int layout) { showScreen(layout, true); }

    private void goBack() {
        if (screenHistory.isEmpty()) {
            if (currentScreen != R.layout.activity_citizen_home) showScreen(R.layout.activity_citizen_home, false);
            else finish();
        } else {
            showScreen(screenHistory.pop(), false);
        }
    }

    private static void text(View root, int id, String value) {
        if (root == null) return;
        TextView view = root.findViewById(id);
        if (view != null) view.setText(value);
    }

    private static final class Row {
        final String title;
        final String detail;
        final String time;
        final String status;
        final String location;
        final Complaint complaint;

        Row(String title, String detail, String time, String status) {
            this(title, detail, time, status, "", null);
        }

        Row(String title, String detail, String time, String status, String location, Complaint complaint) {
            this.title = title;
            this.detail = detail;
            this.time = time;
            this.status = status;
            this.location = location == null ? "" : location;
            this.complaint = complaint;
        }
    }

    private final class CitizenListAdapter extends RecyclerView.Adapter<CitizenListAdapter.RowHolder> {
        private final boolean notifications;
        private final List<Row> rows;

        CitizenListAdapter(boolean notifications, List<Row> rows) {
            this.notifications = notifications;
            this.rows = rows;
        }

        @NonNull @Override public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = notifications ? R.layout.item_citizen_notification_card : R.layout.item_citizen_complaint_card;
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new RowHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull RowHolder holder, int position) {
            Row row = rows.get(position);
            if (notifications) {
                text(holder.itemView, R.id.tvTitle, row.title);
                text(holder.itemView, R.id.tvMessage, row.detail);
                text(holder.itemView, R.id.tvTimestamp, row.time);
            } else {
                text(holder.itemView, R.id.tvTicketId, row.title);
                text(holder.itemView, R.id.tvTitle, row.detail);
                text(holder.itemView, R.id.tvTimestamp, "• " + row.time);
                text(holder.itemView, R.id.tvPriorityBadge, "● " + row.status);
                text(holder.itemView, R.id.tvLocation, row.location);
                TextView badge = holder.itemView.findViewById(R.id.tvPriorityBadge);
                if (badge != null) badge.setTextColor(ContextCompat.getColor(CitizenMainActivity.this, R.color.civic_primary));
            }
            holder.itemView.setOnClickListener(v -> {
                if (notifications) open(R.layout.activity_citizen_notification_details);
                else if (row.complaint != null) openComplaintDetails(row.complaint.getComplaintId());
            });
        }

        @Override public int getItemCount() { return rows.size(); }

        final class RowHolder extends RecyclerView.ViewHolder {
            RowHolder(@NonNull View itemView) { super(itemView); }
        }
    }
}
