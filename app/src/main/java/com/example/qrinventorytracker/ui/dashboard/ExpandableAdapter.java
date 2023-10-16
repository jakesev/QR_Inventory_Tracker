package com.example.qrinventorytracker.ui.dashboard;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.qrinventorytracker.R;

import java.util.HashMap;
import java.util.List;

public class ExpandableAdapter extends BaseExpandableListAdapter {

    private List<String> groupList;
    private HashMap<String, List<String>> childList;
    private Context context;

    public ExpandableAdapter(Context context, List<String> groupList, HashMap<String, List<String>> childList) {
        this.groupList = groupList;
        this.childList = childList;
        this.context = context;
    }

    @Override
    public int getGroupCount() {
        return groupList.size();
    }


    @Override
    public int getChildrenCount(int groupPosition) {
        List<String> children = childList.get(groupList.get(groupPosition));
        return children != null ? children.size() : 0;
    }
    @Override
    public Object getGroup(int groupPosition) {
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childList.get(groupList.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.group_item, parent, false);
        }

        // Get the SKU & Size and Quantity (QTY) for this group
        String groupData = (String) getGroup(groupPosition);
        String[] parts = groupData.split(" \\| "); // Split by " | "
        String skuAndSize = parts[0];
        String quantity = parts[1];


        // Create SpannableString for Quantity and set the blue color
        SpannableString spannableQuantity = new SpannableString(quantity);
        int maroonColor = Color.rgb(128, 0, 0); // RGB values for maroon color

        int blueColor = Color.BLUE; // Blue color
        spannableQuantity.setSpan(new ForegroundColorSpan(blueColor), 0, spannableQuantity.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


        // Finding the index where the size starts (the character 'S')
        int index = skuAndSize.indexOf('S');

        // Create a SpannableString to apply different styles to different parts
        SpannableString spannableString = new SpannableString(skuAndSize);
        int orangeColor = Color.rgb(204, 85, 0); // RGB values for orange color

        spannableString.setSpan(new ForegroundColorSpan(orangeColor), index, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Change Color.BLUE to the desired color

        // Get the corresponding image resource for SKU and size
        int imageResource = getImageResourceForDesignCode(skuAndSize);

// Extract availableInventoryQuantity from the quantity string
        int availableInventoryQuantity = Integer.parseInt(quantity.trim());

        // Determine the background color based on availableInventoryQuantity and position
        int transparentRed = Color.argb(40, 255, 0, 0); // 10% opaque red
        int baseGray = Color.LTGRAY;
        int red = Color.red(baseGray);
        int green = Color.green(baseGray);
        int blue = Color.blue(baseGray);
        int lighterGray = Color.argb(255, (int) (red + (255 - red) * 0.8), (int) (green + (255 - green) * 0.8), (int) (blue + (255 - blue) * 0.8));
        int backgroundColor;

        // Create SpannableString for Quantity and set the blue color
        if (availableInventoryQuantity == 0) {
            spannableQuantity.setSpan(new ForegroundColorSpan(maroonColor), 0, spannableQuantity.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannableQuantity.setSpan(new ForegroundColorSpan(blueColor), 0, spannableQuantity.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        maroonColor = Color.rgb(128, 0, 0); // RGB values for maroon color

        spannableQuantity.setSpan(new ForegroundColorSpan(blueColor), 0, spannableQuantity.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (availableInventoryQuantity == 0) {
            spannableQuantity.setSpan(new ForegroundColorSpan(maroonColor), 0, spannableQuantity.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannableQuantity.setSpan(new ForegroundColorSpan(blueColor), 0, spannableQuantity.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (availableInventoryQuantity == 0) {
            backgroundColor = transparentRed;
        } else {
            backgroundColor = (groupPosition % 2 == 0) ? lighterGray : Color.WHITE;
        }

        // Apply the background color to the convertView
        convertView.setBackgroundColor(backgroundColor);

        // Set the image in the ImageView
        ImageView designImageView = convertView.findViewById(R.id.group_image); // Assuming this ImageView exists in your layout
        designImageView.setImageResource(imageResource);
        designImageView.setLayoutParams(new TableRow.LayoutParams(250, 180)); // Set width and height as required
        designImageView.setRotation(90); // Rotate the image by 90 degrees

        // Set the text for the TextViews
        TextView skuAndSizeTextView = convertView.findViewById(R.id.skuAndSizeTextView);
        skuAndSizeTextView.setText(spannableString);
        TextView quantityTextView = convertView.findViewById(R.id.quantityTextView);
        quantityTextView.setText(spannableQuantity);

        return convertView;
    }

    //500x730
    private int getImageResourceForDesignCode(String designCode) {
        int index = designCode.indexOf('S');
        if (index <= 0) return R.drawable.d; // Default image
        String code = designCode.substring(0, index);
        HashMap<String, Integer> designCodeMap = new HashMap<>();
        designCodeMap.put("220212-2", R.drawable.design_220212_2);designCodeMap.put("220612-3", R.drawable.d_220612_3);
        designCodeMap.put("211225-1", R.drawable.d_211225_1);designCodeMap.put("220628-4", R.drawable.d_220628_4);
        designCodeMap.put("220301-2", R.drawable.d_220301_2);designCodeMap.put("20210221A", R.drawable.d_20210221a);
        designCodeMap.put("20201228A", R.drawable.d_20201228a);designCodeMap.put("220603-5", R.drawable.d_220603_5);
        designCodeMap.put("20210223D", R.drawable.d_20210223d);designCodeMap.put("211120-5", R.drawable.d_211120_5);
        designCodeMap.put("KLB201021", R.drawable.d_klb201021);designCodeMap.put("KLB1900622(CREAM)", R.drawable.d_klb1900622_cream);
        designCodeMap.put("KLB190621(BLUE)", R.drawable.d_klb190621_blue);designCodeMap.put("KLB190621(RED)", R.drawable.d_klb190621_red);
        Integer resourceId = designCodeMap.get(code);
        return resourceId != null ? resourceId : R.drawable.d; // Default image if code is not found
    }
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.child_item, parent, false);
        }
        // Get the unique code for this child
        String uniqueCode = (String) getChild(groupPosition, childPosition);

        // Set the text for the TextView
        TextView uniqueCodeTextView = convertView.findViewById(R.id.uniqueCodeTextView);
        uniqueCodeTextView.setText(uniqueCode);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}

