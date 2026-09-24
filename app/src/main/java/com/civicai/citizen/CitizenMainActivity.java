package com.civicai.citizen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import com.civicai.R;
import com.civicai.model.Complaint;
import com.civicai.model.ComplaintStatus;
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output);
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
        wireToolbar();
        wireBottomNavigation();
        wireActions();
        if (layout == R.layout.activity_citizen_report_complaint) restoreDraftToForm();
        if (layout == R.layout.activity_citizen_ai_analysis) bindAnalysisPreview();
        if (layout == R.layout.activity_citizen_complaint_submitted) bindSubmissionConfirmation();
        if (layout == R.layout.activity_citizen_complaint_details) bindComplaintDetails();
        populateLists();
    }

    private void wireToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> goBack());
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
        click(R.id.btnTrackComplaint, () -> openComplaintDetails(currentComplaintId));
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
        click(R.id.btnCamera, () -> cameraPicker.launch(null));
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
        if (notifications != null) setRows(notifications, Arrays.asList(
                new Row("Complaint status updated", "Your report status will appear here when updated.", "", ""),
                new Row("Civic alerts", "Local civic updates will appear here.", "", "")), true);
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
        open(R.layout.activity_citizen_ai_analysis);
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
    }

    private void submitComplaint() {
        CitizenComplaintStore.Draft draft = CitizenComplaintStore.getDraft(this);
        if (draft == null || TextUtils.isEmpty(draft.title) || TextUtils.isEmpty(draft.description)) {
            Toast.makeText(this, "Go back and complete the complaint details first", Toast.LENGTH_SHORT).show();
            return;
        }
        Complaint complaint = new Complaint();
        complaint.setComplaintId(createTicketId());
        complaint.setUserId("local-citizen");
        complaint.setCitizenName("Citizen");
        complaint.setTitle(draft.title);
        complaint.setDescription(draft.description);
        complaint.setCategory(draft.category);
        complaint.setLocationAddress(displayLocation(draft));
        if (draft.latitude != null) complaint.setLatitude(draft.latitude);
        if (draft.longitude != null) complaint.setLongitude(draft.longitude);
        if (!TextUtils.isEmpty(draft.imageUri)) complaint.setImageUrl(draft.imageUri);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setCreatedAt(System.currentTimeMillis());
        complaint.setUpdatedAt(complaint.getCreatedAt());
        CitizenComplaintStore.saveComplaint(this, complaint);
        CitizenComplaintStore.clearDraft(this);
        currentComplaintId = complaint.getComplaintId();
        showScreen(R.layout.activity_citizen_complaint_submitted, true);
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
        if (CitizenComplaintStore.findComplaint(this, complaintId) == null) {
            Toast.makeText(this, "Submit a complaint to start tracking it", Toast.LENGTH_SHORT).show();
            return;
        }
        currentComplaintId = complaintId;
        open(R.layout.activity_citizen_complaint_details);
    }

    private void bindComplaintDetails() {
        Complaint complaint = CitizenComplaintStore.findComplaint(this, currentComplaintId);
        if (complaint == null) return;
        View root = findViewById(android.R.id.content);
        ComplaintStatus status = complaint.getStatus() == null ? ComplaintStatus.SUBMITTED : complaint.getStatus();
        text(root, R.id.tvDetailTicket, complaint.getComplaintId() + " • " + status.getDisplayName());
        text(root, R.id.tvDetailTitle, complaint.getTitle());
        text(root, R.id.tvDetailLocation, "📍 " + complaint.getLocationAddress());
        ImageView image = findViewById(R.id.imgEvidence);
        if (image != null && !TextUtils.isEmpty(complaint.getImageUrl())) image.setImageURI(Uri.parse(complaint.getImageUrl()));
        TextView progress = findViewById(R.id.tvProgressTitle);
        if (progress != null) progress.setText("Resolution Progress • " + status.getDisplayName());
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
