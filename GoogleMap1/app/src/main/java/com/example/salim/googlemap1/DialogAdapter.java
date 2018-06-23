package com.example.salim.googlemap1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class DialogAdapter extends AppCompatDialogFragment {
    private EditText etlat,etlong;
    private DialogAdapterListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());

        LayoutInflater inflater=getActivity().getLayoutInflater();
        View view=inflater.inflate(R.layout.dialog_add_location,null);

        builder.setView(view)
                    .setTitle("Ajout d'un marqueur")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setPositiveButton("Ajouter", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        String latitude=etlat.getText().toString();
                        String longitude=etlong.getText().toString();
                        listener.addMarker(Double.parseDouble(latitude),Double.parseDouble(longitude));
                        }
                    });

        etlat=view.findViewById(R.id.etlatitude);
        etlong=view.findViewById(R.id.etlongitude);

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener=(DialogAdapterListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+" doit implémenter DialogAdapterListener");
        }
    }

    public interface DialogAdapterListener{
        void addMarker(double latitude, double longitude);
    }
}
