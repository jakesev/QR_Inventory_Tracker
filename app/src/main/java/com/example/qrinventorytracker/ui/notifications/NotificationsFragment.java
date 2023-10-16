package com.example.qrinventorytracker.ui.notifications;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.qrinventorytracker.R;
import com.example.qrinventorytracker.databinding.FragmentDashboardBinding;
import com.example.qrinventorytracker.databinding.FragmentNotificationsBinding;
import com.example.qrinventorytracker.ui.dashboard.ExpandableAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private static final String TAG = "NotificationFragment";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private HashMap<String, Integer> scannedCodes = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://rug-in-a-box-inventory-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference myRef = database.getReference();

        //Open the Scanner for QR Code
        binding.btnScanCode.setOnClickListener(v -> openScanner());

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                updateTableFromFirebase(dataSnapshot); // Call the method here
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle any errors here
            }
        });
        return root;
    }

    private void updateTableFromFirebase(DataSnapshot dataSnapshot) {
        if (binding == null) {
            Log.e(TAG, "Binding is null. Fragment view might be destroyed.");
            return;
        }
        List<String> groupList = new ArrayList<>();
        HashMap<String, List<String>> childList = new HashMap<>();

        try {
            DataSnapshot rugsSnapshot = dataSnapshot.child("rugs");

            for (DataSnapshot rugSnapshot : rugsSnapshot.getChildren()) {
                String skuAndSize = rugSnapshot.getKey();
                Long returnedQuantityLong = rugSnapshot.child("returnedQuantity").getValue(Long.class);
                long returnedQuantity = (returnedQuantityLong != null) ? returnedQuantityLong : 0;

                if (returnedQuantity > 0) { // Check if there are returned items
                    String groupKey = skuAndSize + " | " + returnedQuantity; // Combining SKU&SIZE with returned QTY
                    groupList.add(groupKey);

                    List<String> uniqueCodes = new ArrayList<>();
                    DataSnapshot uniqueSnapshot = rugSnapshot.child("unique");
                    for (DataSnapshot uniqueCodeSnapshot : uniqueSnapshot.getChildren()) {
                        try {
                            String status = uniqueCodeSnapshot.child("status").getValue(String.class);
                            if ("returned".equals(status)) { // Check if the status is "returned"
                                String uniqueCode = uniqueCodeSnapshot.child("uniqueCode").getValue(String.class);
                                uniqueCodes.add(uniqueCode);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing unique code snapshot: " + uniqueCodeSnapshot, e);
                        }
                    }

                    childList.put(groupKey, uniqueCodes);
                }
            }

            // Set up the ExpandableListView with the filtered data
            ExpandableAdapter adapter = new ExpandableAdapter(getContext(), groupList, childList);
            ExpandableListView expandableListView = getView().findViewById(R.id.expandableListView);
            expandableListView.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "An error occurred while processing the data snapshot: ", e);
            if (isAdded()) {
                Toast.makeText(requireContext(), "Error updating table", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void updateTable(String sku) {


    }

    private void openScanner() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setOrientationLocked(false); // Allow portrait orientation
            integrator.setPrompt("Scan a QR code");
            integrator.initiateScan();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getActivity(), "Scan Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String scannedCode = result.getContents();
                checkCodeInDatabase(scannedCode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    private void checkCodeInDatabase(String scannedCode) {
        int uIndex;
        String designCode;

        if (scannedCode.contains("(BLUE)")) {
            uIndex = scannedCode.indexOf('U', scannedCode.indexOf("(BLUE)") + 6);
            designCode = scannedCode.substring(0, uIndex);
        } else {
            uIndex = scannedCode.indexOf('U');
            designCode = scannedCode.substring(0, uIndex);
        }

        int sIndex = scannedCode.indexOf('S');
        String uniqueCode = scannedCode.substring(uIndex + 1, sIndex); // Exclude 'U'
        String size = scannedCode.substring(sIndex); // Include the 'S' in the size
        String mergedCode = designCode + size;

        Log.d(TAG, "designCode: " + designCode);
        Log.d(TAG, "uniqueCode: " + uniqueCode);
        Log.d(TAG, "size: " + size);
        Log.d(TAG, "mergedCode: " + mergedCode);

        DatabaseReference myRef = FirebaseDatabase.getInstance("https://rug-in-a-box-inventory-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("rugs").child(mergedCode);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Design code found: " + mergedCode);
                    DataSnapshot uniqueSnapshot = dataSnapshot.child("unique").child("U" + uniqueCode);
                    if (uniqueSnapshot.exists()) {
                        String status = uniqueSnapshot.child("status").getValue(String.class);
                        if ("sold".equals(status)){
                            // Update status to sold
                            uniqueSnapshot.getRef().child("status").setValue("returned");
                            // Increase returnedQuantity by 1
                            int returnedQuantity = dataSnapshot.child("returnedQuantity").getValue(Integer.class);
                            returnedQuantity++;
                            dataSnapshot.getRef().child("returnedQuantity").setValue(returnedQuantity);

                            // Decrease soldQuantity by 1
                            int soldQuantity = dataSnapshot.child("soldQuantity").getValue(Integer.class);
                            soldQuantity--;
                            dataSnapshot.getRef().child("soldQuantity").setValue(soldQuantity);
                            showToast("Item has been returned: " + mergedCode, true); // true for red background

                        } else if ("inventory".equals(status)) {
                            Toast.makeText(getActivity(), "Item is already in inventory: " + mergedCode, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), "Stock is not in inventory: " + mergedCode, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Item is not in inventory: " + mergedCode, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Item is not in inventory: " + mergedCode, Toast.LENGTH_LONG).show();
                }
            }
            private void showToast(String message, boolean isRedBackground) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) getActivity().findViewById(R.id.custom_toast_container));

                TextView text = layout.findViewById(R.id.custom_toast_message);
                text.setText(message);

                if (isRedBackground) {
                    layout.setBackgroundResource(R.color.red);
                    text.setTextColor(Color.WHITE);
                }

                Toast toast = new Toast(getActivity());
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error querying database", error.toException());
            }
        });
    }

}


