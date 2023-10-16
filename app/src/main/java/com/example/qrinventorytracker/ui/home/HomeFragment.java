package com.example.qrinventorytracker.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.qrinventorytracker.R;
import com.example.qrinventorytracker.databinding.FragmentDashboardBinding;
import com.example.qrinventorytracker.databinding.FragmentHomeBinding;
import com.example.qrinventorytracker.ui.dashboard.ExpandableAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private static final String TAG = "HomeFragment";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private HashMap<String, Integer> scannedCodes = new HashMap<>();
    private List<String> groupList = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://rug-in-a-box-inventory-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference myRef = database.getReference();

        //Open the Scanner for QR Code
        binding.btnScanCode.setOnClickListener(v -> openScanner());

        // Add listener to update the table from Firebase
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Data from Firebase: " + dataSnapshot.toString());
                updateTableFromFirebase(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle any errors here
                Log.w(TAG, "Failed to read value.", error.toException());
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
            // Accessing the "rugs" node directly from the snapshot
            DataSnapshot rugsSnapshot = dataSnapshot.child("rugs");

            for (DataSnapshot rugSnapshot : rugsSnapshot.getChildren()) {

                String skuAndSize = rugSnapshot.getKey();
                Long quantityLong = rugSnapshot.child("availableInventoryQuantity").getValue(Long.class);
                long quantity = (quantityLong != null) ? quantityLong : 0;

                Long quantityReturnedLong = rugSnapshot.child("returnedQuantity").getValue(Long.class);
                long quantityReturned = (quantityReturnedLong != null) ? quantityReturnedLong : 0;

                String groupKey = skuAndSize + " | " + (quantity + quantityReturned); // Combining SKU&SIZE with QTY
                groupList.add(groupKey);

                List<String> uniqueCodes = new ArrayList<>();
                DataSnapshot uniqueSnapshot = rugSnapshot.child("unique");
                for (DataSnapshot uniqueCodeSnapshot : uniqueSnapshot.getChildren()) {
                    try {
                        String status = uniqueCodeSnapshot.child("status").getValue(String.class);
                        if ("inventory".equals(status) || "returned".equals(status)) {
                            String uniqueCode = uniqueCodeSnapshot.child("uniqueCode").getValue(String.class);
                            uniqueCodes.add(uniqueCode);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing unique code snapshot: " + uniqueCodeSnapshot, e);
                    }


                }

                childList.put(groupKey, uniqueCodes);
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred while processing the data snapshot: ", e);
            // Optionally, you can show an error message to the user
            if (isAdded()) {
                Toast.makeText(requireContext(), "E: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }        }

        try {
            ExpandableAdapter adapter = new ExpandableAdapter(getContext(), groupList, childList);
            ExpandableListView expandableListView = binding.expandableListView;

            if (expandableListView != null) {
                expandableListView.setAdapter(adapter);
            } else {
                Log.e(TAG, "expandableListView is null");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting adapter", e);
            Toast.makeText(getContext(), "E: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }
    private void updateTable(String sku) {
        int qty = scannedCodes.getOrDefault(sku, 0) + 1;
        scannedCodes.put(sku, qty);
        // Check if the row for this SKU already exists

    }

    private void openScanner() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setOrientationLocked(true); // Allow portrait orientation
            integrator.setPrompt("Scan a QR code");
            integrator.initiateScan();
        } else {
            requestCameraPermission();
        }
    }

    private HashMap<String, String> parseCode(String code) {
        int uPosition = code.indexOf('U');
        int sPosition = code.indexOf('S');

        // Check if the code contains "(BLUE)" and adjust the positions accordingly
        String colorCode = "(BLUE)";
        if (code.contains(colorCode)) {
            uPosition = code.indexOf('U', code.indexOf(colorCode) + colorCode.length());
            sPosition = code.indexOf('S', uPosition);
        }

        HashMap<String, String> parsedCode = new HashMap<>();
        parsedCode.put("designCode", code.substring(0, uPosition));
        parsedCode.put("uniqueCode", code.substring(uPosition, sPosition));
        parsedCode.put("size", code.substring(sPosition));
        parsedCode.put("skuAndSize", code.substring(0, uPosition) + code.substring(sPosition));

        return parsedCode;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {Toast.makeText(requireContext(), "Scan Cancelled", Toast.LENGTH_LONG).show();}
            else {
                String scannedCode = result.getContents();
                // Check if the scanned code is in the correct format
                if (isValidQRCode(scannedCode)) {addProductToDatabase(scannedCode);}
                else { Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_LONG).show();}
            }
        } else { super.onActivityResult(requestCode, resultCode, data);}
    }

    private void addProductToDatabase(String code) {
        HashMap<String, String> parsedCode = parseCode(code);
        String skuAndSize = parsedCode.get("skuAndSize");
        String uniqueCode = parsedCode.get("uniqueCode");
        String size = parsedCode.get("size");

        DatabaseReference skuAndSizeRef = FirebaseDatabase.getInstance("https://rug-in-a-box-inventory-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("rugs").child(skuAndSize);

        skuAndSizeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.child("unique").getChildren()) {
                    if (uniqueCode.equals(itemSnapshot.child("uniqueCode").getValue(String.class)) &&
                            size.equals(itemSnapshot.child("size").getValue(String.class))) {
                        // This item already exists in the database, check its status
                        String status = itemSnapshot.child("status").getValue(String.class);
                        if ("sold".equals(status)) {
                            showCustomToast("Item is sold", true);
                        } else {
                            showCustomToast("This item has already been scanned", true);
                        }
                        return;
                    }
                }


                // Adding the status field to the parsedCode map
                parsedCode.put("status", "inventory"); // Assuming the status is "inventory" when adding a new item

                int inventoryQuantity = snapshot.child("availableInventoryQuantity").getValue(Integer.class) != null ? snapshot.child("availableInventoryQuantity").getValue(Integer.class) : 0;
                int orderedQuantity = snapshot.child("orderedQuantity").getValue(Integer.class) != null ? snapshot.child("orderedQuantity").getValue(Integer.class) : 0;
                int soldQuantity = snapshot.child("soldQuantity").getValue(Integer.class) != null ? snapshot.child("soldQuantity").getValue(Integer.class) : 0;
                int returnedQuantity = snapshot.child("returnedQuantity").getValue(Integer.class) != null ? snapshot.child("returnedQuantity").getValue(Integer.class) : 0;

                // Increment inventory and ordered quantities
                inventoryQuantity++;
                orderedQuantity++;

                skuAndSizeRef.child("availableInventoryQuantity").setValue(inventoryQuantity);
                skuAndSizeRef.child("orderedQuantity").setValue(orderedQuantity);
                skuAndSizeRef.child("soldQuantity").setValue(soldQuantity); // No change, but set for completeness
                skuAndSizeRef.child("returnedQuantity").setValue(returnedQuantity); // No change, but set for completeness

                skuAndSizeRef.child("unique").child(uniqueCode).setValue(parsedCode);
                updateTable(skuAndSize);

                // Show the custom toast message for a newly added item
                showCustomToast("Item Added: " + skuAndSize, false);
            }

            private void updateQuantities(String skuAndSize, String newStatus) {
                // Path to the specific rug
                DatabaseReference skuAndSizeRef = FirebaseDatabase.getInstance("https://rug-in-a-box-inventory-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .getReference("rugs").child(skuAndSize);
                // Retrieve the current quantities
                skuAndSizeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int inventoryQuantity = dataSnapshot.child("availableInventoryQuantity").getValue(Integer.class) != null ? dataSnapshot.child("availableInventoryQuantity").getValue(Integer.class) : 0;

                        // Update the quantities based on the new status
                        if ("inventory".equals(newStatus)) {
                            inventoryQuantity++;
                        }

                        // Write the updated quantities back to the database
                        skuAndSizeRef.child("availableInventoryQuantity").setValue(inventoryQuantity);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle error
                    }
                });

            }

            private void showCustomToast(String message, boolean isScanned) {
                LayoutInflater inflater = LayoutInflater.from(requireContext());
                View layout = inflater.inflate(R.layout.custom_toast, null, false);

                LinearLayout custom_toast_container = layout.findViewById(R.id.custom_toast_container);
                if (isScanned) {
                    custom_toast_container.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.orange));
                } else {
                    custom_toast_container.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green));
                }

                TextView text = layout.findViewById(R.id.text);
                text.setText(message); // The message you want to display

                Toast toast = new Toast(requireContext());
                toast.setGravity(Gravity.BOTTOM, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private boolean isValidQRCode(String code) {
        // Check if the code contains "U" followed by "S"
        int indexOfU = code.indexOf('U');
        int indexOfS = code.indexOf('S');

        return indexOfU != -1 && indexOfS != -1 && indexOfU < indexOfS;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}


