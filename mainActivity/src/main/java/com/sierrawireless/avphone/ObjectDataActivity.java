package com.sierrawireless.avphone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.model.AvPhoneObjectData;

public class ObjectDataActivity extends Activity implements AdapterView.OnItemSelectedListener {
    TextView title;
    TextView name;
    EditText nameEdit;
    EditText unitEdit;
    EditText defaulEdit;
    Spinner simulationSpin;
    Button saveBtn;
    Button cancelBtn;
    ObjectsManager objectsManager;
    AvPhoneObject object;
    AvPhoneObjectData data;
    int objectPosition;
    int dataPosition;
    boolean add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String[] menuList = {"None", "Increase indefinitely", "Decrease to zero"};


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_data);
        Intent intent = getIntent();
        objectPosition = intent.getIntExtra(ObjectConfigureActivity.OBJECT_POSITION, -1);
        dataPosition = intent.getIntExtra(ObjectConfigureActivity.DATA_POSITION, -1);
        add= intent.getBooleanExtra(ObjectConfigureActivity.ADD, true);

        if (objectPosition == -1 || dataPosition == -1) {
            return;
        }
        title = (TextView)findViewById(R.id.title);
        name = (TextView)findViewById(R.id.name);
        nameEdit = (EditText)findViewById(R.id.nameText);
        unitEdit = (EditText)findViewById(R.id.unitText);
        defaulEdit = (EditText)findViewById(R.id.defaultText);
        simulationSpin = (Spinner)findViewById(R.id.spinner);
        saveBtn = (Button)findViewById(R.id.saveData);
        cancelBtn = (Button)findViewById(R.id.cancelData);

        objectsManager = ObjectsManager.getInstance();
        object = objectsManager.getObjectByIndex(objectPosition);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter =  new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, menuList);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner

        simulationSpin.setAdapter(adapter);
        simulationSpin.setOnItemSelectedListener(this);

        if (add== false) {
            name.setVisibility(View.INVISIBLE);
            nameEdit.setVisibility(View.GONE);
            data = object.datas.get(dataPosition);
            title.setText(data.name);
            unitEdit.setText(data.unit);
            defaulEdit.setText(data.defaults);
            simulationSpin.setSelection(data.modePosition(), false);

        }else{
            title.setText("Add new data");
        }

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                finish();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AvPhoneObjectData.Mode mode = AvPhoneObjectData.modeFromPosition(simulationSpin.getSelectedItemPosition());
                if (add == true) {
                    data = new AvPhoneObjectData(
                            nameEdit.getText().toString(),
                            unitEdit.getText().toString(),
                            defaulEdit.getText().toString(),
                            mode,
                            String.valueOf(objectPosition)
                            );
                    object.datas.add(data);
                }else{
                    data.unit =unitEdit.getText().toString();
                    data.defaults = defaulEdit.getText().toString();
                    data.mode = mode;
                }
                finish();
            }
        });

    }
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

}
