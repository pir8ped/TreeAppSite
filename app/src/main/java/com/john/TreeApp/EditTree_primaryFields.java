package com.john.TreeApp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class EditTree_primaryFields extends Fragment {

    private EditText editLabel;
    private TextView editEnglishName;
    private TextView textLatinName;
    private EditText editRootstock;
    private EditText editVariety;
    private EditText editDescription;

    public EditTree_primaryFields() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment (fragment_edit_primary.xml)
        View view = inflater.inflate(R.layout.fragment_edit_primary, container, false);

        // Initialize UI elements
        editLabel = view.findViewById(R.id.editLabel);
        editEnglishName = view.findViewById(R.id.editEnglishName);
        textLatinName = view.findViewById(R.id.textLatinName);
        editRootstock = view.findViewById(R.id.editRootstock);
        editVariety = view.findViewById(R.id.editVariety);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            editLabel.setText(args.getString("LABEL", ""));
            editEnglishName.setText(args.getString("ENGLISH_NAME", ""));
            textLatinName.setText(args.getString("LATIN_NAME", ""));
            editRootstock.setText(args.getString("ROOTSTOCK", ""));
            editVariety.setText(args.getString("VARIETY", ""));
        }
    }

    public void resetFields() {
        if (editLabel != null) {
            editLabel.setEnabled(true);
            editLabel.setText("");
        }
    }

    private String getTextFromEditText(EditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim(); // trim() to remove leading/trailing whitespace
        } else {
            return ""; // Return empty string if EditText is null or text is null
        }
    }

    public Bundle getPrimaryFieldValues() {
        Bundle fieldValues = new Bundle();
        fieldValues.putString("LABEL", getTextFromEditText(editLabel));
        fieldValues.putString("ROOTSTOCK", getTextFromEditText(editRootstock));
        fieldValues.putString("VARIETY", getTextFromEditText(editVariety));
        return fieldValues;
    }


}
